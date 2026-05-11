# API网关 - 配置说明

## 📋 概述

API网关统一管理所有6个微服务的路由，前端只需要访问网关端口 `8080`，网关会自动将请求转发到对应的后端服务。

## 🔌 端口分配

| 服务 | 端口 | 访问路径 |
|------|------|---------|
| **API网关** | **8080** | **所有API的统一入口** |
| 用户服务 | 8081 | `/api/user/**` |
| 房源服务 | 8082 | `/api/house/**` |
| 订单服务 | 8083 | `/api/order/**` |
| 支付服务 | 8084 | `/api/payment/**`, `/api/account/**` |
| 通知服务 | 8091 | `/api/notification/**` |
| 合同服务 | 8092 | `/api/contract/**` |

## 🚀 使用方式
### 前端配置
**之前**（直接访问各个服务）:
```javascript
// 不同的服务使用不同的端口
const userApi = 'http://localhost:8081/api/user'
const houseApi = 'http://localhost:8082/api/house'
const orderApi = 'http://localhost:8083/api/order'
// ...
```

**现在**（统一通过网关）:
```javascript
// 所有请求都使用网关端口 8080
const baseUrl = 'http://localhost:8080'

// 所有API路径保持不变
axios.get(`${baseUrl}/api/user/login`)         // → 用户服务
axios.get(`${baseUrl}/api/house/list`)         // → 房源服务
axios.post(`${baseUrl}/api/order/create`)      // → 订单服务
axios.post(`${baseUrl}/api/payment/pay`)       // → 支付服务
axios.get(`${baseUrl}/api/notification/list`)  // → 通知服务
axios.get(`${baseUrl}/api/contract/list`)      // → 合同服务
```

## 🎯 路由规则

### 用户服务 (8081)
```
http://localhost:8080/api/user/** → http://localhost:8081/api/user/**
```

**示例**:
- `/api/user/register` → 用户注册
- `/api/user/login` → 用户登录
- `/api/user/info` → 获取用户信息

### 房源服务 (8082)
```
http://localhost:8080/api/house/** → http://localhost:8082/api/house/**
```

**示例**:
- `/api/house/list` → 房源列表
- `/api/house/{id}` → 房源详情
- `/api/house/create` → 发布房源

### 订单服务 (8083)
```
http://localhost:8080/api/order/** → http://localhost:8083/api/order/**
```

**示例**:
- `/api/order/create` → 创建订单
- `/api/order/list` → 订单列表
- `/api/order/{id}` → 订单详情

### 支付服务 (8084)
```
http://localhost:8080/api/payment/** → http://localhost:8084/api/payment/**
http://localhost:8080/api/account/** → http://localhost:8084/api/account/**
```

**示例**:
- `/api/payment/pay` → 支付订单
- `/api/payment/status/{id}` → 支付状态
- `/api/account/balance` → 账户余额

### 通知服务 (8091)
```
http://localhost:8080/api/notification/** → http://localhost:8091/api/notification/**
```

**示例**:
- `/api/notification/unread` → 未读消息
- `/api/notification/list` → 消息列表
- `/api/notification/read/{id}` → 标记已读

### 合同服务 (8092)
```
http://localhost:8080/api/contract/** → http://localhost:8092/api/contract/**
```

**示例**:
- `/api/contract/list` → 合同列表
- `/api/contract/{id}` → 合同详情
- `/api/contract/sign/{id}` → 签署合同

## 🔧 启动顺序

### 推荐启动顺序

1. **用户服务** (8081) - 基础服务
2. **房源服务** (8082) - 基础服务
3. **订单服务** (8083) - 依赖房源服务
4. **支付服务** (8084) - 依赖订单服务
5. **通知服务** (8091) - 独立服务
6. **合同服务** (8092) - 依赖订单服务
7. **API网关** (8080) - 最后启动

### 启动脚本

PowerShell:
```powershell
# 启动所有后端服务
cd apartment-user-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"
Start-Sleep -Seconds 3

cd ..\apartment-house-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"
Start-Sleep -Seconds 3

cd ..\apartment-order-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"
Start-Sleep -Seconds 3

cd ..\apartment-payment-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"
Start-Sleep -Seconds 3

cd ..\apartment-notice-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"
Start-Sleep -Seconds 3

cd ..\apartment-contract-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run"
Start-Sleep -Seconds 3

# 最后启动网关
cd ..\apartment-gateway
mvn spring-boot:run
```

## ✅ 验证网关

### 1. 健康检查
```bash
curl http://localhost:8080/gateway/health
```

**响应**:
```json
{
  "service": "apartment-gateway",
  "status": "UP",
  "port": 8080,
  "version": "1.0.0"
}
```

### 2. 查看路由信息
```bash
curl http://localhost:8080/gateway/routes
```

### 3. 测试路由转发
```bash
# 通过网关访问用户服务
curl http://localhost:8080/api/user/health

# 通过网关访问房源服务
curl http://localhost:8080/api/house/health

# 通过网关访问订单服务
curl http://localhost:8080/api/order/health

# 通过网关访问支付服务
curl http://localhost:8080/api/payment/health

# 通过网关访问通知服务
curl http://localhost:8080/api/notification/health

# 通过网关访问合同服务
curl http://localhost:8080/api/contract/health
```

## 🎨 优势

### 1. 统一入口
- 前端只需记住一个端口 `8080`
- 简化前端配置

### 2. CORS处理
- 网关统一处理跨域问题
- 后端服务无需单独配置CORS

### 3. 负载均衡（未来）
- 可以配置多个实例进行负载均衡
- 支持蓝绿部署和金丝雀发布

### 4. 安全性
- 可以在网关层统一添加认证和鉴权
- 统一的限流和熔断

## 📝 配置文件

**位置**: `apartment-gateway/src/main/resources/application.yml`

**关键配置**:
```yaml
server:
  port: 8080  # 网关端口

spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/user/**
```

## ⚠️ 注意事项

1. **启动顺序**: 必须先启动所有后端服务，最后启动网关
2. **端口冲突**: 确保8080端口没有被占用
3. **服务可用性**: 如果某个后端服务未启动，通过网关访问会返回503错误
4. **前端配置**: 前端需要将所有API请求的baseURL改为 `http://localhost:8080`

---

**版本**: 1.0.0  
**创建时间**: 2026-01-27
