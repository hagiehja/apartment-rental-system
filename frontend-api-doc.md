# 租房系统前端 API 接口文档

## 文档说明

本文档包含所有后端微服务的API接口定义，供前端开发使用。所有接口均返回统一的响应格式。

## 通用说明

### 统一响应格式

```typescript
interface Result<T> {
  code: number;      // 状态码：200成功，其他为错误码
  message: string;   // 消息
  data: T;          // 数据
}
```

### 认证方式

大部分接口需要在请求头中携带用户身份信息：

```http
X-User-Id: <用户ID>
```

> **注意**：生产环境应该使用JWT Token认证，从Token中解析用户ID

### 分页响应格式

```typescript
interface PageResult<T> {
  records: T[];      // 数据列表
  total: number;     // 总记录数
  size: number;      // 每页大小
  current: number;   // 当前页码
  pages: number;     // 总页数
}
```

---

## 1. 用户服务 (User Service)

**Base URL**: `/user`

### 1.1 用户登录

**接口**: `POST /user/login`

**请求参数**:
```typescript
interface UserLoginDTO {
  account: string;    // 账号（用户名或手机号）
  password: string;   // 密码（明文）
}
```

**请求示例**:
```json
{
  "account": "landlord1",
  "password": "123456"
}
```

**响应数据**:
```typescript
interface UserInfoDTO {
  userId: number;     // 用户ID
  username: string;   // 用户名
  phone: string;      // 手机号
  role: string;       // 角色：TENANT(租客) / LANDLORD(房东) / ADMIN(管理员)
  token: string;      // JWT Token
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "landlord1",
    "phone": "13800000001",
    "role": "LANDLORD",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI..."
  }
}
```

---

## 2. 房源服务 (House Service)

**Base URL**: `/house`

### 2.1 发布房源

**接口**: `POST /house`

**请求头**: 需要 `X-User-Id`

**请求参数**:
```typescript
interface HousePublishDTO {
  title: string;              // 房源标题（必填）
  description?: string;       // 房源描述
  province?: string;          // 省份
  city: string;              // 城市（必填）
  district?: string;         // 区县
  address: string;           // 详细地址（必填）
  area?: number;             // 面积（平方米）
  roomCount?: number;        // 房间数
  hallCount?: number;        // 厅数
  bathroomCount?: number;    // 卫生间数
  floor?: number;            // 楼层
  totalFloor?: number;       // 总楼层
  orientation?: string;      // 朝向
  decoration?: string;       // 装修：ROUGH/SIMPLE/FINE/LUXURY
  rentType: string;          // 出租类型：WHOLE(整租)/SHARED(合租)（必填）
  price: number;             // 月租金（必填）
  paymentMethod?: string;    // 付款方式
  facilities?: string[];     // 配套设施列表
  imageUrls?: string[];      // 图片URL列表
  coverImageIndex?: number;  // 封面图索引（默认0）
}
```

**请求示例**:
```json
{
  "title": "精装两室一厅 地铁口",
  "description": "交通便利，家电齐全",
  "city": "深圳市",
  "district": "南山区",
  "address": "科技园南路88号",
  "area": 85.5,
  "roomCount": 2,
  "hallCount": 1,
  "bathroomCount": 1,
  "floor": 15,
  "totalFloor": 30,
  "orientation": "南北",
  "decoration": "FINE",
  "rentType": "WHOLE",
  "price": 5500,
  "paymentMethod": "押一付三",
  "facilities": ["空调", "冰箱", "洗衣机", "热水器"],
  "imageUrls": ["https://example.com/img1.jpg"],
  "coverImageIndex": 0
}
```

**响应数据**: 返回房源ID
```typescript
type Response = Result<number>; // 房源ID
```

### 2.2 房源列表（分页查询）

**接口**: `GET /house/list`

**查询参数**:
```typescript
interface HouseQueryDTO {
  city?: string;          // 城市
  district?: string;      // 区县
  minPrice?: number;      // 最低价格
  maxPrice?: number;      // 最高价格
  roomCount?: number;     // 房间数
  rentType?: string;      // 出租类型
  page?: number;          // 页码（默认1）
  size?: number;          // 每页大小（默认10）
}
```

**请求示例**:
```
GET /house/list?city=深圳市&minPrice=3000&maxPrice=6000&page=1&size=10
```

**响应数据**:
```typescript
interface HouseListDTO {
  houseId: number;        // 房源ID
  title: string;          // 标题
  city: string;           // 城市
  district: string;       // 区县
  area: number;           // 面积
  roomCount: number;      // 房间数
  hallCount: number;      // 厅数
  rentType: string;       // 出租类型
  price: number;          // 租金
  coverImage: string;     // 封面图
  viewCount: number;      // 浏览次数
  createTime: string;     // 创建时间
}

type Response = Result<PageResult<HouseListDTO>>;
```

### 2.3 房源详情

**接口**: `GET /house/{id}`

**路径参数**: `id` - 房源ID

**响应数据**:
```typescript
interface ImageDTO {
  imageId: number;        // 图片ID
  imageUrl: string;       // 图片URL
  isCover: number;        // 是否封面图（0-否/1-是）
  sortOrder: number;      // 排序
}

interface HouseDetailDTO {
  houseId: number;
  landlordId: number;     // 房东ID
  title: string;
  description: string;
  province: string;
  city: string;
  district: string;
  address: string;
  area: number;
  roomCount: number;
  hallCount: number;
  bathroomCount: number;
  floor: number;
  totalFloor: number;
  orientation: string;
  decoration: string;
  rentType: string;
  price: number;
  paymentMethod: string;
  facilities: string[];   // 配套设施列表
  status: string;         // 状态：AVAILABLE/RENTED/OFFLINE
  viewCount: number;
  images: ImageDTO[];     // 图片列表
  createTime: string;
  updateTime: string;
}

type Response = Result<HouseDetailDTO>;
```

### 2.4 更新房源

**接口**: `PUT /house/{id}`

**请求头**: 需要 `X-User-Id`

**路径参数**: `id` - 房源ID

**请求参数**: 同 `HousePublishDTO`

### 2.5 删除房源

**接口**: `DELETE /house/{id}`

**请求头**: 需要 `X-User-Id`

**路径参数**: `id` - 房源ID

### 2.6 下架房源

**接口**: `PUT /house/{id}/offline`

**请求头**: 需要 `X-User-Id`

**路径参数**: `id` - 房源ID

### 2.7 上架房源

**接口**: `PUT /house/{id}/online`

**请求头**: 需要 `X-User-Id`

**路径参数**: `id` - 房源ID

---

## 3. 订单服务 (Order Service)

**Base URL**: `/order`

### 3.1 创建订单

**接口**: `POST /order`

**请求头**: 需要 `X-User-Id`

**请求参数**:
```typescript
interface OrderCreateDTO {
  houseId: number;              // 房源ID（必填）
  rentStartDate: string;        // 租期开始日期，格式：YYYY-MM-DD（必填，必须是未来日期）
  rentMonths: number;           // 租赁月数（必填，1-24）
  installmentEnabled?: boolean; // 是否分期付款（默认false）
  remark?: string;              // 备注
}
```

**请求示例**:
```json
{
  "houseId": 1,
  "rentStartDate": "2026-02-01",
  "rentMonths": 6,
  "installmentEnabled": false,
  "remark": "希望尽快入住"
}
```

**响应数据**:
```typescript
interface OrderCreateResultDTO {
  orderNo: string;              // 订单编号
  totalAmount: number;          // 订单总金额
  firstPaymentAmount: number;   // 首付金额
  expireTime: string;           // 订单过期时间
}

type Response = Result<OrderCreateResultDTO>;
```

### 3.2 查询订单详情

**接口**: `GET /order/{orderNo}`

**请求头**: 需要 `X-User-Id`

**路径参数**: `orderNo` - 订单编号

**响应数据**:
```typescript
interface OrderDetailDTO {
  orderNo: string;
  tenantId: number;
  landlordId: number;
  houseId: number;
  houseTitle: string;
  rentStartDate: string;
  rentEndDate: string;
  rentMonths: number;
  monthlyRent: number;
  deposit: number;
  totalAmount: number;
  firstPaymentAmount: number;
  installmentEnabled: boolean;
  orderStatus: string;          // 订单状态
  paymentStatus: string;        // 支付状态
  remark: string;
  expireTime: string;
  createTime: string;
  payTime: string;
}

type Response = Result<OrderDetailDTO>;
```

### 3.3 我的订单列表

**接口**: `GET /order/my/list`

**请求头**: 需要 `X-User-Id`

**查询参数**:
```typescript
interface OrderQueryDTO {
  orderStatus?: string;   // 订单状态
  paymentStatus?: string; // 支付状态
  page?: number;          // 页码（默认1）
  size?: number;          // 每页大小（默认10）
}
```

**响应数据**:
```typescript
interface OrderListDTO {
  orderNo: string;
  houseTitle: string;
  totalAmount: number;
  orderStatus: string;
  paymentStatus: string;
  createTime: string;
}

type Response = Result<PageResult<OrderListDTO>>;
```

### 3.4 取消订单

**接口**: `PUT /order/{orderNo}/cancel`

**请求头**: 需要 `X-User-Id`

**路径参数**: `orderNo` - 订单编号

**请求参数**:
```typescript
interface CancelOrderRequest {
  cancelReason?: string;  // 取消原因
}
```

### 3.5 查询分期计划

**接口**: `GET /order/{orderNo}/installments`

**路径参数**: `orderNo` - 订单编号

**响应数据**:
```typescript
interface InstallmentDTO {
  installmentId: number;
  periodNo: number;           // 期数
  amount: number;             // 应付金额
  dueDate: string;            // 应付日期
  paymentStatus: string;      // 支付状态
  paymentNo: string;          // 支付单号
  paymentTime: string;        // 实际支付时间
}

type Response = Result<InstallmentDTO[]>;
```

---

## 4. 支付服务 (Payment Service)

**Base URL**: `/payment`

### 4.1 查询账户余额

**接口**: `GET /payment/account/balance`

**请求头**: 需要 `X-User-Id`

**响应数据**:
```typescript
interface AccountBalanceDTO {
  userId: number;
  balance: number;        // 账户余额
  frozenAmount: number;   // 冻结金额
  totalIncome: number;    // 累计收入
  totalExpense: number;   // 累计支出
}

type Response = Result<AccountBalanceDTO>;
```

### 4.2 创建支付单

**接口**: `POST /payment`

**请求头**: 需要 `X-User-Id`

**请求参数**:
```typescript
interface PaymentCreateDTO {
  orderNo: string;         // 订单号（必填）
  installmentId?: number;  // 分期ID（可选）
  paymentMethod: string;   // 支付方式（必填）：BALANCE/ALIPAY/WECHAT
}
```

**请求示例**:
```json
{
  "orderNo": "ORDER20260130001",
  "paymentMethod": "BALANCE"
}
```

**响应数据**:
```typescript
interface PaymentCreateResultDTO {
  paymentNo: string;      // 支付单号
  amount: number;         // 支付金额
  paymentMethod: string;  // 支付方式
  createTime: string;     // 创建时间
}

type Response = Result<PaymentCreateResultDTO>;
```

### 4.3 执行支付

**接口**: `POST /payment/{paymentNo}/pay`

**请求头**: 需要 `X-User-Id`

**路径参数**: `paymentNo` - 支付单号

**响应**: 返回空数据

### 4.4 申请退款

**接口**: `POST /payment/{paymentNo}/refund`

**请求头**: 需要 `X-User-Id`

**路径参数**: `paymentNo` - 支付单号

**响应**: 返回空数据

---

## 5. 合同服务 (Contract Service)

**Base URL**: `/api/contract`

### 5.1 查询合同详情

**接口**: `GET /api/contract/{contractId}`

**路径参数**: `contractId` - 合同ID

**响应数据**:
```typescript
interface Contract {
  id: number;
  contractNo: string;         // 合同编号
  orderId: number;            // 关联订单ID
  landlordId: number;
  tenantId: number;
  houseId: number;
  houseName: string;
  houseAddress: string;
  rentalAmount: number;       // 月租金
  depositAmount: number;      // 押金
  startDate: string;          // 租赁开始日期
  endDate: string;            // 租赁结束日期
  status: string;             // 状态：PENDING/LANDLORD_SIGNED/COMPLETED/ARCHIVED/CANCELLED
  filePath: string;           // 合同文件路径
  landlordSignedAt: string;   // 房东签署时间
  tenantSignedAt: string;     // 租客签署时间
  createdAt: string;
  updatedAt: string;
}

type Response = Result<Contract>;
```

### 5.2 根据订单ID查询合同

**接口**: `GET /api/contract/order/{orderId}`

**路径参数**: `orderId` - 订单ID

**响应数据**: 同上 `Contract`

### 5.3 签署合同

**接口**: `POST /api/contract/sign/{contractId}`

**路径参数**: `contractId` - 合同ID

**请求参数**:
```typescript
interface SignContractDTO {
  userId: number;      // 签署用户ID
  userType: string;    // 用户类型：LANDLORD/TENANT
}
```

**请求示例**:
```json
{
  "userId": 1,
  "userType": "LANDLORD"
}
```

### 5.4 查询用户的合同列表

**接口**: `GET /api/contract/list`

**查询参数**:
```typescript
interface ContractQueryParams {
  userId: number;      // 用户ID（必填）
  userType: string;    // 用户类型：LANDLORD/TENANT（必填）
  status?: string;     // 合同状态（可选）
  page?: number;       // 页码（默认1）
  size?: number;       // 每页大小（默认10）
}
```

**请求示例**:
```
GET /api/contract/list?userId=1&userType=LANDLORD&status=COMPLETED&page=1&size=10
```

**响应数据**:
```typescript
type Response = Result<IPage<Contract>>;

interface IPage<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
```

### 5.5 下载合同文件

**接口**: `GET /api/contract/download/{contractId}`

**路径参数**: `contractId` - 合同ID

**响应**: 文件下载流

---

## 6. 通知服务 (Notification Service)

**Base URL**: `/api/notification`

### 6.1 查询未读消息数量

**接口**: `GET /api/notification/unread/count`

**查询参数**: `userId` - 用户ID

**请求示例**:
```
GET /api/notification/unread/count?userId=1
```

**响应数据**:
```typescript
type Response = Result<number>; // 未读消息数量
```

### 6.2 查询未读消息列表

**接口**: `GET /api/notification/unread`

**查询参数**: `userId` - 用户ID

**响应数据**:
```typescript
interface NotificationMessage {
  id: number;
  userId: number;
  type: string;           // 消息类型：ORDER/PAYMENT/CONTRACT/SYSTEM
  title: string;          // 标题
  content: string;        // 内容
  bizId: number;          // 业务ID
  isRead: number;         // 是否已读（0-未读,1-已读）
  createdAt: string;      // 创建时间
  readAt: string;         // 阅读时间
}

type Response = Result<NotificationMessage[]>;
```

### 6.3 分页查询消息列表

**接口**: `GET /api/notification/list`

**查询参数**:
```typescript
interface NotificationQueryParams {
  userId: number;       // 用户ID（必填）
  type?: string;        // 消息类型（可选）
  page?: number;        // 页码（默认1）
  size?: number;        // 每页大小（默认10）
}
```

**请求示例**:
```
GET /api/notification/list?userId=1&type=CONTRACT&page=1&size=10
```

**响应数据**:
```typescript
type Response = Result<IPage<NotificationMessage>>;
```

### 6.4 查询消息详情

**接口**: `GET /api/notification/{messageId}`

**路径参数**: `messageId` - 消息ID

**查询参数**: `userId` - 用户ID

**响应数据**:
```typescript
type Response = Result<NotificationMessage>;
```

### 6.5 标记消息为已读

**接口**: `POST /api/notification/read/{messageId}`

**路径参数**: `messageId` - 消息ID

**查询参数**: `userId` - 用户ID

### 6.6 标记所有消息为已读

**接口**: `POST /api/notification/read/all`

**查询参数**: `userId` - 用户ID

---

## 数据字典

### 用户角色 (role / userType)
- `TENANT` - 租客
- `LANDLORD` - 房东
- `ADMIN` - 管理员

### 房源状态 (status)
- `AVAILABLE` - 可租
- `RENTED` - 已出租
- `OFFLINE` - 已下架

### 出租类型 (rentType)
- `WHOLE` - 整租
- `SHARED` - 合租

### 装修情况 (decoration)
- `ROUGH` - 毛坯
- `SIMPLE` - 简装
- `FINE` - 精装
- `LUXURY` - 豪装

### 订单状态 (orderStatus)
- `PENDING_PAYMENT` - 待支付
- `PAID` - 已支付
- `CANCELLED` - 已取消
- `REFUNDED` - 已退款

### 支付状态 (paymentStatus)
- `UNPAID` - 未支付
- `PAID` - 已支付
- `REFUNDED` - 已退款

### 支付方式 (paymentMethod)
- `BALANCE` - 余额支付
- `ALIPAY` - 支付宝
- `WECHAT` - 微信支付

### 合同状态 (status)
- `PENDING` - 待签署
- `LANDLORD_SIGNED` - 房东已签署
- `COMPLETED` - 已完成
- `ARCHIVED` - 已归档
- `CANCELLED` - 已作废

### 消息类型 (type)
- `ORDER` - 订单消息
- `PAYMENT` - 支付消息
- `CONTRACT` - 合同消息
- `SYSTEM` - 系统消息

---

## 测试数据

### 测试用户

| 用户名 | 密码 | 角色 | 手机号 | 用户ID |
|--------|------|------|--------|--------|
| landlord1 | 123456 | LANDLORD | 13800000001 | 1 |
| landlord2 | 123456 | LANDLORD | 13800000002 | 2 |
| landlord3 | 123456 | LANDLORD | 13800000003 | 3 |
| tenant1 | 123456 | TENANT | 13900000001 | 4 |
| tenant2 | 123456 | TENANT | 13900000002 | 5 |
| tenant3 | 123456 | TENANT | 13900000003 | 6 |

### 租客账户余额
- 所有租客初始余额：**100,000元**
- 所有房东初始余额：**0元**

### 房源数据
- 总计6套房源，分别由3个房东发布
- 房源ID范围：1-6

---

## 开发建议

### 1. 接口调用流程

#### 租客租房流程
```
1. 登录 → POST /user/login
2. 浏览房源 → GET /house/list
3. 查看房源详情 → GET /house/{id}
4. 创建订单 → POST /order
5. 查询账户余额 → GET /payment/account/balance
6. 创建支付单 → POST /payment
7. 执行支付 → POST /payment/{paymentNo}/pay
8. 查看合同 → GET /api/contract/order/{orderId}
9. 签署合同 → POST /api/contract/sign/{contractId}
```

#### 房东发布房源流程
```
1. 登录 → POST /user/login
2. 发布房源 → POST /house
3. 查看我的房源 → GET /house/list（可按landlordId过滤）
4. 收到租金通知 → GET /api/notification/unread
5. 查看合同 → GET /api/contract/list
6. 签署合同 → POST /api/contract/sign/{contractId}
```

### 2. 错误处理

所有接口可能返回的错误码：
- `200` - 成功
- `401` - 未登录
- `403` - 无权限
- `404` - 资源不存在
- `500` - 服务器错误

### 3. 前端状态管理建议

- 使用Vuex/Pinia存储用户信息(userId, token, role)
- 使用Axios拦截器自动添加`X-User-Id`请求头
- 统一处理接口响应和错误

### 4. 实时通知建议

- 定期轮询未读消息数量接口
- 或使用WebSocket实现实时推送（需要后端支持）
