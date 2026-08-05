#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/start.sh"
[[ -x "$SCRIPT" ]] || { echo "missing executable $SCRIPT" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
BIN="$TMP/bin"
EVENTS="$TMP/events"
mkdir -p "$BIN"

cat > "$BIN/docker" <<'EOF'
#!/bin/sh
echo "docker:$*" >> "$EVENTS_FILE"
exit "${FAKE_DOCKER_EXIT:-0}"
EOF

cat > "$TMP/ready.sh" <<'EOF'
#!/bin/sh
echo "ready" >> "$EVENTS_FILE"
echo "public:${PUBLIC_FRONTEND_URL:-unset}" >> "$EVENTS_FILE"
echo "gateway:${PUBLIC_BASE_URL:-unset}" >> "$EVENTS_FILE"
echo "prometheus:${PUBLIC_PROMETHEUS_URL:-unset}" >> "$EVENTS_FILE"
echo "grafana:${PUBLIC_GRAFANA_URL:-unset}" >> "$EVENTS_FILE"
echo "system is ready"
EOF

chmod +x "$BIN/docker" "$TMP/ready.sh"
export PATH="$BIN:$PATH"
export EVENTS_FILE="$EVENTS"
export READY_SCRIPT="$TMP/ready.sh"
export PUBLIC_HOST="192.168.24.129"

output="$("$SCRIPT")"
grep -q "system is ready" <<<"$output"
grep -q '^docker:compose start$' "$EVENTS"
grep -q '^ready$' "$EVENTS"
grep -q '^public:http://192.168.24.129:5173$' "$EVENTS"
grep -q '^gateway:http://192.168.24.129$' "$EVENTS"
grep -q '^prometheus:http://192.168.24.129:9090$' "$EVENTS"
grep -q '^grafana:http://192.168.24.129:3000$' "$EVENTS"

docker_line="$(grep -n '^docker:' "$EVENTS" | cut -d: -f1)"
ready_line="$(grep -n '^ready$' "$EVENTS" | cut -d: -f1)"
(( docker_line < ready_line ))

: > "$EVENTS"
export FAKE_DOCKER_EXIT=1
if "$SCRIPT" >"$TMP/failure.log" 2>&1; then
    echo "docker start failure unexpectedly succeeded" >&2
    exit 1
fi
if grep -q '^ready$' "$EVENTS"; then
    echo "readiness check ran after docker start failure" >&2
    exit 1
fi

echo "safe start tests passed"
