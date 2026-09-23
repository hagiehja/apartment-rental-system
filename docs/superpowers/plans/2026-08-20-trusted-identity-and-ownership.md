# Trusted Identity and Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Gateway 成为不可绕过的 JWT 身份边界，并消除合同、通知接口中由客户端提交 `userId`、`userType` 或身份头造成的越权风险，同时保持现有登录、房源浏览、合同签署和通知查询功能可用。

**Architecture:** JWT 的签发和解析下沉到 `apartment-common`，User Service 只负责签发和 Nginx 兼容校验，Gateway 对所有外部请求清除伪造身份头、验证 Bearer Token 并注入可信身份。合同与通知服务只使用 Gateway 注入的身份做对象归属校验，前端只发送 Token，不再提交任何可用于后端授权的用户身份字段。Nginx 和 Compose 作为第二道边界，禁止宿主机直连 Gateway，并明确阻断内部接口。

**Tech Stack:** Java 17、Spring Boot 3.2、Spring Cloud Gateway WebFlux、JJWT 0.11.5、JUnit 5、Mockito、Spring MockMvc、Vue 3、Axios、Vite、Nginx、Docker Compose

---

## 实施边界

- 本计划处理可信身份、对象归属和相关前端契约。
- 本计划不改变支付金额、退款金额、订单状态机、MySQL 主从、RocketMQ Outbox 或 Sentinel 规则。
- `/user/verify` 保留给 Nginx 的 `auth_request`，但 Gateway 必须拒绝外部访问 `/api/user/verify`。
- `POST /payment/refund/order/{orderNo}` 暂时保留为已登录用户接口；它的付款人校验、并发幂等和资金状态机在支付安全计划中实现，不能在本计划中误判为纯内部接口而破坏退租流程。
- `ADMIN` 不默认绕过合同归属；后台合同管理需要另设明确的管理接口。
- 不修改或提交现有的 `deploy/monitor/__pycache__/autoscaler.cpython-38.pyc`。

## Task 1: 建立唯一的 JWT 编解码实现

**Files:**

- Modify: `apartment-common/pom.xml`
- Create: `apartment-common/src/main/java/com/example/common/security/JwtPrincipal.java`
- Create: `apartment-common/src/main/java/com/example/common/security/JwtTokenService.java`
- Create: `apartment-common/src/test/java/com/example/common/security/JwtTokenServiceTest.java`

- [ ] **Step 1: 写共享 JWT 服务的失败测试**

测试必须覆盖：

```java
@Test void constructorRejectsBlankSecret()
@Test void constructorRejectsSecretShorterThan48Bytes()
@Test void issueAndParsePreservesUserIdAndRole()
@Test void parseRejectsTamperedToken()
@Test void parseRejectsExpiredToken()
@Test void issueUsesRequestedTtl()
@Test void parseRejectsUnknownRole()
@Test void parseRejectsMissingOrNonPositiveSubject()
```

固定使用注入的 `Clock`，不使用 `Thread.sleep()` 测过期。

- [ ] **Step 2: 运行定向测试并确认红灯原因是缺少实现**

Run:

```powershell
mvn -pl apartment-common -Dtest=JwtTokenServiceTest test
```

Expected: 编译失败，提示 `JwtPrincipal` 或 `JwtTokenService` 不存在；不得是依赖下载或 Java 版本错误。

- [ ] **Step 3: 增加 JJWT 与测试依赖**

在 `apartment-common/pom.xml` 添加：

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

版本沿用父 POM 的 dependency management，不在子模块重复写版本。

- [ ] **Step 4: 实现不可变身份和值校验**

`JwtPrincipal`：

```java
public record JwtPrincipal(
        long userId,
        String username,
        String phone,
        UserRole role) {
}
```

`JwtTokenService` 对外接口：

```java
public final class JwtTokenService {
    public JwtTokenService(String configuredSecret);
    JwtTokenService(String configuredSecret, Clock clock);
    public String issue(JwtPrincipal principal, Duration ttl);
    public Claims parseClaims(String token);
    public JwtPrincipal parsePrincipal(String token);
}
```

实现规则：

- Base64 配置直接使用解码后的 `byte[]`，不能先转成 UTF-8 字符串再编码。
- 非 Base64 配置使用 UTF-8 原始字节。
- HS384 密钥少于 48 字节时启动失败；异常信息不得输出密钥本身。
- 显式使用 `SignatureAlgorithm.HS384`。
- `sub` 必须为正数，`role` 必须能解析成 `UserRole`。
- `issue()` 拒绝空身份、非正 userId、空角色和非正 TTL。

- [ ] **Step 5: 运行测试确认绿灯**

Run:

```powershell
mvn -pl apartment-common -Dtest=JwtTokenServiceTest test
```

Expected: `JwtTokenServiceTest` 全部通过。

- [ ] **Step 6: 提交共享 JWT 基础能力**

```powershell
git add apartment-common/pom.xml apartment-common/src/main/java/com/example/common/security apartment-common/src/test/java/com/example/common/security
git commit -m "feat(security): centralize jwt token handling"
```

## Task 2: User Service 使用共享 JWT 并输出可信角色

**Files:**

- Modify: `apartment-user-service/pom.xml`
- Modify: `apartment-user-service/src/main/java/com/example/user/utils/JWTUtils.java`
- Modify: `apartment-user-service/src/main/java/com/example/user/controller/UserController.java`
- Create: `apartment-user-service/src/test/java/com/example/user/utils/JWTUtilsTest.java`
- Create: `apartment-user-service/src/test/java/com/example/user/controller/UserControllerVerifyTest.java`

- [ ] **Step 1: 写 JWT 配置和 verify 响应的失败测试**

`JWTUtilsTest` 覆盖：

```java
@Test void generateTokenContainsUserIdAndRole()
@Test void generateTokenUsesConfiguredExpirationMs()
@Test void validateTokenRejectsExpiredToken()
@Test void parsePrincipalReturnsLoginIdentity()
```

`UserControllerVerifyTest` 使用 MockMvc 或直接 Controller 单测覆盖：

```java
@Test void verifyReturnsAuthenticatedUserIdAndRoleHeaders()
@Test void verifyRejectsMissingBearerToken()
@Test void verifyRejectsMalformedBearerToken()
```

断言响应同时包含：

```text
X-Auth-User-Id: <JWT subject>
X-Auth-User-Role: <JWT role>
```

- [ ] **Step 2: 运行测试并确认旧实现失败**

Run:

```powershell
mvn -pl apartment-user-service -am "-Dtest=JWTUtilsTest,UserControllerVerifyTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因 `jwt.expiration-ms` 未生效、缺少角色响应头或测试类型不存在而失败。

- [ ] **Step 3: 将 `JWTUtils` 改为兼容外观层**

保留现有调用方需要的方法名，内部委托给共享服务：

```java
public String generateToken(UserInfoDTO userInfo);
public Claims parseToken(String token);
public JwtPrincipal parsePrincipal(String token);
public boolean validateToken(String token);
public Long getUserIdFromToken(String token);
public String getRoleFromToken(String token);
```

读取并真正使用配置：

```java
@Value("${jwt.secret:}")
private String secretRaw;

@Value("${jwt.expiration-ms:3600000}")
private long expirationMs;
```

`@PostConstruct` 只创建一次 `JwtTokenService`。签发时从持久化后的 `UserInfoDTO` 组装 `JwtPrincipal`，请求体中的角色永远不参与签发。

- [ ] **Step 4: `/user/verify` 增加角色响应头**

使用一次 `parsePrincipal()` 得到完整身份，然后设置：

```java
response.setHeader("X-Auth-User-Id", String.valueOf(principal.userId()));
response.setHeader("X-Auth-User-Role", principal.role().name());
```

不能记录完整 Token，校验失败继续返回 401。

- [ ] **Step 5: 清理 User Service 重复 JJWT 声明并回归**

如果 `apartment-user-service/pom.xml` 已直接声明三项 JJWT 依赖，则删除重复声明，由 `apartment-common` 传递；随后运行：

```powershell
mvn -pl apartment-user-service -am "-Dtest=JWTUtilsTest,UserControllerVerifyTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 两个测试类全绿，现有 User Service 测试无回归。

- [ ] **Step 6: 提交 User Service 身份签发改造**

```powershell
git add apartment-user-service/pom.xml apartment-user-service/src/main/java/com/example/user/utils/JWTUtils.java apartment-user-service/src/main/java/com/example/user/controller/UserController.java apartment-user-service/src/test/java/com/example/user
git commit -m "fix(user): issue and verify trusted jwt roles"
```

## Task 3: Gateway 成为最终身份边界

**Files:**

- Modify: `apartment-gateway/pom.xml`
- Modify: `apartment-gateway/src/main/resources/application.yml`
- Create: `apartment-gateway/src/main/java/com/example/gateway/config/JwtConfiguration.java`
- Create: `apartment-gateway/src/main/java/com/example/gateway/security/GatewaySecurityPolicy.java`
- Create: `apartment-gateway/src/main/java/com/example/gateway/security/JwtAuthenticationGlobalFilter.java`
- Create: `apartment-gateway/src/test/java/com/example/gateway/security/GatewaySecurityPolicyTest.java`
- Create: `apartment-gateway/src/test/java/com/example/gateway/security/JwtAuthenticationGlobalFilterTest.java`

- [ ] **Step 1: 写路径策略和过滤器失败测试**

`GatewaySecurityPolicyTest` 必须证明“HTTP 方法 + 完整路径”匹配：

```java
@ParameterizedTest void exactPublicEndpointsAllowAnonymousAccess(...)
@Test void houseDetailIsPublicForGetOnly()
@Test void lookalikePathsAreNotPublic()
@ParameterizedTest void internalEndpointsAreAlwaysRejected(...)
```

公开路径仅包含：

- `OPTIONS /**`
- `POST /api/user/login`
- `POST /api/user/register`
- `GET /gateway/health`
- `GET /api/{user|house|order|payment|contract|notification}/health`
- `GET /api/house/list`
- `GET /api/house/{数字ID}`

内部拒绝路径至少包含：

- `/api/user/verify`
- `/api/house/{数字ID}/status`
- `/api/order/id/{数字ID}`
- `/api/order/{orderNo}/installments`
- `/api/order/{orderNo}/validate`
- `/api/order/{orderNo}/payment-success`
- `/api/order/{orderNo}/refund-success`
- `/api/contract/generate`
- `/api/contract/cancel/order/{数字ID}`
- `/api/contract/cancel/{数字ID}`
- `/api/contract/landlord-income`
- `/api/notification/send`
- `/api/notification/test/insert`
- `/api/notification/fix-templates`

`JwtAuthenticationGlobalFilterTest` 覆盖：

```java
@Test void publicRequestRemovesSpoofedIdentityHeaders()
@Test void protectedEndpointWithoutTokenReturns401()
@Test void malformedBearerTokenReturns401()
@Test void tamperedTokenReturns401()
@Test void expiredTokenReturns401()
@Test void validTokenOverwritesSpoofedIdentityHeaders()
@Test void tenantTokenCannotSpoofAdminRole()
@ParameterizedTest void internalEndpointReturns403EvenWithAdminToken(...)
@Test void unauthorizedResponseUsesUnifiedJsonShape()
@Test void filterRunsBeforeRouteFilters()
```

- [ ] **Step 2: 运行 Gateway 测试并确认红灯**

Run:

```powershell
mvn -pl apartment-gateway -am "-Dtest=GatewaySecurityPolicyTest,JwtAuthenticationGlobalFilterTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因安全策略和过滤器类型不存在而编译失败。

- [ ] **Step 3: 配置 Gateway 的共享 JWT 服务**

在 Gateway 增加 `apartment-common` 依赖，并在 `application.yml` 增加：

```yaml
jwt:
  secret: ${JWT_SECRET:}
```

`JwtConfiguration` 只负责创建：

```java
@Bean
JwtTokenService jwtTokenService(@Value("${jwt.secret:}") String secret) {
    return new JwtTokenService(secret);
}
```

空密钥或弱密钥必须让应用启动失败，不能回退到默认开发密钥。

- [ ] **Step 4: 实现精确路径策略**

使用 Spring `PathPatternParser` 或等价的完整匹配，不允许用 `contains()`、未锚定正则或单纯 `startsWith()` 作为授权依据。

`POST /api/payment/refund/order/{orderNo}` 不加入内部拒绝表，但必须属于普通受保护路径。

- [ ] **Step 5: 实现 WebFlux 全局过滤器**

执行顺序固定为：

1. 从请求副本无条件删除客户端的 `X-User-Id` 和 `X-User-Role`。
2. 内部路径直接返回 HTTP 403，不转发。
3. 无 Token 且命中公开白名单时匿名放行。
4. 其他路径严格要求单个 `Authorization: Bearer <token>`。
5. 用 `JwtTokenService.parsePrincipal()` 一次完成签名、过期和角色验证。
6. 使用 `headers.set()` 注入唯一 `X-User-Id`、`X-User-Role`。
7. 继续过滤链。

过滤器顺序返回 `-100`。401/403 响应为 UTF-8 JSON：

```json
{"code":401,"message":"未登录或登录已过期","data":null}
```

日志只能记录请求方法、路径和错误类型，不能记录 Token。

- [ ] **Step 6: 定向测试与模块回归**

```powershell
mvn -pl apartment-gateway -am "-Dtest=GatewaySecurityPolicyTest,JwtAuthenticationGlobalFilterTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl apartment-common,apartment-user-service,apartment-gateway -am test
```

Expected: Gateway 安全测试全绿；Common、User、Gateway 全部测试通过。

- [ ] **Step 7: 提交 Gateway 身份边界**

```powershell
git add apartment-gateway/pom.xml apartment-gateway/src/main/resources/application.yml apartment-gateway/src/main/java/com/example/gateway apartment-gateway/src/test/java/com/example/gateway
git commit -m "feat(gateway): enforce jwt identity boundary"
```

## Task 4: 封闭 Compose 和 Nginx 绕过路径

**Files:**

- Modify: `docker-compose.app.yml`
- Modify: `deploy/nginx/nginx.conf`
- Modify: `scripts/verify-ha-config.py`
- Modify: `README.md`
- Modify: `frontend/vite.config.js`
- Create: `frontend/.env.development.example`

- [ ] **Step 1: 扩展静态部署验证并确认旧配置失败**

在 `scripts/verify-ha-config.py` 增加断言：

- `docker-compose.app.yml` 的 Gateway 不得出现宿主机 `ports`。
- Nginx 必须显式阻断 `/api/house/<id>/status`。
- Nginx 必须覆盖 `X-User-Id` 和 `X-User-Role`。
- 公开房源详情规则必须区分 GET，不能把 PUT/DELETE 误判为匿名读取。
- 主 HA Compose 只有 Nginx 暴露业务 HTTP 入口。

Run:

```powershell
python scripts/verify-ha-config.py
```

Expected: 在旧 `docker-compose.app.yml` 发布 Gateway 或 Nginx 规则未修复处失败，并明确打印具体规则名。

- [ ] **Step 2: 移除 Gateway 宿主端口映射**

把 `docker-compose.app.yml` 中 Gateway 的：

```yaml
ports:
  - "${GATEWAY_PORT:-8080}:8080"
```

改为仅容器网络可见：

```yaml
expose:
  - "8080"
```

README 将业务入口统一写为 `http://localhost` 或由 `HOST_HTTP_PORT` 指定的 Nginx 端口，不再引导访问 `localhost:8080`。

- [ ] **Step 3: 收紧 Nginx 身份头和内部路径规则**

- 登录、注册和公开 GET 路径也必须清空客户端 `X-User-Id`、`X-User-Role`。
- `auth_request_set` 同时读取 `X-Auth-User-Id`、`X-Auth-User-Role`。
- 受保护请求使用 `proxy_set_header` 覆盖两项身份头。
- `/api/house/<数字ID>/status` 在公网入口返回 403。
- 房源详情只允许 `GET /api/house/<数字ID>` 匿名，PUT/DELETE 进入受保护链。
- 保留现有订单内部回调 403 规则。

- [ ] **Step 4: Vite 开发代理只指向 Nginx**

`frontend/vite.config.js` 使用 `loadEnv` 读取：

```text
VITE_API_PROXY_TARGET=http://127.0.0.1:80
```

`/api` 和 `/img` 均代理到 Nginx，不得默认代理到 Gateway `:8080`。在 `frontend/.env.development.example` 写入同一示例值。

- [ ] **Step 5: 验证静态部署配置和前端构建**

```powershell
python scripts/verify-ha-config.py
docker compose --env-file .env.app -f docker-compose.yml config --quiet
docker compose --env-file .env.app -f docker-compose.app.yml config --quiet
npm --prefix frontend run build
```

Expected: 静态验证、两份 Compose 解析和前端构建全部通过。若本机没有可用 Docker，只能将两条 Docker 命令记录为“环境阻塞”，不能声称 Compose 已运行。

- [ ] **Step 6: 提交部署边界修复**

```powershell
git add docker-compose.app.yml deploy/nginx/nginx.conf scripts/verify-ha-config.py README.md frontend/vite.config.js frontend/.env.development.example
git commit -m "fix(deploy): prevent gateway authentication bypass"
```

## Task 5: 合同服务按可信身份执行对象归属校验

**Files:**

- Modify: `apartment-contract-service/src/main/java/com/example/contract/controller/ContractController.java`
- Modify: `apartment-contract-service/src/main/java/com/example/contract/service/ContractService.java`
- Modify: `apartment-contract-service/src/main/java/com/example/contract/service/impl/ContractServiceImpl.java`
- Modify: `apartment-contract-service/src/main/java/com/example/contract/dto/SignContractDTO.java`
- Create: `apartment-contract-service/src/main/java/com/example/contract/dto/TerminateContractDTO.java`
- Create: `apartment-contract-service/src/test/java/com/example/contract/service/impl/ContractServiceAuthorizationTest.java`
- Create: `apartment-contract-service/src/test/java/com/example/contract/controller/ContractControllerAuthorizationTest.java`

- [ ] **Step 1: 写对象归属服务测试**

覆盖：

```java
@Test void getContractByIdAllowsTenantOwner()
@Test void getContractByIdAllowsLandlordOwner()
@Test void getContractByIdRejectsUnrelatedTenantWith403()
@Test void getContractByOrderIdRejectsUnrelatedLandlordWith403()
@Test void getContractListRejectsInvalidRoleBeforeQuery()
@Test void signContractUsesAuthenticatedActorIdentity()
@Test void signContractRejectsRoleOwnershipMismatch()
@Test void terminateContractRejectsLandlordOwner()
@Test void calculateRefundRejectsNonOwner()
```

断言拒绝路径不会写 `ContractSignatureMapper`，不会更新合同状态。

- [ ] **Step 2: 写 Controller 身份契约失败测试**

覆盖：

```java
@Test void listUsesTrustedHeadersAndHasNoUserIdQueryContract()
@Test void signUsesTrustedHeadersAndIgnoresBodyIdentityFields()
@Test void detailOwnershipFailureReturnsHttp403()
@Test void downloadOwnershipFailureReturnsHttp403()
@Test void missingIdentityReturnsHttp401()
```

确认 Controller 不再把 `BusinessException(403, ...)` 包成 HTTP 200、业务错误码 500。

- [ ] **Step 3: 运行测试并确认旧实现失败**

```powershell
mvn -pl apartment-contract-service -am "-Dtest=ContractServiceAuthorizationTest,ContractControllerAuthorizationTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 旧签名缺少 actor 身份或越权断言失败。

- [ ] **Step 4: 收敛 DTO 和 Service 接口**

`SignContractDTO` 只保留 `signatureData`；客户端提交的 `userId`、`userType`、`ipAddress` 全部删除。新增 `TerminateContractDTO`，只保留 `reason`。

Service 对外签名改为：

```java
Contract getContractById(Long contractId, Long actorUserId, String actorRole);
Contract getContractByOrderId(Long orderId, Long actorUserId, String actorRole);
IPage<Contract> getContractList(Long actorUserId, String actorRole, String status, Integer page, Integer size);
void signContract(Long contractId, Long actorUserId, String actorRole, SignContractDTO dto, String clientIp);
BigDecimal terminateContract(Long contractId, Long actorUserId, String actorRole, String reason);
BigDecimal calculateRefund(Long contractId, Long actorUserId, String actorRole);
```

- [ ] **Step 5: 集中实现身份与归属规则**

实现并复用：

```java
private UserRole requireActorRole(Long actorUserId, String actorRole);
private void assertContractOwner(Contract contract, Long actorUserId, UserRole actorRole);
private Contract getOwnedContract(Long contractId, Long actorUserId, String actorRole);
```

规则：

- 空身份返回 401。
- TENANT 只能访问 `tenantId` 等于自身的合同。
- LANDLORD 只能访问 `landlordId` 等于自身的合同。
- 非法角色、ADMIN 或非合同双方返回 403。
- 不存在返回 404。
- 退租只能由合同租客发起。
- 签署 IP 从 `HttpServletRequest.getRemoteAddr()` 获得，不接受请求体 IP。
- 列表必须先校验角色，再添加 owner 查询条件；非法角色绝不能落入无条件分页。

- [ ] **Step 6: Controller 全部读取可信头并保留内部方法**

外部合同详情、按订单查询、列表、签署、下载、退款预览、退租统一读取：

```java
@RequestHeader(value = "X-User-Id", required = false) Long actorUserId
@RequestHeader(value = "X-User-Role", required = false) String actorRole
```

内部 `/contract/generate`、`/contract/cancel/order/{orderId}`、`/contract/landlord-income` 继续供服务间直连，但由 Gateway/Nginx 拒绝公网访问。

移除外部接口的通用 `catch (Exception)`，让 `GlobalExceptionHandler` 保留真实 HTTP 401/403/404。

- [ ] **Step 7: 修复已确认的取消合同 Feign 路径不一致**

把订单服务的 `ContractFeignClient` 路径统一到 Controller 的：

```text
/contract/cancel/order/{orderId}
```

添加或扩展 Feign 契约测试，断言路径包含 `/order/`。

- [ ] **Step 8: 运行合同定向测试和现有回归**

```powershell
mvn -pl apartment-contract-service,apartment-order-service -am "-Dtest=ContractServiceAuthorizationTest,ContractControllerAuthorizationTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl apartment-contract-service,apartment-order-service -am test
```

Expected: 新授权测试全绿；合同和订单现有测试无回归。

- [ ] **Step 9: 提交合同归属修复**

```powershell
git add apartment-contract-service apartment-order-service/src/main/java/com/example/order/feign/ContractFeignClient.java
git commit -m "fix(contract): enforce authenticated ownership"
```

## Task 6: 通知服务移除客户端 userId 并保护删除操作

**Files:**

- Modify: `apartment-notice-service/src/main/java/com/example/notification/controller/NotificationController.java`
- Modify: `apartment-notice-service/src/main/java/com/example/notification/service/NotificationService.java`
- Modify: `apartment-notice-service/src/main/java/com/example/notification/service/impl/NotificationServiceImpl.java`
- Create: `apartment-notice-service/src/test/java/com/example/notification/service/impl/NotificationServiceAuthorizationTest.java`
- Create: `apartment-notice-service/src/test/java/com/example/notification/controller/NotificationControllerAuthorizationTest.java`

- [ ] **Step 1: 写通知归属失败测试**

Service 测试：

```java
@Test void getMessageByIdAllowsOwner()
@Test void getMessageByIdRejectsNonOwnerWith403()
@Test void markAsReadRejectsNonOwnerWithoutUpdate()
@Test void deleteByIdRejectsNonOwnerWithoutDelete()
@Test void markAllAsReadScopesUpdateToAuthenticatedUser()
```

Controller 测试：

```java
@Test void unreadCountUsesTrustedHeaderAndHasNoUserIdQueryContract()
@Test void messageListUsesTrustedHeaderAndHasNoUserIdQueryContract()
@Test void messageDetailUsesTrustedHeader()
@Test void markAsReadUsesTrustedHeader()
@Test void ownershipFailureReturnsHttp403()
@Test void maintenanceRoutesAreNotPubliclyMapped()
```

- [ ] **Step 2: 运行测试并确认旧实现失败**

```powershell
mvn -pl apartment-notice-service -am "-Dtest=NotificationServiceAuthorizationTest,NotificationControllerAuthorizationTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因 Controller 仍要求 query `userId`、删除没有 owner 参数或维护路由仍存在而失败。

- [ ] **Step 3: 收敛 Service 契约并实现统一归属查询**

Service 方法统一为：

```java
Long getUnreadCount(Long actorUserId);
List<NotificationMessage> getUnreadMessages(Long actorUserId);
IPage<NotificationMessage> getMessageList(Long actorUserId, String type, Integer page, Integer size);
NotificationMessage getMessageById(Long messageId, Long actorUserId);
void markAsRead(Long messageId, Long actorUserId);
void markAllAsRead(Long actorUserId);
void deleteById(Long messageId, Long actorUserId);
```

集中实现：

```java
private void requireAuthenticated(Long actorUserId);
private NotificationMessage getOwnedMessage(Long messageId, Long actorUserId);
```

消息不存在返回 404，非本人返回 403；`getMessageById`、`markAsRead`、`deleteById` 都复用同一个归属方法。

- [ ] **Step 4: Controller 只使用可信身份头**

未读数、未读列表、分页、详情、已读、全部已读、删除全部移除 query `userId`，改读 `X-User-Id`。

删除公开 Controller 映射：

- `POST /notification/send`
- `POST /notification/test/insert`
- `POST /notification/fix-templates`

`NotificationService.sendNotification()` 保留给 RocketMQ Consumer 调用，不从外部 HTTP 暴露。

移除通用异常吞噬，让全局异常处理输出真实 HTTP 状态。

- [ ] **Step 5: 运行通知定向测试和模块回归**

```powershell
mvn -pl apartment-notice-service -am "-Dtest=NotificationServiceAuthorizationTest,NotificationControllerAuthorizationTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl apartment-notice-service -am test
```

Expected: 授权测试全绿，RocketMQ Consumer 编译和现有通知业务不受影响。

- [ ] **Step 6: 提交通知归属修复**

```powershell
git add apartment-notice-service
git commit -m "fix(notification): enforce message ownership"
```

## Task 7: 同步前端身份和合同/通知接口契约

**Files:**

- Modify: `frontend/src/api/request.js`
- Modify: `frontend/src/api/contract.js`
- Modify: `frontend/src/api/notification.js`
- Modify: `frontend/src/views/contract/ContractList.vue`
- Modify: `frontend/src/views/contract/ContractDetail.vue`
- Modify: relevant notification views found by `Select-String -Path frontend/src/**/*.vue -Pattern 'notificationApi|userId'`

- [ ] **Step 1: 列出旧身份参数的所有前端调用点**

```powershell
Get-ChildItem frontend/src -Recurse -File | Select-String -Pattern 'X-User-Id|X-User-Role|userType|notificationApi|signContract|terminateContract|downloadContract'
```

保存命令输出用于逐项核对，不能只改当前打开的页面。

- [ ] **Step 2: 请求拦截器仅发送 Bearer Token**

删除前端主动设置 `X-User-Id`、`X-User-Role` 的逻辑。本地 `userInfo.role` 只能控制界面显示，不能作为后端授权输入。

响应拦截器对 `responseType: 'blob'` 直接返回二进制响应，普通 JSON 继续执行现有 `{code,message,data}` 逻辑。

- [ ] **Step 3: 更新合同 API 与页面参数**

- 合同列表不再发送 `userId`、`userType`。
- 签署 body 只发送 `signatureData`。
- 退租 body 只发送 `reason`，不发送前端计算的 `refundAmount`。
- 下载改为 Axios `blob` 请求，再使用临时 Object URL 触发浏览器下载；结束后调用 `URL.revokeObjectURL()`。
- 页面角色判断仅决定按钮显隐；后端 403 必须展示真实错误。

- [ ] **Step 4: 更新通知 API 与页面参数**

未读数、未读列表、分页、详情、单条已读、全部已读、删除都不再拼接 `userId` query 参数。

- [ ] **Step 5: 确认旧身份字段已清除并构建**

```powershell
Get-ChildItem frontend/src -Recurse -File | Select-String -Pattern 'X-User-Id|X-User-Role'
npm --prefix frontend run build
```

Expected: 第一条命令没有命中前端手工身份头；生产构建成功。

- [ ] **Step 6: 提交前端契约同步**

```powershell
git add frontend/src/api frontend/src/views/contract frontend/src/views/notification frontend/vite.config.js frontend/.env.development.example
git commit -m "fix(frontend): rely on trusted backend identity"
```

## Task 8: 全仓回归与负向联调验收

**Files:**

- Modify: `docs/superpowers/plans/2026-08-20-trusted-identity-and-ownership.md`（只勾选真实完成项）
- Modify: `experiments/report/EXPERIMENT-REPORT.md`（仅在运行证据真实产生后补充结果）

- [ ] **Step 1: 运行全仓后端测试**

```powershell
mvn test
```

Expected: Reactor `BUILD SUCCESS`，所有新旧测试 0 failures、0 errors。

- [ ] **Step 2: 运行前端生产构建**

```powershell
npm --prefix frontend run build
```

Expected: Vite build 成功，不出现接口导入或组件模板编译错误。

- [ ] **Step 3: 运行静态部署校验**

```powershell
python scripts/verify-ha-config.py
docker compose --env-file .env.app -f docker-compose.yml config --quiet
```

Expected: 脚本通过；Compose 可解析。Docker 不可用时记录实际错误和环境边界。

- [ ] **Step 4: 可用 Docker 时启动并检查容器边界**

```powershell
docker compose --env-file .env.app -f docker-compose.yml up -d --wait
docker compose port apartment-gateway 8080
docker compose port apartment-user-service 8081
docker compose port nginx 80
docker compose exec -T nginx nginx -t
```

Expected:

- Gateway、User Service 不返回宿主映射。
- Nginx 返回宿主端口映射。
- Nginx 配置测试成功。

- [ ] **Step 5: 执行匿名和伪造身份负向测试**

```powershell
curl.exe -sS -o NUL -w "%{http_code}`n" "http://localhost/api/order/my/list?pageNum=1&pageSize=1" -H "X-User-Id: 44" -H "X-User-Role: ADMIN"
curl.exe -sS -o NUL -w "%{http_code}`n" "http://localhost/api/order/my/list?pageNum=1&pageSize=1" -H "Authorization: Bearer invalid-token" -H "X-User-Id: 44"
curl.exe -sS -o NUL -w "%{http_code}`n" -X PUT "http://localhost/api/house/999999999/status?status=RENTED"
curl.exe -sS -o NUL -w "%{http_code}`n" "http://localhost/api/user/verify"
```

Expected: 前两条 401，后两条 403；任何请求都不能到达对应业务写路径。

- [ ] **Step 6: 执行真实 Token 身份覆盖测试**

先通过 `POST /api/user/login` 获取 TENANT Token，再请求受保护接口并故意附带：

```text
X-User-Id: <另一用户ID>
X-User-Role: ADMIN
```

Expected: Gateway 下游只收到 Token 中的 TENANT 身份；不能查看、签署、下载或删除其他用户的合同/通知。

- [ ] **Step 7: 执行业务正向冒烟**

至少验证：

1. 匿名注册、登录、房源列表、房源详情仍可用。
2. 租客登录后能查看自己的合同、签署、下载和查看退款预览。
3. 房东登录后能查看自己的合同，但不能发起租客退租。
4. 用户能查看、已读、删除自己的通知。
5. 用户访问他人合同或通知得到 HTTP 403。
6. 内部订单到合同生成、RocketMQ 到通知落库的服务间直连仍可用。

- [ ] **Step 8: 只记录真实运行证据**

在 `EXPERIMENT-REPORT.md` 中写清：

- 运行命令和日期。
- 测试数量与失败数量。
- 哪些 curl 得到 401/403/2xx。
- Docker 不可用或业务依赖未启动时明确写“未验证”，不能用静态代码结论替代。

- [ ] **Step 9: 最终审查和提交**

执行：

```powershell
git status --short
git diff --check
git log --oneline -8
```

确认未提交无关缓存、密钥、构建产物后：

```powershell
git add docs/superpowers/plans/2026-08-20-trusted-identity-and-ownership.md experiments/report/EXPERIMENT-REPORT.md
git commit -m "docs(security): record trusted identity verification"
```

## 完成判定

只有同时满足以下条件才能声明本计划完成：

- Common、User、Gateway 的 JWT 与过滤器测试通过。
- 合同和通知的越权、缺失身份、删除归属测试通过。
- 前端不再设置身份头或传递用于授权的 `userId/userType`。
- Gateway 与下游服务没有宿主机业务端口暴露。
- 全仓 Maven 测试和前端构建通过。
- 有可用运行环境时，伪造身份、跨用户访问和内部接口公网调用均按预期返回 401/403。
- 任何未执行的 Docker/E2E 验收都被明确标为未验证。
