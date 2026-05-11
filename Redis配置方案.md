# Redis/Redisson 配置方案

## 📊 服务Redis需求分析

### 🔴 强烈需要Redis的服务

#### 1. 用户服务 (8081) ⭐⭐⭐
**使用场景**:
- ✅ JWT Token缓存和黑名单管理
- ✅ 登录Session管理
- ✅ 用户信息缓存（减少数据库查询）
- ✅ 验证码存储和验证
- ✅ 登录限流（防止暴力破解）

**缓存键设计**:
```
user:token:{userId}          # 用户Token
user:info:{userId}           # 用户信息缓存
user:login:limit:{username}  # 登录限流
user:captcha:{sessionId}     # 验证码
```

#### 2. 订单服务 (8083) ⭐⭐⭐
**使用场景**:
- ✅ 分布式锁（防止重复下单）
- ✅ 订单状态缓存
- ✅ 订单编号生成（Redis INCR）
- ✅ 订单超时自动取消（延迟队列）

**缓存键设计**:
```
order:lock:{userId}:{houseId}  # 下单锁
order:info:{orderId}           # 订单缓存
order:no:seq                   # 订单号序列
order:timeout:{orderId}        # 超时订单
```

#### 3. 支付服务 (8084) ⭐⭐⭐
**使用场景**:
- ✅ 支付流水号生成（分布式锁+INCR）
- ✅ 防重复支付（分布式锁）
- ✅ 支付状态缓存
- ✅ 账户余额缓存
- ✅ 支付超时自动关闭

**缓存键设计**:
```
payment:lock:{orderId}         # 支付锁
payment:no:seq                 # 支付流水号序列
payment:status:{paymentId}     # 支付状态缓存
account:balance:{userId}       # 账户余额缓存
```

### 🟡 建议使用Redis的服务

#### 4. 合同服务 (8092) ⭐⭐
**使用场景**:
- 合同编号生成（分布式锁）
- 合同状态缓存
- 签署防重复

**缓存键设计**:
```
contract:lock:{contractId}     # 签署锁
contract:no:seq                # 合同号序列
contract:info:{contractId}     # 合同缓存
```

#### 5. 房源服务 (8082) ⭐
**使用场景**:
- 热门房源列表缓存
- 房源详情缓存
- 房源搜索结果缓存

**缓存键设计**:
```
house:hot:list                 # 热门房源
house:info:{houseId}           # 房源详情
house:search:{query}           # 搜索结果
```

### 🟢 可选使用Redis的服务

#### 6. 通知服务 (8091) ⭐
**使用场景**:
- 未读消息计数缓存
- 消息去重

**缓存键设计**:
```
notification:unread:{userId}   # 未读计数
notification:sent:{msgId}      # 消息去重
```

---

## 🔧 实施方案

### 优先级顺序

**第一批**（核心业务）:
1. 用户服务
2. 订单服务
3. 支付服务

**第二批**（扩展功能）:
4. 合同服务
5. 房源服务

**第三批**（优化功能）:
6. 通知服务

---

## 📝 配置步骤

### 1. 用户服务添加Redisson

#### pom.xml
```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

#### application.yml
```yaml
spring:
  data:
    redis:
      host: 192.168.24.129
      port: 6379
      # password: your_password  # 如果有密码
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

# Redisson配置
redisson:
  config: |
    singleServerConfig:
      address: "redis://192.168.24.129:6379"
      database: 0
      # password: "your_password"  # 如果有密码
      connectionPoolSize: 64
      connectionMinimumIdleSize: 10
      timeout: 3000
```

### 2. 订单服务添加Redisson

同样的配置,但使用不同的database:
```yaml
spring:
  data:
    redis:
      database: 1  # 订单服务使用database 1
```

### 3. 支付服务添加Redisson

```yaml
spring:
  data:
    redis:
      database: 2  # 支付服务使用database 2
```

### 4. 合同服务添加Redisson

```yaml
spring:
  data:
    redis:
      database: 3  # 合同服务使用database 3
```

### 5. 房源服务添加Redisson

```yaml
spring:
  data:
    redis:
      database: 4  # 房源服务使用database 4
```

### 6. 通知服务添加Redisson

```yaml
spring:
  data:
    redis:
      database: 5  # 通知服务使用database 5
```

---

## 💻 使用示例

### 1. 分布式锁示例（订单服务）

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final RedissonClient redissonClient;
    
    public Long createOrder(CreateOrderDTO dto) {
        // 防止重复下单: 每个用户对同一房源同时只能下一单
        String lockKey = "order:lock:" + dto.getUserId() + ":" + dto.getHouseId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁,最多等待5秒,锁自动释放时间10秒
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("操作太频繁,请稍后再试");
            }
            
            // 执行下单逻辑
            Order order = new Order();
            // ... 业务逻辑
            orderMapper.insert(order);
            
            return order.getId();
        } catch (InterruptedException e) {
            throw new RuntimeException("获取锁失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 2. 缓存示例（用户服务）

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final RedissonClient redissonClient;
    private final UserMapper userMapper;
    
    public User getUserInfo(Long userId) {
        String cacheKey = "user:info:" + userId;
        RBucket<User> bucket = redissonClient.getBucket(cacheKey);
        
        // 尝试从缓存获取
        User user = bucket.get();
        if (user != null) {
            return user;
        }
        
        // 缓存未命中,从数据库查询
        user = userMapper.selectById(userId);
        if (user != null) {
            // 写入缓存,30分钟过期
            bucket.set(user, 30, TimeUnit.MINUTES);
        }
        
        return user;
    }
}
```

### 3. 计数器示例（支付服务）

```java
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    
    private final RedissonClient redissonClient;
    
    public String generatePaymentNo() {
        // 使用Redis INCR生成唯一流水号
        RAtomicLong counter = redissonClient.getAtomicLong("payment:no:seq");
        long seq = counter.incrementAndGet();
        
        // 格式: PAY + 日期 + 序列号
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("PAY%s%08d", date, seq);
    }
}
```

---

## ✅ 验证Redis连接

### 测试连接
```bash
# 在命令行测试Redis连接
redis-cli -h 192.168.24.129 -p 6379 ping
# 应该返回: PONG
```

### 查看Redis数据
```bash
# 连接到Redis
redis-cli -h 192.168.24.129

# 切换到database 0 (用户服务)
SELECT 0
KEYS user:*

# 切换到database 1 (订单服务)
SELECT 1
KEYS order:*

# 切换到database 2 (支付服务)
SELECT 2
KEYS payment:*
```

---

## 📊 Database分配表

| Database | 服务 | 用途 |
|----------|------|------|
| 0 | 用户服务 | Token、Session、用户信息缓存 |
| 1 | 订单服务 | 订单锁、订单缓存、订单号生成 |
| 2 | 支付服务 | 支付锁、支付缓存、流水号生成 |
| 3 | 合同服务 | 合同锁、合同缓存、合同号生成 |
| 4 | 房源服务 | 房源缓存、热门房源、搜索缓存 |
| 5 | 通知服务 | 未读计数、消息去重 |

---

## 🚨 注意事项

1. **Redis密码**: 如果192.168.24.129的Redis设置了密码,需要在配置中添加
2. **网络连通性**: 确保服务器能访问192.168.24.129:6379
3. **防火墙**: 确保6379端口已开放
4. **持久化**: 建议Redis开启AOF持久化,防止数据丢失
5. **监控**: 建议使用Redis的监控工具,如Redis Commander或RedisInsight

---

## 📦 下一步行动

**立即实施**: 用户、订单、支付服务（核心业务）
**后续优化**: 合同、房源、通知服务（扩展功能）

**需要我帮你配置哪个服务？**

---

**Redis服务器**: 192.168.24.129:6379  
**版本**: 1.0.0  
**创建时间**: 2026-01-27
