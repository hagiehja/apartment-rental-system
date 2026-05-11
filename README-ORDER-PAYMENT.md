# 订单和支付服务 - 快速启动指南

## 服务概述

- **订单服务**：端口 8088，管理租赁订单和分期付款计划
- **支付服务**：端口 8087，管理用户余额和支付交易

## 快速启动

### 1. 初始化数据库

```bash
# 连接MySQL
mysql -uroot -p123456

# 使用数据库
use apartment_db;

# 执行初始化SQL
source init-database.sql;
```

### 2. 启动服务

#### 方式一：使用 Maven  
```bash
# 启动订单服务
cd apartment-order-service
mvn spring-boot:run

# 启动支付服务  
cd apartment-payment-service
mvn spring-boot:run
```

#### 方式二：使用 IDEA
直接运行各服务的 Application 类

### 3. 验证服务

```bash
# 检查订单服务
curl http://localhost:8088/order/health

# 检查支付服务 
curl http://localhost:8087/payment/health
```

## 核心功能

### 订单服务 API

- POST `/order` - 创建订单（支持分期）
- GET `/order/{orderNo}` - 查询订单详情
- GET `/order/my/list` - 我的订单列表
- PUT `/order/{orderNo}/cancel` - 取消订单
- GET `/order/{orderNo}/installments` - 查询分期计划

### 支付服务 API

- GET `/payment/account/balance` - 查询余额
- POST `/payment` - 创建支付单
- POST `/payment/{paymentNo}/pay` - 执行支付
- GET `/payment/list` - 支付列表

## 测试流程

1. 创建订单（设置 installmentEnabled=true 启用分期）
2. 查询用户余额（默认10万元）
3. 创建支付单（支付首期）
4. 执行支付
5. 查询订单状态

## 注意事项

- 订单30分钟未支付自动取消
- 用户初始余额：100,000.00元
- 分期模式：首付（押金+首月租金）+ 月付

## 数据库表

- `rental_order` - 租赁订单表
- `install ment_plan` - 分期付款计划表
- `user_account` - 用户账户表
- `account_transaction` - 账户流水表
- `payment` - 支付单表
