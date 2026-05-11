# 公寓租赁系统 - 前端

基于 Vue 3 + JavaScript + CSS 构建的前端应用。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **样式**: 原生 CSS (使用CSS变量实现主题)

## 项目结构

```
frontend/
├── src/
│   ├── api/                 # API服务封装
│   │   ├── request.js       # Axios实例和拦截器
│   │   ├── user.js          # 用户服务
│   │   ├── house.js         # 房源服务
│   │   ├── order.js         # 订单服务
│   │   ├── payment.js       # 支付服务
│   │   ├── contract.js      # 合同服务
│   │   └── notification.js  # 通知服务
│   ├── router/
│   │   └── index.js         # 路由配置
│   ├── styles/
│   │   └── main.css         # 全局样式
│   ├── views/               # 页面组件
│   │   ├── Login.vue        # 登录页
│   │   ├── Home.vue         # 首页
│   │   ├── house/           # 房源相关
│   │   │   ├── HouseList.vue
│   │   │   ├── HouseDetail.vue
│   │   │   ├── HousePublish.vue
│   │   │   └── MyHouses.vue
│   │   ├── order/           # 订单相关
│   │   │   ├── OrderCreate.vue
│   │   │   ├── OrderList.vue
│   │   │   └── OrderDetail.vue
│   │   ├── payment/         # 支付相关
│   │   │   ├── Payment.vue
│   │   │   └── Wallet.vue
│   │   ├── contract/        # 合同相关
│   │   │   ├── ContractList.vue
│   │   │   └── ContractDetail.vue
│   │   └── notification/    # 通知相关
│   │       └── Notifications.vue
│   ├── App.vue              # 根组件
│   └── main.js              # 入口文件
├── index.html
├── package.json
└── vite.config.js           # Vite配置
```

## 页面功能

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录 | /login | 用户登录，支持快捷测试账号 |
| 首页 | / | 热门房源展示，城市搜索 |
| 房源列表 | /houses | 筛选搜索房源 |
| 房源详情 | /house/:id | 查看房源详情，发起租房 |
| 发布房源 | /house/publish | 房东发布新房源 |
| 我的房源 | /my-houses | 房东管理房源 |
| 创建订单 | /order/create/:houseId | 确认租期，创建订单 |
| 订单列表 | /orders | 查看所有订单 |
| 订单详情 | /order/:orderNo | 订单详情，分期计划 |
| 支付 | /payment/:orderNo | 订单支付 |
| 钱包 | /wallet | 账户余额管理 |
| 合同列表 | /contracts | 查看合同列表 |
| 合同详情 | /contract/:id | 合同内容，签署 |
| 消息通知 | /notifications | 系统消息 |

## 快速开始

### 安装依赖

```bash
cd frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

## 后端服务配置

开发模式下使用Vite代理转发API请求：

- 前端: http://localhost:5173
- Gateway: http://localhost:9000

确保后端Gateway服务已启动。

## 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| landlord1 | 123456 | 房东 |
| landlord2 | 123456 | 房东 |
| tenant1 | 123456 | 租客 |
| tenant2 | 123456 | 租客 |

## 主要功能

### 租客功能
- 搜索筛选房源
- 查看房源详情
- 创建租赁订单
- 在线支付
- 查看/签署合同
- 接收消息通知

### 房东功能
- 发布房源
- 管理房源（上下架、编辑、删除）
- 查看订单
- 签署合同
- 查看收益
