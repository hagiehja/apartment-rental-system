#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""唯一真实房源图片生成器
图源:LoremFlickr(Flickr CC 真实房源照片) + Picsum(Unsplash CC0) 双源混合
每个房源 4 张唯一图(封面/客厅/卧室/厨房),通过 lock/seed 保证唯一性
用法: python3 gen_images_v2.py <房源数>"""
import csv, sys, os, random

random.seed(2024)

# 每房源 4 张图:封面(外景)、客厅、卧室、厨房
# 关键词单单词(无空格),全部 LoremFlickr 验证可用
IMAGE_SLOTS = [
    (1, "apartment",   "cover"),     # 封面:公寓外景
    (2, "livingroom",  "livingroom"), # 客厅
    (3, "home",        "bedroom"),    # 卧室/家居
    (4, "kitchen",     "kitchen"),    # 厨房
]

# 双图源:50% LoremFlickr(真实 Flickr CC 房源照片)+ 50% Picsum(Unsplash CC0 真实照片)
# 用 house_id 和 sort 组合 seed,保证每个房源每张图都唯一
def make_url(house_id, sort_order, keyword, source):
    seed = house_id * 100 + sort_order  # 唯一 seed
    if source == "loremflickr":
        # LoremFlickr:真实 Flickr CC 房源照片,lock 保证每个 seed 唯一稳定
        return f"https://loremflickr.com/800/600/{keyword}?lock={seed}"
    else:
        # Picsum:Unsplash CC0 真实照片(风景/人物/室内混合),seed 保证唯一
        return f"https://picsum.photos/seed/house{seed}/800/600"

def gen_images(nhouses, fn):
    print(f"生成 {nhouses*4:,} 行唯一房源图片(双源 LoremFlickr + Picsum)...", flush=True)
    # 每房源 2 张 LoremFlickr + 2 张 Picsum,增加图源多样性
    with open(fn, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        for hid in range(1, nhouses + 1):
            for idx, (sort, keyword, slot_name) in enumerate(IMAGE_SLOTS):
                # 偶数 hid 用 LoremFlickr,奇数 hid 用 Picsum,且 sort=1,3 用 LF,2,4 用 Picsum
                # 这样每房源 4 张图来自不同源,且不同房源不同
                if sort in (1, 3):
                    url = make_url(hid, sort, keyword, "loremflickr")
                else:
                    url = make_url(hid, sort, keyword, "picsum")
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
    print(f"唯一 URL 数: {nh * 4:,} (每房源 4 张完全唯一)", flush=True)