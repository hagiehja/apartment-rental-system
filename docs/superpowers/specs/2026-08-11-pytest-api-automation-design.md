# Pytest 接口自动化与 Jenkins Allure 报告设计

## 1. 目标

在不引入复杂测试平台的前提下，使用 Python、pytest 和 requests 为公寓租赁系统建立一套可重复执行的接口自动化测试。测试覆盖 Gateway、用户、房源、订单、支付、合同和通知服务，并通过 Jenkins 自动运行、发布 Allure 报告。

这套测试以简历展示和面试讲解为重要目标，因此只保留容易理解、能够实际运行的能力：公共请求客户端、登录 Fixture、动态测试数据、统一断言、pytest 标记、端到端业务流程和 Jenkins 流水线。

## 2. 执行边界

- Jenkins 不负责启动或部署公寓系统，只测试已经启动的测试环境。
- 被测入口由 `API_BASE_URL` 指定，正式运行前必须先通过 `GET /gateway/health`。
- 测试账号密码通过本地环境变量或 Jenkins Credentials 注入，不写入仓库。
- 普通运行覆盖公开接口、JWT 业务接口和完整租赁流程。
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

`ApiClient` 基于 `requests.Session`，统一处理：

- Base URL 拼接
- JWT `Authorization: Bearer ...` 请求头
- 默认超时
- JSON 请求
- Allure 请求/响应附件

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

## 7. pytest 标记与命令

定义以下标记：

- `smoke`
- `auth`
- `business`
- `internal`
- `maintenance`
- `destructive`
- `e2e`

默认安全运行：

```powershell
pytest api-tests/tests -m "not internal and not maintenance and not destructive" \
  --alluredir=api-tests/allure-results --clean-alluredir \
  --junitxml=api-tests/junit-results.xml
```

完整测试环境中显式运行副作用用例：

```powershell
pytest api-tests/tests -m "destructive or e2e" \
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
7. 运行 pytest，生成 `allure-results` 和 JUnit XML。
8. 无论成功或失败都发布 JUnit 与 Allure 结果。
9. 归档必要报告，不归档密码、Token、虚拟环境或完整响应中的敏感信息。

流水线提供布尔参数 `RUN_INTERNAL`、`RUN_MAINTENANCE` 和 `RUN_DESTRUCTIVE`。默认均为 `false`。

现有 Jenkins 容器没有 Docker Socket，因此 Pipeline 不执行 `docker compose up/down`。Jenkins 镜像或 Agent 需要补充 Python、Allure Jenkins Plugin 及 Allure Commandline；具体安装纳入实施计划。

## 9. Allure 展示规则

- `feature` 使用微服务名称。
- `story` 使用业务能力，例如登录、房源查询、创建订单。
- `title` 使用可读的中文用例名称。
- 请求方法、URL、参数和经过脱敏的响应在失败时作为附件保存。
- Authorization、密码、Token、手机号等敏感信息必须脱敏。
- 一个测试只验证一个主要行为，端到端流程除外。

## 10. 验收标准

- `pytest --collect-only` 能收集七个服务及端到端测试。
- 缺少 Base URL 或账号时给出明确错误或按用例粒度跳过，不产生误导性的通过结果。
- 公共健康检查能够识别被测目标不是公寓 Gateway 的情况。
- 安全测试能证明受保护接口未登录时被拒绝，内部订单接口通过 Nginx 时返回 403。
- Allure 结果包含服务、业务步骤、执行时间和脱敏后的失败附件。
- Jenkins 能发布 JUnit 和 Allure 报告，并以 pytest 退出码决定流水线状态。
- 只有在真实测试环境运行后，才汇报通过率、响应时间或完整业务流程通过情况。
