# 图片代理恢复与 Compose 一键启动设计

## 目标

- 恢复 `/img/real/...` 为优先展示 Pollinations 生成的真实房间风格图片。
- Pollinations 暂时限流时允许使用 Picsum 兜底，但兜底图不能污染长期缓存。
- 图片代理纳入根 `docker-compose.yml`，以后执行 `docker compose up -d` 自动构建并启动。
- 迁移时保留当前匿名缓存卷，出现问题可以回退。

## 不在本次范围

- 不批量修改 `house_image` 表中的 160 万条图片 URL。
- 不引入对象存储或房东上传功能。
- 不承诺 Pollinations 图片是真实房东实拍；它们是 AI 生成的房产摄影风格图片。

## 方案选择

采用“修复缓存语义 + 新命名缓存卷 + Compose 正式托管”。只清缓存会在下次 429 时再次缓存 Picsum；批量改数据库 URL 的风险和成本过高。

## Compose 架构

新增 `deploy/image-proxy/docker-compose.yml`：

- 服务名：`image-proxy`。
- 镜像名：`apartment-image-proxy:latest`，从 `deploy/image-proxy/Dockerfile` 构建。
- 容器名继续使用 `apartment-ha-image-proxy-1`，保持 Nginx 现有上游名称兼容。
- 加入根项目默认网络 `apartment-system_default`。
- 使用 `restart: unless-stopped`。
- 使用 `/health` 健康检查。
- 使用 Compose 命名卷 `image-proxy-cache-v2:/data/img-cache`，替代当前匿名卷。

根 `docker-compose.yml` include 图片代理文件。Nginx 的 `depends_on` 增加 `image-proxy: condition: service_healthy`，使一键启动时先确认代理可用。

## 图片请求与缓存流程

1. 请求 `/img/real/{slot}/{seed}/{w}/{h}`。
2. 新命名卷存在该键时，返回缓存并携带：
   - `X-Cache: HIT`
   - `X-Image-Source: pollinations`
   - `Cache-Control: public, max-age=2592000, immutable`
3. 未命中时请求 Pollinations，成功后写入磁盘并返回 `MISS`。
4. Pollinations 两次失败或返回 429 时，请求 Picsum 兜底。
5. Picsum 兜底响应携带：
   - `X-Cache: BYPASS`
   - `X-Image-Source: picsum-fallback`
   - `Cache-Control: no-store`
6. 兜底图不写入磁盘；后续请求会再次尝试 Pollinations。

旧兼容路由 `/img/{sub}` 继续保留，不影响已有旧数据。

## Nginx 行为

删除 `/img/` location 中强制设置的一小时 `expires` 和 `Cache-Control`，让图片代理决定缓存语义。连接、读取超时以及动态 Docker DNS 配置保持不变。

## 迁移与回退

部署前记录旧容器镜像 ID 和匿名卷名称。停止并删除旧容器时不使用 `-v`，因此旧缓存卷仍保留。随后执行：

```bash
docker compose up -d --build image-proxy nginx
```

若验证失败，可停止 Compose 图片代理，用原镜像 ID 和旧匿名卷重新创建原容器。

## 测试与验收

- 单元测试：Pollinations 成功图可缓存；Picsum 回退图不写缓存且返回 `no-store`。
- Compose 回归测试：默认服务包含 `image-proxy`；使用命名卷；配置健康检查；Nginx 依赖图片代理健康。
- `docker compose config --quiet` 通过。
- 使用未请求过的新 seed 验证：第一次 `MISS + pollinations`，第二次 `HIT + pollinations`。
- 重启图片代理后同一 seed 仍为 `HIT`，证明命名卷持久化。
- 根目录 `docker compose up -d` 后图片代理、Nginx、业务网关、Prometheus、Grafana均保持健康。
- 浏览器执行一次 `Ctrl+Shift+R` 后展示新图片。

## 风险控制

- 上游再次限流时页面可能临时显示 Picsum，但不会形成长期污染。
- 不删除旧匿名卷，降低迁移风险。
- 不触碰业务表，避免大批量数据库写入。
