import os
import tempfile
import unittest
from unittest.mock import patch

import app


class FakeResponse:
    def __init__(self, status, content_type, payload):
        self.status = status
        self.content_type = content_type
        self.payload = payload

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, traceback):
        return False

    async def read(self):
        return self.payload


class FakeSession:
    def __init__(self, response):
        self.response = response

    def get(self, url, timeout):
        return self.response


class FetchTests(unittest.IsolatedAsyncioTestCase):
    async def test_fetch_rejects_large_html_error_page(self):
        payload = b"<html>upstream error</html>" + b"x" * 3000
        session = FakeSession(FakeResponse(200, "text/html", payload))

        result = await app.fetch(session, "https://example.invalid/image")

        self.assertIsNone(result)

    async def test_fetch_accepts_jpeg_response(self):
        payload = b"\xff\xd8\xff" + b"x" * 3000
        session = FakeSession(FakeResponse(200, "image/jpeg", payload))

        result = await app.fetch(session, "https://example.invalid/image")

        self.assertEqual(result, payload)


class FlickrTests(unittest.IsolatedAsyncioTestCase):
    def test_loremflickr_url_maps_each_slot_to_a_room_keyword(self):
        expected = {
            "cover": "apartment",
            "living": "livingroom",
            "bed": "bedroom",
            "kitchen": "kitchen",
        }

        for slot, keyword in expected.items():
            with self.subTest(slot=slot):
                self.assertEqual(
                    app.loremflickr_url(slot, "38937901", "800", "600"),
                    f"https://loremflickr.com/800/600/{keyword}?lock=38937901",
                )

    def test_invalid_flickr_route_parameters_are_rejected(self):
        invalid = [
            ("garage", "38937901", "800", "600"),
            ("cover", "not-a-number", "800", "600"),
            ("cover", "0", "800", "600"),
            ("cover", "2147483648", "800", "600"),
            ("cover", "38937901", "1600", "1200"),
        ]
        for params in invalid:
            with self.subTest(params=params), self.assertRaises(app.web.HTTPNotFound):
                app.validate_flickr_params(*params)

    async def test_loremflickr_retries_twice_before_success(self):
        payload = b"\xff\xd8\xff" + b"x" * 3000

        with patch.object(app, "fetch", side_effect=[None, None, payload]) as fetch_mock:
            result = await app.fetch_loremflickr(
                object(), "living", "38937902", "800", "600"
            )

        self.assertEqual(result, app.FetchResult(payload, "loremflickr", True))
        self.assertEqual(fetch_mock.await_count, 3)


class ServeTests(unittest.IsolatedAsyncioTestCase):
    async def test_cacheable_pollinations_response_is_written_and_reused(self):
        payload = b"\xff\xd8\xff" + b"x" * 3000
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(app, "CACHE_DIR", cache_dir):
            calls = 0

            async def fetcher():
                nonlocal calls
                calls += 1
                return app.FetchResult(payload, "pollinations", True)

            first = await app.serve(
                "real/living/101/800/600", fetcher, "pollinations"
            )
            second = await app.serve(
                "real/living/101/800/600", fetcher, "pollinations"
            )

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

            response = await app.serve(
                "real/cover/202/800/600", fetcher, "pollinations"
            )

            self.assertEqual(response.headers["X-Cache"], "BYPASS")
            self.assertEqual(response.headers["X-Image-Source"], "picsum-fallback")
            self.assertEqual(response.headers["Cache-Control"], "no-store")
            self.assertFalse(os.path.exists(app.cache_file("real/cover/202/800/600")))

    async def test_loremflickr_cache_hit_reports_flickr_source(self):
        payload = b"\xff\xd8\xff" + b"z" * 3000
        sub = "flickr/cover/38937901/800/600"
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(
            app, "CACHE_DIR", cache_dir
        ):
            calls = 0

            async def fetcher():
                nonlocal calls
                calls += 1
                return app.FetchResult(payload, "loremflickr", True)

            first = await app.serve(sub, fetcher, "loremflickr")
            second = await app.serve(sub, fetcher, "loremflickr")

            self.assertEqual(first.headers["X-Cache"], "MISS")
            self.assertEqual(second.headers["X-Cache"], "HIT")
            self.assertEqual(second.headers["X-Image-Source"], "loremflickr")
            self.assertEqual(calls, 1)

    async def test_invalid_cached_bytes_are_refetched(self):
        sub = "flickr/bed/38937903/800/600"
        valid = b"\xff\xd8\xff" + b"v" * 3000
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(
            app, "CACHE_DIR", cache_dir
        ):
            cache_path = app.cache_file(sub)
            os.makedirs(os.path.dirname(cache_path), exist_ok=True)
            with open(cache_path, "wb") as cache_file:
                cache_file.write(b"not-a-jpeg" * 400)
            with open(cache_path + ".source", "w", encoding="ascii") as source_file:
                source_file.write("loremflickr")
            calls = 0

            async def fetcher():
                nonlocal calls
                calls += 1
                return app.FetchResult(valid, "loremflickr", True)

            response = await app.serve(sub, fetcher, "loremflickr")
            self.assertEqual(response.headers["X-Cache"], "MISS")
            self.assertEqual(calls, 1)
            with open(cache_path, "rb") as cache_file:
                self.assertEqual(cache_file.read(), valid)

    async def test_cache_without_matching_source_metadata_is_refetched(self):
        sub = "flickr/living/38937902/800/600"
        valid = b"\xff\xd8\xff" + b"n" * 3000
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(
            app, "CACHE_DIR", cache_dir
        ):
            cache_path = app.cache_file(sub)
            os.makedirs(os.path.dirname(cache_path), exist_ok=True)
            with open(cache_path, "wb") as cache_file:
                cache_file.write(valid)

            async def fetcher():
                return app.FetchResult(valid, "loremflickr", True)

            response = await app.serve(sub, fetcher, "loremflickr")
            self.assertEqual(response.headers["X-Cache"], "MISS")
            with open(cache_path + ".source", encoding="ascii") as source_file:
                self.assertEqual(source_file.read(), "loremflickr")

    async def test_missing_loremflickr_returns_uncached_placeholder(self):
        sub = "flickr/kitchen/38937904/800/600"
        with tempfile.TemporaryDirectory() as cache_dir, patch.object(
            app, "CACHE_DIR", cache_dir
        ):
            async def fetcher():
                return None

            response = await app.serve(sub, fetcher, "loremflickr")

            self.assertEqual(response.status, 200)
            self.assertEqual(response.content_type, "image/svg+xml")
            self.assertEqual(response.headers["X-Cache"], "BYPASS")
            self.assertEqual(response.headers["X-Image-Source"], "placeholder")
            self.assertEqual(response.headers["Cache-Control"], "no-store")
            self.assertFalse(os.path.exists(app.cache_file(sub)))


if __name__ == "__main__":
    unittest.main()
