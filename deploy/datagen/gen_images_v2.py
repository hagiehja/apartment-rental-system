#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成由 image-proxy 回源 LoremFlickr 的稳定房源图片路径。"""
import csv
import os
import sys


IMAGE_SLOTS = [
    (1, "cover"),
    (2, "living"),
    (3, "bed"),
    (4, "kitchen"),
]


def make_url(house_id, sort_order, slot):
    seed = house_id * 100 + sort_order
    return f"/img/flickr/{slot}/{seed}/800/600"


def gen_images(nhouses, filename):
    print(f"生成 {nhouses * 4:,} 行 LoremFlickr 房源图片路径...", flush=True)
    with open(filename, "w", encoding="utf-8", newline="") as csv_file:
        writer = csv.writer(csv_file)
        for house_id in range(1, nhouses + 1):
            for sort_order, slot in IMAGE_SLOTS:
                image_url = make_url(house_id, sort_order, slot)
                is_cover = 1 if sort_order == 1 else 0
                writer.writerow([house_id, image_url, is_cover, sort_order])
            if house_id % 100000 == 0:
                print(f"  {house_id:,}/{nhouses:,}", flush=True)
    print("=== house_image CSV 完成 ===", flush=True)


if __name__ == "__main__":
    house_count = int(sys.argv[1]) if len(sys.argv) > 1 else 400000
    output = "/tmp/house_images.csv"
    gen_images(house_count, output)
    size_mb = os.path.getsize(output) / 1024 / 1024
    print(f"CSV 大小: {size_mb:.1f} MB", flush=True)
    print(f"唯一 URL 数: {house_count * 4:,}", flush=True)
