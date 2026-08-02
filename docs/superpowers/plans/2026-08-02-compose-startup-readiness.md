# Compose Startup Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让整套系统在同时启动或 Docker 自动恢复时按真实就绪状态启动，并提供一个等待到可登录的一键入口。

**Architecture:** 基础设施通过 Docker healthcheck 暴露就绪状态；Java 镜像入口脚本在启动 JVM 前等待现有 `.env.app` 中的 Nacos、MySQL、Redis 和 RocketMQ 地址；Compose 用 `service_healthy` 串联业务服务、网关、Nginx 与前端；用户脚本负责冷缓存预热和最终验收。

**Tech Stack:** Docker Compose、POSIX shell、Python unittest、Spring Boot Actuator、Nginx、MariaDB/MySQL、Redis Sentinel、Nacos、RocketMQ。

---

### Task 1: Compose 启动契约测试

**Files:**
- Create: `scripts/test_compose_startup_readiness.py`
- Test: `docker-compose.yml`
- Test: `docker-compose.ha.yml`
- Test: `deploy/mysql-ha/docker-compose.yml`
- Test: `deploy/redis-ha/docker-compose.yml`
- Test: `deploy/nacos-cluster/docker-compose.yml`
- Test: `deploy/rocketmq/docker-compose.yml`

- [ ] **Step 1: 写失败测试**

测试调用 `docker compose config --format json`，断言基础设施具有 healthcheck、六个业务服务等待 `nacos1: service_healthy`、网关等待六个业务服务健康、Nginx 等待网关健康、前端等待 Nginx 健康。

- [ ] **Step 2: 运行并确认 RED**

Run: `python3 scripts/test_compose_startup_readiness.py`
Expected: FAIL，指出当前 Nacos/MySQL/Redis/RocketMQ 或 `service_started` 依赖不满足。

- [ ] **Step 3: 暂不修改生产配置**

保留失败证据，进入 Task 2 和 Task 3 后逐项转绿。

### Task 2: Java 启动前依赖等待

**Files:**
- Create: `deploy/scripts/wait-for-dependencies.sh`
- Create: `scripts/test_wait_for_dependencies.sh`
- Modify: `Dockerfile.service`

- [ ] **Step 1: 写失败测试**

测试通过临时 PATH 注入假的 `curl` 和 `nc`，验证等待顺序为 Nacos HTTP、MySQL、Redis、RocketMQ；依赖持续失败时脚本必须在设定次数后非零退出；日志不能打印密码变量。

- [ ] **Step 2: 运行并确认 RED**

Run: `bash scripts/test_wait_for_dependencies.sh`
Expected: FAIL，提示 `deploy/scripts/wait-for-dependencies.sh` 不存在。

- [ ] **Step 3: 实现最小等待脚本**

脚本读取 `NACOS_ADDR`、`MYSQL_HOST`、`MYSQL_PORT`、`REDIS_HOST`、`REDIS_PORT`、`ROCKETMQ_NAMESRV`，用 `curl`/`nc` 有界重试；成功后执行传入命令。地址日志只显示主机与端口，不显示密码。

- [ ] **Step 4: 接入 Java 镜像**

在运行时镜像安装 `netcat-openbsd`，复制脚本到 `/app/wait-for-dependencies.sh`，入口改为等待脚本执行 `java $JAVA_OPTS -jar /app/app.jar`。

- [ ] **Step 5: 运行并确认 GREEN**

Run: `bash scripts/test_wait_for_dependencies.sh`
Expected: PASS，覆盖成功、失败超时与秘密不输出。

- [ ] **Step 6: 提交**

```bash
git add Dockerfile.service deploy/scripts/wait-for-dependencies.sh scripts/test_wait_for_dependencies.sh
git commit -m "fix(startup): wait for application dependencies"
```

### Task 3: 基础设施健康检查与 Compose 分层依赖

**Files:**
- Modify: `deploy/mysql-ha/docker-compose.yml`
- Modify: `deploy/redis-ha/docker-compose.yml`
- Modify: `deploy/nacos-cluster/docker-compose.yml`
- Modify: `deploy/rocketmq/docker-compose.yml`
- Modify: `docker-compose.ha.yml`
- Test: `scripts/test_compose_startup_readiness.py`

- [ ] **Step 1: 为基础设施添加真实 healthcheck**

MySQL 使用本地管理命令，Redis 使用认证 PING，Sentinel 使用 PING，Nacos 使用 readiness HTTP，RocketMQ 使用进程/端口探测。所有 JVM 基础设施配置合理 `start_period`。

- [ ] **Step 2: 修改应用分层依赖**

六个业务服务等待 `nacos1: service_healthy`；网关等待六个业务服务 `service_healthy`；Nginx 等待网关与图片代理健康；前端等待 Nginx 健康。Nginx healthcheck 同时验证自身 `/health` 和网关 actuator。

- [ ] **Step 3: 运行静态契约测试**

Run: `python3 scripts/test_compose_startup_readiness.py`
Expected: PASS。

- [ ] **Step 4: 验证 Compose 合并配置**

Run: `docker compose config --quiet`
Expected: exit 0，无循环依赖或未定义服务。

- [ ] **Step 5: 提交**

```bash
git add docker-compose.ha.yml deploy/mysql-ha/docker-compose.yml deploy/redis-ha/docker-compose.yml deploy/nacos-cluster/docker-compose.yml deploy/rocketmq/docker-compose.yml scripts/test_compose_startup_readiness.py
git commit -m "fix(startup): enforce compose readiness ordering"
```

### Task 4: 用户就绪等待与冷缓存预热

**Files:**
- Create: `scripts/wait-until-ready.sh`
- Create: `scripts/test_wait_until_ready.sh`
- Modify: `README.md`

- [ ] **Step 1: 写失败测试**

使用假的 `curl` 验证脚本会轮询前端、网关、房源列表、房源图片、Prometheus 和 Grafana；前几次失败后可恢复；永久失败时输出组件名称与日志命令并非零退出。

- [ ] **Step 2: 运行并确认 RED**

Run: `bash scripts/test_wait_until_ready.sh`
Expected: FAIL，提示就绪脚本不存在。

- [ ] **Step 3: 实现就绪脚本**

脚本默认访问 `http://127.0.0.1`、前端 5173、Prometheus 9090、Grafana 3000；成功请求房源列表以完成缓存预热，从响应提取首张内部图片路径并验证图片 200。全部成功后输出前端、监控地址。

- [ ] **Step 4: 更新使用说明**

README 明确日常命令为 `docker compose up -d && ./scripts/wait-until-ready.sh`，说明 `docker compose restart` 不作为一键启动入口，但同时重启时 Java 等待脚本仍会保护依赖顺序。

- [ ] **Step 5: 运行并确认 GREEN**

Run: `bash scripts/test_wait_until_ready.sh`
Expected: PASS。

- [ ] **Step 6: 提交**

```bash
git add README.md scripts/wait-until-ready.sh scripts/test_wait_until_ready.sh
git commit -m "feat(startup): wait until the system is usable"
```

### Task 5: 虚拟机部署与重启验收

**Files:**
- Deploy the files changed in Tasks 2-4 to `/home/hyz/apartment-rental-system`

- [ ] **Step 1: 在虚拟机运行全部脚本测试与 Compose 校验**

Run: `python3 scripts/test_compose_startup_readiness.py && bash scripts/test_wait_for_dependencies.sh && bash scripts/test_wait_until_ready.sh && docker compose config --quiet`
Expected: 全部 PASS，Compose exit 0。

- [ ] **Step 2: 构建 Java 服务镜像**

Run: `docker compose build apartment-user-service apartment-house-service apartment-order-service apartment-payment-service apartment-notice-service apartment-contract-service apartment-gateway`
Expected: 所有镜像构建成功。

- [ ] **Step 3: 完整启动验收**

Run: `docker compose up -d && ./scripts/wait-until-ready.sh`
Expected: 脚本最终报告前端、登录依赖、房源、图片、9090、3000 均可用。

- [ ] **Step 4: 同时重启恢复验收**

Run: `docker compose restart && ./scripts/wait-until-ready.sh`
Expected: 中间状态由脚本等待，最终成功；业务容器不会在 Nacos 未就绪时启动 JVM。

- [ ] **Step 5: 最终验证**

检查 `docker compose ps`、网关/业务日志、列表与详情响应、真实图片 `X-Image-Source`、Prometheus 和 Grafana；确认没有修改数据库或删除回滚卷。
