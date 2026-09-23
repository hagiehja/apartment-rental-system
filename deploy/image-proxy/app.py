"""
图片代理服务:真实房源图库 + Pollinations 补充 + 可选 Picsum 最后兜底 + 磁盘缓存
路径:
  /img/real/{slot}/{seed}/{w}/{h}   按 slot 生成真实房源照片(cover/living/bed/kitchen)
  /img/{sub}                        兼容旧路径,默认不再代理 Picsum
"""
import os, hashlib, logging, glob, aiohttp
from aiohttp import web
from urllib.parse import quote

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
log = logging.getLogger("img-proxy")

CACHE_DIR = os.environ.get('CACHE_DIR', '/data/img-cache')
REAL_GALLERY_DIR = os.environ.get('REAL_GALLERY_DIR', '/data/real-gallery')
ENABLE_PICSUM_LAST_FALLBACK = os.environ.get('ENABLE_PICSUM_LAST_FALLBACK', '').lower() in ('1', 'true', 'yes')
ENABLE_LEGACY_PICSUM_FALLBACK = os.environ.get('ENABLE_LEGACY_PICSUM_FALLBACK', '').lower() in ('1', 'true', 'yes')
os.makedirs(CACHE_DIR, exist_ok=True)

_SESSION = None

async def get_session():
    global _SESSION
    if _SESSION is None or _SESSION.closed:
        _SESSION = aiohttp.ClientSession(
            timeout=aiohttp.ClientTimeout(total=45, connect=8),
            headers={'User-Agent': 'Mozilla/5.0 apartment-image-proxy'})
    return _SESSION

# slot -> Pollinations prompt(真实房源照片风格)
SLOT_PROMPTS = {
    "cover":   "modern apartment building exterior real estate photography daylight",
    "living":  "modern bright living room interior with sofa real estate photography",
    "bed":     "cozy bedroom interior with bed real estate photography",
    "kitchen": "modern kitchen interior with cabinets real estate photography",
}

def legacy_picsum_enabled():
    return ENABLE_LEGACY_PICSUM_FALLBACK

def picsum_last_fallback_enabled():
    return ENABLE_PICSUM_LAST_FALLBACK

def normalized_slot(slot):
    aliases = {
        "livingroom": "living",
        "bedroom": "bed",
    }
    slot = aliases.get(slot, slot)
    return slot if slot in SLOT_PROMPTS else "living"

def real_source_urls(slot, seed, w, h):
    slot = normalized_slot(slot)
    prompt = SLOT_PROMPTS[slot]
    urls = [
        f"https://image.pollinations.ai/prompt/{quote(prompt)}?width={w}&height={h}&nologo=true&seed={seed}",
        f"https://image.pollinations.ai/prompt/{quote(prompt)}?width={w}&height={h}&nologo=true&seed={seed}01",
    ]
    if picsum_last_fallback_enabled():
        urls.append(f"https://picsum.photos/seed/house{seed}/{w}/{h}")
    return urls

def gallery_file(slot, seed):
    slot = normalized_slot(slot)
    patterns = [
        os.path.join(REAL_GALLERY_DIR, slot, "*.jpg"),
        os.path.join(REAL_GALLERY_DIR, slot, "*.jpeg"),
        os.path.join(REAL_GALLERY_DIR, slot, "*.png"),
        os.path.join(REAL_GALLERY_DIR, "all", "*.jpg"),
        os.path.join(REAL_GALLERY_DIR, "all", "*.jpeg"),
        os.path.join(REAL_GALLERY_DIR, "all", "*.png"),
    ]
    files = []
    for pattern in patterns:
        files.extend(glob.glob(pattern))
    files = sorted(files)
    if not files:
        return None
    idx = int(hashlib.md5(f"{slot}:{seed}".encode("utf-8")).hexdigest(), 16) % len(files)
    return files[idx]

def cache_file(sub):
    key = hashlib.md5(sub.encode('utf-8')).hexdigest()
    return os.path.join(CACHE_DIR, key[:2], key)

async def fetch(session, url, timeout=45):
    try:
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=timeout)) as resp:
            if resp.status == 200:
                data = await resp.read()
                if len(data) > 2000:
                    return data
                log.warning("too small %s -> %d bytes", url[:80], len(data))
            else:
                log.warning("upstream %s -> %s", url[:80], resp.status)
    except Exception as e:
        log.warning("fetch fail %s: %s", url[:80], e)
    return None

async def serve(sub, fetcher):
    cf = cache_file(sub)
    if os.path.exists(cf):
        try:
            with open(cf, 'rb') as f:
                data = f.read()
            return web.Response(body=data, content_type='image/jpeg',
                headers={'Cache-Control': 'public, max-age=2592000, immutable', 'X-Cache': 'HIT'})
        except Exception:
            pass
    data = await fetcher()
    if not data:
        return web.Response(status=502, text="upstream unavailable")
    try:
        os.makedirs(os.path.dirname(cf), exist_ok=True)
        with open(cf + '.tmp', 'wb') as f:
            f.write(data)
        os.replace(cf + '.tmp', cf)
    except Exception as e:
        log.error("cache write failed: %s", e)
    log.info("MISS %s -> %d bytes", sub, len(data))
    return web.Response(body=data, content_type='image/jpeg',
        headers={'Cache-Control': 'public, max-age=2592000, immutable', 'X-Cache': 'MISS'})

async def real_handler(request):
    slot = normalized_slot(request.match_info['slot'])
    seed = request.match_info['seed']
    w = request.match_info['w']
    h = request.match_info['h']
    sub = f"real/{slot}/{seed}/{w}/{h}"
    session = await get_session()
    async def fetcher():
        gf = gallery_file(slot, seed)
        if gf:
            try:
                with open(gf, "rb") as f:
                    data = f.read()
                if len(data) > 2000:
                    return data
                log.warning("gallery image too small %s -> %d bytes", gf, len(data))
            except Exception as e:
                log.warning("gallery read fail %s: %s", gf, e)
        for url in real_source_urls(slot, seed, w, h):
            data = await fetch(session, url, 45)
            if data:
                return data
        return None
    return await serve(sub, fetcher)

async def proxy_handler(request):
    sub = request.match_info['sub']
    if not legacy_picsum_enabled():
        return web.Response(status=404, text="legacy picsum proxy disabled; use /img/real/{slot}/{seed}/{w}/{h}")
    session = await get_session()
    async def fetcher():
        return await fetch(session, f"https://picsum.photos/{sub}", 20)
    return await serve(sub, fetcher)

async def health_handler(request):
    return web.Response(text="ok")

async def on_cleanup(app):
    global _SESSION
    if _SESSION and not _SESSION.closed:
        await _SESSION.close()

def make_app():
    app = web.Application(client_max_size=1024 * 1024)
    app.router.add_get('/health', health_handler)
    app.router.add_get('/img/real/{slot}/{seed}/{w}/{h}', real_handler)
    app.router.add_get('/img/{sub:.*}', proxy_handler)
    app.on_cleanup.append(on_cleanup)
    return app

if __name__ == '__main__':
    web.run_app(make_app(), host='0.0.0.0', port=8088, access_log=None)
