import json
import logging
import re
from typing import Any

import allure
import requests

SENSITIVE_KEYS = {
    "authorization",
    "password",
    "token",
    "access_token",
    "refresh_token",
    "jwt",
}
logger = logging.getLogger(__name__)


def redact(value: Any, key: str = "") -> Any:
    if key.lower() in SENSITIVE_KEYS:
        return "[REDACTED]"
    if key.lower() == "phone" and isinstance(value, str) and len(value) == 11:
        return f"{value[:3]}****{value[-4:]}"
    if isinstance(value, str):
        value = re.sub(r"(?<!\d)(1\d{2})\d{4}(\d{4})(?!\d)", r"\1****\2", value)
        value = re.sub(
            r"(?i)(authorization|access_token|refresh_token|token|password)=([^&\s]+)",
            r"\1=[REDACTED]",
            value,
        )
        return value
    if isinstance(value, dict):
        return {item_key: redact(item_value, item_key) for item_key, item_value in value.items()}
    if isinstance(value, (list, tuple)):
        return [redact(item) for item in value]
    return value


def _decode_json(value: Any) -> Any:
    if isinstance(value, bytes):
        value = value.decode("utf-8", errors="replace")
    if not isinstance(value, str):
        return value
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return value


def _response_body(response: requests.Response) -> Any:
    try:
        return response.json()
    except ValueError:
        return response.text


def attach_response_failure(response: requests.Response) -> None:
    request = response.request
    detail = {
        "method": request.method,
        "url": redact(request.url),
        "request": redact(
            {
                "headers": dict(request.headers),
                "body": _decode_json(request.body),
            }
        ),
        "response": redact(
            {
                "status": response.status_code,
                "body": _response_body(response),
            }
        ),
    }
    allure.attach(
        json.dumps(detail, ensure_ascii=False, indent=2, default=str),
        name="失败请求详情",
        attachment_type=allure.attachment_type.JSON,
    )


class ApiClient:
    def __init__(self, base_url: str, timeout: float = 10, token: str | None = None):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        if token:
            self.session.headers["Authorization"] = f"Bearer {token}"

    def request(self, method: str, path: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}/{path.lstrip('/')}"
        timeout = kwargs.pop("timeout", self.timeout)
        logger.info("%s %s", method, url)
        try:
            response = self.session.request(method, url, timeout=timeout, **kwargs)
        except requests.RequestException:
            detail = {
                "method": method,
                "url": redact(url),
                "request": redact(kwargs),
                "response": None,
            }
            allure.attach(
                json.dumps(detail, ensure_ascii=False, indent=2, default=str),
                name="请求异常详情",
                attachment_type=allure.attachment_type.JSON,
            )
            raise
        return response

    def get(self, path: str, **kwargs) -> requests.Response:
        return self.request("GET", path, **kwargs)

    def post(self, path: str, **kwargs) -> requests.Response:
        return self.request("POST", path, **kwargs)

    def put(self, path: str, **kwargs) -> requests.Response:
        return self.request("PUT", path, **kwargs)

    def delete(self, path: str, **kwargs) -> requests.Response:
        return self.request("DELETE", path, **kwargs)
