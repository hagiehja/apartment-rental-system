# 🏠 寓见云 · SmartLiving — 智能公寓租赁系统

> 基于 **Spring Cloud 2023 + Vue 3** 的微服务架构公寓租赁平台,集成 FM/DeepFM 混合推荐、Redis Sentinel 高可用、RocketMQ 异步解耦、Sentinel 流量治理。

---

## ✨ 核心特性

| 模块 | 能力 |
|-----|-----|
| 🔐 鉴权 | JWT (HS384) + BCrypt(cost=10) + Nginx auth_request 零信任网关 |
| 🏠 房源 | 整租/合租、多维度筛选、地理标签、浏览量统计 |
| 🤖 推荐 | **FM + DeepFM 混合排序模型**(TensorFlow 离线训练,Java 在线推理) |
| 📦 订单 | RocketMQ 异步下单、库存锁定、超时取消 |
| 💰 支付 | 钱包账户、冻结/解冻、退款、事务一致性 |
| 📝 合同 | 电子签约、自动到期、PDF 存档 |
| 🔔 通知 | 站内信、未读数、RocketMQ 广播 |
| ⚙️ 运维 | Docker Compose 一键部署、健康探活、日志轮转、限流熔断 |

---

## 🏗️ 技术栈

### 后端
- **JDK 17** · **Spring Boot 3.2** · **Spring Cloud 2023**
- **Nacos** 服务注册与配置中心
- **MySQL 8** 主从复制 + **HikariCP** 连接池
- **Redis 7** Sentinel 高可用 + **Redisson** 分布式锁
- **RocketMQ 5** 异步消息
- **MyBatis-Plus** 参数化查询(防 SQL 注入)
- **Sentinel** 流量治理/熔断/限流
- **JWT (jjwt 0.12)** HS384 签名

### 前端
- **Vue 3** Composition API + **Vite 5**
- **Pinia** 状态管理 · **Vue Router 4**
- **Element Plus** UI 组件库 · **Axios** HTTP 客户端

### 推荐系统
- **TensorFlow 2.15** 离线训练 FM / DeepFM 模型
- **NumPy** 手写 FM 实现(用于教学对照)
- **Java** 在线推理(无 Python 依赖,毫秒级响应)

### 运维
- **Docker / Docker Compose** 多服务编排
- **Nginx** 反向代理 + JWT 网关 + 限流
- **Grafana / Prometheus** 指标监控

---

## 📦 模块结构

```
apartment-rental-system/
├── apartment-gateway            # Spring Cloud Gateway 入口
├── apartment-user-service       # 用户、JWT、RBAC
├── apartment-house-service      # 房源 + FM/DeepFM 推荐
├── apartment-order-service      # 订单、RocketMQ 下单
├── apartment-payment-service    # 钱包、流水、退款
├── apartment-contract-service   # 电子合同
├── apartment-notice-service     # 站内通知
├── frontend/                    # Vue 3 前端
├── deploy/
│   ├── nginx/                   # Nginx 配置(JWT 网关 + 限流)
│   ├── recommendation/          # FM/DeepFM 模型训练脚本
│   ├── image-proxy/             # 图片代理服务
│   └── scripts/                 # 部署辅助脚本
├── docker-compose.ha.yml        # 高可用编排
├── docker-compose.app.yml       # 应用服务编排
└── pom.xml                      # Maven 父工程
```

---

## 🚀 快速开始

### 环境要求
- Docker 24+ & Docker Compose v2.20+(需支持 `include` 语法)
- Node.js 18+(仅前端开发需要,生产部署已在 Docker 内)

### 1. 克隆代码
```bash
git clone https://github.com/hagiehja/apartment-rental-system.git
cd apartment-rental-system
```

### 2. 配置环境变量
```bash
cp .env.app.example .env.app
# 按需修改 .env.app 里的 IP / 密码(默认指向 192.168.24.129)
```

### 3. 一键启动 ⭐
```bash
docker compose up -d
```

**这一条命令会自动按依赖顺序启动全部 25 个服务**:
- 中间件(17):MySQL 主从 + Redis Sentinel(1主2从3哨兵) + Nacos 集群(3 节点) + RocketMQ + Prometheus/Grafana 监控
- 应用(8):Gateway / User / House / Order / Payment / Notice / Contract + Nginx

> 首次启动会在 Docker 内编译 Java(多阶段构建),约 5-10 分钟,后续启动秒级。

### 4. 查看状态
```bash
docker compose ps                                  # 全部服务状态
docker compose logs -f apartment-gateway           # 看某个服务日志
```

### 5. 访问应用
| 服务 | 地址 | 账号 |
|------|------|------|
| 应用网关 | http://localhost:8080 | - |
| Nacos 控制台 | http://localhost:8848/nacos | nacos / nacos |
| Grafana 监控 | http://localhost:3000 | admin / admin |
| RocketMQ 面板 | http://localhost:8082 | - |

### 6. 停止 / 重启
```bash
docker compose down                 # 停止全部
docker compose down -v              # 停止并删除数据(慎用!)
docker compose restart              # 重启全部
docker compose up -d --build xxx    # 重新构建某个服务
```

### 7. (可选)前端开发模式
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

---

---

## 🔧 配置说明

所有环境相关的配置都通过环境变量注入,默认值见各服务的 `application.yml`。
关键变量(完整列表见 `.env.app.example`):

| 变量名 | 说明 |
|-------|-----|
| `MYSQL_HOST` / `MYSQL_PORT` | 数据库地址 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 |
| `NACOS_ADDR` | Nacos 注册中心地址 |
| `JWT_SECRET` | JWT 签名密钥(Base64 编码,长度 ≥ 32 字符) |
| `ROCKETMQ_NAMESRV` | RocketMQ NameServer 地址 |

---

## 📐 架构亮点

### 1. 零信任鉴权链
所有受保护 API 经 Nginx `auth_request` 子请求校验 JWT,通过响应头注入可信 `X-User-Id`,后端只信任网关注入的标识,**杜绝前端伪造身份**。

### 2. FM + DeepFM 混合推荐
- **离线**:TensorFlow 训练 DeepFM,导出权重为 JSON
- **在线**:Java 加载权重,实时计算 FM 一阶/二阶交叉 + Deep 部分
- **融合**:协同过滤 + 内容召回 + FM 排序,**毫秒级响应**

### 3. 高可用数据层
- MySQL 主从复制 + 读写分离
- Redis Sentinel 自动主从切换
- RocketMQ 集群保证消息不丢

### 4. 全局异常处理
6 个微服务统一 `@RestControllerAdvice`,返回标准 `{code, message, data}` 结构,**不暴露内部堆栈和路径**。

---

## 🧪 测试

```bash
# 单元测试
mvn test

# 推荐模型测试
mvn test -pl apartment-house-service -Dtest=HybridRecommendationScorerTest
mvn test -pl apartment-house-service -Dtest=DeepFmScorerTest
```

---

## 📄 License

MIT License — 仅供学习交流使用。

---

>