#!/usr/bin/env sh
set -eu

COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.ha.yml}
GATEWAY_REPLICAS=${GATEWAY_REPLICAS:-2}
ORDER_REPLICAS=${ORDER_REPLICAS:-3}
PAYMENT_REPLICAS=${PAYMENT_REPLICAS:-1}
HOUSE_REPLICAS=${HOUSE_REPLICAS:-1}
USER_REPLICAS=${USER_REPLICAS:-1}
NOTICE_REPLICAS=${NOTICE_REPLICAS:-1}
CONTRACT_REPLICAS=${CONTRACT_REPLICAS:-1}

docker compose -f "$COMPOSE_FILE" up -d --build \
  --scale apartment-gateway="$GATEWAY_REPLICAS" \
  --scale apartment-order-service="$ORDER_REPLICAS" \
  --scale apartment-payment-service="$PAYMENT_REPLICAS" \
  --scale apartment-house-service="$HOUSE_REPLICAS" \
  --scale apartment-user-service="$USER_REPLICAS" \
  --scale apartment-notice-service="$NOTICE_REPLICAS" \
  --scale apartment-contract-service="$CONTRACT_REPLICAS"

docker compose -f "$COMPOSE_FILE" ps
