"""
图片代理服务:跟随重定向 + 磁盘缓存
路径:/img/{sub}  例如 /img/seed/house1-1/800/600
"""
import os, hashlib, asyncio, logging
import aiohttp
from aiohttp import web

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
log = logging.getLogger("img-proxy")

CACHE_DIR = os.environ.get('CACHE_DIR', '/data/img-cache')
MAX_SIZE = int(os.environ.get('MAX_CACHE_GB', '2')) * 1024 ** 3
os.makedirs(CACHE_DIR, exist_ok=True)

# 复用 HTTP client(连接池)
_HTTP_TIMEOUT = aiohttp.ClientTimeout(total=15, connect=5)
_SESSION: aiohttp.ClientSession = None

async def get_session():
    global _SESSION
    if _SESSION is None or _SESSION.closed:
        _SESSION = aiohttp.ClientSession(timeout=_HTTP_TIMEOUT, headers={
            'User-Agent': 'Mozilla/5.0 apartment-image-proxy'
        })
    return _SESSION


async def proxy_handler(request):
    sub = request.match_info['sub']
    # sub 形如 seed/house1-1/800/600
    cache_key = hashlib.md5(sub.encode('utf-8')).hexdigest()
    cache_file = os.path.join(CACHE_DIR, cache_key[:2], cache_key)

    # 1) 磁盘缓存命中
    if os.path.exists(cache_file):
        try:
            with open(cache_file, 'rb') as f:
                data = f.read()
            log.info("HIT %s (%d bytes)", sub, len(data))
            return web.Response(
                body=data, content_type='image/jpeg',
                headers={
                    'Cache-Control': 'public, max-age=2592000, immutable',
                    'X-Cache': 'HIT',
                },
            )
        except Exception:
            pass  # 缓存读失败,继续回源

    # 2) 回源(自动跟随重定向)
    url = f"https://picsum.photos/{sub}"
    session = await get_session()
    try:
        async with session.get(url) as resp:
            if resp.status != 200:
                log.warning("upstream %s -> %s", url, resp.status)
                return web.Response(status=502, text=f"upstream status {resp.status}")
            data = await resp.read()
    except Exception as e:
        log.error("fetch %s failed: %s", url, e)
        return web.Response(status=502, text=f"fetch error: {e}")

    # 3) 写入磁盘缓存(原子写)
    try:
        os.makedirs(os.path.dirname(cache_file), exist_ok=True)
        tmp = cache_file + ".tmp"
        with open(tmp, 'wb') as f:
            f.write(data)
        os.replace(tmp, cache_file)
        log.info("MISS %s -> fetched %d bytes, cached", sub, len(data))
    except Exception as e:
        log.error("cache write failed: %s", e)

    return web.Response(
        body=data, content_type='image/jpeg',
        headers={
            'Cache-Control': 'public, max-age=2592000, immutable',
            'X-Cache': 'MISS',
        },
    )


async def health_handler(request):
    return web.Response(text="ok")


async def on_cleanup(app):
    global _SESSION
    if _SESSION and not _SESSION.closed:
        await _SESSION.close()


def make_app():
    app = web.Application(client_max_size=1024 * 1024)
    app.router.add_get('/health', health_handler)
    app.router.add_get('/img/{sub:.*}', proxy_handler)
    app.on_cleanup.append(on_cleanup)
    return app


if __name__ == '__main__':
    web.run_app(make_app(), host='0.0.0.0', port=8088, access_log=None)
