#!/usr/bin/env sh
set -eu

COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.ha.yml}
HOST_HTTP_PORT=${HOST_HTTP_PORT:-80}

docker compose -f "$COMPOSE_FILE" ps
printf '
Nginx health:
'
curl -fsS "http://127.0.0.1:${HOST_HTTP_PORT}/health" || true
printf '
Gateway health through Nginx:
'
curl -fsS "http://127.0.0.1:${HOST_HTTP_PORT}/gateway/health" || true
printf '
'
