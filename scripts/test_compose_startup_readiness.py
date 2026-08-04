#!/usr/bin/env python3
"""Static startup-readiness contract tests with no third-party dependencies."""
import re
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


if __name__ == "__main__":
    unittest.main()
