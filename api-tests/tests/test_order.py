from datetime import date, timedelta

import allure
import pytest

from data.test_data import new_order
from utils.api_client import ApiClient
from utils.assertions import assert_forbidden, assert_rejected, assert_success


@allure.feature("订单服务")
@pytest.mark.smoke
def test_order_health(anonymous_client):
    assert "Order Service" in assert_success(anonymous_client.get("/api/order/health"))


@allure.feature("订单服务")
@pytest.mark.business
def test_create_detail_and_list_order(created_order, tenant_client):
    order_no = created_order["orderNo"]
    detail = assert_success(tenant_client.get(f"/api/order/{order_no}"))
    assert detail["orderNo"] == order_no
    mine = assert_success(tenant_client.get("/api/order/my/list", params={"pageNum": 1, "pageSize": 10}))
    assert "records" in mine


@allure.feature("订单服务")
@pytest.mark.business
@pytest.mark.parametrize(
    "payload",
    [
        pytest.param({}, id="empty-body"),
        pytest.param({"houseId": 1, "rentStartDate": date.today().isoformat(), "rentMonths": 1}, id="non-future-date"),
        pytest.param({"houseId": 1, "rentStartDate": (date.today() + timedelta(days=2)).isoformat(), "rentMonths": 0}, id="zero-months"),
        pytest.param({"houseId": 1, "rentStartDate": (date.today() + timedelta(days=2)).isoformat(), "rentMonths": 25}, id="too-many-months"),
    ],
)
def test_create_order_rejects_invalid_payload(tenant_client, payload):
    assert_rejected(tenant_client.post("/api/order", json=payload))


@allure.feature("订单服务")
@pytest.mark.auth
def test_create_order_requires_login(anonymous_client):
    assert_rejected(anonymous_client.post("/api/order", json=new_order(1)), allowed_http=(401, 403))


@allure.feature("订单服务")
@pytest.mark.business
def test_missing_order_is_rejected(tenant_client):
    assert_rejected(tenant_client.get("/api/order/NOT-EXIST-999999"))


@allure.feature("订单服务")
@pytest.mark.business
def test_landlord_order_list(landlord_client):
    result = assert_success(landlord_client.get("/api/order/landlord/orders", params={"pageNum": 1, "pageSize": 10}))
    assert "records" in result


@allure.feature("订单服务")
@pytest.mark.business
def test_cancel_order(created_order, tenant_client):
    order_no = created_order["orderNo"]
    assert_success(tenant_client.put(f"/api/order/{order_no}/cancel", json={"cancelReason": "接口自动化取消"}))
    assert_rejected(tenant_client.put(f"/api/order/{order_no}/cancel", json={"cancelReason": "重复取消"}))


@allure.feature("订单服务")
@allure.story("内部接口不允许经公网入口访问")
@pytest.mark.auth
@pytest.mark.parametrize(
    ("method", "path"),
    [
        ("GET", "/api/order/id/999999999"),
        ("GET", "/api/order/NO-999/installments"),
        ("GET", "/api/order/NO-999/validate"),
        ("POST", "/api/order/NO-999/payment-success"),
        ("POST", "/api/order/NO-999/refund-success"),
    ],
)
def test_order_internal_routes_are_blocked_by_nginx(tenant_client, method, path):
    assert_forbidden(tenant_client.request(method, path))


@allure.feature("订单服务")
@pytest.mark.internal
def test_order_internal_queries(settings, created_order):
    if not settings.order_service_url:
        pytest.skip("缺少 ORDER_SERVICE_URL")
    client = ApiClient(settings.order_service_url, timeout=settings.timeout)
    order_no = created_order["orderNo"]
    assert_success(client.get(f"/order/id/{created_order['orderId']}"))
    assert_success(client.get(f"/order/{order_no}/installments"))
    assert_success(client.get(f"/order/{order_no}/validate"))
