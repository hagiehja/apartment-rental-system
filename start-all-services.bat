@echo off
chcp 65001 >nul
echo ========================================
echo  公寓租赁系统 - 后端服务启动脚本
echo ========================================
echo.

REM 检查 Java 环境
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Java 环境，请先安装 JDK 17
    pause
    exit /b 1
)

echo [1/7] 启动用户服务 (端口: 8081)...
start "User Service" java -jar apartment-user-service\target\apartment-user-service-0.0.1-SNAPSHOT.jar
timeout /t 10 /nobreak >nul

echo [2/7] 启动房源服务 (端口: 8083)...
start "House Service" java -jar apartment-house-service\target\apartment-house-service-0.0.1-SNAPSHOT.jar
timeout /t 8 /nobreak >nul

echo [3/7] 启动订单服务 (端口: 8088)...
start "Order Service" java -jar apartment-order-service\target\apartment-order-service-0.0.1-SNAPSHOT.jar
timeout /t 8 /nobreak >nul

echo [4/7] 启动支付服务 (端口: 8087)...
start "Payment Service" java -jar apartment-payment-service\target\apartment-payment-service-0.0.1-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

echo [5/7] 启动合同服务 (端口: 8092)...
start "Contract Service" java -jar apartment-contract-service\target\apartment-contract-service-0.0.1-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

echo [6/7] 启动通知服务 (端口: 8091)...
start "Notice Service" java -jar apartment-notice-service\target\apartment-notice-service-0.0.1-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

echo [7/7] 启动 API 网关 (端口: 8080)...
start "API Gateway" java -jar apartment-gateway\target\apartment-gateway-0.0.1-SNAPSHOT.jar

echo.
echo ========================================
echo  ✅ 所有服务启动命令已执行！
echo ========================================
echo.
echo 请等待 30 秒后访问 Nacos 控制台验证:
echo http://192.168.24.129:8848/nacos
echo.
echo 网关地址: http://localhost:8080
echo.
pause
