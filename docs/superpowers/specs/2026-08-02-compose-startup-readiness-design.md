# Docker Compose 启动就绪设计

## 目标

解决整套系统启动后登录、网关和房源列表短时间返回 502/503/504 的问题。用户执行一键启动后，必须能明确知道系统何时真正可用，不能把“容器已启动”误认为“业务已就绪”。

## 已确认根因

1. MySQL、Redis 和 Nacos 缺少足够的健康检查。
2. Java 服务与 Nacos、数据库同时启动，Nacos 客户端可能在注册中心仍处于 STARTING 时注册失败。
3. 网关当前只等待业务容器启动，没有等待业务服务健康及完成 Nacos 注册。
4. Nginx 当前只等待网关容器启动，可能把请求转发给尚未可用的网关。
5. 房源列表第一次查询需要加载数据库数据并建立缓存，冷启动请求可能明显慢于后续请求。
6. `docker compose restart` 不会重新执行完整的依赖创建顺序，因此不能把 `depends_on` 作为唯一保障。

## 选定方案

采用四层就绪控制，并保留现有服务架构：

1. 基础设施健康检查。
2. Java 容器启动前主动等待依赖。
3. 网关和 Nginx 按业务健康状态分层启动。
4. 提供面向用户的一键等待脚本，在所有关键接口通过后才报告系统可用。

不通过单纯增加 Nginx 超时掩盖依赖未就绪问题。

## 基础设施层

### MySQL

- master 与 slave 使用数据库自带的管理命令检查本地连接。
- slave 只有自身能够接受查询后才标记健康。
- Nacos 至少等待 MySQL master 健康。

### Redis 与 Sentinel

- Redis master/slave 使用认证后的 `PING` 检查。
- Sentinel 使用 `PING` 和 master 发现结果检查。
- Java 服务启动等待其实际使用的 Redis/Sentinel 端口可连接。

### Nacos

- 三个节点使用 readiness HTTP 接口检查。
- 业务服务至少等待主入口 Nacos 节点 readiness 成功。
- 健康检查包含足够的 `start_period`，避免 JVM 初始化阶段被误杀。

### RocketMQ

- nameserver 和 broker 增加端口级健康检查。
- 依赖消息队列的服务在 Java 启动前等待 nameserver 可连接。

## 应用层

新增 `deploy/scripts/wait-for-dependencies.sh`，并复制进 Java 服务镜像。脚本从现有 `.env.app` 变量读取地址，不写死密码或业务地址。

启动顺序：

1. 等待 Nacos readiness。
2. 等待 MySQL master、slave TCP 端口。
3. 等待 Redis/Sentinel TCP 端口。
4. 等待 RocketMQ nameserver TCP 端口。
5. 依赖全部成功后执行 Java 进程。

等待过程必须有超时、明确日志和非零退出码。失败后由现有 `restart: unless-stopped` 重新尝试，不能启动一个无法注册的半健康 Java 进程。

## 网关、Nginx 与前端

- 网关的 `depends_on` 改为等待六个业务服务 `service_healthy`。
- Nginx 等待网关和图片代理 `service_healthy`。
- 前端等待 Nginx `service_healthy`。
- Nginx 保留动态 Docker DNS 解析，避免容器重建后缓存旧 IP。
- 不把 Nginx 超时无限放大；冷查询通过预热解决。

## 用户启动入口

新增 `scripts/wait-until-ready.sh`：

```bash
docker compose up -d
./scripts/wait-until-ready.sh
```

脚本轮询并验证：前端、网关健康、登录路由可达、房源列表、已缓存房源图片、Prometheus 9090、Grafana 3000。房源列表首次成功同时完成冷缓存预热。只有全部成功才输出可访问地址；超时则输出失败组件和对应日志查看命令。

明确规定：日常启动使用 `docker compose up -d`，不再使用 `docker compose restart` 作为整套系统启动命令。即使 Docker 守护进程同时恢复全部容器，应用容器内部等待脚本仍会阻止 Java 过早启动。

## 测试与验收

1. 静态测试验证所有关键服务具有健康检查和正确的 `service_healthy` 依赖。
2. Shell 测试验证等待脚本成功、超时、依赖失败和状态输出。
3. `docker compose config` 必须通过。
4. 在虚拟机执行一次完整 `docker compose down` 后 `up -d`，等待脚本必须成功。
5. 再执行一次整套容器同时 restart，确认等待脚本最终成功且日志中没有业务服务永久注册失败。
6. 最终验证登录、房源列表、详情、图片、9090、3000 均为成功状态。

## 安全与回滚

- 脚本只读取环境变量，不打印密码、令牌或完整连接串。
- 不修改数据库数据和现有数据卷。
- 回滚只需恢复 Compose、Dockerfile 和等待脚本相关提交，然后重新构建 Java 服务镜像。
