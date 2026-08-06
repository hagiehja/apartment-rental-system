#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/docker_manager.sh"
[[ -f "$SCRIPT" ]] || { echo "missing $SCRIPT" >&2; exit 1; }

TEST_TMP="$(mktemp -d)"
trap 'rm -rf "$TEST_TMP"' EXIT
FAKE_BIN="$TEST_TMP/bin"
STACK_DIR="$TEST_TMP/stack"
EVENTS_FILE="$TEST_TMP/events"
STATE_FILE="$TEST_TMP/states"
HOOK_FILE="$TEST_TMP/hooks"
mkdir -p "$FAKE_BIN" "$STACK_DIR"
: > "$EVENTS_FILE"
: > "$STATE_FILE"
: > "$HOOK_FILE"
touch "$STACK_DIR/docker-compose.yml"

export EVENTS_FILE STATE_FILE
export FAKE_DOCKER_INFO=ok
export FAKE_COMPOSE_V2=1
export FAKE_COMPOSE_V1=1
export FAKE_COMPOSE_SERVICES=$'frontend\nimage-proxy\nnginx'
export FAKE_FAIL_ACTION=""
export FAKE_BASH_EXEC_FAIL=0

cat > "$FAKE_BIN/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
set -u
record() { printf 'docker:%s\n' "$*" >> "$EVENTS_FILE"; }
state_line() { grep -F -m1 "$1|" "$STATE_FILE" 2>/dev/null || true; }

case "${1:-}" in
  info)
    case "${FAKE_DOCKER_INFO:-ok}" in
      ok) exit 0 ;;
      permission) echo 'permission denied while trying to connect to the Docker daemon socket' >&2; exit 1 ;;
      *) echo 'Cannot connect to the Docker daemon' >&2; exit 1 ;;
    esac
    ;;
  compose)
    shift
    case "${1:-}" in
      version) [[ "${FAKE_COMPOSE_V2:-1}" == 1 ]] ;;
      config) printf '%s\n' "${FAKE_COMPOSE_SERVICES:-}" ;;
      *) record "compose $*"; [[ "${FAKE_FAIL_ACTION:-}" != "compose:$*" ]] ;;
    esac
    ;;
  inspect)
    name="${@: -1}"
    line="$(state_line "$name")"
    [[ -n "$line" ]] || exit 1
    if [[ "$*" == *--format* ]]; then
      IFS='|' read -r _ status health <<< "$line"
      printf '%s|%s\n' "$status" "${health:-none}"
    fi
    ;;
  start|stop|restart|rm)
    action="$1"; name="${2:-}"
    record "$action $name"
    [[ "${FAKE_FAIL_ACTION:-}" != "$action:$name" ]]
    ;;
  logs)
    shift
    record "logs $*"
    ;;
  exec)
    shift
    record "exec $*"
    if [[ "$*" == *' bash' && "${FAKE_BASH_EXEC_FAIL:-0}" == 1 ]]; then exit 1; fi
    ;;
  *) record "$*" ;;
esac
FAKE_DOCKER

cat > "$FAKE_BIN/docker-compose" <<'FAKE_COMPOSE'
#!/usr/bin/env bash
set -u
case "${1:-}" in
  version) [[ "${FAKE_COMPOSE_V1:-1}" == 1 ]] ;;
  config) printf '%s\n' "${FAKE_COMPOSE_SERVICES:-}" ;;
  *) printf 'docker-compose:%s\n' "$*" >> "$EVENTS_FILE" ;;
esac
FAKE_COMPOSE

chmod +x "$FAKE_BIN/docker" "$FAKE_BIN/docker-compose"
export PATH="$FAKE_BIN:$PATH"
export NO_COLOR=1

# shellcheck source=/dev/null
source "$SCRIPT"
STACK_DIRS[main]="$STACK_DIR"

fail() { echo "FAIL: $*" >&2; exit 1; }
assert_eq() {
  local expected="$1" actual="$2" message="${3:-}"
  [[ "$expected" == "$actual" ]] || fail "${message:-值不相等}: expected=[$expected], actual=[$actual]"
}
assert_contains() {
  local haystack="$1" needle="$2"
  [[ "$haystack" == *"$needle"* ]] || fail "输出缺少 [$needle]: $haystack"
}
assert_event() { grep -Fqx "$1" "$EVENTS_FILE" || fail "未记录调用: $1"; }
assert_no_event() { ! grep -Fqx "$1" "$EVENTS_FILE" || fail "出现了不应执行的调用: $1"; }
reset_events() { : > "$EVENTS_FILE"; }
set_states() { printf '%s\n' "$@" > "$STATE_FILE"; }

assert_eq 25 "${#CONTAINERS[@]}" "容器配置数量"
assert_eq /home/hyz/apartment-rental-system "${STACK_DIRS_DEFAULT_MAIN:-${STACK_DIRS[main]}}" "默认部署目录"
if grep -Eqi 'InsightOS|user-service-container|order-service-container|my-redis' "$SCRIPT"; then
  fail "脚本仍包含示例或 InsightOS 容器名"
fi

# Docker 环境预检应区分 daemon 与权限问题。
FAKE_DOCKER_INFO=down
output="$(check_docker_environment 2>&1 || true)"
assert_contains "$output" "Docker daemon 未启动"
FAKE_DOCKER_INFO=permission
output="$(check_docker_environment 2>&1 || true)"
assert_contains "$output" "Docker 权限不足"
FAKE_DOCKER_INFO=ok
check_docker_environment >/dev/null

# Compose 优先 v2，并兼容 v1。
FAKE_COMPOSE_V2=1
detect_compose_command
assert_eq "docker compose" "${COMPOSE_CMD[*]}"
FAKE_COMPOSE_V2=0
detect_compose_command
assert_eq "docker-compose" "${COMPOSE_CMD[*]}"
FAKE_COMPOSE_V2=1
detect_compose_command

# 状态映射覆盖四种显示值。
set_states \
  'running-ok|running|healthy' \
  'running-nohealth|running|none' \
  'stopped-one|exited|none' \
  'bad-one|running|unhealthy' \
  'restarting-one|restarting|none'
assert_eq "运行中" "$(container_status running-ok)"
assert_eq "运行中" "$(container_status running-nohealth)"
assert_eq "已停止" "$(container_status stopped-one)"
assert_eq "未创建" "$(container_status absent-one)"
assert_eq "状态异常" "$(container_status bad-one)"
assert_eq "状态异常" "$(container_status restarting-one)"

FRONTEND_ENTRY='前端|apartment-frontend|frontend|main'
IMAGE_ENTRY='图片代理|apartment-ha-image-proxy-1|image-proxy|main'

# 启动：运行中不重复；已停止 docker start；未创建走 Compose。
set_states 'apartment-frontend|running|healthy'
reset_events
output="$(start_container "$FRONTEND_ENTRY")"
assert_contains "$output" "容器已经在运行"
assert_no_event 'docker:start apartment-frontend'

set_states 'apartment-frontend|exited|none'
reset_events
start_container "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:start apartment-frontend'

set_states
reset_events
start_container "$IMAGE_ENTRY" >/dev/null
assert_event 'docker:compose up -d image-proxy'

# 特殊启动钩子只对指定容器执行。
SPECIAL_START_HOOKS[apartment-frontend]="printf 'hook-ran\\n' >> '$HOOK_FILE'"
set_states 'apartment-frontend|exited|none'
reset_events
start_container "$FRONTEND_ENTRY" >/dev/null
assert_eq hook-ran "$(tail -n 1 "$HOOK_FILE")"
unset 'SPECIAL_START_HOOKS[apartment-frontend]'

# 停止与重启。
set_states 'apartment-frontend|exited|none'
reset_events
output="$(stop_container "$FRONTEND_ENTRY")"
assert_contains "$output" "容器未运行"
assert_no_event 'docker:stop apartment-frontend'

set_states 'apartment-frontend|running|healthy'
reset_events
stop_container "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:stop apartment-frontend'
reset_events
restart_container "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:restart apartment-frontend'

set_states
reset_events
restart_container "$IMAGE_ENTRY" >/dev/null
assert_event 'docker:compose up -d image-proxy'

# 日志、终端 bash -> sh 回退。
set_states 'apartment-frontend|running|healthy'
reset_events
show_recent_logs "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:logs --tail 100 apartment-frontend'
reset_events
follow_logs "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:logs -f --tail 100 apartment-frontend'
reset_events
FAKE_BASH_EXEC_FAIL=1
enter_container "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:exec -it apartment-frontend bash'
assert_event 'docker:exec -it apartment-frontend sh'
FAKE_BASH_EXEC_FAIL=0

# 删除必须确认，且不能携带 volume 选项。
set_states 'apartment-frontend|exited|none'
reset_events
printf 'n\n' | delete_container "$FRONTEND_ENTRY" >/dev/null
assert_no_event 'docker:rm apartment-frontend'
printf 'y\n' | delete_container "$FRONTEND_ENTRY" >/dev/null
assert_event 'docker:rm apartment-frontend'
if grep -Eq 'docker (volume rm|rm .*-v)|docker compose down' "$SCRIPT"; then
  fail "脚本包含危险的卷删除或 compose down 命令"
fi

# 批量失败必须继续处理后续容器并报告失败数。
CONTAINERS=(
  '前端|apartment-frontend|frontend|main'
  '图片代理|apartment-ha-image-proxy-1|image-proxy|main'
)
set_states \
  'apartment-frontend|running|healthy' \
  'apartment-ha-image-proxy-1|running|healthy'
FAKE_FAIL_ACTION='restart:apartment-frontend'
reset_events
output="$(restart_all_containers 2>&1 || true)"
assert_event 'docker:restart apartment-frontend'
assert_event 'docker:restart apartment-ha-image-proxy-1'
assert_contains "$output" "失败 1 个"
FAKE_FAIL_ACTION=''

echo "docker_manager tests passed"
