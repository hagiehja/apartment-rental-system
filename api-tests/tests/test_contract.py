import allure
import pytest

from utils.api_client import ApiClient
from utils.assertions import assert_rejected, assert_success


def _contract_id(settings):
    if not settings.test_contract_id:
        pytest.skip("缺少 TEST_CONTRACT_ID，合同资源由 E2E 支付后生成")
    return settings.test_contract_id


@allure.feature("合同服务")
@pytest.mark.smoke
def test_contract_health(anonymous_client):
    data = assert_success(anonymous_client.get("/api/contract/health"))
    assert data["status"] == "UP"


@allure.feature("合同服务")
@pytest.mark.business
def test_contract_detail_order_list_and_download(settings, tenant_client):
    contract_id = _contract_id(settings)
    detail = assert_success(tenant_client.get(f"/api/contract/{contract_id}"))
    order_id = detail["orderId"]
    assert_success(tenant_client.get(f"/api/contract/order/{order_id}"))
    result = assert_success(
        tenant_client.get(
            "/api/contract/list",
            params={"userId": tenant_client.user_id, "userType": "TENANT", "page": 1, "size": 10},
        )
    )
    assert "records" in result
    response = tenant_client.get(f"/api/contract/download/{contract_id}")
    assert response.status_code == 200


@allure.feature("合同服务")
@pytest.mark.business
@pytest.mark.parametrize(
    "path",
    ["/api/contract/999999999", "/api/contract/order/999999999", "/api/contract/999999999/calculate-refund"],
)
def test_missing_contract_is_rejected(tenant_client, path):
    assert_rejected(tenant_client.get(path))


@allure.feature("合同服务")
@pytest.mark.business
def test_contract_list_rejects_missing_or_invalid_params(tenant_client):
    assert_rejected(tenant_client.get("/api/contract/list", params={"userType": "TENANT"}))
    assert_rejected(
        tenant_client.get(
            "/api/contract/list",
            params={"userId": tenant_client.user_id, "userType": "INVALID", "page": 0, "size": -1},
        )
    )


@allure.feature("合同服务")
@pytest.mark.business
def test_sign_contract_rejects_invalid_identity(tenant_client):
    assert_rejected(
        tenant_client.post(
            "/api/contract/sign/999999999",
            json={"userId": tenant_client.user_id, "userType": "INVALID", "signatureData": "", "ipAddress": "127.0.0.1"},
        )
    )


@allure.feature("合同服务")
@pytest.mark.destructive
def test_terminate_contract(settings, tenant_client):
    contract_id = _contract_id(settings)
    assert_success(tenant_client.post(f"/api/contract/{contract_id}/terminate", json={"reason": "接口自动化退租"}))


@allure.feature("合同服务")
@pytest.mark.internal
def test_contract_internal_routes(settings, created_order):
    if not settings.contract_service_url:
        pytest.skip("缺少 CONTRACT_SERVICE_URL")
    client = ApiClient(settings.contract_service_url, timeout=settings.timeout)
    assert_rejected(client.post("/contract/generate", json={}))
    assert_rejected(client.post(f"/contract/cancel/order/{created_order['orderId']}"))
    assert_rejected(client.post("/contract/landlord-income", params={"orderNo": "NOT-EXIST", "amount": 1}))
