#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""唯一真实房源图片生成器
图源:本项目 image-proxy 的真实房源图库 + Pollinations 补充
每个房源 4 张固定映射图(封面/客厅/卧室/厨房),通过 house_id + sort 保证稳定差异
用法: python3 gen_images_v2.py <房源数>"""
import csv, sys, os

# 每房源 4 张图:封面(外景)、客厅、卧室、厨房
IMAGE_SLOTS = [
    (1, "cover"),   # 封面:公寓外景
    (2, "living"),  # 客厅
    (3, "bed"),     # 卧室
    (4, "kitchen"), # 厨房
]

def make_url(house_id, sort_order, slot):
    seed = house_id * 100 + sort_order  # 唯一 seed
    return f"/img/real/{slot}/{seed}/800/600"

def gen_images(nhouses, fn):
    print(f"生成 {nhouses*4:,} 行固定映射房源图片(/img/real + 真实图库优先)...", flush=True)
    with open(fn, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        for hid in range(1, nhouses + 1):
            for sort, slot in IMAGE_SLOTS:
                url = make_url(hid, sort, slot)
                is_cover = 1 if sort == 1 else 0
                w.writerow([hid, url, is_cover, sort])
            if hid % 100000 == 0:
                print(f"  {hid:,}/{nhouses:,}", flush=True)
    print("=== house_image CSV 完成 ===", flush=True)

if __name__ == "__main__":
    nh = int(sys.argv[1]) if len(sys.argv) > 1 else 400000
    gen_images(nh, "/tmp/house_images.csv")
    size_mb = os.path.getsize("/tmp/house_images.csv") / 1024 / 1024
    print(f"CSV 大小: {size_mb:.1f} MB", flush=True)
    print(f"唯一 URL 数: {nh * 4:,} (每房源 4 张稳定映射)", flush=True)
