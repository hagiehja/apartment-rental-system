import allure
import pytest


@allure.feature("Gateway")
@allure.story("健康检查")
@allure.title("Gateway 健康检查能够识别真实公寓网关")
@pytest.mark.smoke
def test_gateway_health(anonymous_client):
    response = anonymous_client.get("/gateway/health")
    assert response.status_code == 200, response.text
    body = response.json()
    assert body.get("service") == "apartment-gateway", body
    assert body.get("status") == "UP", body


@allure.feature("Gateway")
@allure.story("路由信息")
@allure.title("登录用户可以查询 Gateway 路由")
@pytest.mark.smoke
def test_gateway_routes(tenant_client):
    response = tenant_client.get("/gateway/routes")
    assert response.status_code == 200, response.text
    body = response.json()
    assert body.get("gateway") == "apartment-gateway", body
    assert "services" in body, body
