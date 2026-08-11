import re
from pathlib import Path


COVERED_ENDPOINTS = {
    ("GET", "/gateway/health"),
    ("GET", "/gateway/routes"),
    ("GET", "/api/user/health"),
    ("GET", "/api/user/verify"),
    ("POST", "/api/user/login"),
    ("POST", "/api/user/register"),
    ("GET", "/api/user/batch"),
    ("GET", "/api/user/stats"),
    ("GET", "/api/user/admin/list"),
    ("PUT", "/api/user/admin/{targetUserId}/role"),
    ("POST", "/api/house"),
    ("GET", "/api/house/list"),
    ("GET", "/api/house/{id}"),
    ("PUT", "/api/house/{id}"),
    ("DELETE", "/api/house/{id}"),
    ("PUT", "/api/house/{id}/offline"),
    ("PUT", "/api/house/{id}/online"),
    ("GET", "/api/house/health"),
    ("PUT", "/api/house/{id}/status"),
    ("GET", "/api/house/recommend"),
    ("POST", "/api/house/behavior/track"),
    ("POST", "/api/house/preference"),
    ("GET", "/api/house/preference"),
    ("GET", "/api/house/recommend/model-info"),
    ("POST", "/api/order"),
    ("GET", "/api/order/{orderNo}"),
    ("GET", "/api/order/id/{orderId}"),
    ("GET", "/api/order/my/list"),
    ("PUT", "/api/order/{orderNo}/cancel"),
    ("GET", "/api/order/{orderNo}/installments"),
    ("GET", "/api/order/{orderNo}/validate"),
    ("POST", "/api/order/{orderNo}/payment-success"),
    ("POST", "/api/order/{orderNo}/refund-success"),
    ("GET", "/api/order/health"),
    ("GET", "/api/order/landlord/orders"),
    ("GET", "/api/payment/account/balance"),
    ("GET", "/api/payment/account/transactions"),
    ("POST", "/api/payment/account/recharge"),
    ("POST", "/api/payment"),
    ("POST", "/api/payment/{paymentNo}/pay"),
    ("POST", "/api/payment/{paymentNo}/refund"),
    ("POST", "/api/payment/refund/order/{orderNo}"),
    ("GET", "/api/payment/health"),
    ("POST", "/api/contract/generate"),
    ("GET", "/api/contract/{contractId}"),
    ("GET", "/api/contract/order/{orderId}"),
    ("POST", "/api/contract/sign/{contractId}"),
    ("GET", "/api/contract/list"),
    ("GET", "/api/contract/download/{contractId}"),
    ("POST", "/api/contract/cancel/order/{orderId}"),
    ("GET", "/api/contract/health"),
    ("POST", "/api/contract/{contractId}/terminate"),
    ("GET", "/api/contract/{contractId}/calculate-refund"),
    ("POST", "/api/contract/landlord-income"),
    ("POST", "/api/notification/send"),
    ("GET", "/api/notification/unread/count"),
    ("GET", "/api/notification/unread"),
    ("GET", "/api/notification/list"),
    ("GET", "/api/notification/{messageId}"),
    ("POST", "/api/notification/read/{messageId}"),
    ("POST", "/api/notification/read/all"),
    ("DELETE", "/api/notification/{messageId}"),
    ("GET", "/api/notification/health"),
    ("POST", "/api/notification/test/insert"),
    ("POST", "/api/notification/fix-templates"),
}


def _controller_endpoints():
    root = Path(__file__).resolve().parents[2]
    controllers = list(root.glob("apartment-*-service/src/main/java/**/*Controller.java"))
    controllers += list(root.glob("apartment-gateway/src/main/java/**/*Controller.java"))
    endpoints = set()
    for controller in controllers:
        source = controller.read_text(encoding="utf-8")
        base_match = re.search(r'@RequestMapping\("([^"]+)"\)', source)
        if not base_match:
            continue
        base = base_match.group(1)
        prefix = "" if base.startswith("/gateway") else "/api"
        for method, path in re.findall(r'@(Get|Post|Put|Delete)Mapping(?:\("([^"]*)"\))?', source):
            endpoints.add((method.upper(), f"{prefix}{base}{path}"))
    return endpoints


def test_all_controller_endpoints_are_in_automation_inventory():
    actual = _controller_endpoints()
    assert len(actual) == 65
    assert actual == COVERED_ENDPOINTS


def test_jenkins_pipeline_has_required_gates_and_switches():
    root = Path(__file__).resolve().parents[2]
    pipeline = (root / "Jenkinsfile").read_text(encoding="utf-8")
    for parameter in ("RUN_INTERNAL", "RUN_MAINTENANCE", "RUN_DESTRUCTIVE", "RUN_E2E"):
        assert parameter in pipeline
    stages = ["Health Check", "Smoke Gate", "Regression", "Internal", "Maintenance", "Destructive", "E2E"]
    positions = [pipeline.index(f"stage('{stage}')") for stage in stages]
    assert positions == sorted(positions)
    assert "junit allowEmptyResults" in pipeline
    assert "allure includeProperties" in pipeline
