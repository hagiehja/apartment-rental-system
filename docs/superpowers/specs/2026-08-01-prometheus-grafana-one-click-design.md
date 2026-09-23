# Prometheus 与 Grafana 一键启动设计

## 目标

在虚拟机 `/home/hyz/apartment-rental-system` 中执行一次：

```bash
docker compose up -d
```

即可随业务系统自动启动：

- Prometheus：`http://192.168.24.129:9090`
- Grafana：`http://192.168.24.129:3000`

本次不默认启动 Node Exporter、cAdvisor、自动扩缩容或压测脚本。

## 当前问题

- 虚拟机根 `docker-compose.yml` 注释了监控 Compose include，因此根 Compose 看不到监控服务。
- Prometheus 和 Grafana 带有 `monitor` profile，普通 `docker compose up -d` 不会启动它们。
- Prometheus 使用旧的 `apartment-ha-*` 容器名，当前项目实际为 `apartment-system`。
- Node Exporter 和 cAdvisor 未启动，但 Prometheus 仍配置了对应采集任务，会产生无意义的 DOWN target。
- Grafana 没有自动配置 Prometheus 数据源，首次启动后还需要人工添加。

## 设计

### Compose 集成

- 根 `docker-compose.yml` 正常 include `deploy/monitor/docker-compose.yml`。
- Prometheus、Grafana 删除 profile，成为默认服务。
- Node Exporter 保留 `monitor` profile，cAdvisor 保留 `full` profile，二者不随普通一键启动运行。
- Grafana 保持 `depends_on: prometheus: condition: service_healthy`，只在 Prometheus 健康后启动。
- 继续使用现有持久化卷，重启不会丢失 Prometheus 数据和 Grafana 配置。

### Prometheus 采集

- 保留 Prometheus 自身采集。
- 删除默认启动范围外的 Node Exporter、cAdvisor 采集任务。
- 应用采集目标改用 Compose 服务 DNS 名，避免项目名或容器编号变化导致失效：
  - `apartment-gateway:8080`
  - `apartment-user-service:8081`
  - `apartment-house-service:8083`
  - `apartment-order-service:8088`
  - `apartment-payment-service:8087`
  - `apartment-notice-service:8091`
  - `apartment-contract-service:8092`

### Grafana 数据源

- 新增 Grafana provisioning 文件，自动创建名为 `Prometheus` 的默认数据源。
- Grafana 通过 Compose 网络访问 `http://prometheus:9090`，不依赖宿主机 IP。
- 本次沿用现有 Grafana 管理员账号配置，不新增账号系统。

## 失败处理

- Prometheus 健康检查失败时，Grafana不会提前启动。
- 业务服务尚未健康时，Prometheus 可先启动并显示 target 暂时 DOWN；业务恢复后会自动重新采集。
- 监控启动失败不负责重启业务容器，避免监控系统反向影响业务系统。

## 测试与验收

自动配置测试验证：

- 默认根 Compose 包含 Prometheus、Grafana。
- 默认根 Compose 不包含 Node Exporter、cAdvisor。
- 端口固定为 9090、3000。
- Grafana 健康依赖 Prometheus。
- Prometheus 不再出现 `apartment-ha-*`、Node Exporter、cAdvisor 默认 targets。
- Grafana 默认 Prometheus 数据源存在。

虚拟机冷启动验收：

1. 停止根 Compose 服务。
2. 执行 `docker compose up -d`。
3. 验证业务 Gateway 返回 200。
4. 验证 Prometheus `/-/healthy` 返回 200。
5. 验证 Grafana `/api/health` 返回 200。
6. 验证 Prometheus targets API 能看到应用服务采集状态。

## 不在本次范围

- Node Exporter、cAdvisor 默认启动。
- 自动扩缩容脚本。
- `keep-alive.sh`、`auto-recovery.sh` 修复。
- Grafana 仪表盘设计与告警通知。
