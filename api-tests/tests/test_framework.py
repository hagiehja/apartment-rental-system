import pytest

from config import Settings
from data.test_data import new_house, new_user
from utils.api_client import ApiClient, redact
from utils.assertions import assert_rejected, assert_success


class FakeResponse:
    status_code = 200
    text = '{"code":200,"message":"ok","data":{"value":1}}'

    def json(self):
        return {"code": 200, "message": "ok", "data": {"value": 1}}


class FakeBusinessErrorResponse(FakeResponse):
    text = '{"code":400,"message":"invalid","data":null}'

    def json(self):
        return {"code": 400, "message": "invalid", "data": None}


def test_settings_require_base_url(monkeypatch):
    monkeypatch.delenv("API_BASE_URL", raising=False)
    with pytest.raises(RuntimeError, match="API_BASE_URL"):
        Settings.from_env()


def test_settings_normalize_base_url(monkeypatch):
    monkeypatch.setenv("API_BASE_URL", "http://example.test/")
    settings = Settings.from_env()
    assert settings.base_url == "http://example.test"
    assert settings.timeout == 10.0


def test_redact_sensitive_fields():
    value = redact(
        {
            "password": "secret",
            "token": "jwt-value",
            "phone": "13800138000",
            "nested": {"Authorization": "Bearer abc"},
        }
    )
    assert value["password"] == "[REDACTED]"
    assert value["token"] == "[REDACTED]"
    assert value["phone"] == "138****8000"
    assert value["nested"]["Authorization"] == "[REDACTED]"
    assert redact("https://example.test?phone=13800138000&token=abc") == (
        "https://example.test?phone=138****8000&token=[REDACTED]"
    )


def test_http_verbs_delegate_to_request(monkeypatch):
    client = ApiClient("http://example.test", timeout=3)
    calls = []

    def fake_request(method, path, **kwargs):
        calls.append((method, path, kwargs))
        return FakeResponse()

    monkeypatch.setattr(client, "request", fake_request)
    client.get("/a")
    client.post("/b", json={"x": 1})
    client.put("/c")
    client.delete("/d")
    assert [call[0] for call in calls] == ["GET", "POST", "PUT", "DELETE"]


def test_assert_success_checks_business_code():
    assert assert_success(FakeResponse()) == {"value": 1}


def test_factories_generate_unique_values():
    first = new_user("TENANT")
    second = new_user("TENANT")
    assert first["username"] != second["username"]
    assert first["phone"] != second["phone"]
    assert new_house()["title"] != new_house()["title"]


def test_assert_rejected_accepts_business_error():
    body = assert_rejected(FakeBusinessErrorResponse())
    assert body["message"] == "invalid"


def test_client_does_not_attach_expected_http_error(monkeypatch):
    client = ApiClient("http://example.test")
    response = FakeResponse()
    response.status_code = 400
    monkeypatch.setattr(client.session, "request", lambda *args, **kwargs: response)
    attachments = []
    monkeypatch.setattr("utils.api_client.attach_response_failure", attachments.append)

    assert client.get("/negative-case").status_code == 400
    assert attachments == []
