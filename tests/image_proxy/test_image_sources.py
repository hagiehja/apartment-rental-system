import csv
import importlib.util
import os
import tempfile
import sys
import types
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def load_module(name, path):
    if "aiohttp" not in sys.modules:
        aiohttp = types.ModuleType("aiohttp")
        aiohttp.ClientTimeout = lambda *args, **kwargs: None
        aiohttp.ClientSession = lambda *args, **kwargs: None
        web = types.SimpleNamespace(
            Application=lambda *args, **kwargs: types.SimpleNamespace(router=types.SimpleNamespace(add_get=lambda *a, **k: None), on_cleanup=[]),
            Response=lambda *args, **kwargs: None,
            run_app=lambda *args, **kwargs: None,
        )
        aiohttp.web = web
        sys.modules["aiohttp"] = aiohttp
        sys.modules["aiohttp.web"] = web
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ImageSourceTests(unittest.TestCase):
    def setUp(self):
        self.tmpdir = tempfile.TemporaryDirectory()
        os.environ["CACHE_DIR"] = str(Path(self.tmpdir.name) / "cache")
        os.environ["REAL_GALLERY_DIR"] = str(Path(self.tmpdir.name) / "gallery")
        os.environ.pop("ENABLE_PICSUM_LAST_FALLBACK", None)
        os.environ.pop("ENABLE_LEGACY_PICSUM_FALLBACK", None)

    def tearDown(self):
        self.tmpdir.cleanup()

    def test_real_source_plan_never_uses_picsum(self):
        app = load_module("image_proxy_app", "deploy/image-proxy/app.py")

        urls = app.real_source_urls("living", "1002", "800", "600")

        self.assertTrue(urls)
        self.assertTrue(all("picsum.photos" not in url for url in urls))
        self.assertTrue(any("image.pollinations.ai" in url for url in urls))

    def test_legacy_proxy_does_not_use_picsum_by_default(self):
        app = load_module("image_proxy_app", "deploy/image-proxy/app.py")

        self.assertFalse(app.legacy_picsum_enabled())

    def test_generated_house_images_use_real_proxy_without_picsum(self):
        gen = load_module("gen_images_v2", "deploy/datagen/gen_images_v2.py")
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "house_images.csv"

            gen.gen_images(3, out)
            with out.open(encoding="utf-8") as f:
                rows = list(csv.reader(f))

        self.assertEqual(len(rows), 12)
        self.assertTrue(all("/img/real/" in row[1] for row in rows))
        self.assertTrue(all("picsum.photos" not in row[1] for row in rows))
        self.assertEqual(rows[0][1], "/img/real/cover/101/800/600")


if __name__ == "__main__":
    unittest.main()
