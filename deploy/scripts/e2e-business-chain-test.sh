#!/bin/bash
# ===================================================================
# E2E 全链路测试: 下单 → 支付 → 合同 → 通知 (走 RocketMQ)
# 在 VM 上执行
# ===================================================================
GW="http://localhost:8080"
TENANT_ID=851961     # tenant1
LANDLORD_ID=851962   # landlord1

echo "================================================================"
echo "  E2E 全链路测试 - $(date '+%H:%M:%S')"
echo "================================================================"

# ---- 0. 找一个 AVAILABLE 房源 ----
echo ""
echo "[0] 查询可用房源..."
HOUSE_RESP=$(curl -s "$GW/api/house/list?pageSize=1&status=AVAILABLE")
echo "$HOUSE_RESP" | head -c 300
HOUSE_ID=$(echo "$HOUSE_RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data']['records'][0]['houseId'])" 2>/dev/null)
echo ""
echo "  → 选中房源: houseId=$HOUSE_ID"

# ---- 1. 创建订单 ----
echo ""
echo "[1] 创建订单 (tenant=$TENANT_ID, house=$HOUSE_ID)..."
ORDER_RESP=$(curl -s -X POST "$GW/api/order" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $TENANT_ID" \
  -d "{\"houseId\":$HOUSE_ID,\"rentStartDate\":\"2026-08-01\",\"rentMonths\":6,\"installmentEnabled\":false,\"remark\":\"E2E测试\"}")
echo "$ORDER_RESP" | head -c 400
ORDER_NO=$(echo "$ORDER_RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data']['orderNo'])" 2>/dev/null)
echo ""
echo "  → 订单号: $ORDER_NO"

if [ -z "$ORDER_NO" ] || [ "$ORDER_NO" = "None" ]; then
  echo "❌❌❌ 创建订单失败,终止测试"; exit 1
fi
echo "  ✅ 订单创建成功"

# ---- 2. 模拟支付成功 ----
echo ""
echo "[2] 模拟支付成功..."
PAY_RESP=$(curl -s -X POST "$GW/api/order/$ORDER_NO/payment-success" \
  -H "X-User-Id: $TENANT_ID")
echo "$PAY_RESP" | head -c 300
echo ""

# ---- 3. 验证合同生成 ----
echo ""
echo "[3] 查询合同 (应已由 order-service 触发生成)..."
sleep 2
CONTRACT_RESP=$(curl -s "$GW/api/contract/order/$(echo $ORDER_RESP | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['orderId'])" 2>/dev/null)?userId=$TENANT_ID")
echo "$CONTRACT_RESP" | head -c 400
echo ""

# ---- 4. 验证通知 (走 RocketMQ 异步) ----
echo ""
echo "[4] 查询通知 (RocketMQ 异步投递)..."
sleep 3  # 给 RocketMQ 消费留时间
UNREAD=$(curl -s "$GW/api/notification/unread/count?userId=$TENANT_ID")
echo "  未读通知数: $UNREAD"
echo ""
echo "[4b] 最新通知列表..."
curl -s "$GW/api/notification/list?userId=$TENANT_ID&pageNum=1&pageSize=5" | python3 -m json.tool 2>/dev/null | head -30

# ---- 5. RocketMQ 主题与消费情况 ----
echo ""
echo "[5] RocketMQ 消息轨迹 (Dashboard: http://192.168.24.129:8888)..."
echo "  主题: TOPIC_ORDER_PAY_SUCCESS, NOTIFICATION_TOPIC"
echo "  日志检查:"
docker logs apartment-notice-service 2>&1 | grep -i "rocketmq\|收到\|通知消息" | tail -8

echo ""
echo "================================================================"
echo "  E2E 测试完成 - $(date '+%H:%M:%S')"
echo "================================================================"
