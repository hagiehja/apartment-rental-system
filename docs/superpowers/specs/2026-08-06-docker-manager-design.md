# 公寓租赁系统 Docker 容器管理脚本设计

## 目标

新增 `/home/hyz/apartment-rental-system/docker_manager.sh`，为 Ubuntu 22.04 提供中文、彩色、交互式 Docker 容器管理面板。脚本只管理当前公寓租赁系统 Compose 项目的 25 个真实服务，不包含 InsightOS 或示例容器名称。

## 文件与部署

- 仓库脚本：`docker_manager.sh`
- 自动测试：`scripts/test_docker_manager.sh`
- 虚拟机部署路径：`/home/hyz/apartment-rental-system/docker_manager.sh`
- Compose 根目录：`/home/hyz/apartment-rental-system`
- 所有服务属于 `main` 部署栈，由根 `docker-compose.yml` 的 `include` 统一管理。

## 顶部配置

脚本顶部集中定义以下配置，业务函数中不散落容器名称：

```bash
CONTAINERS=(
  "MySQL主库|mysql-ha-master|mysql-ha-master|main"
  "MySQL从库|mysql-ha-slave|mysql-ha-slave|main"
  "Redis主库|redis-master|redis-master|main"
  "Redis从库|redis-slave|redis-slave|main"
  "Redis哨兵1|redis-sentinel-1|redis-sentinel-1|main"
  "Redis哨兵2|redis-sentinel-2|redis-sentinel-2|main"
  "Redis哨兵3|redis-sentinel-3|redis-sentinel-3|main"
  "Nacos节点1|nacos1|nacos1|main"
  "Nacos节点2|nacos2|nacos2|main"
  "Nacos节点3|nacos3|nacos3|main"
  "RocketMQ名称服务|rocketmq-namesrv|namesrv|main"
  "RocketMQ代理|rocketmq-broker|broker|main"
  "RocketMQ控制台|rocketmq-dashboard|dashboard|main"
  "用户服务|apartment-system-apartment-user-service-1|apartment-user-service|main"
  "房源服务|apartment-system-apartment-house-service-1|apartment-house-service|main"
  "订单服务|apartment-system-apartment-order-service-1|apartment-order-service|main"
  "支付服务|apartment-system-apartment-payment-service-1|apartment-payment-service|main"
  "通知服务|apartment-system-apartment-notice-service-1|apartment-notice-service|main"
  "合同服务|apartment-system-apartment-contract-service-1|apartment-contract-service|main"
  "API网关|apartment-system-apartment-gateway-1|apartment-gateway|main"
  "图片代理|apartment-ha-image-proxy-1|image-proxy|main"
  "Nginx入口|apartment-system-nginx-1|nginx|main"
  "前端|apartment-frontend|frontend|main"
  "Prometheus|prometheus|prometheus|main"
  "Grafana|grafana|grafana|main"
)

declare -A STACK_DIRS=(
  ["main"]="/home/hyz/apartment-rental-system"
)

declare -A SPECIAL_START_HOOKS=()
```

`SPECIAL_START_HOOKS` 以真实容器名称为键，以启动前需要执行的 Bash 命令为值。当前 25 个服务均不需要特殊钩子，因此默认留空。普通容器绝不执行 `xhost` 或修改 `DISPLAY`。

## 状态模型

每次显示菜单前对全部配置项执行只读检查：

- Docker 中不存在容器：`未创建`
- `.State.Running=true` 且没有异常健康状态：`运行中`
- 容器存在且正常退出或未运行：`已停止`
- `dead`、`restarting`、`paused` 或健康状态为 `unhealthy`：`状态异常`

表格固定显示：`编号 | 服务名称 | 容器名称 | 当前状态`。未创建项仍保留在表格中，保证用户能通过 Compose 创建它。

## 命令发现与 Compose 规则

启动时依次检查：

1. `docker` 命令是否存在。
2. Docker daemon 是否可访问。
3. 当前用户是否有 Docker 权限；权限不足时给出加入 `docker` 组的提示。
4. 优先使用 `docker compose`，不可用时回退 `docker-compose`。
5. 对每个部署栈检查目录、Compose 文件和服务名称。

Compose 文件允许 `docker-compose.yml`、`docker-compose.yaml`、`compose.yml` 或 `compose.yaml`。执行 Compose 命令前进入 `STACK_DIRS` 指定目录。使用 `config --services` 确认服务真实存在，不靠字符串猜测。

## 单容器操作

选择编号后进入子菜单：

1. 启动：运行中则提示；已创建未运行时执行 `docker start`；未创建时执行启动前钩子并用 Compose `up -d 服务名称`；两者都不可用时输出具体原因。
2. 停止：仅在运行中执行 `docker stop`，否则提示未运行或未创建。
3. 重启：容器存在时优先 `docker restart`；未创建但 Compose 服务存在时执行 `up -d`。
4. 最近日志：执行 `docker logs --tail 100`。
5. 实时日志：提示 `Ctrl+C`，执行 `docker logs -f --tail 100`；中断日志后返回菜单，不终止脚本。
6. 进入终端：要求容器运行；先尝试 `bash`，失败后尝试 `sh`。
7. 删除：显示 `确认删除容器 xxx？(y/N)`；仅执行 `docker rm`，绝不添加 `-v`，不调用 `compose down`。
8. 返回上一级。

每个操作都捕获失败并输出中文错误，失败只结束本次操作，不退出主循环。

## 批量操作

主菜单提供：

- `a`：按 `CONTAINERS` 配置顺序启动全部服务，单项失败后继续处理其余项。
- `s`：停止全部已运行容器，单项失败后继续。
- `r`：重启全部；存在容器使用 `docker restart`，未创建项使用 Compose 创建。
- `0`：退出。

配置顺序即批量启动顺序：MySQL、Redis、Nacos、RocketMQ、六个业务服务、网关、图片代理、Nginx、前端、监控。Compose 自身的 `depends_on` 继续负责依赖健康门控。

## 输入与信号处理

- 编号必须是十进制正整数且在 1 到 25 范围内。
- 主菜单只接受编号、`a`、`s`、`r`、`0`。
- 删除确认只接受 `y` 或 `Y`。
- 实时日志允许 `Ctrl+C` 返回子菜单；脚本主循环不因该信号退出。
- 非交互终端或 `NO_COLOR` 环境下禁用颜色控制码，便于测试和日志采集。

## 测试与验收

`scripts/test_docker_manager.sh` 通过临时目录中的假 `docker` 和 `docker-compose` 命令测试真实脚本函数，不访问生产容器。至少覆盖：

- Docker/daemon/权限检查的错误信息。
- 新版与旧版 Compose 命令发现。
- 四种状态映射。
- 运行中启动不重复执行。
- 已停止容器使用 `docker start`。
- 未创建容器使用正确栈目录和 Compose 服务。
- 重启、停止、最近日志、实时日志、终端回退和删除二次确认。
- 单项失败不会终止批量处理。
- 配置中恰好包含 25 个真实服务且无示例名称。

最终验收命令：

```bash
bash -n docker_manager.sh
bash scripts/test_docker_manager.sh
chmod +x docker_manager.sh
./docker_manager.sh
docker compose config --services
docker compose ps -a
```

真实环境验收只读取状态和打开/退出菜单，不自动执行停止、删除或重启操作。
