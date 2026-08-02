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
