# Nacos 配置文件导入指南

## 访问 Nacos 控制台
打开浏览器访问: http://192.168.24.129:8848/nacos
默认账号密码: nacos / nacos

## 配置文件列表

需要在 Nacos 配置中心创建以下 7 个配置文件:

| Data ID | Group | 格式 | 说明 |
|---------|-------|------|------|
| apartment-user-service.yaml | DEFAULT_GROUP | YAML | 用户服务配置 |
| apartment-house-service.yaml | DEFAULT_GROUP | YAML | 房源服务配置 |
| apartment-order-service.yaml | DEFAULT_GROUP | YAML | 订单服务配置 |
| apartment-payment-service.yaml | DEFAULT_GROUP | YAML | 支付服务配置 |
| apartment-contract-service.yaml | DEFAULT_GROUP | YAML | 合同服务配置 |
| apartment-notice-service.yaml | DEFAULT_GROUP | YAML | 通知服务配置 |
| apartment-gateway.yaml | DEFAULT_GROUP | YAML | 网关服务配置 |

## 导入步骤

### 方式一：通过控制台手动创建

1. 登录 Nacos 控制台
2. 点击左侧菜单 "配置管理" -> "配置列表"
3. 点击右上角 "+" 按钮创建配置
4. 填写表单:
   - **Data ID**: apartment-user-service.yaml
   - **Group**: DEFAULT_GROUP
   - **配置格式**: YAML
   - **配置内容**: 从 nacos-configs-all.yaml 复制对应服务的配置
5. 点击 "发布" 按钮
6. 重复步骤 3-5，创建其余 6 个配置

### 方式二：使用 Nacos Open API（批量导入）

可以使用以下 PowerShell 脚本批量创建配置:

```powershell
# 设置 Nacos 地址
$nacosServer = "http://192.168.24.129:8848"

# 配置文件路径
$configFile = "nacos-configs-all.yaml"

# 解析配置文件并逐个导入
# 注意：需要手动分割配置内容
```

## 配置要点说明

### 1. 数据库配置
- user, house, order, payment 服务使用 `apartment_db`
- contract 服务使用 `apartment_contract`
- notice 服务使用 `apartment_notification`

### 2. Redis 配置
- user-service: database 0
- order-service: database 1
- payment-service: database 2
- 地址: 192.168.24.129:6379

### 3. 网关路由
所有路由已改为 `lb://service-name` 格式，支持负载均衡

### 4. 服务间调用
合同服务的 order/notification URL 已改为服务名形式

## 验证配置是否生效

启动某个服务后，观察日志输出:
```
Located property source: [BootstrapPropertySource {name='bootstrapProperties-apartment-user-service.yaml,DEFAULT_GROUP'}]
```
出现此日志表示成功从 Nacos 加载配置。
