"""
图片代理服务：Pollinations 按场景生成房产摄影风格图片，Picsum 仅临时兜底。

路径：
  /img/real/{slot}/{seed}/{w}/{h}  按场景生成图片（cover/living/bed/kitchen）
  /img/{sub}                       兼容旧 Picsum 路径
"""
import hashlib
import logging
import os
from dataclasses import dataclass
from urllib.parse import quote

import aiohttp
from aiohttp import web

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("img-proxy")


@dataclass(frozen=True)
class FetchResult:
    data: bytes
    source: str
    cacheable: bool


LONG_CACHE = "public, max-age=2592000, immutable"
CACHE_DIR = os.environ.get("CACHE_DIR", "/data/img-cache")
os.makedirs(CACHE_DIR, exist_ok=True)

_SESSION = None


async def get_session():
    global _SESSION
    if _SESSION is None or _SESSION.closed:
        _SESSION = aiohttp.ClientSession(
            timeout=aiohttp.ClientTimeout(total=45, connect=8),
            headers={"User-Agent": "Mozilla/5.0 apartment-image-proxy"},
        )
    return _SESSION


SLOT_PROMPTS = {
    "cover": "modern apartment building exterior real estate photography daylight",
    "living": "modern bright living room interior with sofa real estate photography",
    "bed": "cozy bedroom interior with bed real estate photography",
    "kitchen": "modern kitchen interior with cabinets real estate photography",
}


def cache_file(sub):
    key = hashlib.md5(sub.encode("utf-8")).hexdigest()
    return os.path.join(CACHE_DIR, key[:2], key)


async def fetch(session, url, timeout=45):
    try:
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=timeout)) as resp:
            if resp.status == 200:
                data = await resp.read()
                if (
                    len(data) > 2000
                    and resp.content_type.lower() in {"image/jpeg", "image/jpg"}
                    and data[:3] == bytes((0xFF, 0xD8, 0xFF))
                ):
                    return data
                log.warning(
                    "invalid image %s type=%s bytes=%d",
                    url[:80],
                    resp.content_type,
                    len(data),
                )
            else:
                log.warning("upstream %s -> %s", url[:80], resp.status)
    except Exception as exc:
        log.warning("fetch fail %s: %s", url[:80], exc)
    return None


async def serve(sub, fetcher):
    cf = cache_file(sub)
    if os.path.exists(cf):
        try:
            with open(cf, "rb") as file:
                data = file.read()
            return web.Response(
                body=data,
                content_type="image/jpeg",
                headers={
                    "Cache-Control": LONG_CACHE,
                    "X-Cache": "HIT",
                    "X-Image-Source": "pollinations",
                },
            )
        except Exception:
            pass

    result = await fetcher()
    if not result:
        return web.Response(
            status=502,
            text="upstream unavailable",
            headers={
                "Cache-Control": "no-store",
                "X-Cache": "BYPASS",
                "X-Image-Source": "unavailable",
            },
        )

    if result.cacheable:
        try:
            os.makedirs(os.path.dirname(cf), exist_ok=True)
            with open(cf + ".tmp", "wb") as file:
                file.write(result.data)
            os.replace(cf + ".tmp", cf)
        except Exception as exc:
            log.error("cache write failed: %s", exc)

    cache_status = "MISS" if result.cacheable else "BYPASS"
    cache_control = LONG_CACHE if result.cacheable else "no-store"
    log.info("%s %s source=%s -> %d bytes", cache_status, sub, result.source, len(result.data))
    return web.Response(
        body=result.data,
        content_type="image/jpeg",
        headers={
            "Cache-Control": cache_control,
            "X-Cache": cache_status,
            "X-Image-Source": result.source,
        },
    )


async def real_handler(request):
    slot = request.match_info["slot"]
    seed = request.match_info["seed"]
    width = request.match_info["w"]
    height = request.match_info["h"]
    sub = f"real/{slot}/{seed}/{width}/{height}"
    prompt = SLOT_PROMPTS.get(slot, SLOT_PROMPTS["living"])
    session = await get_session()

    async def fetcher():
        for _ in range(2):
            url = (
                f"https://image.pollinations.ai/prompt/{quote(prompt)}"
                f"?width={width}&height={height}&nologo=true&seed={seed}"
            )
            data = await fetch(session, url, 45)
            if data:
                return FetchResult(data, "pollinations", True)
        url = f"https://picsum.photos/seed/house{seed}/{width}/{height}"
        data = await fetch(session, url, 15)
        return FetchResult(data, "picsum-fallback", False) if data else None

    return await serve(sub, fetcher)


async def proxy_handler(request):
    sub = request.match_info["sub"]
    session = await get_session()

    async def fetcher():
        data = await fetch(session, f"https://picsum.photos/{sub}", 20)
        return FetchResult(data, "picsum", False) if data else None

    return await serve(sub, fetcher)


async def health_handler(request):
    return web.Response(text="ok")


async def on_cleanup(app):
    global _SESSION
    if _SESSION and not _SESSION.closed:
        await _SESSION.close()


def make_app():
    app = web.Application(client_max_size=1024 * 1024)
    app.router.add_get("/health", health_handler)
    app.router.add_get("/img/real/{slot}/{seed}/{w}/{h}", real_handler)
    app.router.add_get("/img/{sub:.*}", proxy_handler)
    app.on_cleanup.append(on_cleanup)
    return app


if __name__ == "__main__":
    web.run_app(make_app(), host="0.0.0.0", port=8088, access_log=None)
