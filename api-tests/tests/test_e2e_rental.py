import time

import allure
import pytest

from data.test_data import new_order
from utils.assertions import assert_success


def _wait_for_contract(client, order_id, attempts=10, interval=1):
    last_response = None
    for _ in range(attempts):
        last_response = client.get(f"/api/contract/order/{order_id}")
        if last_response.status_code == 200:
            body = last_response.json()
            if body.get("code") == 200 and body.get("data"):
                return body["data"]
        time.sleep(interval)
    pytest.fail(f"合同异步生成超时，最后响应：{last_response.text[:500] if last_response else '无响应'}")


@allure.feature("完整租赁流程")
@allure.story("充值、下单、支付、合同、通知")
@pytest.mark.e2e
@pytest.mark.destructive
def test_complete_rental_flow(published_house, tenant_client, landlord_client):
    with allure.step("租客创建订单"):
        order = assert_success(
            tenant_client.post("/api/order", json=new_order(published_house["houseId"]))
        )
        order_no = order["orderNo"]
        detail = assert_success(tenant_client.get(f"/api/order/{order_no}"))
        order_id = detail["orderId"]

    with allure.step("模拟充值并完成余额支付"):
        assert_success(tenant_client.post("/api/payment/account/recharge", json={"amount": 100000}))
        payment = assert_success(
            tenant_client.post(
                "/api/payment",
                json={"orderNo": order_no, "paymentMethod": "BALANCE"},
            )
        )
        assert_success(tenant_client.post(f"/api/payment/{payment['paymentNo']}/pay"))
        paid_order = assert_success(tenant_client.get(f"/api/order/{order_no}"))
        assert paid_order["paymentStatus"] == "PAID"

    with allure.step("等待合同生成并由双方签署"):
        contract = _wait_for_contract(tenant_client, order_id)
        contract_id = contract["id"]
        assert_success(
            tenant_client.post(
                f"/api/contract/sign/{contract_id}",
                json={
                    "userId": tenant_client.user_id,
                    "userType": "TENANT",
                    "signatureData": "pytest-tenant-signature",
                    "ipAddress": "127.0.0.1",
                },
            )
        )
        assert_success(
            landlord_client.post(
                f"/api/contract/sign/{contract_id}",
                json={
                    "userId": landlord_client.user_id,
                    "userType": "LANDLORD",
                    "signatureData": "pytest-landlord-signature",
                    "ipAddress": "127.0.0.1",
                },
            )
        )

    with allure.step("验证租客收到业务通知"):
        notifications = assert_success(
            tenant_client.get(
                "/api/notification/list",
                params={"userId": tenant_client.user_id, "page": 1, "size": 20},
            )
        )
        assert notifications["records"], "支付和合同流程完成后应产生通知"
