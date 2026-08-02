import json
import os
import subprocess
from pathlib import Path


root = Path(os.environ.get("APARTMENT_PROJECT_ROOT", Path(__file__).resolve().parents[1]))
compose_file = root / "docker-compose.yml"


def compose(*args):
    result = subprocess.run(
        ["docker", "compose", "-f", str(compose_file), *args],
        cwd=root,
        check=True,
        text=True,
        capture_output=True,
    )
    return result.stdout


services = set(compose("config", "--services").splitlines())
assert "image-proxy" in services

config = json.loads(compose("config", "--format", "json"))
proxy = config["services"]["image-proxy"]
assert proxy["image"] == "apartment-image-proxy:latest"
assert proxy["restart"] == "unless-stopped"
assert proxy["healthcheck"]
assert any(volume["target"] == "/data/img-cache" for volume in proxy["volumes"])
assert config["services"]["nginx"]["depends_on"]["image-proxy"]["condition"] == "service_healthy"

print("image proxy compose test passed")
