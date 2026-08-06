# Docker Container Manager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and deploy a Chinese interactive Bash panel that safely manages all 25 real apartment-system Docker Compose services.

**Architecture:** Keep every mutable environment value in arrays at the top of `docker_manager.sh`. Source-safe functions handle validation, state mapping, Compose discovery, single-container actions, and batch actions; the interactive main loop only dispatches validated input. A standalone Bash test injects fake Docker commands through `PATH` and verifies behavior without touching production containers.

**Tech Stack:** Bash 4+, Docker CLI, Docker Compose v2 or docker-compose v1, Ubuntu 22.04, fake-command shell tests.

---

### Task 1: Add failing contract tests

**Files:**
- Create: `scripts/test_docker_manager.sh`
- Test: `docker_manager.sh`

- [ ] **Step 1: Create the test harness before the production script exists**

The test creates fake `docker` and `docker-compose` executables, records calls in `EVENTS_FILE`, sources `docker_manager.sh`, and invokes functions directly:

```bash
#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/docker_manager.sh"
[[ -f "$SCRIPT" ]] || { echo "missing $SCRIPT" >&2; exit 1; }
source "$SCRIPT"
[[ ${#CONTAINERS[@]} -eq 25 ]]
[[ ${STACK_DIRS[main]} == /home/hyz/apartment-rental-system ]]
assert_eq "运行中" "$(container_status apartment-frontend)"
start_container "前端|apartment-frontend|frontend|main"
grep -q '^docker:start apartment-frontend$' "$EVENTS_FILE"
```

The complete test must cover Docker preflight errors, Compose v2/v1 selection, four display states, start/stop/restart, two log modes, bash-to-sh fallback, delete confirmation, special-hook execution, and failure-tolerant batch processing.

- [ ] **Step 2: Run the test in Ubuntu and verify RED**

Run:

```bash
bash -n scripts/test_docker_manager.sh
bash scripts/test_docker_manager.sh
```

Expected: syntax succeeds, then the test fails with `missing docker_manager.sh`.

- [ ] **Step 3: Commit only the failing test**

```bash
git add scripts/test_docker_manager.sh
git commit -m "test: define Docker manager behavior"
```

### Task 2: Implement the complete manager

**Files:**
- Create: `docker_manager.sh`
- Modify: `scripts/test_docker_manager.sh`

- [ ] **Step 1: Add the exact 25-service configuration**

Use four pipe-separated fields and one real stack directory:

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
declare -A STACK_DIRS=([main]="/home/hyz/apartment-rental-system")
declare -A SPECIAL_START_HOOKS=()
```

- [ ] **Step 2: Add source-safe preflight and Compose discovery**

Implement these functions without exiting the parent shell:

```bash
check_docker_environment
detect_compose_command
find_compose_file STACK_DIR
compose_service_exists SERVICE STACK
run_compose STACK up -d SERVICE
```

`check_docker_environment` distinguishes missing CLI, stopped daemon, and permission denied. `detect_compose_command` prefers `docker compose`, then `docker-compose`. `run_compose` executes inside the configured stack directory and always returns its command status.

- [ ] **Step 3: Add state and single-container operations**

Implement exact status labels and safe command choices:

```bash
container_exists NAME
container_is_running NAME
container_status NAME
start_container ENTRY
stop_container ENTRY
restart_container ENTRY
show_recent_logs ENTRY
follow_logs ENTRY
enter_container ENTRY
delete_container ENTRY
```

`delete_container` must call only `docker rm NAME` after a literal `y` or `Y`; it must never contain `docker rm -v`, `docker volume rm`, or `docker compose down`.

- [ ] **Step 4: Add batch operations and menus**

Implement:

```bash
start_all_containers
stop_all_containers
restart_all_containers
print_container_table
container_action_menu ENTRY
main_menu
```

Batch functions continue after errors and return a failure summary. `main_menu` accepts only `1..25`, `a`, `s`, `r`, and `0`. The file ends with a source guard:

```bash
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
```

- [ ] **Step 5: Run RED-to-GREEN tests**

Run:

```bash
bash -n docker_manager.sh
bash -n scripts/test_docker_manager.sh
bash scripts/test_docker_manager.sh
```

Expected: all commands exit 0 and the test prints `docker_manager tests passed`.

- [ ] **Step 6: Commit implementation**

```bash
git add docker_manager.sh scripts/test_docker_manager.sh
git update-index --chmod=+x docker_manager.sh scripts/test_docker_manager.sh
git commit -m "feat: add interactive Docker container manager"
```

### Task 3: Validate and deploy on Ubuntu

**Files:**
- Deploy: `/home/hyz/apartment-rental-system/docker_manager.sh`
- Deploy: `/home/hyz/apartment-rental-system/scripts/test_docker_manager.sh`

- [ ] **Step 1: Copy to a temporary VM candidate directory**

```bash
mkdir -p /tmp/apartment-docker-manager-candidate/scripts
scp docker_manager.sh hyz@192.168.24.129:/tmp/apartment-docker-manager-candidate/
scp scripts/test_docker_manager.sh hyz@192.168.24.129:/tmp/apartment-docker-manager-candidate/scripts/
```

- [ ] **Step 2: Run syntax, mock, and static safety checks**

```bash
bash -n /tmp/apartment-docker-manager-candidate/docker_manager.sh
bash /tmp/apartment-docker-manager-candidate/scripts/test_docker_manager.sh
grep -F 'InsightOS' /tmp/apartment-docker-manager-candidate/docker_manager.sh && exit 1 || true
grep -E 'docker (volume rm|rm .*-v)|docker compose down' /tmp/apartment-docker-manager-candidate/docker_manager.sh && exit 1 || true
```

Expected: syntax and tests pass, forbidden-pattern checks produce no match.

- [ ] **Step 3: Confirm all configured services match live Compose**

Use `docker compose config --services` and `docker compose ps -a` to compare all 25 service/container pairs. Do not start, stop, restart, or delete containers during this check.

- [ ] **Step 4: Deploy and run a read-only menu smoke test**

```bash
install -m 0755 /tmp/apartment-docker-manager-candidate/docker_manager.sh /home/hyz/apartment-rental-system/docker_manager.sh
cd /home/hyz/apartment-rental-system
printf '0\n' | NO_COLOR=1 ./docker_manager.sh
```

Expected: the table contains 25 rows with real states and exits cleanly after input `0`.

- [ ] **Step 5: Run final verification**

```bash
bash -n docker_manager.sh
bash scripts/test_docker_manager.sh
git diff --check
git status --short
```

Expected: no syntax errors, test pass, no whitespace errors, and no unintended untracked artifacts.
