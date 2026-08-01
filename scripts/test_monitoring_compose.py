#!/usr/bin/env python3
import json
import os
import subprocess
from pathlib import Path


root = Path(os.environ.get("PROJECT_ROOT", Path(__file__).resolve().parents[1]))


def compose(*args: str) -> str:
    result = subprocess.run(
        ["docker", "compose", "-f", str(root / "docker-compose.yml"), *args],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


default_services = set(compose("config", "--services").splitlines())
assert {"prometheus", "grafana"} <= default_services
assert "node-exporter" not in default_services
assert "cadvisor" not in default_services

config = json.loads(compose("config", "--format", "json"))
services = config["services"]


def published_ports(service: str) -> set:
    return {int(port["published"]) for port in services[service].get("ports", [])}


assert 9090 in published_ports("prometheus")
assert 3000 in published_ports("grafana")
assert services["grafana"]["depends_on"]["prometheus"]["condition"] == "service_healthy"

grafana_volumes = services["grafana"].get("volumes", [])
assert any(volume["target"] == "/etc/grafana/provisioning" for volume in grafana_volumes)

prometheus = (root / "deploy/monitor/prometheus.yml").read_text(encoding="utf-8")
for stale in ("apartment-ha-", "node-exporter:9100", "cadvisor:8080"):
    assert stale not in prometheus
for target in (
    "apartment-gateway:8080",
    "apartment-user-service:8081",
    "apartment-house-service:8083",
    "apartment-order-service:8088",
    "apartment-payment-service:8087",
    "apartment-notice-service:8091",
    "apartment-contract-service:8092",
):
    assert target in prometheus

datasource = root / "deploy/monitor/grafana/provisioning/datasources/prometheus.yml"
text = datasource.read_text(encoding="utf-8")
assert "url: http://prometheus:9090" in text
assert "isDefault: true" in text

print("monitoring compose test passed")
