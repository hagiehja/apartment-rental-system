import allure
import pytest

from utils.api_client import ApiClient
from utils.assertions import assert_rejected, assert_success


@allure.feature("通知服务")
@pytest.mark.smoke
def test_notification_health(anonymous_client):
    data = assert_success(anonymous_client.get("/api/notification/health"))
    assert data["status"] == "UP"


@allure.feature("通知服务")
@pytest.mark.business
def test_notification_count_unread_and_list(tenant_client):
    user_id = tenant_client.user_id
    assert isinstance(assert_success(tenant_client.get("/api/notification/unread/count", params={"userId": user_id})), int)
    assert isinstance(assert_success(tenant_client.get("/api/notification/unread", params={"userId": user_id})), list)
    result = assert_success(
        tenant_client.get("/api/notification/list", params={"userId": user_id, "page": 1, "size": 10})
    )
    assert "records" in result


@allure.feature("通知服务")
@pytest.mark.business
@pytest.mark.parametrize(
    "path",
    ["/api/notification/unread/count", "/api/notification/unread", "/api/notification/list"],
)
def test_notification_queries_require_user_id(tenant_client, path):
    assert_rejected(tenant_client.get(path))


@allure.feature("通知服务")
@pytest.mark.business
def test_missing_notification_is_rejected(tenant_client):
    user_id = tenant_client.user_id
    assert_rejected(tenant_client.get("/api/notification/999999999", params={"userId": user_id}))
    assert_rejected(tenant_client.post("/api/notification/read/999999999", params={"userId": user_id}))


@allure.feature("通知服务")
@pytest.mark.business
def test_mark_all_read(tenant_client):
    assert_success(tenant_client.post("/api/notification/read/all", params={"userId": tenant_client.user_id}))


@allure.feature("通知服务")
@pytest.mark.internal
def test_send_read_and_delete_notification(settings, tenant_client):
    if not settings.notification_service_url:
        pytest.skip("缺少 NOTIFICATION_SERVICE_URL")
    client = ApiClient(settings.notification_service_url, timeout=settings.timeout)
    message_id = assert_success(
        client.post(
            "/notification/send",
            json={
                "userId": tenant_client.user_id,
                "templateCode": "ORDER_CREATED",
                "params": {"houseName": "接口自动化房源", "orderNo": "API-TEST", "startDate": "2026-09-01", "endDate": "2026-12-01", "amount": "3500"},
                "bizId": 999999999,
            },
        )
    )
    try:
        assert_success(client.get(f"/notification/{message_id}", params={"userId": tenant_client.user_id}))
        assert_success(client.post(f"/notification/read/{message_id}", params={"userId": tenant_client.user_id}))
    finally:
        assert_success(client.delete(f"/notification/{message_id}"))


@allure.feature("通知服务")
@pytest.mark.maintenance
def test_notification_maintenance_routes(settings):
    if not settings.notification_service_url:
        pytest.skip("缺少 NOTIFICATION_SERVICE_URL")
    client = ApiClient(settings.notification_service_url, timeout=settings.timeout)
    assert_success(client.post("/notification/test/insert"))
    assert_success(client.post("/notification/fix-templates"))
