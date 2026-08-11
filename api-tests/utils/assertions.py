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
    assert response.status_code == 401, response.text


def assert_forbidden(response) -> None:
    assert response.status_code == 403, response.text
