#!/bin/bash

echo "========================================"
echo " 公寓租赁系统 - 后端服务启动脚本"
echo "========================================"
echo ""

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "[错误] 未检测到 Java 环境，请先安装 JDK 17"
    exit 1
fi

# 创建日志目录
mkdir -p logs

echo "[1/7] 启动用户服务 (端口: 8081)..."
nohup java -jar apartment-user-service/target/apartment-user-service-0.0.1-SNAPSHOT.jar > logs/user-service.log 2>&1 &
sleep 10

echo "[2/7] 启动房源服务 (端口: 8083)..."
nohup java -jar apartment-house-service/target/apartment-house-service-0.0.1-SNAPSHOT.jar > logs/house-service.log 2>&1 &
sleep 8

echo "[3/7] 启动订单服务 (端口: 8088)..."
nohup java -jar apartment-order-service/target/apartment-order-service-0.0.1-SNAPSHOT.jar > logs/order-service.log 2>&1 &
sleep 8

echo "[4/7] 启动支付服务 (端口: 8087)..."
nohup java -jar apartment-payment-service/target/apartment-payment-service-0.0.1-SNAPSHOT.jar > logs/payment-service.log 2>&1 &
sleep 5

echo "[5/7] 启动合同服务 (端口: 8092)..."
nohup java -jar apartment-contract-service/target/apartment-contract-service-0.0.1-SNAPSHOT.jar > logs/contract-service.log 2>&1 &
sleep 5

echo "[6/7] 启动通知服务 (端口: 8091)..."
nohup java -jar apartment-notice-service/target/apartment-notice-service-0.0.1-SNAPSHOT.jar > logs/notice-service.log 2>&1 &
sleep 5

echo "[7/7] 启动 API 网关 (端口: 8080)..."
nohup java -jar apartment-gateway/target/apartment-gateway-0.0.1-SNAPSHOT.jar > logs/gateway.log 2>&1 &

echo ""
echo "========================================"
echo " ✅ 所有服务启动命令已执行！"
echo "========================================"
echo ""
echo "日志文件位置: ./logs/"
echo ""
echo "请等待 30 秒后访问 Nacos 控制台验证:"
echo "http://192.168.24.129:8848/nacos"
echo ""
echo "网关地址: http://localhost:8080"
echo ""
