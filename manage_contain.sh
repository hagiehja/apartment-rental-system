#!/bin/bash
#=========================================================================
# manage_contain.sh  —  公寓租赁系统 · 容器可视化管理面板
# 用法: ./manage_contain.sh   (在 VM 192.168.24.129 上运行)
# 功能: 列出全部容器,按序号选择容器 启动/停止/重启/日志/进入/详情
#=========================================================================

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'
BLUE=$'\033[0;34m'; CYAN=$'\033[0;36m'; MAGENTA=$'\033[0;35m'
BOLD=$'\033[1m'; DIM=$'\033[2m'; NC=$'\033[0m'

declare -ga CTR_NAMES
declare -gA CTR_STATUS CTR_PORT CTR_GROUP IDX2NAME
declare -g SELECTED=""

WAIT_INTERVAL_SECONDS="${WAIT_INTERVAL_SECONDS:-3}"
INFRA_READY_TIMEOUT_SECONDS="${INFRA_READY_TIMEOUT_SECONDS:-180}"
APP_READY_TIMEOUT_SECONDS="${APP_READY_TIMEOUT_SECONDS:-240}"

GROUP_ORDER=(db cache registry mq biz)
declare -A GROUP_TITLE=(
  [db]="[数据库 MySQL]"
  [cache]="[缓存 Redis 集群]"
  [registry]="[注册中心 Nacos]"
  [mq]="[消息队列 RocketMQ]"
  [biz]="[业务服务]"
)
BIZ_ORDER=(gateway user house order payment notice contract nginx frontend image-proxy)

classify() {
  local n="${1,,}"
  case "$n" in
    *mysql*) echo "db" ;;
    *redis*) echo "cache" ;;
    *nacos*) echo "registry" ;;
    *rocketmq*) echo "mq" ;;
    *) echo "biz" ;;
  esac
}

biz_sort_key() {
  local n="${1,,}" i
  for i in "${!BIZ_ORDER[@]}"; do
    [[ "$n" == *"${BIZ_ORDER[$i]}"* ]] && { echo "$i"; return; }
  done
  echo 99
}

short_name() {
  local d="$1"
  d="${d#apartment-system-}"
  d="${d#apartment-ha-}"
  d="${d#apartment-}"
  d="${d%-1}"
  echo "$d"
}

render_status() {
  local s="$1"
  case "$s" in
    Up*healthy*)   printf "%s[运行-健康]%s" "$GREEN" "$NC" ;;
    Up*unhealthy*) printf "%s[不健康]%s"   "$RED"   "$NC" ;;
    Up*)           printf "%s[运行中]%s"   "$GREEN" "$NC" ;;
    Exited*)       printf "%s[已停止]%s"   "$RED"   "$NC" ;;
    Restarting*)   printf "%s[重启中]%s"   "$YELLOW" "$NC" ;;
    Created*)      printf "%s[已创建]%s"   "$DIM"   "$NC" ;;
    *)             printf "%s[?%s]%s"      "$YELLOW" "${s%% *}" "$NC" ;;
  esac
}

extract_port() {
  local p="$1" out=""
  while [[ "$p" =~ 0\.0\.0\.0:([0-9]+)\-\> ]]; do
    out="${out:+$out,}${BASH_REMATCH[1]}"
    p="${p#*${BASH_REMATCH[0]}}"
  done
  [[ -z "$out" ]] && out="内网"
  echo "$out"
}

load_containers() {
  CTR_NAMES=()
  IDX2NAME=()
  while IFS='|' read -r name status ports; do
    [[ -z "$name" ]] && continue
    CTR_NAMES+=("$name")
    CTR_STATUS["$name"]="$status"
    CTR_PORT["$name"]="$ports"
    CTR_GROUP["$name"]="$(classify "$name")"
  done < <(docker ps -a --format '{{.Names}}|{{.Status}}|{{.Ports}}' 2>/dev/null | grep -iE 'mysql|redis|nacos|rocketmq|apartment')
}

show_panel() {
  clear
  local total=${#CTR_NAMES[@]} up=0 down=0
  for n in "${CTR_NAMES[@]}"; do
    case "${CTR_STATUS[$n]}" in Up*) ((up++));; *) ((down++));; esac
  done

  echo "${CYAN}======================================================================${NC}"
  echo "${BOLD}          公寓租赁系统 · 容器可视化管理面板${NC}"
  echo "${CYAN}======================================================================${NC}"
  echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')    容器总数: ${BOLD}${total}${NC}    运行: ${GREEN}${up}${NC}    停止: ${RED}${down}${NC}"
  echo

  local idx=1
  for g in "${GROUP_ORDER[@]}"; do
    local items=()
    for n in "${CTR_NAMES[@]}"; do
      [[ "${CTR_GROUP[$n]}" == "$g" ]] && items+=("$n")
    done
    [[ ${#items[@]} -eq 0 ]] && continue

    if [[ "$g" == "biz" ]]; then
      local keyed=()
      for x in "${items[@]}"; do keyed+=("$(biz_sort_key "$x") $x"); done
      IFS=$'\n' items=($(printf '%s\n' "${keyed[@]}" | sort -k1,1n | cut -d' ' -f2-)); unset IFS
    else
      IFS=$'\n' items=($(printf '%s\n' "${items[@]}" | sort)); unset IFS
    fi

    echo "${BOLD}${GROUP_TITLE[$g]}${NC}"
    for n in "${items[@]}"; do
      local mark=" "
      [[ "$n" == "$SELECTED" ]] && mark="${MAGENTA}>${NC}"
      printf "  %s ${YELLOW}[%2d]${NC} %-30s %s  ${DIM}%s${NC}\n" \
        "$mark" "$idx" "$(short_name "$n")" \
        "$(render_status "${CTR_STATUS[$n]}")" \
        "$(extract_port "${CTR_PORT[$n]}")"
      IDX2NAME[$idx]="$n"
      ((idx++))
    done
    echo
  done

  echo "${CYAN}----------------------------------------------------------------------${NC}"
  echo " 操作: ${YELLOW}[序号]${NC}=选中容器   ${GREEN}a${NC}=全部启动   ${RED}x${NC}=全部停止   ${BLUE}r${NC}=刷新"
  echo "       ${MAGENTA}s${NC}=资源占用       ${YELLOW}l${NC}=访问网址     ${RED}q${NC}=退出"
  echo "${CYAN}----------------------------------------------------------------------${NC}"
}

action_menu() {
  local name="$1"
  while true; do
    echo
    echo "${MAGENTA}-- 当前选中: ${BOLD}$(short_name "$name")${NC} ${DIM}($name)${NC}"
    echo "${MAGENTA}   状态: $(render_status "${CTR_STATUS[$name]}")"
    echo "${MAGENTA}------------------------------------------------${NC}"
    echo "${MAGENTA}  ${GREEN}[1]${NC} 启动       ${RED}[2]${NC} 停止       ${YELLOW}[3]${NC} 重启"
    echo "${MAGENTA}  ${BLUE}[4]${NC} 查看日志   ${CYAN}[5]${NC} 进入容器   ${DIM}[6]${NC} 资源占用"
    echo "${MAGENTA}  ${MAGENTA}[7]${NC} 容器详情   ${RED}[0]${NC} 返回"
    echo "${MAGENTA}------------------------------------------------${NC}"
    printf "请选择操作: "; read -r op
    case "$op" in
      1) echo "${GREEN}> 启动 $name ...${NC}"; docker start "$name"; sleep 1.5; return ;;
      2) echo "${RED}> 停止 $name ...${NC}"; docker stop "$name" 2>/dev/null; sleep 1; return ;;
      3) echo "${YELLOW}> 重启 $name ...${NC}"; docker restart "$name"; sleep 2; return ;;
      4) echo "${BLUE}> 日志(最近100行实时流, Ctrl+C 返回):${NC}"; echo
         docker logs -f --tail 100 "$name" 2>&1 || true
         echo; echo "${DIM}(已退出日志)${NC}"; read -rp "按回车继续..." ;;
      5) echo "${CYAN}> 进入容器 ${name}:${NC}"
         if ! docker exec -it "$name" bash 2>/dev/null; then
           docker exec -it "$name" sh 2>/dev/null || echo "${RED}无法进入(容器可能未运行)${NC}"
         fi ;;
      6) echo "${DIM}> 资源占用:${NC}"
         docker stats --no-stream "$name" 2>/dev/null || echo "${RED}无法获取${NC}"
         read -rp "按回车继续..." ;;
      7) echo "${MAGENTA}> 容器详情:${NC}"
         docker inspect "$name" --format '镜像: {{.Config.Image}}
启动: {{.State.StartedAt}}
重启策略: {{.HostConfig.RestartPolicy.Name}}
网络: {{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}
IP: {{range $k,$v := .NetworkSettings.Networks}}{{$v.IPAddress}} {{end}}' 2>/dev/null
         read -rp "按回车继续..." ;;
      0|"") return ;;
      *) echo "${RED}无效选项${NC}"; sleep 0.5 ;;
    esac
  done
}

container_is_running() {
  [[ "$(docker inspect --format '{{.State.Running}}' "$1" 2>/dev/null)" == "true" ]]
}

start_if_needed() {
  local name="$1"
  if ! container_is_running "$name"; then
    echo "  启动 $name"
    docker start "$name" >/dev/null
  fi
}

wait_until() {
  local label="$1" timeout_seconds="$2" probe="$3"
  shift 3
  local started_at=$SECONDS

  printf "  等待 %s" "$label"
  until "$probe" "$@"; do
    if (( SECONDS - started_at >= timeout_seconds )); then
      echo
      echo "${RED}✗ 等待 $label 超时 (${timeout_seconds}s)，后续服务未启动${NC}" >&2
      return 1
    fi
    printf "."
    sleep "$WAIT_INTERVAL_SECONDS"
  done
  echo " ${GREEN}✓ 已就绪${NC}"
}

nacos_container_ready() {
  local name="$1"
  container_is_running "$name" || return 1
  docker exec "$name" bash -ec '
    curl -fsS --max-time 2 http://127.0.0.1:8848/nacos/v1/console/health/readiness >/dev/null &&
    nc -z -w 2 127.0.0.1 9848
  ' >/dev/null 2>&1
}

wait_for_nacos_cluster() {
  local name found=0
  for name in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$name]}" == "registry" ]] || continue
    found=1
    wait_until "Nacos 节点 $name (HTTP + gRPC)" \
      "$INFRA_READY_TIMEOUT_SECONDS" nacos_container_ready "$name" || return 1
  done
  if (( found == 0 )); then
    echo "${RED}✗ 未找到 Nacos 容器，业务服务未启动${NC}" >&2
    return 1
  fi
}

container_health_status() {
  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
    "$1" 2>/dev/null
}

container_is_healthy() {
  local name="$1"
  container_is_running "$name" || return 1
  [[ "$(container_health_status "$name")" == "healthy" ]]
}

wait_for_container_health() {
  local name="$1"
  if [[ "$(container_health_status "$name")" == "none" ]]; then
    echo "${RED}✗ $name 没有 healthcheck，无法确认服务已就绪${NC}" >&2
    return 1
  fi
  wait_until "$(short_name "$name") 健康检查" \
    "$APP_READY_TIMEOUT_SECONDS" container_is_healthy "$name"
}

is_gateway_container() {
  [[ "${1,,}" == *gateway* ]]
}

is_nginx_container() {
  [[ "${1,,}" == *nginx* ]]
}

is_edge_container() {
  local name="${1,,}"
  [[ "$name" == *frontend* || "$name" == *image-proxy* ]]
}

all_up() {
  echo "${GREEN}> 启动全部容器(基础设施优先)...${NC}"

  # 1. 先启动全部基础设施。Nacos 就绪也间接证明其依赖的 MySQL 已可用。
  for n in "${CTR_NAMES[@]}"; do
    case "${CTR_GROUP[$n]}" in db|cache|registry|mq)
      start_if_needed "$n" || return 1
    ;; esac
  done
  wait_for_nacos_cluster || return 1

  # 2. 先启动并等待下游业务服务，避免 Gateway 注册到尚未就绪的实例。
  for n in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$n]}" == "biz" ]] || continue
    is_gateway_container "$n" && continue
    is_nginx_container "$n" && continue
    is_edge_container "$n" && continue
    start_if_needed "$n" || return 1
  done
  for n in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$n]}" == "biz" ]] || continue
    is_gateway_container "$n" && continue
    is_nginx_container "$n" && continue
    is_edge_container "$n" && continue
    wait_for_container_health "$n" || return 1
  done

  # 3. 下游全部健康后再启动 Gateway。
  for n in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$n]}" == "biz" ]] && is_gateway_container "$n" || continue
    start_if_needed "$n" || return 1
    wait_for_container_health "$n" || return 1
  done

  # 4. Gateway 健康后再开放 Nginx，最后启动前端和图片代理。
  for n in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$n]}" == "biz" ]] && is_nginx_container "$n" || continue
    start_if_needed "$n" || return 1
    wait_for_container_health "$n" || return 1
  done
  for n in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$n]}" == "biz" ]] && is_edge_container "$n" || continue
    start_if_needed "$n" || return 1
  done

  echo "${GREEN}✓ 全部服务已按依赖顺序启动并通过健康检查${NC}"
  sleep 1.5
}

all_down() {
  echo "${RED}> 将停止全部 ${#CTR_NAMES[@]} 个容器(数据卷保留)${NC}"
  read -rp "确认停止? [y/N]: " yn
  [[ "$yn" =~ ^[Yy]$ ]] || { echo "已取消"; sleep 1; return; }
  for n in "${CTR_NAMES[@]}"; do
    [[ "${CTR_GROUP[$n]}" == "biz" ]] && { echo "  停止 $n"; docker stop "$n" >/dev/null 2>&1; }
  done
  for n in "${CTR_NAMES[@]}"; do
    case "${CTR_GROUP[$n]}" in db|cache|registry|mq)
      echo "  停止 $n"; docker stop "$n" >/dev/null 2>&1
    ;; esac
  done
  echo "${RED}✓ 已全部停止${NC}"; sleep 1.5
}

show_stats() {
  echo "${MAGENTA}> 实时资源占用:${NC}"; echo
  docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null \
    | grep -iE 'mysql|redis|nacos|rocketmq|apartment|NAME' | head -30
  echo; read -rp "按回车继续..."
}

show_urls() {
  local ip="192.168.24.129"
  echo "${YELLOW}-- 系统访问入口 ----------------------------------------${NC}"
  echo "${YELLOW}  ${BOLD}前端(用户/管理)${NC}: http://${ip}/   (管理后台 /admin)"
  echo "${YELLOW}  ${BOLD}开发端口${NC}:       http://${ip}:5173/"
  echo "${YELLOW}  ${BOLD}网关健康${NC}:       http://${ip}/gateway/health"
  echo "${YELLOW}  ${BOLD}Nacos 控制台${NC}:   http://${ip}:8848/nacos   (nacos/nacos)"
  echo "${YELLOW}  ${BOLD}RocketMQ${NC}:       http://${ip}:8888/"
  echo "${YELLOW}  ${BOLD}MySQL${NC}:          ${ip}:3310(主) 3311(从)   root/123456"
  echo "${YELLOW}  ${BOLD}Redis${NC}:          ${ip}:6379  ApmtRedis2026Secure!"
  echo "${YELLOW}-------------------------------------------------------${NC}"
  echo; read -rp "按回车继续..."
}

main() {
  trap '' SIGINT
  while true; do
    load_containers
    show_panel
    printf "${BOLD}请选择:${NC} "
    read -r choice
    case "$choice" in
      q|Q|exit) echo "${DIM}再见${NC}"; break ;;
      r|R) continue ;;
      a|A) all_up ;;
      x|X) all_down ;;
      s|S) show_stats ;;
      l|L) show_urls ;;
      ''|*[!0-9]*) echo "${RED}无效输入,请输入序号或指令${NC}"; sleep 1 ;;
      *)
        if [[ -n "${IDX2NAME[$choice]:-}" ]]; then
          SELECTED="${IDX2NAME[$choice]}"
          action_menu "$SELECTED"
        else
          echo "${RED}序号 $choice 不存在 (范围 1-${#IDX2NAME[@]})${NC}"
          sleep 1
        fi
        ;;
    esac
  done
  trap - SIGINT
}

main
