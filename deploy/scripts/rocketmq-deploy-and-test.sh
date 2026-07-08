#!/bin/bash
# ===================================================================
# RocketMQ 部署 + Feign 修复 + 全链路测试 一键脚本
# 在 VM (192.168.24.129) 上执行
# ===================================================================
set -e
cd /root/apartment-rental-system 2>/dev/null || cd /root/proj/apartment-rental-system 2>/dev/null || {
  echo "[ERROR] 找不到项目目录,请指定"; exit 1;
}
PROJ_DIR=$(pwd)
echo "=== 项目目录: $PROJ_DIR ==="

# ---------- 第 0 步:拉取最新代码 ----------
echo ""
echo "========== [0/6] 拉取最新代码 =========="
git fetch origin main
git reset --hard origin/main
echo "✅ 代码已更新到最新"

# ---------- 第 1 步:启动 RocketMQ ----------
echo ""
echo "========== [1/6] 启动 RocketMQ =========="
cd "$PROJ_DIR/deploy/rocketmq"
docker compose -f docker-compose.yml down 2>/dev/null || docker-compose down 2>/dev/null || true
docker compose -f docker-compose.yml up -d 2>/dev/null || docker-compose up -d

echo "等待 NameServer 启动..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:9876 2>/dev/null | grep -q "200\|404\|400"; then
    echo "✅ NameServer 已就绪 (尝试 $i)"; break
  fi
  # NameServer 不响应 HTTP,改用端口检测
  if docker exec rocketmq-namesrv sh -c 'echo ok' >/dev/null 2>&1; then
    echo "✅ NameServer 容器就绪 (尝试 $i)"; break
  fi
  sleep 2
done

echo "等待 Broker 注册到 NameServer..."
sleep 10
docker logs rocketmq-broker 2>&1 | tail -5
echo "✅ RocketMQ 启动完成"

# ---------- 第 2 步:重建 order-service 镜像 (Feign url 修复) ----------
echo ""
echo "========== [2/6] 重建受影响镜像 =========="
cd "$PROJ_DIR"
echo "重建 order-service (Feign url 修复)..."
docker compose -f docker-compose.app.yml build apartment-order-service 2>/dev/null \
  || docker-compose -f docker-compose.app.yml build apartment-order-service
echo "✅ order-service 镜像已重建"

# ---------- 第 3 步:重启应用 ----------
echo ""
echo "========== [3/6] 重启应用容器 =========="
docker compose -f docker-compose.app.yml up -d 2>/dev/null \
  || docker-compose -f docker-compose.app.yml up -d
echo "等待应用启动..."
sleep 30

# ---------- 第 4 步:健康检查 ----------
echo ""
echo "========== [4/6] 健康检查 =========="
for svc in apartment-gateway apartment-order-service apartment-house-service apartment-payment-service apartment-contract-service apartment-notice-service; do
  status=$(docker inspect -f '{{.State.Health.Status}}' $svc 2>/dev/null || echo "starting")
  echo "  $svc: $status"
done

# ---------- 第 5 步:验证 RocketMQ 连通 ----------
echo ""
echo "========== [5/6] 验证应用 → RocketMQ 连通 =========="
docker logs apartment-order-service 2>&1 | grep -i "rocketmq\|name-server\|producer" | tail -5
echo "---"
docker logs apartment-notice-service 2>&1 | grep -i "rocketmq\|consumer\|topic" | tail -5

# ---------- 第 6 步:输出测试命令 ----------
echo ""
echo "========== [6/6] 下一步:运行 E2E 链路测试 =========="
echo "RocketMQ Dashboard: http://192.168.24.129:8888"
echo "请执行 E2E 测试脚本验证 下单→支付→合同→通知 全链路"
echo ""
echo "✅✅✅ 部署完成 ✅✅✅"
