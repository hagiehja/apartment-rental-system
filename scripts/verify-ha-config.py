from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

checks = []

def require(condition, message):
    checks.append((condition, message))

ha_compose = ROOT / "docker-compose.ha.yml"
nginx_conf = ROOT / "deploy" / "nginx" / "nginx.conf"
ha_up = ROOT / "deploy" / "scripts" / "ha-up.sh"
ha_status = ROOT / "deploy" / "scripts" / "ha-status.sh"
ha_failover = ROOT / "deploy" / "scripts" / "ha-failover-test.sh"

require(ha_compose.exists(), "docker-compose.ha.yml must exist")
require(nginx_conf.exists(), "deploy/nginx/nginx.conf must exist")
require(ha_up.exists(), "deploy/scripts/ha-up.sh must exist")
require(ha_status.exists(), "deploy/scripts/ha-status.sh must exist")
require(ha_failover.exists(), "deploy/scripts/ha-failover-test.sh must exist")

if ha_compose.exists():
    text = ha_compose.read_text(encoding="utf-8")
    for service in [
        "nginx",
        "apartment-gateway",
        "apartment-user-service",
        "apartment-house-service",
        "apartment-order-service",
        "apartment-payment-service",
        "apartment-notice-service",
        "apartment-contract-service",
    ]:
        require(re.search(rf"^  {re.escape(service)}:\s*$", text, re.M), f"{service} service must be defined")
    require("container_name:" not in text, "HA compose must not use container_name because it blocks --scale")
    require("${HOST_HTTP_PORT:-80}:80" in text, "nginx must expose HOST_HTTP_PORT to port 80")
    gateway_block = re.search(r"^  apartment-gateway:\n(?P<body>(?:    .+\n|\n)+?)(?=^  [a-zA-Z0-9_-]+:|^volumes:|\Z)", text, re.M)
    require(gateway_block is not None, "gateway block must be readable")
    if gateway_block:
        body = gateway_block.group("body")
        require("expose:" in body and '"8080"' in body, "gateway must expose 8080 only inside the compose network")
        require("ports:" not in body, "gateway must not publish host ports in HA compose; nginx is the only public entry")

if nginx_conf.exists():
    text = nginx_conf.read_text(encoding="utf-8")
    require("resolver 127.0.0.11" in text, "nginx must use Docker DNS resolver")
    require("/gateway/health" in text, "nginx must provide or proxy gateway health checks")
    require("proxy_next_upstream" in text, "nginx must retry another gateway on upstream failure")
    require("apartment-gateway" in text, "nginx must proxy to apartment-gateway service name")

failed = [message for ok, message in checks if not ok]
if failed:
    print("HA config verification failed:")
    for message in failed:
        print(f"- {message}")
    sys.exit(1)

print(f"HA config verification passed: {len(checks)} checks")
