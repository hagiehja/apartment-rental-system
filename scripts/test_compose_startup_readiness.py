#!/usr/bin/env python3
"""Static startup-readiness contract tests with no third-party dependencies."""
import json
import os
import re
import shutil
import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative_path):
    return (ROOT / relative_path).read_text(encoding="utf-8")


class StartupReadinessContractTests(unittest.TestCase):
    def test_infrastructure_services_define_healthchecks(self):
        expectations = {
            "deploy/mysql-ha/docker-compose.yml": ("mysql-ha-master", "mysql-ha-slave"),
            "deploy/redis-ha/docker-compose.yml": (
                "redis-master",
                "redis-slave",
                "redis-sentinel-1",
                "redis-sentinel-2",
                "redis-sentinel-3",
            ),
            "deploy/nacos-cluster/docker-compose.yml": ("nacos1", "nacos2", "nacos3"),
            "deploy/rocketmq/docker-compose.yml": ("namesrv", "broker"),
        }
        for path, services in expectations.items():
            text = read(path)
            for service in services:
                with self.subTest(path=path, service=service):
                    block = re.search(
                        rf"(?ms)^  {re.escape(service)}:\n(.*?)(?=^  [a-zA-Z0-9_-]+:|\Z)",
                        text,
                    )
                    self.assertIsNotNone(block, f"missing service {service} in {path}")
                    shared_healthcheck = "x-" in text and "healthcheck:" in text[: block.start()]
                    self.assertTrue(
                        "healthcheck:" in block.group(1) or shared_healthcheck,
                        f"{service} lacks a healthcheck in {path}",
                    )

    def test_application_startup_is_dependency_aware(self):
        dockerfile = read("Dockerfile.service")
        self.assertIn("wait-for-dependencies.sh", dockerfile)
        self.assertIn('ENTRYPOINT ["/app/wait-for-dependencies.sh"]', dockerfile)
        self.assertIn("redis-tools", dockerfile)

        wait_script = read("deploy/scripts/wait-for-dependencies.sh")
        self.assertIn("RW_MASTER_PORT", wait_script)
        self.assertIn("RW_SLAVE_PORT", wait_script)
        self.assertIn("SENTINEL get-master-addr-by-name", wait_script)

        ready_script = read("scripts/wait-until-ready.sh")
        self.assertIn("/api/user/login", ready_script)
        self.assertIn("pageNum=1&pageSize=6", ready_script)
        redis_compose = read("deploy/redis-ha/docker-compose.yml")
        self.assertIn("SENTINEL get-master-addr-by-name mymaster", redis_compose)
        self.assertNotIn("env_file:", redis_compose)
        self.assertIn(
            "env_file: ${APP_ENV_FILE:-.env.app}", read("docker-compose.yml")
        )
        self.assertIn(
            "- ${APP_ENV_FILE:-.env.app}", read("docker-compose.ha.yml")
        )
        self.assertIn("REDIS_PASSWORD=", read(".env.app.example"))

        rocketmq_compose = read("deploy/rocketmq/docker-compose.yml")
        self.assertIn("/dev/tcp/127.0.0.1/9876", rocketmq_compose)
        self.assertIn("/dev/tcp/127.0.0.1/10911", rocketmq_compose)

    def test_compose_uses_healthy_dependency_chain(self):
        text = read("docker-compose.ha.yml")
        self.assertRegex(
            text,
            r"(?ms)x-app-common:.*?depends_on:.*?nacos1:.*?condition: service_healthy",
        )
        for service in (
            "apartment-user-service",
            "apartment-house-service",
            "apartment-order-service",
            "apartment-payment-service",
            "apartment-notice-service",
            "apartment-contract-service",
        ):
            self.assertRegex(
                text,
                rf"(?ms)apartment-gateway:.*?depends_on:.*?{service}:\s*\n\s+condition: service_healthy",
            )
        self.assertRegex(
            text,
            r"(?ms)nginx:.*?depends_on:.*?apartment-gateway:\s*\n\s+condition: service_healthy",
        )
        self.assertRegex(
            text,
            r"(?ms)frontend:.*?depends_on:.*?nginx:\s*\n\s+condition: service_healthy",
        )


    @unittest.skipUnless(shutil.which("docker"), "docker CLI is not available")
    def test_merged_compose_contract(self):
        compose_env = os.environ.copy()
        compose_env["APP_ENV_FILE"] = ".env.app.example"
        result = subprocess.run(
            ["docker", "compose", "config", "--format", "json"], cwd=ROOT,
            env=compose_env, text=True, capture_output=True, check=True,
        )
        services = json.loads(result.stdout)["services"]
        self.assertEqual({"REDIS_PASSWORD"}, set(services["redis-master"]["environment"]))
        self.assertEqual({"REDIS_PASSWORD"}, set(services["redis-sentinel-1"]["environment"]))
        self.assertEqual(
            services["redis-master"]["environment"]["REDIS_PASSWORD"],
            services["apartment-user-service"]["environment"]["REDIS_PASSWORD"],
        )
        for service in (
            "mysql-ha-master", "mysql-ha-slave", "redis-master", "redis-slave",
            "redis-sentinel-1", "redis-sentinel-2", "redis-sentinel-3",
            "nacos1", "nacos2", "nacos3", "namesrv", "broker",
        ):
            self.assertIn("healthcheck", services[service], service)
        for service in (
            "apartment-user-service", "apartment-house-service", "apartment-order-service",
            "apartment-payment-service", "apartment-notice-service", "apartment-contract-service",
        ):
            self.assertEqual("service_healthy", services["apartment-gateway"]["depends_on"][service]["condition"])
        self.assertEqual("service_healthy", services["nginx"]["depends_on"]["apartment-gateway"]["condition"])

if __name__ == "__main__":
    unittest.main()
