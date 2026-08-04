#!/bin/sh
set -eu

MAX_ATTEMPTS="${READY_MAX_ATTEMPTS:-90}"
RETRY_INTERVAL="${READY_RETRY_INTERVAL:-2}"
BASE_URL="${BASE_URL:-http://127.0.0.1}"
FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:5173}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://127.0.0.1:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://127.0.0.1:3000}"

diagnose() {
    label="$1"
    echo "ERROR: $label is not ready after $MAX_ATTEMPTS attempts" >&2
    echo "Inspect with: docker compose ps" >&2
    echo "Inspect with: docker compose logs --tail=100" >&2
}

wait_http() {
    label="$1"
    url="$2"
    attempt=1
    while ! curl -fsS --max-time 10 "$url" >/dev/null 2>&1; do
        if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
            diagnose "$label"
            return 1
        fi
        echo "waiting for $label ($attempt/$MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
    echo "ready: $label" >&2
}

fetch_house_list() {
    attempt=1
    while :; do
        if HOUSE_LIST_JSON="$(curl -fsS --max-time 30 "$BASE_URL/api/house/list?page=1&size=6" 2>/dev/null)" \
            && [ -n "$HOUSE_LIST_JSON" ]; then
            export HOUSE_LIST_JSON
            echo "ready: house list cache warmed" >&2
            return 0
        fi
        if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
            diagnose "House list"
            return 1
        fi
        echo "waiting for house list warm-up ($attempt/$MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
}

extract_first_image() {
    printf '%s' "$HOUSE_LIST_JSON" \
        | grep -oE '/img/(flickr|real)/[^" ]+' \
        | head -n 1
}

echo "checking apartment system readiness..." >&2
wait_http "Frontend" "$FRONTEND_URL/"
wait_http "Gateway" "$BASE_URL/gateway/health"
fetch_house_list

IMAGE_PATH="$(extract_first_image || true)"
if [ -z "$IMAGE_PATH" ]; then
    diagnose "House image path"
    exit 1
fi
wait_http "House image" "$BASE_URL$IMAGE_PATH"
wait_http "Prometheus" "$PROMETHEUS_URL/-/healthy"
wait_http "Grafana" "$GRAFANA_URL/api/health"

cat <<EOF
system is ready
frontend: $FRONTEND_URL
gateway: $BASE_URL
prometheus: $PROMETHEUS_URL
grafana: $GRAFANA_URL
EOF
