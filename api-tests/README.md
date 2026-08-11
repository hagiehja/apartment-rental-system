# Pytest 接口自动化

该目录使用 `pytest + requests + Allure` 覆盖 Gateway、用户、房源、订单、支付、合同和通知服务。当前共有 65 个 Controller 接口，测试同时包含正常流程与参数、鉴权、资源、归属和状态异常场景。

## 运行准备

必须提供已启动的测试环境地址，不能把 Jenkins 的 `8080` 端口当成业务网关：

```powershell
cd api-tests
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:API_BASE_URL = "http://你的测试环境"
$env:TEST_TENANT_ACCOUNT = "租客账号"
$env:TEST_TENANT_PASSWORD = "租客密码"
$env:TEST_LANDLORD_ACCOUNT = "房东账号"
$env:TEST_LANDLORD_PASSWORD = "房东密码"
$env:TEST_ADMIN_ACCOUNT = "管理员账号"
$env:TEST_ADMIN_PASSWORD = "管理员密码"
```

账号密码不要写入代码、`.env` 或 Git。网关会校验 JWT 并注入 `X-User-Id`，客户端不伪造该请求头。

## 常用命令

普通运行只覆盖安全接口和普通业务接口：

```powershell
.\.venv\Scripts\python.exe -m pytest -m "not internal and not maintenance and not destructive and not e2e"
```

先跑冒烟，再跑普通回归：

```powershell
.\.venv\Scripts\python.exe -m pytest -m smoke
.\.venv\Scripts\python.exe -m pytest -m "not smoke and not internal and not maintenance and not destructive and not e2e"
```

以下用例必须显式启用，并且只能对独立测试环境执行：

```powershell
.\.venv\Scripts\python.exe -m pytest -m internal --run-internal
.\.venv\Scripts\python.exe -m pytest -m maintenance --run-maintenance
.\.venv\Scripts\python.exe -m pytest -m "destructive and not e2e" --run-destructive
.\.venv\Scripts\python.exe -m pytest -m e2e --run-e2e
```

直连内部接口时再提供 `USER_SERVICE_URL`、`HOUSE_SERVICE_URL`、`ORDER_SERVICE_URL`、`PAYMENT_SERVICE_URL`、`CONTRACT_SERVICE_URL`、`NOTIFICATION_SERVICE_URL`。单独验证已有合同时可提供 `TEST_CONTRACT_ID`。

生成 Allure 原始结果：

```powershell
.\.venv\Scripts\python.exe -m pytest --alluredir=allure-results
allure serve allure-results
```

## Jenkins

根目录 `Jenkinsfile` 的执行顺序为：

```text
Health Check -> Smoke Gate -> Regression -> 可选 Internal/Maintenance/Destructive/E2E -> JUnit + Allure
```

Jenkins 需安装 Python 3、Allure Jenkins Plugin，并创建 6 个 Secret text Credentials：

- `api-test-tenant-account`
- `api-test-tenant-password`
- `api-test-landlord-account`
- `api-test-landlord-password`
- `api-test-admin-account`
- `api-test-admin-password`

流水线参数为 `RUN_INTERNAL`、`RUN_MAINTENANCE`、`RUN_DESTRUCTIVE`、`RUN_E2E`，默认全部关闭。

## 框架说明

- `ApiClient` 的 `get/post/put/delete` 全部调用统一 `request()`，集中处理 timeout、JWT、日志、脱敏和 Allure 失败附件。
- 请求失败或业务断言失败时才附 method、URL、请求体和响应，Token、密码、手机号等会先脱敏。
- `published_house` 和 `created_order` 使用 `yield` 调接口清理；无法清理的资金/E2E 数据使用随机唯一数据并隔离到显式开关。
- `--strict-markers` 与 `xfail_strict = true` 防止 marker 拼错或严格失败被忽略。
