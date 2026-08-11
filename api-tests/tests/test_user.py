import copy

import allure
import pytest

from data.test_data import new_user
from utils.assertions import assert_rejected, assert_success, assert_unauthorized


@allure.feature("用户服务")
@pytest.mark.smoke
def test_user_health(anonymous_client):
    data = assert_success(anonymous_client.get("/api/user/health"))
    assert "User Service" in data


@allure.feature("用户服务")
@allure.story("注册与批量查询")
@pytest.mark.auth
def test_register_user_and_batch_lookup(anonymous_client):
    payload = new_user("TENANT")
    user = assert_success(anonymous_client.post("/api/user/register", json=payload))
    assert user["username"] == payload["username"]
    result = assert_success(anonymous_client.get("/api/user/batch", params={"ids": user["userId"]}))
    record = result.get(str(user["userId"])) or result.get(user["userId"])
    assert record is not None
    assert record.get("phone") is None


@allure.feature("用户服务")
@allure.story("注册异常")
@pytest.mark.auth
@pytest.mark.parametrize(
    ("field", "value"),
    [
        pytest.param("username", "", id="empty-username"),
        pytest.param("phone", "123", id="invalid-phone"),
        pytest.param("password", "123", id="short-password"),
        pytest.param("role", "ADMIN", id="invalid-role"),
    ],
)
def test_register_rejects_invalid_fields(anonymous_client, field, value):
    payload = new_user("TENANT")
    payload[field] = value
    assert_rejected(anonymous_client.post("/api/user/register", json=payload))


@allure.feature("用户服务")
@allure.story("重复注册")
@pytest.mark.auth
def test_register_rejects_duplicate_account(anonymous_client):
    payload = new_user("TENANT")
    assert_success(anonymous_client.post("/api/user/register", json=payload))
    assert_rejected(anonymous_client.post("/api/user/register", json=payload))


@allure.feature("用户服务")
@allure.story("登录")
@pytest.mark.auth
def test_login_returns_jwt(anonymous_client, settings):
    if not settings.tenant_account or not settings.tenant_password:
        pytest.skip("缺少租客测试账号")
    data = assert_success(
        anonymous_client.post(
            "/api/user/login",
            json={"account": settings.tenant_account, "password": settings.tenant_password},
        )
    )
    assert data.get("token")
    assert data.get("userId")


@allure.feature("用户服务")
@allure.story("登录异常")
@pytest.mark.auth
def test_login_rejects_wrong_password(anonymous_client, settings):
    if not settings.tenant_account:
        pytest.skip("缺少租客测试账号")
    assert_rejected(
        anonymous_client.post(
            "/api/user/login",
            json={"account": settings.tenant_account, "password": "WrongPassword123"},
        )
    )


@allure.feature("用户服务")
@allure.story("Token 验证")
@pytest.mark.auth
def test_verify_accepts_valid_token(tenant_client):
    response = tenant_client.get("/api/user/verify")
    assert response.status_code == 200, response.text


@allure.feature("用户服务")
@allure.story("Token 验证异常")
@pytest.mark.auth
def test_verify_rejects_invalid_token(anonymous_client):
    response = anonymous_client.get(
        "/api/user/verify", headers={"Authorization": "Bearer invalid.jwt.token"}
    )
    assert_unauthorized(response)


@allure.feature("用户服务")
@allure.story("角色统计")
@pytest.mark.business
def test_user_role_stats(anonymous_client):
    data = assert_success(anonymous_client.get("/api/user/stats"))
    assert data is not None


@allure.feature("用户服务")
@allure.story("管理员")
@pytest.mark.business
def test_admin_lists_users(admin_client):
    data = assert_success(admin_client.get("/api/user/admin/list", params={"pageNum": 1, "pageSize": 10}))
    assert "records" in data


@allure.feature("用户服务")
@allure.story("管理员权限异常")
@pytest.mark.auth
def test_tenant_cannot_list_admin_users(tenant_client):
    assert_rejected(tenant_client.get("/api/user/admin/list"))


@allure.feature("用户服务")
@allure.story("管理员修改角色")
@pytest.mark.business
def test_admin_updates_and_restores_created_user_role(anonymous_client, admin_client):
    payload = new_user("TENANT")
    user = assert_success(anonymous_client.post("/api/user/register", json=copy.deepcopy(payload)))
    user_id = user["userId"]
    try:
        assert_success(admin_client.put(f"/api/user/admin/{user_id}/role", json={"role": "LANDLORD"}))
    finally:
        assert_success(admin_client.put(f"/api/user/admin/{user_id}/role", json={"role": "TENANT"}))


@allure.feature("用户服务")
@allure.story("管理员修改角色异常")
@pytest.mark.auth
def test_admin_rejects_invalid_role(admin_client):
    assert_rejected(admin_client.put("/api/user/admin/999999999/role", json={"role": "ROOT"}))
