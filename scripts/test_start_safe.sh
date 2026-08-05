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
echo "system is ready"
EOF

chmod +x "$BIN/docker" "$TMP/ready.sh"
export PATH="$BIN:$PATH"
export EVENTS_FILE="$EVENTS"
export READY_SCRIPT="$TMP/ready.sh"

output="$("$SCRIPT")"
grep -q "system is ready" <<<"$output"
grep -q '^docker:compose start$' "$EVENTS"
grep -q '^ready$' "$EVENTS"

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
