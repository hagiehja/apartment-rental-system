# Image Proxy Recovery and Compose Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore Pollinations-first room-style images without permanently caching Picsum fallbacks, and make the image proxy start automatically with root `docker compose up -d`.

**Architecture:** The image proxy owns source selection and cache headers, while Nginx forwards those headers without overriding them. A dedicated included Compose file builds the proxy, attaches it to the project default network, and persists only verified Pollinations images in a named volume; the old anonymous cache volume remains available for rollback.

**Tech Stack:** Python 3.11, aiohttp 3.10.5, unittest, Docker Compose include, Nginx 1.27 Alpine, PowerShell/SSH deployment.

---

## File map

- Modify `deploy/image-proxy/app.py`: return source/cache metadata and prevent fallback cache poisoning.
- Create `deploy/image-proxy/test_app.py`: isolated async tests for cacheable and bypass responses.
- Create `deploy/image-proxy/docker-compose.yml`: image-proxy service, health check, default network, named cache volume.
- Modify `docker-compose.yml`: include image-proxy in root one-click startup and update usage comment.
- Modify `docker-compose.ha.yml`: make Nginx wait for image-proxy health.
- Modify `deploy/nginx/nginx.conf`: preserve cache headers emitted by the image proxy.
- Create `scripts/test_image_proxy_compose.py`: execute real `docker compose config` and validate integration.

### Task 1: Create an isolated implementation branch

**Files:**
- No file changes.

- [ ] **Step 1: Verify the current worktree is clean**

Run:

```powershell
git status --short
git branch --show-current
```

Expected: no status output; current branch is `codex/prometheus-grafana-one-click`.

- [ ] **Step 2: Create the feature branch from the approved design and plan commits**

Run:

```powershell
git switch -c codex/image-proxy-compose-recovery
```

Expected: `Switched to a new branch 'codex/image-proxy-compose-recovery'`.

### Task 2: Specify cache behavior with failing unit tests

**Files:**
- Create: `deploy/image-proxy/test_app.py`
- Test: `deploy/image-proxy/test_app.py`

- [ ] **Step 1: Add tests for Pollinations caching and fallback bypass**

Create `deploy/image-proxy/test_app.py`:

```python
import os
import tempfile
import unittest
from unittest.mock import patch

import app


class ServeTests(unittest.IsolatedAsyncioTestCase):
    async def test_cacheable_pollinations_response_is_written_and_reused(self):
        payload = b"x" * 3000
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(app, "CACHE_DIR", cache_dir):
            calls = 0

            async def fetcher():
                nonlocal calls
                calls += 1
                return app.FetchResult(payload, "pollinations", True)

            first = await app.serve("real/living/101/800/600", fetcher)
            second = await app.serve("real/living/101/800/600", fetcher)

            self.assertEqual(first.headers["X-Cache"], "MISS")
            self.assertEqual(second.headers["X-Cache"], "HIT")
            self.assertEqual(second.headers["X-Image-Source"], "pollinations")
            self.assertIn("immutable", second.headers["Cache-Control"])
            self.assertEqual(calls, 1)
            self.assertTrue(os.path.exists(app.cache_file("real/living/101/800/600")))

    async def test_fallback_response_is_not_written(self):
        payload = b"y" * 3000
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(app, "CACHE_DIR", cache_dir):
            async def fetcher():
                return app.FetchResult(payload, "picsum-fallback", False)

            response = await app.serve("real/cover/202/800/600", fetcher)

            self.assertEqual(response.headers["X-Cache"], "BYPASS")
            self.assertEqual(response.headers["X-Image-Source"], "picsum-fallback")
            self.assertEqual(response.headers["Cache-Control"], "no-store")
            self.assertFalse(os.path.exists(app.cache_file("real/cover/202/800/600")))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests in the existing image and verify RED**

Run from a Linux/WSL shell at the repository root:

```bash
docker build -t apartment-image-proxy:test ./deploy/image-proxy
docker run --rm -v "$(pwd)/deploy/image-proxy:/app:ro" -w /app apartment-image-proxy:test python -m unittest -v test_app
```

Expected: FAIL because `app.FetchResult` does not exist.

### Task 3: Implement source-aware cache semantics

**Files:**
- Modify: `deploy/image-proxy/app.py:1-119`
- Test: `deploy/image-proxy/test_app.py`

- [ ] **Step 1: Add the result type and response header helpers**

Add after imports:

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class FetchResult:
    data: bytes
    source: str
    cacheable: bool


LONG_CACHE = "public, max-age=2592000, immutable"
```

- [ ] **Step 2: Make `serve` cache only verified results**

Replace `serve` with:

```python
async def serve(sub, fetcher):
    cf = cache_file(sub)
    if os.path.exists(cf):
        try:
            with open(cf, "rb") as f:
                data = f.read()
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
            headers={"Cache-Control": "no-store", "X-Cache": "BYPASS"},
        )

    if result.cacheable:
        try:
            os.makedirs(os.path.dirname(cf), exist_ok=True)
            with open(cf + ".tmp", "wb") as f:
                f.write(result.data)
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
```

- [ ] **Step 3: Return cache metadata from the two handlers**

In `real_handler`, return verified Pollinations data as cacheable and fallback data as non-cacheable:

```python
    async def fetcher():
        for _ in range(2):
            url = f"https://image.pollinations.ai/prompt/{quote(prompt)}?width={w}&height={h}&nologo=true&seed={seed}"
            data = await fetch(session, url, 45)
            if data:
                return FetchResult(data, "pollinations", True)
        url = f"https://picsum.photos/seed/house{seed}/{w}/{h}"
        data = await fetch(session, url, 15)
        return FetchResult(data, "picsum-fallback", False) if data else None
```

Keep the old compatibility route available but non-cacheable, so the named volume contains only verified Pollinations images:

```python
    async def fetcher():
        data = await fetch(session, f"https://picsum.photos/{sub}", 20)
        return FetchResult(data, "picsum", False) if data else None
```

- [ ] **Step 4: Run the unit tests and verify GREEN**

Run:

```bash
docker build -t apartment-image-proxy:test ./deploy/image-proxy
docker run --rm -v "$(pwd)/deploy/image-proxy:/app:ro" -w /app apartment-image-proxy:test python -m unittest -v test_app
```

Expected: two tests pass.

- [ ] **Step 5: Commit the cache fix**

```powershell
git add deploy/image-proxy/app.py deploy/image-proxy/test_app.py
git commit -m "fix(image): avoid caching fallback pictures"
```

### Task 4: Specify root Compose integration with a failing test

**Files:**
- Create: `scripts/test_image_proxy_compose.py`
- Test: `scripts/test_image_proxy_compose.py`

- [ ] **Step 1: Add a real Compose configuration test**

Create `scripts/test_image_proxy_compose.py`:

```python
import json
import os
import subprocess
from pathlib import Path


root = Path(os.environ.get("APARTMENT_PROJECT_ROOT", Path(__file__).resolve().parents[1]))
compose_file = root / "docker-compose.yml"


def compose(*args):
    result = subprocess.run(
        ["docker", "compose", "-f", str(compose_file), *args],
        cwd=root,
        check=True,
        text=True,
        capture_output=True,
    )
    return result.stdout


services = set(compose("config", "--services").splitlines())
assert "image-proxy" in services

config = json.loads(compose("config", "--format", "json"))
proxy = config["services"]["image-proxy"]
assert proxy["image"] == "apartment-image-proxy:latest"
assert proxy["restart"] == "unless-stopped"
assert proxy["healthcheck"]
assert any(volume["target"] == "/data/img-cache" for volume in proxy["volumes"])
assert config["services"]["nginx"]["depends_on"]["image-proxy"]["condition"] == "service_healthy"

print("image proxy compose test passed")
```

- [ ] **Step 2: Run against the current formal VM configuration and verify RED**

Copy the test to the VM, then run:

```bash
APARTMENT_PROJECT_ROOT=/home/hyz/apartment-rental-system python3 scripts/test_image_proxy_compose.py
```

Expected: FAIL because `image-proxy` is absent from the root Compose services.

### Task 5: Add the image proxy to one-click Compose startup

**Files:**
- Create: `deploy/image-proxy/docker-compose.yml`
- Modify: `docker-compose.yml:1-24`
- Modify: `docker-compose.ha.yml:39-53`
- Test: `scripts/test_image_proxy_compose.py`

- [ ] **Step 1: Create the included Compose service**

Create `deploy/image-proxy/docker-compose.yml`:

```yaml
name: apartment-image-proxy

services:
  image-proxy:
    build:
      context: .
      dockerfile: Dockerfile
    image: apartment-image-proxy:latest
    container_name: apartment-ha-image-proxy-1
    restart: unless-stopped
    expose:
      - "8088"
    volumes:
      - image-proxy-cache-v2:/data/img-cache
    networks:
      - default
    healthcheck:
      test: ["CMD", "wget", "-q", "-O", "-", "http://127.0.0.1:8088/health"]
      interval: 10s
      timeout: 3s
      retries: 6

volumes:
  image-proxy-cache-v2:
```

- [ ] **Step 2: Include the service from the root Compose file**

Add before `docker-compose.ha.yml`:

```yaml
  - deploy/image-proxy/docker-compose.yml     # 图片代理，默认随一键部署启动
```

Update the usage comment so `docker compose up -d` explicitly lists image-proxy with Prometheus and Grafana.

- [ ] **Step 3: Make Nginx wait for the proxy health check**

Replace the Nginx dependency list with:

```yaml
    depends_on:
      apartment-gateway:
        condition: service_started
      image-proxy:
        condition: service_healthy
```

- [ ] **Step 4: Run `docker compose config` in a test environment**

Run on the formal VM after copying only the candidate files into a temporary mirrored directory, or use the formal `.env.app` without printing it:

```bash
docker compose config --quiet
APARTMENT_PROJECT_ROOT=/path/to/candidate python3 scripts/test_image_proxy_compose.py
```

Expected: config exits 0 and the test prints `image proxy compose test passed`.

- [ ] **Step 5: Commit Compose integration**

```powershell
git add docker-compose.yml docker-compose.ha.yml deploy/image-proxy/docker-compose.yml scripts/test_image_proxy_compose.py
git commit -m "feat(image): start proxy with root compose"
```

### Task 6: Preserve proxy-owned cache headers through Nginx

**Files:**
- Modify: `deploy/nginx/nginx.conf:49-60`
- Test: `deploy/image-proxy/test_app.py`

- [ ] **Step 1: Remove Nginx's forced cache policy**

Delete these lines from `location /img/`:

```nginx
expires 1h;
add_header Cache-Control "public, max-age=3600";
```

Do not change the proxy timeouts, dynamic upstream variable, or request headers.

- [ ] **Step 2: Validate Nginx syntax with the exact configuration**

Run:

```bash
docker run --rm -v "$(pwd)/deploy/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" nginx:1.27-alpine nginx -t
```

Expected: `syntax is ok` and `test is successful`.

- [ ] **Step 3: Commit Nginx cache forwarding**

```powershell
git add deploy/nginx/nginx.conf
git commit -m "fix(image): preserve proxy cache headers"
```

### Task 7: Deploy with rollback evidence and preserve the old volume

**Files deployed:**
- `docker-compose.yml`
- `docker-compose.ha.yml`
- `deploy/image-proxy/app.py`
- `deploy/image-proxy/Dockerfile`
- `deploy/image-proxy/docker-compose.yml`
- `deploy/nginx/nginx.conf`
- `scripts/test_image_proxy_compose.py`

- [ ] **Step 1: Back up formal configuration files on the VM**

Create timestamped copies alongside the formal files. Verify each source path exists before copying; do not overwrite earlier backups.

- [ ] **Step 2: Record rollback evidence for the manual container**

Run:

```bash
docker inspect apartment-ha-image-proxy-1 --format 'image={{.Image}} mounts={{json .Mounts}} networks={{json .NetworkSettings.Networks}}' \
  > /home/hyz/apartment-rental-system/image-proxy-rollback.txt
```

Expected: the file records image ID `sha256:eb39...` and the old anonymous volume name.

- [ ] **Step 3: Copy candidate files and validate before changing the container**

Run:

```bash
cd /home/hyz/apartment-rental-system
docker compose config --quiet
python3 scripts/test_image_proxy_compose.py
```

Expected: both commands exit 0.

- [ ] **Step 4: Build the new image before removing the old container**

Run:

```bash
docker compose build image-proxy
docker image inspect apartment-image-proxy:latest --format '{{.Id}}'
```

Expected: build succeeds and prints a new image ID.

- [ ] **Step 5: Replace only the old manual container**

Run exact-target checks, then:

```bash
docker stop apartment-ha-image-proxy-1
docker rm apartment-ha-image-proxy-1
docker compose up -d --no-build image-proxy nginx
```

Do not pass `-v`; the old anonymous cache volume must remain.

- [ ] **Step 6: Verify container ownership and health**

Run:

```bash
docker compose ps image-proxy nginx
docker inspect apartment-ha-image-proxy-1 --format 'project={{index .Config.Labels "com.docker.compose.project"}} service={{index .Config.Labels "com.docker.compose.service"}} status={{.State.Health.Status}}'
```

Expected: project `apartment-system`, service `image-proxy`, health `healthy`.

### Task 8: Verify image restoration and one-click startup

**Files:**
- No new files.

- [ ] **Step 1: Verify a fresh seed is sourced from Pollinations**

Use a seed not present in the cache:

```bash
curl -sS -D - -o /dev/null http://127.0.0.1/img/real/living/2026080201/800/600
```

Expected first response: `200`, `X-Cache: MISS`, `X-Image-Source: pollinations`.

- [ ] **Step 2: Verify the same seed is now cached**

Repeat the command.

Expected: `X-Cache: HIT`, `X-Image-Source: pollinations`.

- [ ] **Step 3: Verify named-volume persistence across restart**

Run:

```bash
docker compose restart image-proxy
curl -sS -D - -o /dev/null http://127.0.0.1/img/real/living/2026080201/800/600
```

Expected after health recovery: `X-Cache: HIT`.

- [ ] **Step 4: Verify the root one-click command is idempotent**

Run:

```bash
docker compose up -d --no-build --pull never
docker compose ps
```

Expected: image-proxy, Nginx, business services, Prometheus, and Grafana remain running; no manual `docker run` is required.

- [ ] **Step 5: Run final health checks**

Run:

```bash
curl -fsS http://127.0.0.1/gateway/health
curl -fsS http://127.0.0.1:9090/-/healthy
curl -fsS http://127.0.0.1:3000/api/health
docker logs --tail 50 apartment-ha-image-proxy-1
```

Expected: all HTTP checks succeed; image log shows Pollinations `MISS` then cache `HIT` without storing a fallback.

- [ ] **Step 6: Run repository and runtime verification**

```powershell
git status --short
git diff --check
```

Run the image proxy unit tests, Compose integration test, and Nginx syntax test once more. Expected: all pass and the worktree is clean after commits.

### Task 9: Review and branch handoff

**Files:**
- No new files.

- [ ] **Step 1: Request an independent code review**

Review all commits from the branch base through `HEAD`, focusing on fallback cache poisoning, Compose network/volume semantics, Nginx dependencies, and rollback safety.

- [ ] **Step 2: Address Critical or Important findings**

Add a failing regression test before any code correction, rerun the focused suite, and commit the correction separately.

- [ ] **Step 3: Offer branch integration options**

After verification passes, offer local merge, push/PR, keep branch, or discard. Never remove the preserved VM cache volume during branch cleanup.
