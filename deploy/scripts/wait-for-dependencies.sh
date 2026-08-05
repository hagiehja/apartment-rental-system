#!/bin/sh
set -eu

MAX_ATTEMPTS="${DEPENDENCY_MAX_ATTEMPTS:-90}"
RETRY_INTERVAL="${DEPENDENCY_RETRY_INTERVAL:-2}"

first_endpoint() {
    printf '%s' "$1" | sed 's#^[a-zA-Z]*://##; s/[;,].*$//; s#/.*$##'
}

endpoint_host() {
    first_endpoint "$1" | sed 's/:.*$//'
}

endpoint_port() {
    value="$(first_endpoint "$1")"
    case "$value" in
        *:*) printf '%s' "${value##*:}" ;;
        *) printf '%s' "$2" ;;
    esac
}

wait_http() {
    label="$1"
    url="$2"
    attempt=1
    while ! curl -fsS --max-time 3 "$url" >/dev/null 2>&1; do
        if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
            echo "dependency check timed out: $label" >&2
            return 1
        fi
        echo "waiting for $label ($attempt/$MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
    echo "dependency ready: $label" >&2
}

wait_tcp() {
    label="$1"
    host="$2"
    port="$3"
    attempt=1
    while ! nc -z -w 2 "$host" "$port" >/dev/null 2>&1; do
        if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
            echo "dependency check timed out: $label ($host:$port)" >&2
            return 1
        fi
        echo "waiting for $label at $host:$port ($attempt/$MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
    echo "dependency ready: $label ($host:$port)" >&2
}

wait_sentinel() {
    host="$1"
    port="$2"
    attempt=1
    while ! redis-cli -h "$host" -p "$port" --raw SENTINEL get-master-addr-by-name mymaster 2>/dev/null \
        | awk 'NR == 1 { host = $0 } NR == 2 { port = $0 } END { exit !(NR == 2 && host != "" && port ~ /^[0-9]+$/) }'; do
        if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
            echo "dependency check timed out: Redis Sentinel discovery ($host:$port)" >&2
            return 1
        fi
        echo "waiting for Redis Sentinel discovery at $host:$port ($attempt/$MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
    echo "dependency ready: Redis Sentinel discovery ($host:$port)" >&2
}

NACOS_ENDPOINT="$(first_endpoint "${NACOS_ADDR:-nacos1:8848}")"
NACOS_HOST="$(endpoint_host "$NACOS_ENDPOINT")"
NACOS_PORT="$(endpoint_port "$NACOS_ENDPOINT" 8848)"
wait_http "Nacos" "http://$NACOS_HOST:$NACOS_PORT/nacos/v1/console/health/readiness"

wait_tcp "MySQL" "${MYSQL_HOST:-mysql-ha-master}" "${MYSQL_PORT:-3306}"

if [ "${WAIT_FOR_RW_MYSQL:-false}" = "true" ]; then
    RW_DEFAULT_HOST="${MIDDLEWARE_HOST:-${MYSQL_HOST:-mysql-ha-master}}"
    wait_tcp "MySQL read/write master" "${RW_MASTER_HOST:-$RW_DEFAULT_HOST}" "${RW_MASTER_PORT:-3310}"
    wait_tcp "MySQL read/write slave" "${RW_SLAVE_HOST:-$RW_DEFAULT_HOST}" "${RW_SLAVE_PORT:-3311}"
fi

wait_tcp "Redis master" "${REDIS_HOST:-redis-master}" "${REDIS_PORT:-6379}"

SENTINEL_HOST="${REDIS_SENTINEL_HOST:-${MIDDLEWARE_HOST:-${REDIS_HOST:-redis-sentinel-1}}}"
OLD_IFS="$IFS"
IFS=','
for SENTINEL_PORT in ${REDIS_SENTINEL_PORTS:-26379,26380,26381}; do
    SENTINEL_PORT="$(printf '%s' "$SENTINEL_PORT" | tr -d '[:space:]')"
    [ -n "$SENTINEL_PORT" ] || {
        echo "invalid empty Redis Sentinel port" >&2
        exit 1
    }
    wait_sentinel "$SENTINEL_HOST" "$SENTINEL_PORT"
done
IFS="$OLD_IFS"

ROCKETMQ_ENDPOINT="$(first_endpoint "${ROCKETMQ_NAMESRV:-namesrv:9876}")"
ROCKETMQ_HOST="$(endpoint_host "$ROCKETMQ_ENDPOINT")"
ROCKETMQ_PORT="$(endpoint_port "$ROCKETMQ_ENDPOINT" 9876)"
wait_tcp "RocketMQ nameserver" "$ROCKETMQ_HOST" "$ROCKETMQ_PORT"

echo "all application dependencies are ready" >&2
exec "$@"
