#!/bin/bash
# ===================================================================
# MySQL 主从复制健康检查脚本
# 用法: bash mysql-replication-check.sh
# 检查项:
#   1. Slave_IO_Running / Slave_SQL_Running
#   2. 复制延迟 Seconds_Behind_Master
#   3. 主从位点差距
#   4. 关键表数据一致性
#   5. 最近错误
# ===================================================================
MASTER=mysql-ha-master
SLAVE=mysql-ha-slave
MYSQL_PWD=123456
DB=apartment_db

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "  ${GREEN}✅ $1${NC}"; }
fail() { echo -e "  ${RED}❌ $1${NC}"; }
warn() { echo -e "  ${YELLOW}⚠️  $1${NC}"; }

echo "================================================================"
echo "  MySQL 主从复制健康检查  $(date '+%Y-%m-%d %H:%M:%S')"
echo "================================================================"

# ---------- 1. Slave 状态 ----------
echo ""
echo "[1/5] Slave 复制线程状态"
SLAVE_STATUS=$(docker exec $SLAVE mysql -uroot -p$MYSQL_PWD -e 'SHOW SLAVE STATUS\G' 2>/dev/null)
IO_RUN=$(echo "$SLAVE_STATUS"  | grep "Slave_IO_Running:"    | awk '{print $2}')
SQL_RUN=$(echo "$SLAVE_STATUS" | grep "Slave_SQL_Running:"   | awk '{print $2}')
SECONDS_BEHIND=$(echo "$SLAVE_STATUS" | grep "Seconds_Behind_Master:" | awk '{print $2}')
LAST_ERR=$(echo "$SLAVE_STATUS"  | grep "Last_Error:"        | cut -d: -f2-)
LAST_IO_ERR=$(echo "$SLAVE_STATUS" | grep "Last_IO_Error:"   | cut -d: -f2-)
LAST_SQL_ERR=$(echo "$SLAVE_STATUS" | grep "Last_SQL_Error:" | cut -d: -f2-)

[ "$IO_RUN" = "Yes" ]  && ok "Slave_IO_Running: $IO_RUN"  || fail "Slave_IO_Running: $IO_RUN"
[ "$SQL_RUN" = "Yes" ] && ok "Slave_SQL_Running: $SQL_RUN" || fail "Slave_SQL_Running: $SQL_RUN"

# ---------- 2. 延迟 ----------
echo ""
echo "[2/5] 复制延迟"
if [ "$SECONDS_BEHIND" = "0" ]; then
  ok "延迟: 0 秒(实时同步)"
elif [ "$SECONDS_BEHIND" -lt 30 ] 2>/dev/null; then
  warn "延迟: ${SECONDS_BEHIND} 秒"
else
  fail "延迟: ${SECONDS_BEHIND} 秒(严重落后!)"
fi

# ---------- 3. 位点 ----------
echo ""
echo "[3/5] 主从 binlog 位点"
# 注意:必须锚定行首空白,否则 "Master_Log_File:" 会误匹配 "Relay_Master_Log_File:"
M_FILE=$(echo "$SLAVE_STATUS" | grep -E "^\s*Master_Log_File:"      | awk '{print $2}')
M_POS=$(echo "$SLAVE_STATUS"  | grep -E "^\s*Read_Master_Log_Pos:"  | awk '{print $2}')
E_FILE=$(echo "$SLAVE_STATUS" | grep -E "^\s*Relay_Master_Log_File:"| awk '{print $2}')
E_POS=$(echo "$SLAVE_STATUS"  | grep -E "^\s*Exec_Master_Log_Pos:"  | awk '{print $2}')
echo "  Master 位点: $M_FILE:$M_POS"
echo "  Slave  执行: $E_FILE:$E_POS"
if [ "$M_FILE" = "$E_FILE" ] && [ "$M_POS" = "$E_POS" ]; then
  ok "位点完全一致"
else
  warn "位点有差距(复制中)"
fi

# ---------- 4. 数据一致性 ----------
echo ""
echo "[4/5] 关键表数据一致性 (master vs slave)"
for t in house user rental_order payment house_image; do
  M_CNT=$(docker exec $MASTER mysql -uroot -p$MYSQL_PWD $DB -N -e "SELECT COUNT(*) FROM $t" 2>/dev/null || echo "ERR")
  S_CNT=$(docker exec $SLAVE  mysql -uroot -p$MYSQL_PWD $DB -N -e "SELECT COUNT(*) FROM $t" 2>/dev/null || echo "ERR")
  if [ "$M_CNT" = "$S_CNT" ] && [ "$M_CNT" != "ERR" ]; then
    ok "$t: $M_CNT 行(主从一致)"
  elif [ "$M_CNT" = "ERR" ]; then
    warn "$t: 表不存在或查询失败"
  else
    fail "$t: master=$M_CNT slave=$S_CNT(不一致!)"
  fi
done

# ---------- 5. 最近错误 ----------
echo ""
echo "[5/5] 最近错误"
[ -z "$LAST_ERR" ] || [ "$LAST_ERR" = " " ] && ok "无错误" || fail "Last_Error: $LAST_ERR"
[ -z "$LAST_IO_ERR" ] || [ "$LAST_IO_ERR" = " " ] && ok "无 IO 错误" || fail "Last_IO_Error: $LAST_IO_ERR"
[ -z "$LAST_SQL_ERR" ] || [ "$LAST_SQL_ERR" = " " ] && ok "无 SQL 错误" || fail "Last_SQL_Error: $LAST_SQL_ERR"

echo ""
echo "================================================================"
