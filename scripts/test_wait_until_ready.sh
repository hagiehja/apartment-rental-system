#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/scripts/wait-until-ready.sh"
[[ -x "$SCRIPT" ]] || { echo "missing executable $SCRIPT" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
BIN="$TMP/bin"
EVENTS="$TMP/events"
COUNT="$TMP/count"
mkdir -p "$BIN"
printf '0' > "$COUNT"

cat > "$BIN/curl" <<'EOF'
#!/bin/sh
url=""
for arg in "$@"; do
    case "$arg" in http://*|https://*) url="$arg" ;; esac
done
echo "$url" >> "$EVENTS_FILE"
count=$(( $(cat "$COUNT_FILE") + 1 ))
echo "$count" > "$COUNT_FILE"
if [ "$count" -le "${FAKE_FAIL_FIRST:-0}" ]; then exit 22; fi
case "$url" in
  *"${FAIL_COMPONENT:-__never__}"*) exit 22 ;;
  */api/house/list*) printf '%s' '{"code":200,"data":{"records":[{"imageUrl":"/img/flickr/cover/38937901/800/600"}]}}' ;;
  *) printf '%s' 'ok' ;;
esac
EOF
chmod +x "$BIN/curl"

export PATH="$BIN:$PATH"
export EVENTS_FILE="$EVENTS"
export COUNT_FILE="$COUNT"
export READY_MAX_ATTEMPTS=4
export READY_RETRY_INTERVAL=0
export FAKE_FAIL_FIRST=2

output="$($SCRIPT)"
grep -q "system is ready" <<<"$output"
grep -q 'http://127.0.0.1:5173/' "$EVENTS"
grep -q 'http://127.0.0.1/gateway/health' "$EVENTS"
grep -q 'http://127.0.0.1/api/house/list?page=1&size=6' "$EVENTS"
grep -q 'http://127.0.0.1/img/flickr/cover/38937901/800/600' "$EVENTS"
grep -q 'http://127.0.0.1:9090/-/healthy' "$EVENTS"
grep -q 'http://127.0.0.1:3000/api/health' "$EVENTS"

printf '0' > "$COUNT"
export FAKE_FAIL_FIRST=0
export FAIL_COMPONENT=':3000/'
export READY_MAX_ATTEMPTS=2
if "$SCRIPT" >"$TMP/failure.log" 2>&1; then
    echo "permanent Grafana failure unexpectedly succeeded" >&2
    exit 1
fi
grep -q "Grafana" "$TMP/failure.log"
grep -q "docker compose logs" "$TMP/failure.log"

echo "wait-until-ready tests passed"
