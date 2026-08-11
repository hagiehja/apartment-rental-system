from utils.api_client import attach_response_failure


def assert_http_status(response, expected: int = 200) -> None:
    if response.status_code != expected:
        attach_response_failure(response)
    assert response.status_code == expected, response.text


def assert_success(response):
    assert_http_status(response, 200)
    body = response.json()
    if body.get("code") != 200 or "data" not in body:
        attach_response_failure(response)
    assert body.get("code") == 200, body
    assert "data" in body, body
    return body["data"]


def assert_business_error(response, expected_code: int):
    assert_http_status(response, 200)
    body = response.json()
    if body.get("code") != expected_code:
        attach_response_failure(response)
    assert body.get("code") == expected_code, body
    return body


def assert_unauthorized(response) -> None:
    if response.status_code != 401:
        attach_response_failure(response)
    assert response.status_code == 401, response.text


def assert_forbidden(response) -> None:
    if response.status_code != 403:
        attach_response_failure(response)
    assert response.status_code == 403, response.text


def assert_rejected(response, allowed_http=(400, 401, 403, 404, 409, 422)):
    if response.status_code in allowed_http:
        try:
            return response.json()
        except ValueError:
            return {"message": response.text}
    if response.status_code == 200:
        body = response.json()
        if body.get("code") != 200:
            return body
    attach_response_failure(response)
    raise AssertionError(f"请求未被正确拒绝: HTTP {response.status_code} {response.text}")
