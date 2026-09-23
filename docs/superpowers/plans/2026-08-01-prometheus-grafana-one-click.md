# Prometheus 与 Grafana 一键启动 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让根目录 `docker compose up -d` 默认启动 Prometheus 9090 与 Grafana 3000，并自动接通业务指标和 Grafana 数据源。

**Architecture:** 根 Compose 继续通过 `include` 合并监控 Compose；Prometheus 与 Grafana 取消 profile，Node Exporter 与 cAdvisor 保持手动 profile。Prometheus 使用 Compose 服务 DNS 采集七个应用，Grafana provisioning 自动指向 Prometheus。

**Tech Stack:** Docker Compose 2.35、Prometheus 2.54.1、Grafana 11.2.0、Python 3 配置回归测试。

---

### Task 1: 添加监控 Compose 回归测试

**Files:**
- Create: `scripts/test_monitoring_compose.py`
- Read: `docker-compose.yml`
- Read: `deploy/monitor/docker-compose.yml`
- Read: `deploy/monitor/prometheus.yml`

- [ ] **Step 1: 写失败测试**

测试通过真实 `docker compose config` 检查默认服务、端口、依赖和挂载，并检查 Prometheus targets 与 Grafana provisioning：

```python
#!/usr/bin/env python3
import json
import os
import subprocess
from pathlib import Path

root = Path(os.environ.get("PROJECT_ROOT", Path(__file__).resolve().parents[1]))


def compose(*args: str) -> str:
    result = subprocess.run(
        ["docker", "compose", "-f", str(root / "docker-compose.yml"), *args],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


default_services = set(compose("config", "--services").splitlines())
assert {"prometheus", "grafana"} <= default_services
assert "node-exporter" not in default_services
assert "cadvisor" not in default_services

config = json.loads(compose("config", "--format", "json"))
services = config["services"]


def published_ports(service: str) -> set[int]:
    return {int(port["published"]) for port in services[service].get("ports", [])}


assert 9090 in published_ports("prometheus")
assert 3000 in published_ports("grafana")
assert services["grafana"]["depends_on"]["prometheus"]["condition"] == "service_healthy"

grafana_volumes = services["grafana"].get("volumes", [])
assert any(volume["target"] == "/etc/grafana/provisioning" for volume in grafana_volumes)

prometheus = (root / "deploy/monitor/prometheus.yml").read_text(encoding="utf-8")
for stale in ("apartment-ha-", "node-exporter:9100", "cadvisor:8080"):
    assert stale not in prometheus
for target in (
    "apartment-gateway:8080",
    "apartment-user-service:8081",
    "apartment-house-service:8083",
    "apartment-order-service:8088",
    "apartment-payment-service:8087",
    "apartment-notice-service:8091",
    "apartment-contract-service:8092",
):
    assert target in prometheus

datasource = root / "deploy/monitor/grafana/provisioning/datasources/prometheus.yml"
text = datasource.read_text(encoding="utf-8")
assert "url: http://prometheus:9090" in text
assert "isDefault: true" in text

print("monitoring compose test passed")
```

- [ ] **Step 2: 在虚拟机运行测试并确认失败**

Run:

```bash
PROJECT_ROOT=/home/hyz/apartment-rental-system python3 /tmp/test_monitoring_compose.py
```

Expected: FAIL，因为默认根 Compose 不包含 `prometheus` 和 `grafana`。

### Task 2: 接入默认监控服务

**Files:**
- Modify: `docker-compose.yml:4-10`
- Modify: `deploy/monitor/docker-compose.yml:5-42`
- Modify: `deploy/monitor/prometheus.yml:7-34`
- Create: `deploy/monitor/grafana/provisioning/datasources/prometheus.yml`

- [ ] **Step 1: 更新根 Compose 使用说明**

说明普通 `docker compose up -d` 会启动业务、Prometheus 和 Grafana，根 include 保持：

```yaml
  - deploy/monitor/docker-compose.yml         # Prometheus + Grafana，默认随一键部署启动
```

- [ ] **Step 2: 让 Prometheus 与 Grafana 默认启动**

删除两者的 `profiles: ["monitor"]`；Grafana 增加 provisioning 只读挂载：

```yaml
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
```

Node Exporter 与 cAdvisor 的 profile 保持不变。

- [ ] **Step 3: 修正 Prometheus targets**

仅保留 `prometheus` 和 `apartment-apps` 两个 job；应用 targets 使用七个 Compose 服务 DNS 名，不再使用 `apartment-ha-*` 容器名。

- [ ] **Step 4: 新增 Grafana 默认数据源**

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

- [ ] **Step 5: 运行回归测试并确认通过**

Run:

```bash
PROJECT_ROOT=/home/hyz/apartment-rental-system python3 /tmp/test_monitoring_compose.py
```

Expected: `monitoring compose test passed`。

- [ ] **Step 6: 提交实现**

```bash
git add docker-compose.yml deploy/monitor/docker-compose.yml deploy/monitor/prometheus.yml deploy/monitor/grafana/provisioning/datasources/prometheus.yml scripts/test_monitoring_compose.py
git commit -m "feat(monitor): start prometheus and grafana by default"
```

### Task 3: 部署并验证真实一键启动

**Files:**
- Deploy: `/home/hyz/apartment-rental-system/docker-compose.yml`
- Deploy: `/home/hyz/apartment-rental-system/deploy/monitor/docker-compose.yml`
- Deploy: `/home/hyz/apartment-rental-system/deploy/monitor/prometheus.yml`
- Deploy: `/home/hyz/apartment-rental-system/deploy/monitor/grafana/provisioning/datasources/prometheus.yml`

- [ ] **Step 1: 备份并同步正式配置**

先保存 `*.bak-codex-monitoring`，再通过临时目录安装新配置；不得覆盖已有业务数据卷。

- [ ] **Step 2: 校验正式 Compose**

Run:

```bash
cd /home/hyz/apartment-rental-system
docker compose config --quiet
PROJECT_ROOT=$PWD python3 /tmp/test_monitoring_compose.py
```

Expected: 两条命令退出码均为 0。

- [ ] **Step 3: 执行真实一键启动**

Run:

```bash
docker compose up -d --no-build
```

Expected: Prometheus 先健康，Grafana 随后启动；业务服务保持或恢复健康。

- [ ] **Step 4: 验证端口和业务**

Run:

```bash
curl -fsS http://127.0.0.1:9090/-/healthy
curl -fsS http://127.0.0.1:3000/api/health
curl -fsS http://127.0.0.1/gateway/health
docker compose ps
```

Expected: 三个 HTTP 请求成功；Prometheus、Grafana、Gateway、Nginx 和业务服务均运行，带健康检查的服务显示 healthy。

- [ ] **Step 5: 验证 Prometheus targets 与 Grafana 数据源**

Run:

```bash
curl -fsS http://127.0.0.1:9090/api/v1/targets
curl -fsS -u admin:admin http://127.0.0.1:3000/api/datasources
```

Expected: Prometheus 返回应用 targets；Grafana 返回名为 `Prometheus`、URL 为 `http://prometheus:9090` 的默认数据源。
