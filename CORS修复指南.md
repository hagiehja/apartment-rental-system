# Nacos Gateway CORS 配置修复步骤

## 问题原因
Gateway 从 Nacos 配置中心加载配置，Nacos 中的配置覆盖了本地 `application.yml`，导致修改本地文件无效。

## 解决方案

### 方案 1：更新 Nacos 配置（推荐）

1. **打开 Nacos 控制台**  
   访问: http://192.168.24.129:8848/nacos  
   账号: nacos / nacos

2. **进入配置管理**  
   点击左侧菜单：配置管理 → 配置列表

3. **编辑 Gateway 配置**  
   找到 `apartment-gateway.yaml` (Group: DEFAULT_GROUP)  
   点击"编辑"

4. **修改 CORS 配置**  
   找到第 299 行左右：
   ```yaml
   allowedOrigins: "*"
   ```
   改为：
   ```yaml
   allowedOriginPatterns: "*"
   ```

5. **发布配置**  
   点击"发布"按钮

6. **等待自动刷新**  
   Gateway 会在几秒内自动加载新配置，**无需重启**

---

### 方案 2：禁用 Nacos Config（临时方案）

如果您不想使用 Nacos 配置中心，可以：

1. **修改 bootstrap.yml**
   注释掉 config 部分：
   ```yaml
   # config:
   #   server-addr: 192.168.24.129:8848
   #   namespace: public
   #   file-extension: yaml
   #   group: DEFAULT_GROUP
   ```

2. **重启 Gateway**
   这样 Gateway 将使用本地 application.yml（已修复）

---

## 验证修复

修复后，访问以下地址应该正常返回：
- http://localhost:8080/api/user/login (POST)
- http://localhost:8080/api/house/list (GET)

不应再出现 CORS 500 错误。
