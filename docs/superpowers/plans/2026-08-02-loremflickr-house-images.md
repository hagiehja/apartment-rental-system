# LoremFlickr House Images Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Pollinations-backed `/img/real/...` records with stable, keyword-matched LoremFlickr photography served through a new cached `/img/flickr/...` route.

**Architecture:** Keep the existing image proxy and old routes for rollback, add an isolated Flickr route whose path also busts browser and disk cache, and generate only internal image URLs. Migrate master and slave independently in idempotent `image_id` batches because replication is not active, then clear only house caches and prewarm visible covers at low concurrency.

**Tech Stack:** Python 3.11, aiohttp, unittest, Docker Compose, Nginx, MySQL 5.7, Redis, PowerShell/SSH.

---

### Task 1: Add Flickr proxy behavior with TDD

**Files:**
- Modify: `deploy/image-proxy/test_app.py`
- Modify: `deploy/image-proxy/app.py`

- [ ] **Step 1: Write failing tests for Flickr URL mapping, retry, cache source, and placeholder**

Add tests that assert:

```python
self.assertEqual(
    app.loremflickr_url("living", "38937902", "800", "600"),
    "https://loremflickr.com/800/600/livingroom?lock=38937902",
)
```

Patch `app.fetch` with `side_effect=[None, None, jpeg]`, call the Flickr fetcher, and assert three attempts return `FetchResult(jpeg, "loremflickr", True)`. Add a cache test using sub-path `flickr/cover/38937901/800/600` and assert the second response is `HIT` with `X-Image-Source: loremflickr`. Add a missing-upstream test that returns no result and asserts `200 image/svg+xml`, `X-Image-Source: placeholder`, `X-Cache: BYPASS`, and `Cache-Control: no-store` without creating a cache file.

- [ ] **Step 2: Run tests and prove RED**

Copy `app.py` and `test_app.py` to `/tmp/codex-loremflickr-test` on the VM and run:

```bash
docker run --rm \
  -v /tmp/codex-loremflickr-test/app.py:/app/app.py:ro \
  -v /tmp/codex-loremflickr-test/test_app.py:/app/test_app.py:ro \
  apartment-image-proxy:latest python -m unittest -v test_app.py
```

Expected: new Flickr tests fail because `loremflickr_url`, the Flickr fetcher/route, and placeholder response do not exist.

- [ ] **Step 3: Implement the minimal Flickr route**

Add:

```python
FLICKR_KEYWORDS = {
    "cover": "apartment",
    "living": "livingroom",
    "bed": "bedroom",
    "kitchen": "kitchen",
}

PLACEHOLDER_SVG = b"""<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600"><rect width="800" height="600" fill="#eef1f4"/><path d="M250 390l100-110 75 80 55-55 90 85H250z" fill="#bac3cc"/><circle cx="325" cy="215" r="34" fill="#bac3cc"/></svg>"""

def loremflickr_url(slot, seed, width, height):
    keyword = FLICKR_KEYWORDS.get(slot, FLICKR_KEYWORDS["living"])
    return f"https://loremflickr.com/{width}/{height}/{keyword}?lock={seed}"
```

Refactor `serve(sub, fetcher, cached_source)` so cache HIT reports the route's actual source instead of always reporting Pollinations. If `fetcher()` returns `None`, return `PLACEHOLDER_SVG` with `content_type="image/svg+xml"`, `no-store`, `BYPASS`, and source `placeholder`. Implement a Flickr fetcher that calls `fetch()` at most three times and only returns cacheable `FetchResult` for validated JPEG data. Register:

```python
app.router.add_get("/img/flickr/{slot}/{seed}/{w}/{h}", flickr_handler)
```

Keep `/img/real/...` and `/img/{sub}` behavior unchanged except for passing the correct `cached_source`.

- [ ] **Step 4: Run tests and prove GREEN**

Repeat the VM container test.

Expected: all existing and new image proxy tests pass; HTML responses are still rejected.

- [ ] **Step 5: Commit the proxy change**

```bash
git add deploy/image-proxy/app.py deploy/image-proxy/test_app.py
git commit -m "feat(image): proxy loremflickr house photos"
```

### Task 2: Make generated data use Flickr routes

**Files:**
- Create: `deploy/datagen/test_gen_images_v2.py`
- Modify: `deploy/datagen/gen_images_v2.py`

- [ ] **Step 1: Write failing generator tests**

Test the exact four outputs for house `389379`:

```python
expected = [
    "/img/flickr/cover/38937901/800/600",
    "/img/flickr/living/38937902/800/600",
    "/img/flickr/bed/38937903/800/600",
    "/img/flickr/kitchen/38937904/800/600",
]
```

Generate one house into a temporary CSV and assert four rows, `is_cover=1` only for sort 1, sort values 1-4, and no row contains `picsum`, `pollinations`, or an external `http` URL.

- [ ] **Step 2: Run tests and prove RED**

```bash
python -m unittest -v deploy/datagen/test_gen_images_v2.py
```

Expected: FAIL because the current generator emits external LoremFlickr/Picsum URLs.

- [ ] **Step 3: Implement internal Flickr URL generation**

Use:

```python
IMAGE_SLOTS = [
    (1, "cover"),
    (2, "living"),
    (3, "bed"),
    (4, "kitchen"),
]

def make_url(house_id, sort_order, slot):
    seed = house_id * 100 + sort_order
    return f"/img/flickr/{slot}/{seed}/800/600"
```

Keep CSV columns `[house_id, image_url, is_cover, sort_order]` and progress reporting unchanged.

- [ ] **Step 4: Run generator tests and prove GREEN**

```bash
python -m unittest -v deploy/datagen/test_gen_images_v2.py
```

Expected: PASS.

- [ ] **Step 5: Commit generator changes**

```bash
git add deploy/datagen/gen_images_v2.py deploy/datagen/test_gen_images_v2.py
git commit -m "feat(data): generate flickr house image routes"
```

### Task 3: Add an idempotent master/slave migration tool

**Files:**
- Create: `scripts/migrate_house_images_to_flickr.py`
- Create: `scripts/test_migrate_house_images_to_flickr.py`

- [ ] **Step 1: Write failing migration tests**

Unit-test pure helpers and mocked subprocess calls:

```python
self.assertEqual(
    forward_sql(1, 50000),
    "UPDATE house_image SET image_url=REPLACE(image_url,'/img/real/','/img/flickr/') "
    "WHERE image_id BETWEEN 1 AND 50000 AND image_url LIKE '/img/real/%';",
)
```

Assert rollback reverses only `/img/flickr/%`; dry-run performs count queries but no UPDATE; apply processes both `mysql-ha-master` and `mysql-ha-slave`; verification rejects unequal totals or any remaining `/img/real/%` after a forward migration.

- [ ] **Step 2: Run tests and prove RED**

```bash
python -m unittest -v scripts/test_migrate_house_images_to_flickr.py
```

Expected: FAIL because the migration module does not exist.

- [ ] **Step 3: Implement the migration tool**

Use `subprocess.run([...], input=sql, text=True, check=True, capture_output=True)` with this command so the password stays inside each MySQL container:

```python
[
    "docker", "exec", "-i", container,
    "sh", "-c",
    'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N apartment_db',
]
```

Required CLI:

```text
python3 scripts/migrate_house_images_to_flickr.py              # dry-run
python3 scripts/migrate_house_images_to_flickr.py --apply      # forward
python3 scripts/migrate_house_images_to_flickr.py --rollback   # reverse
```

Use batches of 50,000 `image_id` values. Before any UPDATE, require master/slave totals to match and print counts for `/img/real/%`, `/img/flickr/%`, `/img/seed/%`, external LoremFlickr, and Picsum. Each UPDATE must be prefix-restricted and idempotent. After execution, verify totals are unchanged and both containers have the expected target-prefix count equal to total.

- [ ] **Step 4: Run migration tests and prove GREEN**

```bash
python -m unittest -v scripts/test_migrate_house_images_to_flickr.py
```

Expected: PASS without Docker or database access because subprocess is mocked.

- [ ] **Step 5: Commit migration tooling**

```bash
git add scripts/migrate_house_images_to_flickr.py scripts/test_migrate_house_images_to_flickr.py
git commit -m "feat(data): add reversible flickr image migration"
```

### Task 4: Verify locally before deployment

**Files:**
- Verify all files changed in Tasks 1-3.

- [ ] **Step 1: Run focused tests**

```bash
python -m unittest -v deploy/datagen/test_gen_images_v2.py
python -m unittest -v scripts/test_migrate_house_images_to_flickr.py
```

Run image proxy tests in the VM test container. Expected: all pass.

- [ ] **Step 2: Run repository checks**

```bash
python scripts/test_image_proxy_compose.py
python scripts/test_monitoring_compose.py
mvn -q -s settings.xml test
git diff --check
git status --short
```

Expected: both Compose tests pass, Maven exits 0, no whitespace errors, and only intended changes are present.

- [ ] **Step 3: Review the implementation diff**

Check that no code deletes old volumes/images/routes, no secret is stored, `/img/flickr` never caches placeholder bytes, and migration SQL can only replace the exact expected prefix.

### Task 5: Deploy the proxy without touching business containers

**Files:**
- Deploy: `deploy/image-proxy/app.py`
- Deploy: `deploy/image-proxy/test_app.py`
- Deploy: `deploy/datagen/gen_images_v2.py`
- Deploy: `scripts/migrate_house_images_to_flickr.py`

- [ ] **Step 1: Capture rollback evidence**

On the VM record current image ID, container health, file hashes, database prefix counts, and the existing rollback files. Confirm the old image and both old cache volumes still exist.

- [ ] **Step 2: Sync files and compare hashes**

Copy only the four listed deployment files to `/home/hyz/apartment-rental-system`, then compare SHA-256 between worktree and VM. Expected: exact match.

- [ ] **Step 3: Build and recreate only image-proxy**

```bash
cd /home/hyz/apartment-rental-system
docker compose build image-proxy
docker compose up -d --no-build --force-recreate image-proxy
```

Wait for `healthy`; do not recreate Nginx or business services.

- [ ] **Step 4: Smoke-test Flickr before database migration**

Request one path for each slot with a unique test seed. Expected first response: 200 JPEG, `X-Image-Source: loremflickr`, `MISS`; second response: `HIT`. Stop if any slot returns placeholder after three low-concurrency retries.

### Task 6: Migrate data, clear exact caches, and prewarm

**Files:**
- Execute: `scripts/migrate_house_images_to_flickr.py`

- [ ] **Step 1: Run migration dry-run**

```bash
cd /home/hyz/apartment-rental-system
python3 scripts/migrate_house_images_to_flickr.py
```

Expected: master/slave totals match at 1,600,000, source prefix is `/img/real/%`, and no UPDATE runs.

- [ ] **Step 2: Apply batched migration**

```bash
python3 scripts/migrate_house_images_to_flickr.py --apply
```

Expected on both DB containers: total 1,600,000; Flickr 1,600,000; real/seed/external prefixes 0.

- [ ] **Step 3: Clear only house response caches**

List Redis keys first. Delete only keys matching the verified house-list and house-detail prefixes; do not run `FLUSHDB` or delete unrelated sessions/tokens.

- [ ] **Step 4: Verify API paths**

Request the first list page and a public detail endpoint. Expected: every `coverImage`/`imageUrl` starts with `/img/flickr/`; no `/img/real/` remains in the response.

- [ ] **Step 5: Prewarm at low concurrency**

Extract the first page's visible cover URLs and request them sequentially or with concurrency at most two. Retry placeholders individually. Expected: all visible covers become JPEG cache HITs.

### Task 7: Final verification and review

**Files:**
- Verify deployed system and clean worktree.

- [ ] **Step 1: Verify browser-facing behavior**

Confirm the homepage and house detail page load `/img/flickr/...`, all visible images have natural size 800×600, and repeat loads are cache HITs. Confirm no browser request reaches Pollinations or Picsum.

- [ ] **Step 2: Verify service health**

Check Nginx syntax, image-proxy health, root `docker compose ps`, gateway health, house list HTTP 200 after warm-up, Prometheus 9090, and Grafana 3000.

- [ ] **Step 3: Verify rollback assets remain**

Confirm old image SHA, `apartment-system_image-proxy-cache-v2`, the old anonymous cache volume, and rollback record still exist. Do not delete them.

- [ ] **Step 4: Request code review and fix blockers**

Review the complete diff against the approved spec. Any Important/Critical finding must be reproduced, fixed with a failing test, redeployed, and reverified before completion.

- [ ] **Step 5: Record final evidence**

Report commit SHAs, deployed image SHA, test results, database counts, sample response headers, health checks, and the exact rollback command:

```bash
python3 scripts/migrate_house_images_to_flickr.py --rollback
```
