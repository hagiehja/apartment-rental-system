"""
图片代理服务:真实房源照片(Pollinations 按场景生成)+ picsum 兜底 + 磁盘缓存
路径:
  /img/real/{slot}/{seed}/{w}/{h}   按 slot 生成真实房源照片(cover/living/bed/kitchen)
  /img/{sub}                        兼容旧路径 -> picsum
"""
import os, hashlib, logging, aiohttp
from aiohttp import web
from urllib.parse import quote

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
log = logging.getLogger("img-proxy")

CACHE_DIR = os.environ.get('CACHE_DIR', '/data/img-cache')
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
    slot = request.match_info['slot']
    seed = request.match_info['seed']
    w = request.match_info['w']
    h = request.match_info['h']
    sub = f"real/{slot}/{seed}/{w}/{h}"
    prompt = SLOT_PROMPTS.get(slot, SLOT_PROMPTS["living"])
    session = await get_session()
    async def fetcher():
        for _ in range(2):  # Pollinations 重试2次
            url = f"https://image.pollinations.ai/prompt/{quote(prompt)}?width={w}&height={h}&nologo=true&seed={seed}"
            data = await fetch(session, url, 45)
            if data:
                return data
        url = f"https://picsum.photos/seed/house{seed}/{w}/{h}"  # 兜底
        return await fetch(session, url, 15)
    return await serve(sub, fetcher)

async def proxy_handler(request):
    sub = request.match_info['sub']
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
