#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/deploy/scripts/wait-for-dependencies.sh"
[[ -x "$SCRIPT" ]] || { echo "missing executable $SCRIPT" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
EVENTS="$TMP/events"
BIN="$TMP/bin"
mkdir -p "$BIN"

cat > "$BIN/curl" <<'EOF'
#!/bin/sh
echo "curl:$*" >> "$EVENTS_FILE"
exit "${FAKE_CURL_EXIT:-0}"
EOF
cat > "$BIN/nc" <<'EOF'
#!/bin/sh
echo "nc:$*" >> "$EVENTS_FILE"
exit "${FAKE_NC_EXIT:-0}"
EOF
chmod +x "$BIN/curl" "$BIN/nc"

export PATH="$BIN:$PATH"
export EVENTS_FILE="$EVENTS"
export NACOS_ADDR="registry.test:8848"
export MYSQL_HOST="db.test"
export MYSQL_PORT="3306"
export REDIS_HOST="cache.test"
export REDIS_PORT="26379"
export ROCKETMQ_NAMESRV="mq.test:9876"
export DEPENDENCY_MAX_ATTEMPTS=2
export DEPENDENCY_RETRY_INTERVAL=0

output="$($SCRIPT sh -c 'echo application-started')"
grep -q "application-started" <<<"$output"
grep -q "curl:.*registry.test:8848/nacos/v1/console/health/readiness" "$EVENTS"
grep -q "nc:.*db.test 3306" "$EVENTS"
grep -q "nc:.*cache.test 26379" "$EVENTS"
grep -q "nc:.*mq.test 9876" "$EVENTS"

curl_line="$(grep -n '^curl:' "$EVENTS" | head -1 | cut -d: -f1)"
mysql_line="$(grep -n 'db.test 3306' "$EVENTS" | head -1 | cut -d: -f1)"
redis_line="$(grep -n 'cache.test 26379' "$EVENTS" | head -1 | cut -d: -f1)"
mq_line="$(grep -n 'mq.test 9876' "$EVENTS" | head -1 | cut -d: -f1)"
(( curl_line < mysql_line && mysql_line < redis_line && redis_line < mq_line ))

export FAKE_NC_EXIT=1
if "$SCRIPT" true >"$TMP/failure.log" 2>&1; then
  echo "dependency failure unexpectedly succeeded" >&2
  exit 1
fi
grep -q "dependency check timed out" "$TMP/failure.log"

export MYSQL_PASSWORD="DO_NOT_PRINT_THIS_PASSWORD"
export REDIS_PASSWORD="DO_NOT_PRINT_THIS_REDIS_PASSWORD"
export FAKE_NC_EXIT=0
"$SCRIPT" true >"$TMP/secret.log" 2>&1
if grep -q "DO_NOT_PRINT_THIS" "$TMP/secret.log"; then
  echo "secret leaked to startup log" >&2
  exit 1
fi

echo "wait-for-dependencies tests passed"
