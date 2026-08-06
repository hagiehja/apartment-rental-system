#!/usr/bin/env bash

# Apartment Rental System Docker 容器交互式管理面板
# 兼容 Ubuntu 22.04、Docker Compose v2 和 docker-compose v1。
# 所有可修改的环境配置都集中在本文件顶部。

# ============================== 可修改配置 ==============================

# 格式：显示名称|真实容器名称|Compose 服务名称|所属部署栈
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

# 每个部署栈对应一个 Compose 文件所在目录。
declare -A STACK_DIRS=(
  ["main"]="/home/hyz/apartment-rental-system"
)
STACK_DIRS_DEFAULT_MAIN="/home/hyz/apartment-rental-system"

# 可选启动前钩子，键必须是真实容器名称。
# 示例写法（当前项目没有需要特殊钩子的容器）：
# SPECIAL_START_HOOKS[某容器名]='xhost +local:docker; export DISPLAY=${DISPLAY:-:0}'
declare -A SPECIAL_START_HOOKS=()

# 可在测试或特殊环境中覆盖 Docker 命令。
DOCKER_BIN="${DOCKER_BIN:-docker}"
COMPOSE_CMD=()

# ============================== 输出样式 ==============================

if [[ -t 1 && -z "${NO_COLOR:-}" ]]; then
  C_RED=$'\033[31m'
  C_GREEN=$'\033[32m'
  C_YELLOW=$'\033[33m'
  C_BLUE=$'\033[34m'
  C_BOLD=$'\033[1m'
  C_RESET=$'\033[0m'
else
  C_RED=""
  C_GREEN=""
  C_YELLOW=""
  C_BLUE=""
  C_BOLD=""
  C_RESET=""
fi

info() { printf '%s[信息]%s %s\n' "$C_BLUE" "$C_RESET" "$*"; }
success() { printf '%s[成功]%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
warn() { printf '%s[提示]%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
error() { printf '%s[错误]%s %s\n' "$C_RED" "$C_RESET" "$*" >&2; }

# ============================== 基础解析与检查 ==============================

parse_entry() {
  local entry="${1:-}"
  IFS='|' read -r DISPLAY_NAME CONTAINER_NAME COMPOSE_SERVICE STACK_NAME <<< "$entry"
  if [[ -z "${DISPLAY_NAME:-}" || -z "${CONTAINER_NAME:-}" || -z "${STACK_NAME:-}" ]]; then
    error "容器配置格式错误：$entry"
    return 1
  fi
}

check_docker_environment() {
  local output
  if ! command -v "$DOCKER_BIN" >/dev/null 2>&1; then
    error "未安装 Docker，请先安装 Docker Engine。"
    return 1
  fi

  if ! output="$($DOCKER_BIN info 2>&1)"; then
    if [[ "$output" == *"permission denied"* || "$output" == *"Permission denied"* ]]; then
      error "Docker 权限不足：当前用户不能访问 Docker daemon。请将用户加入 docker 组后重新登录。"
    else
      error "Docker daemon 未启动或无法连接。请先启动 Docker 服务。"
    fi
    return 1
  fi
  return 0
}

detect_compose_command() {
  if "$DOCKER_BIN" compose version >/dev/null 2>&1; then
    COMPOSE_CMD=("$DOCKER_BIN" compose)
    return 0
  fi
  if command -v docker-compose >/dev/null 2>&1 && docker-compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
    return 0
  fi
  COMPOSE_CMD=()
  error "未找到 Docker Compose。请安装 Compose v2 插件或 docker-compose v1。"
  return 1
}

find_compose_file() {
  local stack_dir="${1:-}" candidate
  [[ -d "$stack_dir" ]] || { error "Compose 部署目录不存在：$stack_dir"; return 1; }
  for candidate in docker-compose.yml docker-compose.yaml compose.yml compose.yaml; do
    if [[ -f "$stack_dir/$candidate" ]]; then
      printf '%s\n' "$stack_dir/$candidate"
      return 0
    fi
  done
  error "目录中未找到 Compose 文件：$stack_dir"
  return 1
}

run_compose() {
  local stack="${1:-}"
  shift || true
  local stack_dir="${STACK_DIRS[$stack]:-}"
  [[ -n "$stack_dir" ]] || { error "未配置部署栈：$stack"; return 1; }
  find_compose_file "$stack_dir" >/dev/null || return 1
  ((${#COMPOSE_CMD[@]} > 0)) || detect_compose_command || return 1
  (cd "$stack_dir" && "${COMPOSE_CMD[@]}" "$@")
}

compose_service_exists() {
  local service="${1:-}" stack="${2:-}" services
  [[ -n "$service" ]] || return 1
  services="$(run_compose "$stack" config --services 2>/dev/null)" || return 1
  grep -Fxq "$service" <<< "$services"
}

validate_configuration() {
  local entry stack seen="|" count=0
  declare -A container_names=()
  for entry in "${CONTAINERS[@]}"; do
    parse_entry "$entry" || return 1
    ((count += 1))
    if [[ -n "${container_names[$CONTAINER_NAME]:-}" ]]; then
      error "真实容器名称重复：$CONTAINER_NAME"
      return 1
    fi
    container_names[$CONTAINER_NAME]=1
    stack="$STACK_NAME"
    [[ -n "${STACK_DIRS[$stack]:-}" ]] || { error "服务 $DISPLAY_NAME 使用了未配置的部署栈：$stack"; return 1; }
    if [[ "$seen" != *"|$stack|"* ]]; then
      find_compose_file "${STACK_DIRS[$stack]}" >/dev/null || return 1
      seen+="$stack|"
    fi
  done
  ((count > 0)) || { error "没有配置任何容器。"; return 1; }
}

# ============================== 容器状态 ==============================

container_exists() {
  "$DOCKER_BIN" inspect "${1:-}" >/dev/null 2>&1
}

container_state_record() {
  "$DOCKER_BIN" inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${1:-}" 2>/dev/null
}

container_is_running() {
  local record status
  record="$(container_state_record "${1:-}")" || return 1
  IFS='|' read -r status _ <<< "$record"
  [[ "$status" == "running" ]]
}

container_status() {
  local name="${1:-}" record state health
  if ! record="$(container_state_record "$name")"; then
    printf '未创建\n'
    return 0
  fi
  IFS='|' read -r state health <<< "$record"
  case "$state" in
    running)
      if [[ "$health" == "unhealthy" ]]; then
        printf '状态异常\n'
      else
        printf '运行中\n'
      fi
      ;;
    exited|created) printf '已停止\n' ;;
    *) printf '状态异常\n' ;;
  esac
}

run_special_start_hook() {
  local name="${1:-}"
  local hook="${SPECIAL_START_HOOKS[$name]:-}"
  [[ -n "$hook" ]] || return 0
  info "正在执行 $name 的启动前钩子……"
  if (eval -- "$hook"); then
    success "启动前钩子执行完成：$name"
  else
    error "启动前钩子执行失败，已取消启动：$name"
    return 1
  fi
}

# ============================== 单个容器操作 ==============================

start_container() {
  parse_entry "${1:-}" || return 1

  if container_is_running "$CONTAINER_NAME"; then
    warn "容器已经在运行：$CONTAINER_NAME"
    return 0
  fi

  if container_exists "$CONTAINER_NAME"; then
    run_special_start_hook "$CONTAINER_NAME" || return 1
    info "正在启动容器：$CONTAINER_NAME"
    if "$DOCKER_BIN" start "$CONTAINER_NAME"; then
      success "容器启动成功：$CONTAINER_NAME"
      return 0
    fi
    error "容器启动失败：$CONTAINER_NAME"
    return 1
  fi

  if [[ -z "${COMPOSE_SERVICE:-}" ]]; then
    error "容器未创建，且没有配置 Compose 服务名称：$CONTAINER_NAME"
    return 1
  fi
  if ! compose_service_exists "$COMPOSE_SERVICE" "$STACK_NAME"; then
    error "容器未创建，Compose 中也找不到服务：$COMPOSE_SERVICE（部署栈：$STACK_NAME）"
    return 1
  fi
  run_special_start_hook "$CONTAINER_NAME" || return 1
  info "正在通过 Compose 创建并启动：$COMPOSE_SERVICE"
  if run_compose "$STACK_NAME" up -d "$COMPOSE_SERVICE"; then
    success "Compose 服务启动成功：$COMPOSE_SERVICE"
    return 0
  fi
  error "Compose 服务启动失败：$COMPOSE_SERVICE"
  return 1
}

stop_container() {
  parse_entry "${1:-}" || return 1
  if ! container_exists "$CONTAINER_NAME"; then
    warn "容器未创建：$CONTAINER_NAME"
    return 0
  fi
  if ! container_is_running "$CONTAINER_NAME"; then
    warn "容器未运行：$CONTAINER_NAME"
    return 0
  fi
  info "正在停止容器：$CONTAINER_NAME"
  if "$DOCKER_BIN" stop "$CONTAINER_NAME"; then
    success "容器停止成功：$CONTAINER_NAME"
    return 0
  fi
  error "容器停止失败：$CONTAINER_NAME"
  return 1
}

restart_container() {
  parse_entry "${1:-}" || return 1
  if container_exists "$CONTAINER_NAME"; then
    run_special_start_hook "$CONTAINER_NAME" || return 1
    info "正在重启容器：$CONTAINER_NAME"
    if "$DOCKER_BIN" restart "$CONTAINER_NAME"; then
      success "容器重启成功：$CONTAINER_NAME"
      return 0
    fi
    error "容器重启失败：$CONTAINER_NAME"
    return 1
  fi

  if [[ -z "${COMPOSE_SERVICE:-}" ]] || ! compose_service_exists "$COMPOSE_SERVICE" "$STACK_NAME"; then
    error "容器不存在，Compose 中也找不到可创建的服务：${COMPOSE_SERVICE:-未配置}"
    return 1
  fi
  run_special_start_hook "$CONTAINER_NAME" || return 1
  info "容器尚未创建，正在通过 Compose 启动：$COMPOSE_SERVICE"
  if run_compose "$STACK_NAME" up -d "$COMPOSE_SERVICE"; then
    success "Compose 服务启动成功：$COMPOSE_SERVICE"
    return 0
  fi
  error "Compose 服务启动失败：$COMPOSE_SERVICE"
  return 1
}

show_recent_logs() {
  parse_entry "${1:-}" || return 1
  container_exists "$CONTAINER_NAME" || { error "容器未创建，无法查看日志：$CONTAINER_NAME"; return 1; }
  "$DOCKER_BIN" logs --tail 100 "$CONTAINER_NAME"
}

follow_logs() {
  parse_entry "${1:-}" || return 1
  container_exists "$CONTAINER_NAME" || { error "容器未创建，无法查看日志：$CONTAINER_NAME"; return 1; }
  warn "正在实时查看日志，按 Ctrl+C 返回操作菜单。"
  "$DOCKER_BIN" logs -f --tail 100 "$CONTAINER_NAME"
  local status=$?
  [[ $status -eq 130 ]] && return 0
  return "$status"
}

enter_container() {
  parse_entry "${1:-}" || return 1
  if ! container_is_running "$CONTAINER_NAME"; then
    error "容器未运行，无法进入终端：$CONTAINER_NAME"
    return 1
  fi
  info "尝试使用 bash 进入容器：$CONTAINER_NAME"
  if "$DOCKER_BIN" exec -it "$CONTAINER_NAME" bash; then
    return 0
  fi
  warn "容器中没有可用的 bash，改用 sh。"
  "$DOCKER_BIN" exec -it "$CONTAINER_NAME" sh
}

delete_container() {
  parse_entry "${1:-}" || return 1
  if ! container_exists "$CONTAINER_NAME"; then
    warn "容器未创建，无需删除：$CONTAINER_NAME"
    return 0
  fi
  local answer
  printf '%s确认删除容器 %s？(y/N) %s' "$C_YELLOW" "$CONTAINER_NAME" "$C_RESET"
  IFS= read -r answer || answer=""
  if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    warn "已取消删除。"
    return 0
  fi
  if "$DOCKER_BIN" rm "$CONTAINER_NAME"; then
    success "已删除容器本身：$CONTAINER_NAME（数据卷和宿主机文件未删除）"
    return 0
  fi
  error "删除失败：$CONTAINER_NAME。若容器仍在运行，请先停止它。"
  return 1
}

# ============================== 批量操作 ==============================

run_batch() {
  local action="${1:-}" label="${2:-操作}" entry failures=0 index=0 total=${#CONTAINERS[@]}
  for entry in "${CONTAINERS[@]}"; do
    ((index += 1))
    parse_entry "$entry" || { ((failures += 1)); continue; }
    printf '\n%s[%d/%d] %s：%s%s\n' "$C_BOLD" "$index" "$total" "$label" "$DISPLAY_NAME" "$C_RESET"
    case "$action" in
      start) start_container "$entry" || ((failures += 1)) ;;
      stop) stop_container "$entry" || ((failures += 1)) ;;
      restart) restart_container "$entry" || ((failures += 1)) ;;
      *) error "未知批量操作：$action"; return 1 ;;
    esac
  done
  if ((failures == 0)); then
    success "批量${label}完成：共 $total 个，失败 0 个。"
    return 0
  fi
  error "批量${label}完成：共 $total 个，失败 $failures 个；其余容器已继续处理。"
  return 1
}

start_all_containers() { run_batch start "启动"; }
stop_all_containers() { run_batch stop "停止"; }
restart_all_containers() { run_batch restart "重启"; }

# ============================== 交互菜单 ==============================

status_color() {
  case "${1:-}" in
    运行中) printf '%s' "$C_GREEN" ;;
    已停止) printf '%s' "$C_YELLOW" ;;
    未创建) printf '%s' "$C_BLUE" ;;
    *) printf '%s' "$C_RED" ;;
  esac
}

print_container_table() {
  local index=1 entry status color
  printf '%sDocker 容器管理面板%s\n' "$C_BOLD" "$C_RESET"
  printf '%-4s | %-22s | %-48s | %s\n' "编号" "服务名称" "容器名称" "当前状态"
  printf '%s\n' '-----+------------------------+--------------------------------------------------+----------'
  for entry in "${CONTAINERS[@]}"; do
    if parse_entry "$entry"; then
      status="$(container_status "$CONTAINER_NAME")"
      color="$(status_color "$status")"
      printf '%-4d | %-22s | %-48s | %s%s%s\n' "$index" "$DISPLAY_NAME" "$CONTAINER_NAME" "$color" "$status" "$C_RESET"
    else
      printf '%-4d | %-22s | %-48s | %s状态异常%s\n' "$index" "配置错误" "-" "$C_RED" "$C_RESET"
    fi
    ((index += 1))
  done
}

pause_screen() {
  [[ -t 0 ]] || return 0
  printf '\n按 Enter 键继续……'
  IFS= read -r _ || true
}

clear_screen() {
  [[ -t 1 && -n "${TERM:-}" ]] && clear || true
}

container_action_menu() {
  local entry="${1:-}" choice
  parse_entry "$entry" || return 1
  while true; do
    clear_screen
    printf '%s服务：%s%s\n' "$C_BOLD" "$DISPLAY_NAME" "$C_RESET"
    printf '容器：%s\n当前状态：%s\n\n' "$CONTAINER_NAME" "$(container_status "$CONTAINER_NAME")"
    printf '1. 启动容器\n'
    printf '2. 停止容器\n'
    printf '3. 重启容器\n'
    printf '4. 查看最近 100 行日志\n'
    printf '5. 实时查看日志\n'
    printf '6. 进入容器终端\n'
    printf '7. 删除容器\n'
    printf '0. 返回上一级\n\n'
    printf '请输入操作编号：'
    IFS= read -r choice || return 0
    case "$choice" in
      1) start_container "$entry"; pause_screen ;;
      2) stop_container "$entry"; pause_screen ;;
      3) restart_container "$entry"; pause_screen ;;
      4) show_recent_logs "$entry" || true; pause_screen ;;
      5) follow_logs "$entry" || true; pause_screen ;;
      6) enter_container "$entry" || true; pause_screen ;;
      7) delete_container "$entry" || true; pause_screen ;;
      0) return 0 ;;
      *) error "输入无效，请输入 0 到 7。"; pause_screen ;;
    esac
  done
}

main_menu() {
  local choice count=${#CONTAINERS[@]}
  while true; do
    clear_screen
    print_container_table
    printf '\n批量操作：a 启动全部 | s 停止全部 | r 重启全部 | 0 退出\n'
    printf '请输入容器编号或操作字母：'
    IFS= read -r choice || { printf '\n'; return 0; }
    case "$choice" in
      0) info "已退出 Docker 容器管理面板。"; return 0 ;;
      a|A) start_all_containers || true; pause_screen ;;
      s|S) stop_all_containers || true; pause_screen ;;
      r|R) restart_all_containers || true; pause_screen ;;
      *[!0-9]*|'') error "输入无效，请输入 1-$count、a、s、r 或 0。"; pause_screen ;;
      *)
        if ((choice >= 1 && choice <= count)); then
          container_action_menu "${CONTAINERS[$((choice - 1))]}"
        else
          error "编号超出范围，请输入 1-$count。"
          pause_screen
        fi
        ;;
    esac
  done
}

main() {
  check_docker_environment || return 1
  validate_configuration || return 1
  if ! detect_compose_command; then
    warn "仍可管理已创建的容器，但无法通过 Compose 创建缺失容器。"
  fi
  main_menu
}

# 被测试脚本 source 时不自动打开交互菜单。
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
