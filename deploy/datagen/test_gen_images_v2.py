import csv
import importlib.util
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("gen_images_v2.py")
SPEC = importlib.util.spec_from_file_location("gen_images_v2", MODULE_PATH)
gen_images_v2 = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(gen_images_v2)


class ImageUrlTests(unittest.TestCase):
    def test_each_slot_uses_an_internal_flickr_route(self):
        actual = [
            gen_images_v2.make_url(389379, sort_order, slot)
            for sort_order, slot in gen_images_v2.IMAGE_SLOTS
        ]

        self.assertEqual(
            actual,
            [
                "/img/flickr/cover/38937901/800/600",
                "/img/flickr/living/38937902/800/600",
                "/img/flickr/bed/38937903/800/600",
                "/img/flickr/kitchen/38937904/800/600",
            ],
        )

    def test_generated_csv_has_four_internal_images_and_one_cover(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "house_images.csv"

            gen_images_v2.gen_images(1, output)

            with output.open(encoding="utf-8", newline="") as csv_file:
                rows = list(csv.reader(csv_file))

        self.assertEqual(len(rows), 4)
        self.assertEqual([row[2] for row in rows], ["1", "0", "0", "0"])
        self.assertEqual([row[3] for row in rows], ["1", "2", "3", "4"])
        for row in rows:
            self.assertTrue(row[1].startswith("/img/flickr/"))
            self.assertNotIn("http", row[1])
            self.assertNotIn("picsum", row[1])
            self.assertNotIn("pollinations", row[1])


if __name__ == "__main__":
    unittest.main()
