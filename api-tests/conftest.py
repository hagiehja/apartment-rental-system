import logging

import pytest

from config import Settings
from data.test_data import new_house, new_order
from utils.api_client import ApiClient
from utils.assertions import assert_success

logger = logging.getLogger(__name__)


def pytest_addoption(parser):
    parser.addoption("--run-internal", action="store_true", help="运行微服务内部接口")
    parser.addoption("--run-maintenance", action="store_true", help="运行数据维护接口")
    parser.addoption("--run-destructive", action="store_true", help="运行资金和关键状态接口")
    parser.addoption("--run-e2e", action="store_true", help="运行完整租赁端到端流程")


def pytest_collection_modifyitems(config, items):
    enabled = {
        "internal": config.getoption("--run-internal"),
        "maintenance": config.getoption("--run-maintenance"),
        "destructive": config.getoption("--run-destructive"),
        "e2e": config.getoption("--run-e2e"),
    }
    for item in items:
        if "e2e" in item.keywords and not enabled["e2e"]:
            item.add_marker(pytest.mark.skip(reason="需要 --run-e2e 显式启用"))
            continue
        if "destructive" in item.keywords and not (enabled["destructive"] or enabled["e2e"]):
            item.add_marker(pytest.mark.skip(reason="需要 --run-destructive 显式启用"))
        if "internal" in item.keywords and not enabled["internal"]:
            item.add_marker(pytest.mark.skip(reason="需要 --run-internal 显式启用"))
        if "maintenance" in item.keywords and not enabled["maintenance"]:
            item.add_marker(pytest.mark.skip(reason="需要 --run-maintenance 显式启用"))


@pytest.fixture(scope="session")
def settings() -> Settings:
    return Settings.from_env()


@pytest.fixture(scope="session")
def anonymous_client(settings) -> ApiClient:
    return ApiClient(settings.base_url, timeout=settings.timeout)


def _login(settings: Settings, account: str | None, password: str | None, role: str) -> ApiClient:
    if not account or not password:
        pytest.skip(f"缺少 {role} 测试账号环境变量")
    client = ApiClient(settings.base_url, timeout=settings.timeout)
    response = client.post("/api/user/login", json={"account": account, "password": password})
    user_info = assert_success(response)
    token = user_info.get("token")
    if not token:
        pytest.fail(f"{role} 登录响应缺少 token")
    authenticated = ApiClient(settings.base_url, timeout=settings.timeout, token=token)
    authenticated.user_info = user_info
    authenticated.user_id = user_info.get("userId")
    return authenticated


@pytest.fixture(scope="session")
def tenant_client(settings) -> ApiClient:
    return _login(settings, settings.tenant_account, settings.tenant_password, "TENANT")


@pytest.fixture(scope="session")
def landlord_client(settings) -> ApiClient:
    return _login(settings, settings.landlord_account, settings.landlord_password, "LANDLORD")


@pytest.fixture(scope="session")
def admin_client(settings) -> ApiClient:
    return _login(settings, settings.admin_account, settings.admin_password, "ADMIN")


@pytest.fixture
def published_house(landlord_client):
    payload = new_house()
    house_id = assert_success(landlord_client.post("/api/house", json=payload))
    resource = {"houseId": house_id, "payload": payload}
    try:
        yield resource
    finally:
        try:
            response = landlord_client.delete(f"/api/house/{house_id}")
            body = response.json() if response.headers.get("content-type", "").startswith("application/json") else {}
            if response.status_code != 200 or body.get("code") != 200:
                logger.warning("测试房源清理失败 houseId=%s status=%s body=%s", house_id, response.status_code, body)
        except Exception as exc:  # teardown 不覆盖原始测试结果
            logger.warning("测试房源清理异常 houseId=%s error=%s", house_id, exc)


@pytest.fixture
def created_order(tenant_client, published_house):
    payload = new_order(published_house["houseId"])
    result = assert_success(tenant_client.post("/api/order", json=payload))
    order_no = result["orderNo"]
    detail = assert_success(tenant_client.get(f"/api/order/{order_no}"))
    resource = {
        "orderNo": order_no,
        "orderId": detail["orderId"],
        "detail": detail,
        "payload": payload,
    }
    try:
        yield resource
    finally:
        try:
            response = tenant_client.put(
                f"/api/order/{order_no}/cancel",
                json={"cancelReason": "pytest cleanup"},
            )
            body = response.json() if response.headers.get("content-type", "").startswith("application/json") else {}
            if response.status_code != 200 or body.get("code") != 200:
                logger.warning("测试订单清理失败 orderNo=%s status=%s body=%s", order_no, response.status_code, body)
        except Exception as exc:  # teardown 不覆盖原始测试结果
            logger.warning("测试订单清理异常 orderNo=%s error=%s", order_no, exc)
