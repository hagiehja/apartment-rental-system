import allure
import pytest

from utils.assertions import assert_rejected, assert_success


@allure.feature("支付服务")
@pytest.mark.smoke
def test_payment_health(anonymous_client):
    assert "Payment Service" in assert_success(anonymous_client.get("/api/payment/health"))


@allure.feature("支付服务")
@pytest.mark.business
def test_account_balance_and_transactions(tenant_client):
    balance = assert_success(tenant_client.get("/api/payment/account/balance"))
    assert "balance" in balance
    transactions = assert_success(tenant_client.get("/api/payment/account/transactions"))
    assert isinstance(transactions, list)


@allure.feature("支付服务")
@pytest.mark.auth
@pytest.mark.parametrize("path", ["/api/payment/account/balance", "/api/payment/account/transactions"])
def test_payment_account_requires_login(anonymous_client, path):
    assert_rejected(anonymous_client.get(path), allowed_http=(401, 403))


@allure.feature("支付服务")
@pytest.mark.business
@pytest.mark.parametrize(
    "payload",
    [
        pytest.param({}, id="empty-body"),
        pytest.param({"orderNo": "", "paymentMethod": "BALANCE"}, id="empty-order-no"),
        pytest.param({"orderNo": "NO-999", "paymentMethod": None}, id="missing-method"),
        pytest.param({"orderNo": "NO-999", "paymentMethod": "UNKNOWN"}, id="invalid-method"),
    ],
)
def test_create_payment_rejects_invalid_payload(tenant_client, payload):
    assert_rejected(tenant_client.post("/api/payment", json=payload))


@allure.feature("支付服务")
@pytest.mark.business
@pytest.mark.parametrize(
    ("path", "method"),
    [
        ("/api/payment/NOT-EXIST/pay", "POST"),
        ("/api/payment/NOT-EXIST/refund", "POST"),
        ("/api/payment/refund/order/NOT-EXIST", "POST"),
    ],
)
def test_payment_state_change_rejects_missing_resource(tenant_client, path, method):
    assert_rejected(tenant_client.request(method, path))


@allure.feature("支付服务")
@allure.story("充值、支付、退款")
@pytest.mark.destructive
def test_recharge_pay_and_refund(created_order, tenant_client):
    assert_success(tenant_client.post("/api/payment/account/recharge", json={"amount": 100000}))
    payment = assert_success(
        tenant_client.post(
            "/api/payment",
            json={"orderNo": created_order["orderNo"], "paymentMethod": "BALANCE"},
        )
    )
    payment_no = payment["paymentNo"]
    assert_success(tenant_client.post(f"/api/payment/{payment_no}/pay"))
    assert_success(tenant_client.post(f"/api/payment/{payment_no}/refund"))


@allure.feature("支付服务")
@pytest.mark.destructive
def test_recharge_rejects_non_positive_amount(tenant_client):
    assert_rejected(tenant_client.post("/api/payment/account/recharge", json={"amount": -1}))
