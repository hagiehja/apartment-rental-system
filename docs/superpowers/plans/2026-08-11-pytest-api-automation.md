# Pytest API Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 pytest、requests、Allure 和 Jenkins，为 Gateway 及六个业务微服务建立覆盖正常与异常场景、默认安全、可生成报告的接口自动化测试。

**Architecture:** 测试代码放在独立的 `api-tests` 目录，通过一个 `ApiClient` 统一发送请求、脱敏并在失败时附加 Allure 信息；`conftest.py` 提供认证客户端和 API 级资源清理 Fixture。Jenkins 只面向已部署环境运行 health、smoke、普通回归和显式启用的特殊测试，不负责启动业务容器。

**Tech Stack:** Python 3、pytest、requests、allure-pytest、JUnit XML、Jenkins Pipeline

---

## 文件结构

- Create: `api-tests/requirements.txt`：固定测试依赖。
- Create: `api-tests/pytest.ini`：注册 marker，启用严格 marker 和严格 xfail。
- Create: `api-tests/config.py`：读取 Base URL、超时、账号及可选直连地址。
- Create: `api-tests/utils/api_client.py`：统一 HTTP 请求、日志、脱敏和失败附件。
- Create: `api-tests/utils/assertions.py`：统一 HTTP 与业务响应断言。
- Create: `api-tests/data/test_data.py`：生成随机唯一的用户、房源和订单数据。
- Create: `api-tests/conftest.py`：登录 Fixture、角色客户端和 API 资源清理。
- Create: `api-tests/tests/test_gateway.py`：Gateway health、routes 和目标识别。
- Create: `api-tests/tests/test_user.py`：用户公开、认证、管理员和权限接口。
- Create: `api-tests/tests/test_house.py`：房源 CRUD、状态、推荐、偏好和行为接口。
- Create: `api-tests/tests/test_order.py`：订单业务接口及内部接口外部 403 防护。
- Create: `api-tests/tests/test_payment.py`：余额、流水、充值、支付和退款。
- Create: `api-tests/tests/test_contract.py`：合同查询、签署、下载和退租。
- Create: `api-tests/tests/test_notification.py`：通知查询、已读、内部和维护接口。
- Create: `api-tests/tests/test_rental_flow.py`：完整租赁 E2E。
- Create: `api-tests/tests/test_framework.py`：框架单元测试。
- Create: `api-tests/README.md`：本地与 Jenkins 使用说明。
- Create: `Jenkinsfile`：health、smoke、回归、可选测试、JUnit 和 Allure。
- Modify: `.gitignore`：忽略虚拟环境和报告产物。

### Task 1: 建立 pytest 配置和环境读取

**Files:**
- Create: `api-tests/requirements.txt`
- Create: `api-tests/pytest.ini`
- Create: `api-tests/config.py`
- Create: `api-tests/tests/test_framework.py`
- Modify: `.gitignore`

- [ ] **Step 1: 写配置失败测试**

在 `api-tests/tests/test_framework.py` 写入：

```python
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
```

- [ ] **Step 2: 运行并确认失败原因正确**

Run:

```powershell
$env:PYTHONPATH='api-tests'; python -m pytest api-tests/tests/test_framework.py -q
```

Expected: FAIL，错误为 `ModuleNotFoundError: No module named 'config'`。

- [ ] **Step 3: 添加最小依赖、pytest 配置和 Settings**

`api-tests/requirements.txt`：

```text
pytest>=8,<10
requests>=2.32,<3
allure-pytest>=2.13,<3
```

`api-tests/pytest.ini`：

```ini
[pytest]
addopts = --strict-markers
xfail_strict = true
testpaths = tests
markers =
    smoke: 健康检查和核心冒烟接口
    auth: 登录、Token 和权限接口
    business: 普通业务接口
    internal: 仅供微服务内部调用的接口
    maintenance: 数据插入、修复等维护接口
    destructive: 修改资金或关键业务状态的接口
    e2e: 完整租赁端到端流程
```

`api-tests/config.py`：

```python
from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    base_url: str
    timeout: float
    tenant_account: str | None
    tenant_password: str | None
    landlord_account: str | None
    landlord_password: str | None
    admin_account: str | None
    admin_password: str | None

    @classmethod
    def from_env(cls) -> "Settings":
        base_url = os.getenv("API_BASE_URL", "").strip().rstrip("/")
        if not base_url:
            raise RuntimeError("API_BASE_URL is required")
        return cls(
            base_url=base_url,
            timeout=float(os.getenv("API_TIMEOUT", "10")),
            tenant_account=os.getenv("TEST_TENANT_ACCOUNT"),
            tenant_password=os.getenv("TEST_TENANT_PASSWORD"),
            landlord_account=os.getenv("TEST_LANDLORD_ACCOUNT"),
            landlord_password=os.getenv("TEST_LANDLORD_PASSWORD"),
            admin_account=os.getenv("TEST_ADMIN_ACCOUNT"),
            admin_password=os.getenv("TEST_ADMIN_PASSWORD"),
        )
```

向 `.gitignore` 追加：

```text
api-tests/.venv/
api-tests/allure-results/
api-tests/allure-report/
api-tests/junit-*.xml
api-tests/.pytest_cache/
api-tests/**/__pycache__/
```

- [ ] **Step 4: 运行配置测试**

Run:

```powershell
$env:PYTHONPATH='api-tests'; python -m pytest api-tests/tests/test_framework.py -q
```

Expected: `2 passed`。

- [ ] **Step 5: 提交基础配置**

```powershell
git add .gitignore api-tests/requirements.txt api-tests/pytest.ini api-tests/config.py api-tests/tests/test_framework.py
git commit -m "test: add api automation configuration"
```

### Task 2: 实现统一 ApiClient、脱敏和断言

**Files:**
- Create: `api-tests/utils/__init__.py`
- Create: `api-tests/utils/api_client.py`
- Create: `api-tests/utils/assertions.py`
- Modify: `api-tests/tests/test_framework.py`

- [ ] **Step 1: 添加失败测试**

在 `test_framework.py` 追加：

```python
from utils.api_client import ApiClient, redact
from utils.assertions import assert_success


class FakeResponse:
    status_code = 200

    def json(self):
        return {"code": 200, "message": "ok", "data": {"value": 1}}


def test_redact_sensitive_fields():
    value = redact({
        "password": "secret",
        "token": "jwt-value",
        "phone": "13800138000",
        "nested": {"Authorization": "Bearer abc"},
    })
    assert value["password"] == "[REDACTED]"
    assert value["token"] == "[REDACTED]"
    assert value["phone"] == "138****8000"
    assert value["nested"]["Authorization"] == "[REDACTED]"


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
```

- [ ] **Step 2: 运行并确认缺少实现**

Run: `$env:PYTHONPATH='api-tests'; python -m pytest api-tests/tests/test_framework.py -q`

Expected: FAIL，缺少 `utils.api_client`。

- [ ] **Step 3: 实现最小公共组件**

`api-tests/utils/api_client.py`：

```python
import json
import logging
from typing import Any

import allure
import requests

SENSITIVE = {"authorization", "password", "token", "access_token", "refresh_token"}
logger = logging.getLogger(__name__)


def redact(value: Any, key: str = "") -> Any:
    if key.lower() in SENSITIVE:
        return "[REDACTED]"
    if key.lower() == "phone" and isinstance(value, str) and len(value) == 11:
        return f"{value[:3]}****{value[-4:]}"
    if isinstance(value, dict):
        return {k: redact(v, k) for k, v in value.items()}
    if isinstance(value, list):
        return [redact(item) for item in value]
    return value


def _response_body(response) -> Any:
    try:
        return response.json()
    except ValueError:
        return response.text


def attach_response_failure(response) -> None:
    request = response.request
    detail = {
        "method": request.method,
        "url": request.url,
        "request": redact({"headers": dict(request.headers), "body": request.body}),
        "response": redact({
            "status": response.status_code,
            "body": _response_body(response),
        }),
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

    def request(self, method: str, path: str, **kwargs):
        url = f"{self.base_url}/{path.lstrip('/')}"
        timeout = kwargs.pop("timeout", self.timeout)
        logger.info("%s %s", method, url)
        try:
            response = self.session.request(method, url, timeout=timeout, **kwargs)
        except requests.RequestException:
            self._attach_failure(method, url, kwargs, None)
            raise
        if response.status_code >= 400:
            attach_response_failure(response)
        return response

    def _attach_failure(self, method: str, url: str, kwargs: dict, response) -> None:
        detail = {
            "method": method,
            "url": url,
            "request": redact(kwargs),
            "response": None,
        }
        allure.attach(
            json.dumps(detail, ensure_ascii=False, indent=2, default=str),
            name="失败请求详情",
            attachment_type=allure.attachment_type.JSON,
        )

    def get(self, path: str, **kwargs):
        return self.request("GET", path, **kwargs)

    def post(self, path: str, **kwargs):
        return self.request("POST", path, **kwargs)

    def put(self, path: str, **kwargs):
        return self.request("PUT", path, **kwargs)

    def delete(self, path: str, **kwargs):
        return self.request("DELETE", path, **kwargs)
```

`api-tests/utils/assertions.py`：

```python
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


def assert_unauthorized(response):
    assert response.status_code == 401, response.text


def assert_forbidden(response):
    assert response.status_code == 403, response.text
```

- [ ] **Step 4: 运行框架测试并提交**

Run: `$env:PYTHONPATH='api-tests'; python -m pytest api-tests/tests/test_framework.py -q`

Expected: `5 passed`。

```powershell
git add api-tests/utils api-tests/tests/test_framework.py
git commit -m "test: add reusable api client and assertions"
```

### Task 3: 添加数据工厂和认证 Fixture

**Files:**
- Create: `api-tests/data/__init__.py`
- Create: `api-tests/data/test_data.py`
- Create: `api-tests/conftest.py`
- Modify: `api-tests/tests/test_framework.py`

- [ ] **Step 1: 先测试唯一数据生成**

```python
from data.test_data import new_user, new_house


def test_factories_generate_unique_values():
    first = new_user("TENANT")
    second = new_user("TENANT")
    assert first["username"] != second["username"]
    assert first["phone"] != second["phone"]
    assert new_house()["title"] != new_house()["title"]
```

Run: `$env:PYTHONPATH='api-tests'; python -m pytest api-tests/tests/test_framework.py -q`

Expected: FAIL，缺少 `data.test_data`。

- [ ] **Step 2: 实现数据工厂**

```python
from datetime import datetime, timedelta
import random
import uuid


def _suffix() -> str:
    return uuid.uuid4().hex[:8]


def new_user(role: str) -> dict:
    number = random.randint(0, 99_999_999)
    return {
        "username": f"api_{role.lower()}_{_suffix()}",
        "phone": f"199{number:08d}",
        "password": "ApiTest123",
        "role": role,
    }


def new_house() -> dict:
    suffix = _suffix()
    return {
        "title": f"接口自动化测试房源_{suffix}",
        "description": "pytest 创建，可在测试结束后删除",
        "province": "北京市",
        "city": "北京市",
        "district": "朝阳区",
        "address": f"测试路{suffix}号",
        "area": 60,
        "roomCount": 2,
        "hallCount": 1,
        "bathroomCount": 1,
        "floor": 5,
        "totalFloor": 18,
        "orientation": "SOUTH",
        "decoration": "FINE",
        "rentType": "WHOLE",
        "price": 3500,
        "paymentMethod": "MONTHLY",
        "facilities": ["WIFI", "AIR_CONDITIONER"],
        "imageUrls": [],
        "coverImageIndex": 0,
    }


def new_order(house_id: int) -> dict:
    return {
        "houseId": house_id,
        "rentStartDate": (datetime.now().date() + timedelta(days=7)).isoformat(),
        "rentMonths": 3,
        "installmentEnabled": False,
        "remark": "pytest e2e",
    }
```

- [ ] **Step 3: 实现基础 Fixture**

`conftest.py` 实现 `settings`、`anonymous_client`、三类账号登录客户端和 `published_house`。缺少对应账号时使用 `pytest.skip`；`published_house` 用 `yield` 返回 ID，结束后调用 `DELETE /api/house/{id}`，清理失败时记录 warning 而不覆盖原始测试失败。

- [ ] **Step 4: 验证单元测试和收集**

```powershell
$env:API_BASE_URL='http://127.0.0.1:9'
$env:PYTHONPATH='api-tests'
python -m pytest api-tests/tests/test_framework.py -q
python -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -q
```

Expected: 工厂测试通过；收集阶段不访问网络。

- [ ] **Step 5: 提交 Fixture**

```powershell
git add api-tests/data api-tests/conftest.py api-tests/tests/test_framework.py
git commit -m "test: add api fixtures and test data factories"
```

### Task 4: 添加 Gateway、用户和房源测试

**Files:**
- Create: `api-tests/tests/test_gateway.py`
- Create: `api-tests/tests/test_user.py`
- Create: `api-tests/tests/test_house.py`

- [ ] **Step 1: 写 Gateway smoke 测试并验证当前环境失败**

```python
import allure
import pytest


@allure.feature("Gateway")
@pytest.mark.smoke
def test_gateway_health(anonymous_client):
    response = anonymous_client.get("/gateway/health")
    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "apartment-gateway"
    assert body["status"] == "UP"
```

Run: `python -m pytest -c api-tests/pytest.ini api-tests/tests/test_gateway.py -q`

Expected: 当前没有可用 Gateway 时 FAIL，且不能把 Jenkins 响应识别为通过。

- [ ] **Step 2: 添加用户接口用例**

覆盖 health、register、login、verify、batch、stats、admin list、admin role update。异常场景使用参数化覆盖注册缺少必填项、非法手机号、短密码、非法角色、重复账号，登录空账号/错误密码，verify 缺少或伪造 Token，batch 空 ID/不存在 ID，以及未登录和非管理员访问管理接口。注册数据使用 `new_user()`；管理员修改角色只针对本次测试创建的用户，并在结束前恢复原角色。

- [ ] **Step 3: 添加房源接口用例**

覆盖 publish、list、detail、update、offline、online、delete、recommend、behavior track、preference save/get、model-info。异常场景覆盖发布缺少标题/城市/地址/价格、非法租赁类型、负数价格，列表非法分页和价格区间，查询不存在房源，未登录修改，非房东本人修改/删除，重复上下架，不存在用户推荐，以及行为类型非法。房源 CRUD 使用 `published_house` Fixture，删除用例自行创建独立房源，避免与其他用例共享状态。

- [ ] **Step 4: 做静态收集验证并提交**

Run:

```powershell
$env:API_BASE_URL='http://127.0.0.1:9'
$env:PYTHONPATH='api-tests'
python -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -q
```

Expected: 无未知 marker、导入错误或重复测试名。

```powershell
git add api-tests/tests/test_gateway.py api-tests/tests/test_user.py api-tests/tests/test_house.py
git commit -m "test: cover gateway user and house apis"
```

### Task 5: 添加订单、支付、合同和通知测试

**Files:**
- Create: `api-tests/tests/test_order.py`
- Create: `api-tests/tests/test_payment.py`
- Create: `api-tests/tests/test_contract.py`
- Create: `api-tests/tests/test_notification.py`

- [ ] **Step 1: 添加普通业务接口**

订单覆盖 create、detail、my list、landlord list、cancel；支付覆盖 balance 和 transactions；合同覆盖 detail、order query、list、sign、download、refund preview；通知覆盖 unread count、unread list、paged list、detail、mark read、mark all read 和 delete。

异常场景至少覆盖：订单缺少房源、过去日期、租期 0/25、不存在房源、未登录、越权查询、重复取消；支付缺少订单号/方式、不存在支付单、余额不足、重复支付、未支付退款、重复退款；合同不存在、错误签署角色、重复签署、越权退租；通知不存在、其他用户读取/删除、重复已读。涉及资金或关键状态的异常场景继续标记 `destructive`，不进入普通回归。

- [ ] **Step 2: 隔离特殊接口**

对充值、支付、退款和退租添加 `@pytest.mark.destructive`；对订单回调、合同生成/取消/收入、通知发送添加 `@pytest.mark.internal`；对通知测试数据插入和模板修复添加 `@pytest.mark.maintenance`。

通过 Nginx 调用订单内部接口的安全测试属于普通回归，断言 HTTP 403。直连功能测试只有在提供对应服务直连地址时执行，否则按用例跳过。

- [ ] **Step 3: 验证 marker 选择不包含危险用例**

Run:

```powershell
$env:API_BASE_URL='http://127.0.0.1:9'
$env:PYTHONPATH='api-tests'
python -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -m "not internal and not maintenance and not destructive and not e2e" -q
python -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -m "destructive and not e2e" -q
python -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -m "e2e" -q
```

Expected: 三组集合互不把 E2E 重复计入 destructive；未知 marker 为失败。

- [ ] **Step 4: 提交服务测试**

```powershell
git add api-tests/tests/test_order.py api-tests/tests/test_payment.py api-tests/tests/test_contract.py api-tests/tests/test_notification.py
git commit -m "test: cover order payment contract and notification apis"
```

### Task 6: 添加完整租赁 E2E

**Files:**
- Create: `api-tests/tests/test_rental_flow.py`

- [ ] **Step 1: 写 E2E 用例**

用例同时标记 `e2e` 和 `destructive`，按房东发布房源、租客查询、创建订单、充值、创建支付单、支付、查询订单、查询/签署合同和查询通知的顺序执行。每一步用 `allure.step` 命名，并从上一步响应提取 `houseId`、`orderNo`、`paymentNo` 和 `contractId`。

- [ ] **Step 2: 添加清理和依赖失败说明**

在 `try/finally` 或 Fixture teardown 中优先删除尚可删除的房源；不可逆订单和交易使用唯一数据。异步合同或通知未出现时，断言错误必须包含缺失阶段，不使用无条件 sleep，也不伪造通过。

在主流程之外增加关键状态异常断言：已占用或已下架房源不能再次正常下单、已支付订单不能重复支付、已经退款的支付单不能再次退款。所有这些断言都保留 `e2e` 与 `destructive` 标记。

- [ ] **Step 3: 收集验证并提交**

Run: `python -m pytest -c api-tests/pytest.ini api-tests/tests/test_rental_flow.py --collect-only -q`

Expected: E2E 只收集一条主流程，marker 已注册。

```powershell
git add api-tests/tests/test_rental_flow.py
git commit -m "test: add rental workflow api automation"
```

### Task 7: 编写 Jenkinsfile 与 Allure 发布

**Files:**
- Create: `Jenkinsfile`
- Create: `api-tests/README.md`

- [ ] **Step 1: 写 Jenkinsfile 静态失败检查**

先在计划执行时使用 PowerShell `Select-String` 检查尚不存在的 `Jenkinsfile`，确认缺少 `RUN_E2E`、Smoke Gate、JUnit 和 Allure 发布。

- [ ] **Step 2: 创建 Pipeline**

Jenkinsfile 必须包含参数 `API_BASE_URL`、`RUN_INTERNAL`、`RUN_MAINTENANCE`、`RUN_DESTRUCTIVE`、`RUN_E2E`，并按以下阶段执行：Checkout、Python Toolchain、Install、Health Gate、Smoke Gate、Regression、Optional Tests。可选 destructive 使用 `destructive and not e2e`，E2E 使用 `e2e`。

账号通过 `withCredentials` 注入；每个 pytest 阶段将结果追加到同一个 `allure-results` 目录，并生成不同的 JUnit 文件。`post { always { ... } }` 中执行 `junit` 和 `allure`。

- [ ] **Step 3: 写使用说明**

README 说明本地虚拟环境、环境变量、默认安全命令、特殊 marker 命令、Allure 查看方法、Jenkins Credentials ID 和当前不启动 Docker 的边界。不得包含真实密码或 Token。

- [ ] **Step 4: 静态验证并提交**

Run:

```powershell
Select-String -Path Jenkinsfile -Pattern 'RUN_E2E','Health Gate','Smoke Gate','destructive and not e2e','junit','allure'
git diff --check
```

Expected: 所有关键项均匹配，格式检查退出 0。

```powershell
git add Jenkinsfile api-tests/README.md
git commit -m "ci: run api automation with Allure"
```

### Task 8: 完整静态验证与真实环境验收

**Files:**
- Verify: `api-tests/`
- Verify: `Jenkinsfile`

- [ ] **Step 1: 安装依赖并运行框架单元测试**

```powershell
python -m venv api-tests/.venv
api-tests\.venv\Scripts\python.exe -m pip install -r api-tests/requirements.txt
$env:API_BASE_URL='http://127.0.0.1:9'
$env:PYTHONPATH='api-tests'
api-tests\.venv\Scripts\python.exe -m pytest api-tests/tests/test_framework.py -q
```

Expected: 框架单元测试全部通过。

- [ ] **Step 2: 验证收集和安全选择**

```powershell
api-tests\.venv\Scripts\python.exe -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -q
api-tests\.venv\Scripts\python.exe -m pytest -c api-tests/pytest.ini api-tests/tests --collect-only -m "not internal and not maintenance and not destructive and not e2e" -q
```

Expected: 全部用例可收集，无未知 marker；默认集合不含特殊用例。

同时生成一份按 Controller 方法维护的覆盖清单，核对源码统计的 65 个接口均能映射到至少一个 pytest 用例；对参数化异常场景记录用例 ID，使 Allure 中能够区分失败参数。

- [ ] **Step 3: 在真实测试环境执行 health 和 smoke**

设置真实 `API_BASE_URL` 后运行：

```powershell
api-tests\.venv\Scripts\python.exe -m pytest -c api-tests/pytest.ini api-tests/tests -m smoke -q --alluredir=api-tests/allure-results --clean-alluredir --junitxml=api-tests/junit-smoke.xml
```

Expected: 只有真实 Gateway 返回 `service=apartment-gateway`、`status=UP` 后才能通过。

- [ ] **Step 4: 在独立测试环境执行普通回归和可选流程**

先运行默认安全集合；只有用户明确提供测试环境和测试账号后，才运行 destructive/E2E。记录实际 passed、failed、skipped 数量，不把收集成功当成接口通过。

- [ ] **Step 5: 验证 Jenkins 发布**

在安装 Python、Allure Plugin 和 Allure Commandline 的 Jenkins Agent 上运行一次 Pipeline，确认 Smoke Gate、JUnit 和 Allure 页面真实出现。当前 Jenkins 或网关不可用时如实记录阻塞，不声称流水线通过。

- [ ] **Step 6: 最终范围检查**

```powershell
git diff --check
git status --short
git log -8 --oneline
```

Expected: 只包含本计划列出的接口自动化、Jenkinsfile、文档及相关提交。
