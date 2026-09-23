#!/usr/bin/env bash
# =====================================================================
# 一键安装 logrotate 配置 - 在宿主机 192.168.24.129 上以 sudo 执行
# 用法:  sudo bash setup-logrotate.sh
# =====================================================================
set -eu

CONF_SRC="$(dirname "$(readlink -f "$0")")/logrotate-apartment.conf"
CONF_DST="/etc/logrotate.d/apartment"

# 1. 检查文件存在
if [ ! -f "$CONF_SRC" ]; then
    echo "❌ 找不到 $CONF_SRC"
    exit 1
fi

# 2. 复制配置
cp "$CONF_SRC" "$CONF_DST"
chmod 644 "$CONF_DST"
echo "✅ 配置已安装到 $CONF_DST"

# 3. Dry-run 测试
echo ""
echo "=== Dry-run 测试 (不实际执行) ==="
logrotate -d "$CONF_DST" 2>&1 | tail -20 || true

# 4. 立即强制执行一次 (清理当前所有大日志)
echo ""
echo "=== 是否立即强制执行一次? (y/N) ==="
read -r answer
if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
    logrotate -fv "$CONF_DST" 2>&1 | tail -30
    echo ""
    echo "✅ 执行完毕"
    echo "📊 当前磁盘使用情况:"
    df -h /
else
    echo "⏭  已跳过. 系统会在每天 /etc/cron.daily/logrotate 自动执行"
fi

# 5. 检查 cron 是否启用
echo ""
echo "=== cron 状态检查 ==="
if systemctl is-active cron 2>/dev/null || systemctl is-active crond 2>/dev/null; then
    echo "✅ cron 服务运行中"
else
    echo "⚠️  cron 未运行, logrotate 不会自动执行!"
    echo "   启动: sudo systemctl start cron || sudo systemctl start crond"
fi
