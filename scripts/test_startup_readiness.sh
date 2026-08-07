#!/usr/bin/env bash
set -euo pipefail

SCRIPT_UNDER_TEST="${SCRIPT_UNDER_TEST:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/manage_contain.sh}"

# 只加载函数，避免进入交互式 main 循环。
source <(sed '${/^main$/d;}' "$SCRIPT_UNDER_TEST")

declare -a EVENTS=()
declare -gA CTR_STATUS=()
declare -gA CTR_GROUP=()

sleep() {
  EVENTS+=("sleep:$1")
}

docker() {
  local command="${1:-}"
  shift || true

  case "$command" in
    start)
      EVENTS+=("start:$1")
      ;;
    inspect)
      local container="${@: -1}"
      if [[ "$*" == *'.State.Running'* ]]; then
        printf 'false\n'
      elif [[ "$*" == *'.State.Health.Status'* ]]; then
        EVENTS+=("health:$container")
        printf 'healthy\n'
      fi
      ;;
    exec)
      local container="${1:-unknown}"
      EVENTS+=("probe:$container")
      ;;
    *)
      printf 'unexpected docker command: %s %s\n' "$command" "$*" >&2
      return 1
      ;;
  esac
}

wait_for_nacos_cluster() {
  EVENTS+=("probe:nacos1")
}

wait_for_container_health() {
  EVENTS+=("health:$1")
}

event_index() {
  local expected="$1" i
  for i in "${!EVENTS[@]}"; do
    [[ "${EVENTS[$i]}" == "$expected" ]] && { printf '%s\n' "$i"; return 0; }
  done
  printf 'missing event: %s\nall events: %s\n' "$expected" "${EVENTS[*]}" >&2
  return 1
}

assert_before() {
  local first="$1" second="$2" first_index second_index
  first_index="$(event_index "$first")"
  second_index="$(event_index "$second")"
  (( first_index < second_index )) || {
    printf 'expected %s before %s\nall events: %s\n' "$first" "$second" "${EVENTS[*]}" >&2
    return 1
  }
}

CTR_NAMES=(
  apartment-system-nginx-1
  apartment-frontend
  apartment-system-apartment-gateway-1
  apartment-system-apartment-user-service-1
  nacos1
  mysql-ha-master
)

for container in "${CTR_NAMES[@]}"; do
  CTR_STATUS["$container"]="Exited (0)"
  CTR_GROUP["$container"]="$(classify "$container")"
done

WAIT_INTERVAL_SECONDS=0
INFRA_READY_TIMEOUT_SECONDS=2
APP_READY_TIMEOUT_SECONDS=2

all_up

assert_before "probe:nacos1" "start:apartment-system-apartment-user-service-1"
assert_before "start:apartment-system-apartment-user-service-1" "health:apartment-system-apartment-user-service-1"
assert_before "health:apartment-system-apartment-user-service-1" "start:apartment-system-apartment-gateway-1"
assert_before "health:apartment-system-apartment-gateway-1" "start:apartment-system-nginx-1"
assert_before "health:apartment-system-nginx-1" "start:apartment-frontend"

if [[ " ${EVENTS[*]} " == *" sleep:3 "* ]]; then
  printf 'fixed sleep 3 is still used: %s\n' "${EVENTS[*]}" >&2
  exit 1
fi

printf 'startup readiness ordering test passed\n'
