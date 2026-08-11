import copy

import allure
import pytest

from data.test_data import new_house
from utils.api_client import ApiClient
from utils.assertions import assert_rejected, assert_success


@allure.feature("房源服务")
@pytest.mark.smoke
def test_house_health(anonymous_client):
    data = assert_success(anonymous_client.get("/api/house/health"))
    assert "House Service" in data


@allure.feature("房源服务")
@allure.story("房源列表")
@pytest.mark.smoke
def test_house_list(anonymous_client):
    data = assert_success(
        anonymous_client.get("/api/house/list", params={"pageNum": 1, "pageSize": 10})
    )
    assert "records" in data
    assert "total" in data


@allure.feature("房源服务")
@allure.story("房源列表异常")
@pytest.mark.business
@pytest.mark.parametrize(
    "params",
    [
        pytest.param({"pageNum": 0}, id="zero-page"),
        pytest.param({"pageSize": -1}, id="negative-size"),
        pytest.param({"minPrice": 5000, "maxPrice": 1000}, id="reversed-price"),
    ],
)
def test_house_list_rejects_invalid_query(anonymous_client, params):
    assert_rejected(anonymous_client.get("/api/house/list", params=params))


@allure.feature("房源服务")
@allure.story("发布与详情")
@pytest.mark.business
def test_publish_house_and_get_detail(published_house, anonymous_client):
    detail = assert_success(anonymous_client.get(f"/api/house/{published_house['houseId']}"))
    assert detail["title"] == published_house["payload"]["title"]


@allure.feature("房源服务")
@allure.story("发布异常")
@pytest.mark.business
@pytest.mark.parametrize(
    ("field", "value"),
    [
        pytest.param("title", "", id="empty-title"),
        pytest.param("city", "", id="empty-city"),
        pytest.param("address", "", id="empty-address"),
        pytest.param("price", None, id="missing-price"),
        pytest.param("price", -1, id="negative-price"),
        pytest.param("rentType", "INVALID", id="invalid-rent-type"),
    ],
)
def test_publish_rejects_invalid_payload(landlord_client, field, value):
    payload = new_house()
    payload[field] = value
    assert_rejected(landlord_client.post("/api/house", json=payload))


@allure.feature("房源服务")
@allure.story("房源详情异常")
@pytest.mark.business
def test_get_missing_house_is_rejected(anonymous_client):
    assert_rejected(anonymous_client.get("/api/house/999999999"))


@allure.feature("房源服务")
@allure.story("更新房源")
@pytest.mark.business
def test_update_house(published_house, landlord_client, anonymous_client):
    payload = copy.deepcopy(published_house["payload"])
    payload["title"] = f"{payload['title']}_已修改"
    assert_success(landlord_client.put(f"/api/house/{published_house['houseId']}", json=payload))
    detail = assert_success(anonymous_client.get(f"/api/house/{published_house['houseId']}"))
    assert detail["title"] == payload["title"]


@allure.feature("房源服务")
@allure.story("越权更新")
@pytest.mark.auth
def test_tenant_cannot_update_landlord_house(published_house, tenant_client):
    assert_rejected(
        tenant_client.put(
            f"/api/house/{published_house['houseId']}",
            json=published_house["payload"],
        )
    )


@allure.feature("房源服务")
@allure.story("上下架")
@pytest.mark.business
def test_offline_and_online_house(published_house, landlord_client):
    house_id = published_house["houseId"]
    assert_success(landlord_client.put(f"/api/house/{house_id}/offline"))
    assert_success(landlord_client.put(f"/api/house/{house_id}/online"))


@allure.feature("房源服务")
@allure.story("删除房源")
@pytest.mark.business
def test_delete_created_house(landlord_client):
    house_id = assert_success(landlord_client.post("/api/house", json=new_house()))
    assert_success(landlord_client.delete(f"/api/house/{house_id}"))
    assert_rejected(landlord_client.delete(f"/api/house/{house_id}"))


@allure.feature("房源服务")
@allure.story("内部状态更新")
@pytest.mark.internal
def test_internal_update_house_status(settings, published_house):
    if not settings.house_service_url:
        pytest.skip("缺少 HOUSE_SERVICE_URL")
    client = ApiClient(settings.house_service_url, timeout=settings.timeout)
    assert_success(
        client.put(
            f"/house/{published_house['houseId']}/status",
            params={"status": "AVAILABLE"},
        )
    )


@allure.feature("房源服务")
@allure.story("个性化推荐")
@pytest.mark.business
def test_house_recommendation(tenant_client):
    data = assert_success(
        tenant_client.get("/api/house/recommend", params={"pageNum": 1, "pageSize": 10})
    )
    assert "records" in data


@allure.feature("房源服务")
@allure.story("行为记录")
@pytest.mark.business
def test_track_behavior_and_reject_invalid_type(tenant_client, published_house):
    payload = {"houseId": published_house["houseId"], "behaviorType": "VIEW", "source": "PYTEST"}
    assert_success(tenant_client.post("/api/house/behavior/track", json=payload))
    payload["behaviorType"] = "INVALID"
    assert_rejected(tenant_client.post("/api/house/behavior/track", json=payload))


@allure.feature("房源服务")
@allure.story("用户偏好")
@pytest.mark.business
def test_save_and_get_preference(tenant_client):
    payload = {
        "city": "北京市",
        "district": "朝阳区",
        "minPrice": 2000,
        "maxPrice": 5000,
        "roomCount": 2,
        "rentType": "WHOLE",
    }
    assert_success(tenant_client.post("/api/house/preference", json=payload))
    saved = assert_success(tenant_client.get("/api/house/preference"))
    assert saved["city"] == payload["city"]


@allure.feature("房源服务")
@allure.story("推荐模型")
@pytest.mark.business
def test_recommendation_model_info(tenant_client):
    data = assert_success(tenant_client.get("/api/house/recommend/model-info"))
    assert isinstance(data, dict)
