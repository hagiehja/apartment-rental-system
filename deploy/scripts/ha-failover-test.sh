#!/usr/bin/env sh
set -eu

COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.ha.yml}
TARGET_SERVICE=${TARGET_SERVICE:-apartment-gateway}
HOST_HTTP_PORT=${HOST_HTTP_PORT:-80}
TEST_URL=${TEST_URL:-http://127.0.0.1:${HOST_HTTP_PORT}/gateway/health}

container_id=$(docker compose -f "$COMPOSE_FILE" ps -q "$TARGET_SERVICE" | head -n 1)
if [ -z "$container_id" ]; then
  echo "No running container found for $TARGET_SERVICE" >&2
  exit 1
fi

echo "Stopping one $TARGET_SERVICE container: $container_id"
docker stop "$container_id" >/dev/null

echo "Checking $TEST_URL after failure"
for i in 1 2 3 4 5 6 7 8 9 10; do
  if curl -fsS "$TEST_URL" >/tmp/apartment-ha-check.out 2>/tmp/apartment-ha-check.err; then
    echo "Failover check passed on attempt $i"
    cat /tmp/apartment-ha-check.out
    exit 0
  fi
  echo "Attempt $i failed; waiting 2s"
  sleep 2
done

echo "Failover check failed" >&2
cat /tmp/apartment-ha-check.err >&2 || true
exit 1
