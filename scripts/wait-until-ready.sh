#!/bin/sh
set -eu

MAX_ATTEMPTS="${READY_MAX_ATTEMPTS:-90}"
RETRY_INTERVAL="${READY_RETRY_INTERVAL:-2}"
READY_TIMEOUT_SECONDS="${READY_TIMEOUT_SECONDS:-600}"
BASE_URL="${BASE_URL:-http://127.0.0.1}"
FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:5173}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://127.0.0.1:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://127.0.0.1:3000}"
STARTED_AT="$(date +%s)"
DEADLINE=$((STARTED_AT + READY_TIMEOUT_SECONDS))

expired() {
    attempt="$1"
    now="$(date +%s)"
    [ "$attempt" -ge "$MAX_ATTEMPTS" ] || [ "$now" -ge "$DEADLINE" ]
}

diagnose() {
    label="$1"
    service="${2:-}"
    elapsed=$(( $(date +%s) - STARTED_AT ))
    echo "ERROR: $label is not ready after ${elapsed}s" >&2
    echo "Inspect with: docker compose ps" >&2
    if [ -n "$service" ]; then
        echo "Inspect with: docker compose logs --tail=100 $service" >&2
    else
        echo "Inspect with: docker compose logs --tail=100" >&2
    fi
}

wait_http() {
    label="$1"
    url="$2"
    service="${3:-}"
    attempt=1
    while ! curl -fsS --max-time 10 "$url" >/dev/null 2>&1; do
        if expired "$attempt"; then
            diagnose "$label" "$service"
            return 1
        fi
        echo "waiting for $label ($attempt/$MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
    echo "ready: $label" >&2
}

wait_login_route() {
    attempt=1
    while :; do
        response="$(curl -sS --max-time 10 -w '\n%{http_code}' \
            -H 'Content-Type: application/json' \
            --data 'readiness-invalid-json' \
            "$BASE_URL/api/user/login" 2>/dev/null || true)"
        status="$(printf '%s\n' "$response" | tail -n 1)"
        body="$(printf '%s\n' "$response" | sed '$d')"
        case "$status" in
            200|400)
                if printf '%s' "$body" | grep -Eq '"code":[[:space:]]*400'; then
                    echo "ready: Login route (expected structured validation rejection)" >&2
                    return 0
                fi
                ;;
        esac
        if expired "$attempt"; then
            diagnose "Login route" "apartment-user-service apartment-gateway nginx"
            return 1
        fi
        echo "waiting for Login route ($attempt/$MAX_ATTEMPTS, HTTP ${status:-none})" >&2
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
}

fetch_house_list() {
    attempt=1
    while :; do
        if HOUSE_LIST_JSON="$(curl -fsS --max-time 30 "$BASE_URL/api/house/list?pageNum=1&pageSize=6" 2>/dev/null)" \
            && [ -n "$HOUSE_LIST_JSON" ]; then
            export HOUSE_LIST_JSON
            echo "ready: house list cache warmed" >&2
            return 0
        fi
        if expired "$attempt"; then
            diagnose "House list" "apartment-house-service apartment-gateway nginx"
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
wait_http "Frontend" "$FRONTEND_URL/" "frontend"
wait_http "Gateway" "$BASE_URL/gateway/health" "apartment-gateway nginx"
wait_login_route
fetch_house_list

IMAGE_PATH="$(extract_first_image || true)"
if [ -z "$IMAGE_PATH" ]; then
    diagnose "House image path" "image-proxy"
    exit 1
fi
wait_http "House image" "$BASE_URL$IMAGE_PATH" "image-proxy nginx"
wait_http "Prometheus" "$PROMETHEUS_URL/-/healthy" "prometheus"
wait_http "Grafana" "$GRAFANA_URL/api/health" "grafana"

cat <<EOF
system is ready
frontend: $FRONTEND_URL
gateway: $BASE_URL
prometheus: $PROMETHEUS_URL
grafana: $GRAFANA_URL
EOF
