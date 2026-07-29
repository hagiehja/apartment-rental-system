# 更新日志 (Changelog)

本项目所有重要版本变更记录。版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [v1.1.0] - 2026-07-28 代码规范化重构 ✨

### 新增
- 公共模块 `apartment-common`(统一 Result / PageResult / 异常处理 / 枚举)
- 状态字符串 → 强类型枚举(UserRole / HouseStatus / ContractStatus)
- 全局异常处理器 GlobalExceptionHandler

### 重构
- `RuntimeException` → `BusinessException`(31 处)
- 删除 `allow-circular-references`(6 处,消除潜在初始化风险)
- 删除死代码 `JwtUtil`(与 `JWTUtils` 重复)
- SQL 日志改用 Slf4j,不再打印到控制台

### 修复
- User 实体 create_time/update_time 字段映射

---

## [v1.0.0] - 2026-07-27 首个正式版 🎉

### 新增
- 推荐系统增强(用户行为埋点 + 召回排序)
- 工程化完善(README / 部署文档 / 启动指南)
- RocketMQ 部署脚本
- 全链路 E2E 测试

---

## [v0.9.0] - 2026-07-25 微服务生态完善

### 新增
- RocketMQ 5 异步消息(订单状态推送)
- Feign 服务间调用
- DeepFM 推荐算法 Java 端推理
- Sentinel 流控 / 熔断 / 降级

---

## [v0.5.0] - 2026-07-20 容器化与高可用基线

### 新增
- Docker Compose 一键部署
- Redis Sentinel 高可用(1主2从3哨兵)
- Redisson 分布式锁
- Nginx 反向代理 + 健康检查自愈

---

## [v0.1.0] - 2026-07-15 初始原型

### 新增
- 基础微服务架构(用户/房源/订单/合同/支付 5 大服务)
- Spring Cloud + Nacos 注册中心
- MyBatis-Plus + MySQL
- JWT 鉴权
- 基础 CRUD 接口
