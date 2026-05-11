<template>
  <div class="wallet-page">
    <div class="container">
      <div class="page-header">
        <h1>我的钱包</h1>
        <p class="text-secondary">查看账户余额和交易记录</p>
      </div>
      
      <!-- 手动刷新按钮 -->
      <div class="refresh-bar mb-md">
        <button 
          class="btn btn-small btn-secondary"
          @click="manualRefresh"
          :disabled="loading"
        >
          🔄 {{ loading ? '刷新中...' : '刷新余额' }}
        </button>
        <span class="hint-text">最后更新: 实时同步</span>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <!-- 实时更新提示条 -->
      <div v-if="showRefreshTip" class="refresh-notification animated-slide-down">
        <div class="notification-content">
          <span class="icon">✨</span>
          <div class="text-wrapper">
            <p class="title">
              {{refreshReason === 'refund' ? '💰 退款已到账' : 
                refreshReason === 'payment' ? '💳 支付已完成' : 
                refreshReason === 'income' ? '📈 收入已入账' : 
                '🔄 钱包已更新'}}
            </p>
            <p class="detail" v-if="balanceChangeDetails?.changeAmount">
              <span v-if="balanceChangeDetails.changeAmount > 0" class="positive">
                +¥{{ Math.abs(balanceChangeDetails.changeAmount).toFixed(2) }}
              </span>
              <span v-else class="negative">
                -¥{{ Math.abs(balanceChangeDetails.changeAmount).toFixed(2) }}
              </span>
              <span class="time">{{ balanceChangeDetails.changeAmount > 0 ? '已入账' : '已支出' }}</span>
            </p>
          </div>
        </div>
        <button class="close-btn" @click="showRefreshTip = false">×</button>
      </div>
      
      <template v-else>
        <!-- 余额卡片 -->
        <div class="balance-card card">
          <div class="card-body">
            <div class="balance-grid">
              <div class="balance-item main">
                <span class="balance-label">可用余额</span>
                <span class="balance-value">¥{{ displayBalance?.toFixed(2) || '0.00' }}</span>
              </div>
              <div class="balance-item">
                <span class="balance-label">冻结金额</span>
                <span class="balance-value">¥{{ account.frozenAmount?.toFixed(2) || '0.00' }}</span>
              </div>
              <div class="balance-item">
                <span class="balance-label">累计收入</span>
                <span class="balance-value text-success">¥{{ account.totalIncome?.toFixed(2) || '0.00' }}</span>
              </div>
              <div class="balance-item">
                <span class="balance-label">累计支出</span>
                <span class="balance-value text-error">¥{{ account.totalExpense?.toFixed(2) || '0.00' }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 交易记录 -->
        <div class="transaction-card card mt-md">
          <div class="card-header">
            <h3>📝 交易明细</h3>
          </div>
          <div class="card-body">
            <div v-if="transactions.length === 0" class="empty-state text-center py-lg">
              <p class="text-secondary">暂无交易记录</p>
            </div>
            <table v-else class="transaction-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>类型</th>
                  <th>金额</th>
                  <th>关联单号</th>
                  <th>备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in transactions" :key="item.transactionId">
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td>
                    <span class="tag" :class="getTypeClass(item.transactionType)">
                      {{ getTypeText(item.transactionType) }}
                    </span>
                  </td>
                  <td :class="getAmountClass(item.transactionType)">
                    {{ item.transactionType === 'INCOME' || item.transactionType === 'REFUND' ? '+' : '-' }}
                    ¥{{ Math.abs(item.amount).toFixed(2) }}
                  </td>
                  <td class="text-xs text-secondary">{{ item.relatedNo || '-' }}</td>
                  <td>{{ item.remark }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { getAccountBalance, getAccountTransactions } from '../../api/payment'
import { useWalletStore } from '../../stores/walletStore'

export default {
  name: 'Wallet',
  setup() {
    const walletStore = useWalletStore()
    const account = ref({})
    const transactions = ref([])
    const loading = ref(false)
    
    // 用于显示实时更新提示
    const showRefreshTip = ref(false)
    const refreshReason = ref('')
    const balanceChangeDetails = ref(null)
    
    // 加载数据
    const loadData = async () => {
      loading.value = true
      try {
        await walletStore.loadWalletData()
        account.value = walletStore.account
        transactions.value = walletStore.transactions
      } catch (error) {
        console.error('加载钱包数据失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 格式化时间
    const formatDateTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    }

    // 类型文本
    const getTypeText = (type) => {
      const map = {
        'PAYMENT': '支付',
        'INCOME': '收入',
        'REFUND': '退款',
        'WITHDRAW': '提现'
      }
      return map[type] || type
    }

    // 类型样式
    const getTypeClass = (type) => {
      const map = {
        'PAYMENT': 'tag-default',
        'INCOME': 'tag-success',
        'REFUND': 'tag-warning',
        'WITHDRAW': 'tag-primary'
      }
      return map[type] || ''
    }

    // 金额样式
    const getAmountClass = (type) => {
       if (type === 'INCOME' || type === 'REFUND') return 'text-success font-bold'
       return 'text-error font-bold'
    }
    
    /**
     * 监听钱包余额变化（实时更新）
     * 当其他页面（如合同详情）执行支付/退款时，本页面自动更新
     */
    let unsubscribe = null
    const setupBalanceListener = () => {
      unsubscribe = walletStore.onBalanceChange((reason, details) => {
        console.log('💳 钱包余额变化监听事件:', reason, details)
        
        // 避免无限循环：initial_load 事件来自 loadData() 内部，不应该再次调用 loadData()
        if (reason === 'initial_load') return
        
        refreshReason.value = reason
        balanceChangeDetails.value = details
        showRefreshTip.value = true
        
        // 重新加载数据
        loadData()
        
        // 3秒后自动隐藏提示
        setTimeout(() => {
          showRefreshTip.value = false
        }, 3000)
      })
    }
    
    /**
     * 手动刷新钱包数据
     */
    const manualRefresh = async () => {
      loading.value = true
      try {
        await walletStore.fullRefresh('manual_refresh')
        account.value = walletStore.account
        transactions.value = walletStore.transactions
        showRefreshTip.value = true
        refreshReason.value = 'manual_refresh'
        
        setTimeout(() => {
          showRefreshTip.value = false
        }, 2000)
      } finally {
        loading.value = false
      }
    }
    
    onMounted(() => {
      // 初始化监听器
      setupBalanceListener()
      // 页面进入时刷新钱包数据
      loadData()
    })
    
    onUnmounted(() => {
      // 页面卸载时取消监听
      if (unsubscribe) {
        unsubscribe()
      }
    })
    
    return {
      account,
      transactions,
      loading,
      showRefreshTip,
      refreshReason,
      balanceChangeDetails,
      displayBalance: walletStore.displayBalance,
      formatDateTime,
      getTypeText,
      getTypeClass,
      getAmountClass,
      manualRefresh
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

.balance-card {
  margin-bottom: var(--spacing-md);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

@media (max-width: 768px) {
  .balance-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.balance-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.balance-item.main {
  grid-column: span 1;
}

.balance-label {
  font-size: 14px;
  opacity: 0.9;
}

.balance-value {
  font-size: 24px;
  font-weight: 600;
}

.balance-item.main .balance-value {
  font-size: 32px;
}

/* 交易表格样式 */
.transaction-table {
  width: 100%;
  border-collapse: collapse;
}

.transaction-table th,
.transaction-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.transaction-table th {
  background-color: #f8f9fa;
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 14px;
}

.transaction-table tr:last-child td {
  border-bottom: none;
}

.text-success { color: #52c41a !important; }
.text-error { color: #ff4d4f !important; }
.text-warning { color: #faad14 !important; }
.font-bold { font-weight: 600; }
.text-xs { font-size: 12px; }

.conservation-tag {
  display: inline-block;
  font-size: 10px;
  background: #f6ffed;
  color: #52c41a;
  padding: 0 4px;
  border: 1px solid #b7eb8f;
  border-radius: 2px;
  margin-left: 4px;
  vertical-align: middle;
}

/* 实时更新通知样式 */
.refresh-notification {
  background: linear-gradient(135deg, #67C26B 0%, #4CAF50 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
  animation: slideDown 0.3s ease-out;
}

.refresh-notification.animated-slide-down {
  animation: slideDown 0.3s ease-out forwards;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notification-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.notification-content .icon {
  font-size: 20px;
}

.text-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.notification-content .title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.notification-content .detail {
  margin: 0;
  font-size: 12px;
  opacity: 0.9;
}

.detail .positive {
  font-weight: 600;
  margin-right: 6px;
}

.detail .negative {
  font-weight: 600;
  color: #ffeb3b;
  margin-right: 6px;
}

.detail .time {
  opacity: 0.8;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.2s;
  padding: 0;
  margin-left: 12px;
}

.close-btn:hover {
  opacity: 1;
}

/* 刷新栏样式 */
.refresh-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #f5f5f5;
  border-radius: 6px;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  border: none;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-small:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hint-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #f0f0f0;
  color: var(--text-secondary);
}

.tag-success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.tag-warning { background: #fff7e6; color: #faad14; border: 1px solid #ffe58f; }
.tag-error { background: #fff1f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.tag-primary { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }

</style>
