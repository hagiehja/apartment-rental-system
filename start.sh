#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
READY_SCRIPT="${READY_SCRIPT:-$ROOT/scripts/wait-until-ready.sh}"

cd "$ROOT"
echo "starting existing apartment containers..." >&2
docker compose start
echo "containers started; waiting for the complete system..." >&2
exec "$READY_SCRIPT"
