#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
READY_SCRIPT="${READY_SCRIPT:-$ROOT/scripts/wait-until-ready.sh}"

PUBLIC_HOST="${PUBLIC_HOST:-$(hostname -I 2>/dev/null | awk 'NR == 1 { print $1; exit }')}"
PUBLIC_HOST="${PUBLIC_HOST:-127.0.0.1}"
export PUBLIC_FRONTEND_URL="${PUBLIC_FRONTEND_URL:-http://$PUBLIC_HOST:${FRONTEND_PORT:-5173}}"
export PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-http://$PUBLIC_HOST}"
export PUBLIC_PROMETHEUS_URL="${PUBLIC_PROMETHEUS_URL:-http://$PUBLIC_HOST:9090}"
export PUBLIC_GRAFANA_URL="${PUBLIC_GRAFANA_URL:-http://$PUBLIC_HOST:3000}"

cd "$ROOT"
echo "starting existing apartment containers..." >&2
docker compose start
echo "containers started; waiting for the complete system..." >&2
exec "$READY_SCRIPT"
