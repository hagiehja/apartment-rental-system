import pytest

from config import Settings


def test_settings_require_base_url(monkeypatch):
    monkeypatch.delenv("API_BASE_URL", raising=False)
    with pytest.raises(RuntimeError, match="API_BASE_URL"):
        Settings.from_env()


def test_settings_normalize_base_url(monkeypatch):
    monkeypatch.setenv("API_BASE_URL", "http://example.test/")
    settings = Settings.from_env()
    assert settings.base_url == "http://example.test"
    assert settings.timeout == 10.0
