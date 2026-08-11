# Pytest 接口自动化与 Jenkins Allure 报告设计

## 1. 目标

在不引入复杂测试平台的前提下，使用 Python、pytest 和 requests 为公寓租赁系统建立一套可重复执行的接口自动化测试。测试覆盖 Gateway、用户、房源、订单、支付、合同和通知服务的正常与异常场景，并通过 Jenkins 自动运行、发布 Allure 报告。

这套测试以简历展示和面试讲解为重要目标，因此只保留容易理解、能够实际运行的能力：公共请求客户端、登录 Fixture、动态测试数据、统一断言、pytest 标记、端到端业务流程和 Jenkins 流水线。

## 2. 执行边界

- Jenkins 不负责启动或部署公寓系统，只测试已经启动的测试环境。
- 被测入口由 `API_BASE_URL` 指定，正式运行前必须先通过 `GET /gateway/health`。
- 测试账号密码通过本地环境变量或 Jenkins Credentials 注入，不写入仓库。
- 普通运行只覆盖安全接口和普通业务接口；E2E、资金类和关键状态修改用例必须显式启用。
- 安全的异常用例属于普通回归；会产生资金变化、订单状态变化或不可逆数据的异常用例继续受 `destructive` 或 `e2e` 标记约束。
- 内部回调接口默认只验证其无法从 Nginx 外部访问；直连微服务的功能测试使用 `internal` 标记并显式启用。
- 数据修复和测试数据插入接口使用 `maintenance` 标记，默认不运行。
- 充值、支付、退款和退租等会修改资金或关键状态的用例使用 `destructive` 标记，只允许在独立测试环境中执行。

## 3. 技术栈

- Python 3
- pytest
- requests
- allure-pytest
- pytest-html 不纳入第一版，避免与 Allure 重复
- Jenkins Pipeline
- Jenkins Allure Plugin
- JUnit XML，作为 Jenkins 的基础测试结果和失败门禁

## 4. 文件结构

```text
api-tests/
├── requirements.txt
├── pytest.ini
├── config.py
├── conftest.py
├── utils/
│   ├── api_client.py
│   └── assertions.py
├── data/
│   └── test_data.py
└── tests/
    ├── test_gateway.py
    ├── test_user.py
    ├── test_house.py
    ├── test_order.py
    ├── test_payment.py
    ├── test_contract.py
    ├── test_notification.py
    └── test_rental_flow.py

Jenkinsfile
```

不为每个微服务建立额外 Client 类，也不引入 YAML 数据驱动、JSON Schema 文件或数据库清理框架。接口数量继续增长后再考虑拆分。

## 5. 公共组件

### 5.1 配置

`config.py` 从环境变量读取：

- `API_BASE_URL`
- `API_TIMEOUT`
- `TEST_TENANT_ACCOUNT`、`TEST_TENANT_PASSWORD`
- `TEST_LANDLORD_ACCOUNT`、`TEST_LANDLORD_PASSWORD`
- `TEST_ADMIN_ACCOUNT`、`TEST_ADMIN_PASSWORD`
- 可选的各微服务直连地址，供 `internal` 测试使用

没有提供 `API_BASE_URL` 时，测试在收集阶段给出明确错误，不默认使用 `127.0.0.1:8080`，防止误测 Jenkins。

### 5.2 HTTP 客户端

`ApiClient` 基于 `requests.Session`，内部只实现一个公共 `request()` 方法，`get()`、`post()`、`put()` 和 `delete()` 都调用该方法。`request()` 统一处理：

- Base URL 拼接
- JWT `Authorization: Bearer ...` 请求头
- 默认超时
- 请求日志
- 请求与响应脱敏
- Allure 失败附件

正常请求只在 Allure 中保留步骤、状态和耗时，不重复附加大段 JSON。请求失败或断言失败时，附件包含 method、URL、请求体和响应内容；Authorization、Token、密码、手机号等字段必须先脱敏。

客户端不主动发送 `X-User-Id`。经过 Nginx 时，由 Nginx 验证 JWT 并注入真实用户 ID。

### 5.3 统一断言

`assertions.py` 至少提供：

- `assert_http_status(response, expected)`
- `assert_success(response)`：同时检查 HTTP 200、业务 `code` 和 `data` 字段
- `assert_business_error(response, expected_code)`
- `assert_unauthorized(response)`
- `assert_forbidden(response)`

项目可能以 HTTP 200 返回业务失败，因此测试不能只检查 HTTP 状态码。

### 5.4 Fixture 与测试数据

`conftest.py` 提供匿名、租客、房东和管理员客户端，并负责登录后提取 Token。账号型 Fixture 为 session 级；房源、订单和支付单等依赖型 Fixture 为 function 级，避免用例之间共享可变业务状态。

`test_data.py` 根据当前时间和随机后缀生成用户名、手机号、房源标题等唯一数据，防止重复运行时发生冲突。

测试数据优先通过 API 清理，不引入数据库清理框架。会创建资源的 Fixture 使用 `yield`：准备阶段创建房源等测试资源，用例执行完成后在清理阶段调用对应删除接口。对于没有删除接口、已经进入不可逆状态或清理失败的数据，使用随机唯一值避免污染后续运行，并在测试日志中记录未清理资源标识。

## 6. 接口覆盖

### 6.1 Gateway

- 健康检查
- 路由信息

### 6.2 用户服务

- 健康检查、注册、登录、错误密码、JWT 验证
- 批量用户信息、角色统计
- 管理员用户列表和角色修改
- 未登录和非管理员访问验证

### 6.3 房源服务

- 发布、列表、详情、修改、删除
- 上架、下架和状态更新
- 推荐、行为记录、用户偏好和模型信息
- 未登录访问受保护接口验证

### 6.4 订单服务

- 创建订单、订单详情、租客订单列表、房东订单列表、取消订单
- 分期计划、支付校验、支付成功和退款成功回调划入 `internal`
- Nginx 对内部接口的 403 防护纳入普通安全测试

### 6.5 支付服务

- 余额、交易流水、充值、创建支付单、执行支付和退款
- 充值、支付、退款标记为 `destructive`

### 6.6 合同服务

- 合同详情、按订单查询、合同列表、签署、下载、退租和退款预览
- 合同生成、取消和房东收入记录划入 `internal`
- 退租标记为 `destructive`

### 6.7 通知服务

- 未读数量、未读列表、分页列表、详情、标记已读、全部已读和删除
- 发送通知划入 `internal`
- 插入测试消息和修复模板划入 `maintenance`

### 6.8 端到端流程

端到端用例按顺序执行：

```text
房东与租客登录
  -> 房东发布房源
  -> 租客查询房源和详情
  -> 租客创建订单
  -> 充值并创建支付单
  -> 完成支付
  -> 查询订单状态
  -> 查询并签署合同
  -> 查询通知
```

如果测试环境中的异步消息尚未连通，端到端用例必须准确失败或跳过并说明缺少的依赖，不能把部分流程写成完整通过。

### 6.9 异常用例矩阵

每个接口至少覆盖正常主路径，并根据接口输入和权限补充以下适用的异常场景：

- 必填参数缺失、空字符串和 `null`
- 字段格式错误，例如手机号、日期、金额和枚举值错误
- 数值边界，例如分页为 0、租期小于 1 或大于 24、金额为 0 或负数
- 未携带 Token、Token 无效或过期
- 角色不匹配，例如租客调用房东或管理员接口
- 资源不存在，例如房源、订单、支付单、合同或消息 ID 不存在
- 资源归属错误，例如用户操作其他用户的订单、房源、合同或通知
- 重复操作，例如重复注册、重复取消、重复支付、重复退款或重复签署
- 状态流转错误，例如下架房源下单、已取消订单支付或未支付订单退款

异常用例优先使用 `pytest.mark.parametrize` 表达同一接口的多组非法输入，避免复制整段请求代码。断言必须同时检查 HTTP 状态和业务 `code/message`；如果当前实现没有阻止应当被拒绝的操作，用例应真实失败并暴露缺陷，不能按现状降低安全预期。

## 7. pytest 标记与命令

定义以下标记：

- `smoke`
- `auth`
- `business`
- `internal`
- `maintenance`
- `destructive`
- `e2e`

`pytest.ini` 必须启用：

```ini
[pytest]
addopts = --strict-markers
xfail_strict = true
```

所有 marker 必须在 `pytest.ini` 中显式注册，marker 拼写错误时直接失败，避免本应隔离的 `destructive`、`internal` 或 `maintenance` 用例被误执行。

默认安全运行：

```powershell
pytest api-tests/tests -m "not internal and not maintenance and not destructive and not e2e" \
  --alluredir=api-tests/allure-results --clean-alluredir \
  --junitxml=api-tests/junit-results.xml
```

完整测试环境中显式运行副作用用例：

```powershell
pytest api-tests/tests -m "destructive and not e2e" \
  --alluredir=api-tests/allure-results --clean-alluredir \
  --junitxml=api-tests/junit-results.xml
```

E2E 单独运行：

```powershell
pytest api-tests/tests -m "e2e" \
  --alluredir=api-tests/allure-results --clean-alluredir \
  --junitxml=api-tests/junit-results.xml
```

## 8. Jenkins Pipeline

根目录 `Jenkinsfile` 负责：

1. 检出代码并记录提交 SHA。
2. 检查 Python 版本。
3. 创建 `.venv` 并安装 `api-tests/requirements.txt`。
4. 使用 Jenkins Credentials 注入测试账号。
5. 使用参数 `API_BASE_URL` 指定测试环境。
6. 先请求 `/gateway/health`，错误目标或服务未启动时立即失败。
7. 执行 `smoke` 冒烟门禁；冒烟失败时不进入普通回归和可选测试。
8. 执行普通回归，排除 `internal`、`maintenance`、`destructive` 和 `e2e`。
9. 根据参数分别执行可选的 internal、maintenance、destructive 和 E2E 测试；destructive 阶段排除 `e2e`，避免完整流程重复执行。
10. 每个 pytest 阶段生成独立 Allure 结果并汇总 JUnit XML。
11. 无论成功或失败都发布 JUnit 与 Allure 结果。
12. 归档必要报告，不归档密码、Token、虚拟环境或完整响应中的敏感信息。

流水线提供布尔参数 `RUN_INTERNAL`、`RUN_MAINTENANCE`、`RUN_DESTRUCTIVE` 和 `RUN_E2E`。默认均为 `false`。

流水线阶段顺序固定为：

```text
health 检查
  -> smoke 冒烟门禁
  -> 普通回归
  -> 可选 internal / maintenance / destructive / e2e
  -> JUnit + Allure
```

现有 Jenkins 容器没有 Docker Socket，因此 Pipeline 不执行 `docker compose up/down`。Jenkins 镜像或 Agent 需要补充 Python、Allure Jenkins Plugin 及 Allure Commandline；具体安装纳入实施计划。

## 9. Allure 展示规则

- `feature` 使用微服务名称。
- `story` 使用业务能力，例如登录、房源查询、创建订单。
- `title` 使用可读的中文用例名称。
- 正常请求不附加大段 JSON；请求或断言失败时，附加 method、URL、请求体和经过脱敏的响应内容。
- Authorization、密码、Token、手机号等敏感信息必须脱敏。
- 一个测试只验证一个主要行为，端到端流程除外。

## 10. 验收标准

- `pytest --collect-only` 能收集七个服务及端到端测试。
- 65 个 Controller 接口均有覆盖记录；每个接口至少一个主路径用例，并对适用接口覆盖参数、认证、权限、资源和状态类异常场景。
- 缺少 Base URL 或账号时给出明确错误或按用例粒度跳过，不产生误导性的通过结果。
- 公共健康检查能够识别被测目标不是公寓 Gateway 的情况。
- 安全测试能证明受保护接口未登录时被拒绝，内部订单接口通过 Nginx 时返回 403。
- Allure 结果包含服务、业务步骤、执行时间和脱敏后的失败附件。
- `pytest.ini` 启用 `--strict-markers` 和 `xfail_strict = true`，未注册 marker 或意外 XPASS 会使测试失败。
- 可删除的 API 测试数据由 `yield` Fixture 在用例结束后清理；无法清理的数据使用随机唯一值并留下日志。
- Jenkins 的 smoke 阶段是普通回归的前置门禁，失败后不会继续向测试环境施加业务请求。
- Jenkins 能发布 JUnit 和 Allure 报告，并以 pytest 退出码决定流水线状态。
- 只有在真实测试环境运行后，才汇报通过率、响应时间或完整业务流程通过情况。
