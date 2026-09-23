#!/usr/bin/env python3
import json
import os
import subprocess
from pathlib import Path


project_root = Path(os.environ.get("PROJECT_ROOT", Path(__file__).resolve().parents[1]))
result = subprocess.run(
    ["docker", "compose", "-f", str(project_root / "docker-compose.yml"), "config", "--format", "json"],
    cwd=project_root,
    check=True,
    capture_output=True,
    text=True,
)
config = json.loads(result.stdout)
services = config["services"]


def require_healthy_dependency(service: str, dependency: str) -> None:
    depends_on = services[service].get("depends_on", {})
    actual = depends_on.get(dependency, {}).get("condition")
    assert actual == "service_healthy", (
        f"{service} must wait for {dependency} to be healthy; got {actual!r}"
    )


backend_services = (
    "apartment-user-service",
    "apartment-house-service",
    "apartment-order-service",
    "apartment-payment-service",
    "apartment-notice-service",
    "apartment-contract-service",
)

for backend in backend_services:
    require_healthy_dependency(backend, "nacos1")

for backend in backend_services:
    require_healthy_dependency("apartment-gateway", backend)

require_healthy_dependency("nginx", "apartment-gateway")
require_healthy_dependency("frontend", "nginx")

print("compose startup dependency test passed")
