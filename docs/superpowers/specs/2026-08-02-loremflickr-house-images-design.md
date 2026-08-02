# LoremFlickr 房源图片恢复设计

## 目标

将当前由 Pollinations 生成的房屋风格图片替换为 LoremFlickr 按场景关键词返回的真实 Flickr 照片。每套房源继续保持四张稳定图片：公寓外观、客厅、卧室和厨房；图片代理仍由根 Docker Compose 一键启动并使用磁盘缓存。

## 已确认现状

- 当前主库和从库的 `house_image` 各有 1,600,000 行，全部使用 `/img/real/...` 路径。
- `/img/real/...` 当前由 Pollinations 回源，已有缓存会继续返回 AI 图片。
- 浏览器对这些 URL 使用 30 天 `immutable` 缓存，因此仅替换后端实现不能让客户端立即换图。
- Git 历史中的 `gen_images_v2.py` 使用固定公式 `house_id * 100 + sort_order` 生成 seed，并曾同时使用 LoremFlickr 与 Picsum。
- LoremFlickr 当前从 Windows 和 VM 请求均可返回 HTTP 200 JPEG。
- MySQL 主从复制未连通，数据变更必须分别在 master 和 slave 执行并验证。

## 路由与场景映射

新增路由：

```text
/img/flickr/{slot}/{seed}/{width}/{height}
```

场景到 LoremFlickr 关键词的映射：

| slot | 关键词 | 用途 |
| --- | --- | --- |
| `cover` | `apartment` | 公寓或住宅外观 |
| `living` | `livingroom` | 客厅 |
| `bed` | `bedroom` | 卧室 |
| `kitchen` | `kitchen` | 厨房 |

上游 URL 使用 `lock={seed}`，让同一房源、同一场景稳定对应同一张照片。旧 `/img/real/...` 路由继续保留，仅用于兼容和回滚，不再写入新数据。

## 请求与缓存流程

1. 浏览器请求新的 `/img/flickr/...` URL；新路径天然绕过旧浏览器缓存。
2. 图片代理按完整内部路径计算缓存键。由于路径前缀不同，不会命中旧 Pollinations 缓存。
3. 缓存命中时直接返回 JPEG，并携带 `X-Image-Source: loremflickr`、`X-Cache: HIT`。
4. 缓存未命中时请求 LoremFlickr，最多重试三次，并校验 HTTP 状态、JPEG Content-Type、JPEG 文件头和最小长度。
5. 仅校验通过的 LoremFlickr JPEG 写入命名卷；成功响应使用长期缓存。
6. 三次请求都失败时返回本地中性占位图，使用 `Cache-Control: no-store`，不使用 Pollinations 或 Picsum，也不污染图片缓存。

缓存文件继续使用无扩展名的哈希文件。文件数量必须按普通文件统计，不能用 `.jpg`/`.png` 扩展名判断。

## 数据生成脚本

`deploy/datagen/gen_images_v2.py` 改为为四个 slot 全部生成内部 `/img/flickr/...` 路径，不再把 LoremFlickr、Picsum 外部 URL 直接写入数据库。seed 仍使用 `house_id * 100 + sort_order`，保持历史稳定性。

## 现有 160 万行迁移

迁移前分别记录 master、slave 的总数和各路径前缀数量，并确认磁盘余量。迁移只转换满足 `/img/real/%` 的记录：

```text
/img/real/cover/38937901/800/600
→ /img/flickr/cover/38937901/800/600
```

按 `image_id` 范围分批更新，避免一次更新 160 万行造成长事务和长时间锁表。master 和 slave 分别执行相同批次；每端完成后必须满足：

- 总行数仍为 1,600,000；
- `/img/flickr/%` 为 1,600,000；
- `/img/real/%`、`/img/seed/%` 和外部 LoremFlickr/Picsum URL 均为 0；
- 抽样四个 sort 的 slot 与 seed 计算一致。

迁移是可逆的：回滚时将 `/img/flickr/` 原样替换回 `/img/real/`。旧路由、旧缓存卷和旧镜像均保留，不执行删除。

## 缓存与页面刷新

数据库路径前缀改变后，API 会返回全新的图片 URL，因此无需让用户手动清理浏览器缓存。迁移完成后清除房源列表和详情相关 Redis 缓存，再以低并发方式预热首页可见封面，避免 LoremFlickr 冷请求同时打满 VM 外网连接。

## 错误处理与回滚

- LoremFlickr 超时或返回非 JPEG：重试，最终返回不缓存的本地占位图。
- 批次更新失败：停止后续批次，根据 `image_id` 和路径前缀统计定位已完成范围；修正后重跑具有幂等性。
- 页面异常：先将已修改的 `/img/flickr/` 路径批量替换回 `/img/real/`，无需重建旧图片代理。
- 新图片代理异常：Compose 重建旧镜像，命名卷和旧匿名卷继续保留。

## 测试与验收

- 单元测试覆盖 slot 到关键词映射、JPEG 校验、重试、成功缓存、占位图不缓存、旧路由兼容。
- Compose 测试确认 image-proxy 仍由根 Compose 启动、命名卷挂载、健康检查和 Nginx 依赖不变。
- 部署后验证四个 slot 均返回 800×600 JPEG，首次为 MISS、再次为 HIT，来源为 LoremFlickr。
- 验证首页和详情页使用 `/img/flickr/...`，不再请求 `/img/real/...`、Pollinations 或 Picsum。
- 验证网关、房源列表、Prometheus、Grafana、Nginx 和全部 Compose 健康状态未回归。

## 非目标

- 本次不新增房东上传图片功能或对象存储。
- 不声称 LoremFlickr 图片是对应房东真实上传的房源照片；它们是按场景关键词匹配的真实摄影图片。
- 不删除旧图片、旧缓存卷、旧镜像或回滚记录。
