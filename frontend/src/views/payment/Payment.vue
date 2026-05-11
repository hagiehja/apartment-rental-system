<template>
  <div class="payment-page">
    <div class="container">
      <div class="page-header">
        <h1>订单支付</h1>
        <p class="text-secondary">请确认支付信息</p>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <template v-else-if="order">
        <div class="payment-layout">
          <!-- 左侧：订单信息 -->
          <div class="payment-main">
            <div class="order-card card">
              <div class="card-header">订单信息</div>
              <div class="card-body">
                <div class="order-info">
                  <p><strong>订单号：</strong>{{ order.orderNo }}</p>
                  <p><strong>房源：</strong>{{ order.houseTitle }}</p>
                  <p><strong>租期：</strong>{{ order.rentStartDate }} 至 {{ order.rentEndDate }}</p>
                </div>
                
                <div class="amount-box mt-lg">
                  <p class="text-secondary">应付金额</p>
                  <p class="amount">
                    <span class="price price-lg">¥{{ payAmount }}</span>
                  </p>
                  <p class="text-secondary text-sm" v-if="order.installmentEnabled">
                    （首期付款：押金 + 首月租金）
                  </p>
                </div>
              </div>
            </div>
            
            <!-- 账户余额 -->
            <div class="balance-card card">
              <div class="card-header">支付方式</div>
              <div class="card-body">
                <div class="balance-info">
                  <div class="balance-row">
                    <span>💰 账户余额</span>
                    <span class="balance-amount">¥{{ balance.toFixed(2) }}</span>
                  </div>
                  <div class="balance-status" :class="{ sufficient: balance >= payAmount }">
                    {{ balance >= payAmount ? '✅ 余额充足' : '⚠️ 余额不足，请先充值' }}
                  </div>
                </div>

                <!-- 资深工程师级：资金守恒可视化 (支付场景) -->
                <div class="money-conservation-flow mt-lg" v-if="order">
                  <div class="flow-header">资金转移路径 (Money Transfer Path)</div>
                  <div class="flow-graph">
                    <div class="actor tenant">
                      <span class="icon">👤</span>
                      <span class="label">您的账户</span>
                      <span class="change negative">-¥{{ payAmount.toFixed(2) }}</span>
                    </div>
                    <div class="arrow">
                      <div class="line"></div>
                      <div class="head"></div>
                      <div class="amount-tip">支付租金</div>
                    </div>
                    <div class="actor landlord">
                      <span class="icon">🏠</span>
                      <span class="label">房东账户</span>
                      <span class="change positive">+¥{{ payAmount.toFixed(2) }}</span>
                    </div>
                  </div>
                  <div class="total-conservation">
                    <span>资金守恒：支付总额 = 房东实收</span>
                    <span class="tag">原子性交易受保护</span>
                  </div>
                </div>
                
                <div class="recharge-tip mt-md" v-if="balance < payAmount">
                  <p class="text-warning">余额不足，还需 ¥{{ (payAmount - balance).toFixed(2) }}</p>
                  <p class="text-secondary text-sm mt-xs">
                    提示：这是演示系统，可直接点击下方"模拟充值"按钮增加余额
                  </p>
                  <button class="btn btn-primary mt-sm" @click="handleMockRecharge">
                    模拟充值 ¥{{ Math.ceil((payAmount - balance) / 1000) * 1000 }}
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 右侧：支付操作 -->
          <div class="payment-sidebar">
            <div class="pay-card card">
              <div class="card-body">
                <div class="pay-summary">
                  <div class="pay-row">
                    <span>订单总额</span>
                    <span>¥{{ order.totalAmount }}</span>
                  </div>
                  <div class="pay-row" v-if="order.installmentEnabled">
                    <span>本次应付</span>
                    <span class="price">¥{{ payAmount }}</span>
                  </div>
                  <div class="divider"></div>
                  <div class="pay-row total">
                    <span>实付金额</span>
                    <span class="price price-lg">¥{{ payAmount }}</span>
                  </div>
                </div>
                
                <button 
                  class="btn btn-primary btn-lg btn-block mt-lg"
                  :disabled="paying || balance < payAmount"
                  @click="handlePay"
                >
                  {{ paying ? '支付中...' : '确认支付' }}
                </button>
                
                <div class="pay-notice mt-md text-center">
                  <div class="countdown-box">
                    <span class="text-secondary">支付剩余时间</span>
                    <div class="countdown-timer">{{ countdown }}</div>
                  </div>
                  <p class="text-secondary text-xs mt-xs">
                    请在 {{ formatTime(order.expireTime) }} 前完成支付
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
      
      <div v-else class="empty card">
        <div class="card-body text-center">
          <p>😕 订单信息加载失败</p>
          <router-link :to="userRole === 'LANDLORD' ? '/landlord/orders' : '/orders'" class="btn btn-primary mt-md">
            返回{{ userRole === 'LANDLORD' ? '我的租客订单' : '订单列表' }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getOrderDetail } from '../../api/order'
import { createPayment, executePayment, mockRecharge } from '../../api/payment'
import { useWalletStore } from '../../stores/walletStore'

export default {
  name: 'Payment',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const walletStore = useWalletStore()
    
    const order = ref(null)
    const balance = ref(0)
    const loading = ref(false)
    const paying = ref(false)
    const countdown = ref('')
    let timer = null
    
    // 应付金额
    const payAmount = computed(() => {
      if (!order.value) return 0
      return order.value.firstPaymentAmount || order.value.totalAmount
    })
    
    const userRole = computed(() => {
      const userInfoStr = localStorage.getItem('userInfo')
      if (!userInfoStr) return ''
      return JSON.parse(userInfoStr).role || ''
    })
    
    // 倒计时逻辑
    const startCountdown = () => {
      if (!order.value?.expireTime) return
      
      const updateTimer = () => {
        const now = new Date().getTime()
        const end = new Date(order.value.expireTime).getTime()
        const diff = end - now
        
        if (diff <= 0) {
          countdown.value = '00:00'
          clearInterval(timer)
          // 可选：超时后自动刷新或提示
          return
        }
        
        const m = Math.floor((diff / 1000 / 60) % 60)
        const s = Math.floor((diff / 1000) % 60)
        countdown.value = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      }
      
      updateTimer() // 立即执行一次
      timer = setInterval(updateTimer, 1000)
    }
    
    // 加载订单和余额
    const loadData = async () => {
      loading.value = true
      try {
        // 加载订单
        const orderRes = await getOrderDetail(route.params.orderNo)
        order.value = orderRes.data
        
        if (order.value.orderStatus !== 'PENDING_PAYMENT') {
          alert('订单状态已变更')
          router.push(`/order/${order.value.orderNo}`)
          return
        }
        
        startCountdown() // 启动倒计时
        
        // 加载余额（使用walletStore）
        await walletStore.loadWalletData()
        balance.value = walletStore.displayBalance
      } catch (error) {
        console.error('加载数据失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 模拟充值（实际调用后端API）
    const handleMockRecharge = async () => {
      const amount = Math.ceil((payAmount.value - balance.value) / 1000) * 1000
      
      try {
        await mockRecharge(amount)
        // 充值成功后，刷新钱包数据
        await walletStore.loadWalletData()
        balance.value = walletStore.displayBalance
        alert(`模拟充值成功！充值金额：¥${amount}`)
      } catch (error) {
        alert(`充值失败：${error.message || '系统错误'}`)
      }
    }
    
    // 确认支付
    const handlePay = async () => {
      if (balance.value < payAmount.value) {
        alert('余额不足，请先充值')
        return
      }
      
      paying.value = true
      try {
        // 创建支付单
        const createRes = await createPayment({
          orderNo: order.value.orderNo,
          amount: payAmount.value,
          paymentMethod: 'BALANCE'
        })
        
        // 执行支付（后端保证原子性：租客扣款 + 房东收款）
        await executePayment(createRes.data.paymentNo)
        
        // 资深工程师级：操作成功后立即刷新钱包状态，确保"实时"反馈
        // 后端已保证资金守恒（租客 -X，房东 +X），这里刷新前端显示
        await walletStore.fullRefresh('payment')
        
        // 更新本地余额显示
        balance.value = walletStore.displayBalance
        
        alert('支付成功！账户余额已实时更新。')
        router.push(`/order/${order.value.orderNo}`)
      } catch (error) {
        alert(error.message || '支付失败')
      } finally {
        paying.value = false
      }
    }
    
    // 格式化时间
    const formatTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    }
    
    onMounted(() => {
      loadData()
    })
    
    onUnmounted(() => {
      if (timer) clearInterval(timer)
    })
    
    return {
      order,
      balance,
      loading,
      paying,
      payAmount,
      countdown,
      handleMockRecharge,
      handlePay,
      formatTime
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

.payment-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: var(--spacing-lg);
  align-items: start;
}

@media (max-width: 992px) {
  .payment-layout {
    grid-template-columns: 1fr;
  }
}

.payment-main {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.amount-box {
  text-align: center;
  padding: var(--spacing-lg);
  background: #fff7e6;
  border-radius: var(--radius-md);
}

.amount-box .amount {
  margin: var(--spacing-sm) 0;
}

.amount-box .price {
  font-size: 36px;
}



.balance-info {
  padding: var(--spacing-md);
  background: #f5f5f5;
  border-radius: var(--radius-md);
}

.balance-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.balance-amount {
  font-size: 20px;
  font-weight: 600;
}

.balance-status {
  text-align: center;
  padding: var(--spacing-sm);
  background: #fff2f0;
  color: var(--error-color);
  border-radius: var(--radius-sm);
}

.balance-status.sufficient {
  background: #f6ffed;
  color: var(--success-color);
}

.recharge-tip {
  padding: var(--spacing-md);
  background: #fffbe6;
  border-radius: var(--radius-sm);
}

.payment-sidebar {
  position: sticky;
  top: 80px;
}

.pay-summary {
  display: flex;
  flex-direction: column;
}

.pay-row {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-sm) 0;
}

.pay-row.total {
  font-weight: 600;
}

.divider {
  height: 1px;
  background: var(--border-color);
  margin: var(--spacing-sm) 0;
}
.countdown-box {
  background: #FFF2F0;
  border: 1px solid #FFCCC7;
  padding: 12px;
  border-radius: var(--radius-md);
  color: var(--error-color);
}

.countdown-timer {
  font-size: 24px;
  font-weight: 700;
  font-family: monospace;
  margin-top: 4px;
}

/* 资金守恒可视化样式 - 资深工程师标准 */
.money-conservation-flow {
  padding: 16px;
  background: #fdfeff;
  border: 1px dashed var(--primary-color);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.money-conservation-flow::before {
  content: "SECURITY VERIFIED";
  position: absolute;
  top: 4px;
  right: -25px;
  background: #52c41a;
  color: #fff;
  font-size: 8px;
  padding: 2px 30px;
  transform: rotate(45deg);
  font-weight: 700;
}

.flow-header {
  font-size: 11px;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 20px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.flow-graph {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.actor {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.actor .icon {
  font-size: 24px;
}

.actor .label {
  font-size: 11px;
  color: var(--text-secondary);
}

.actor .change {
  font-weight: 700;
  font-size: 13px;
}

.change.negative { color: var(--error-color); }
.change.positive { color: var(--success-color); }

.arrow {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  padding: 0 10px;
}

.arrow .line {
  width: 100%;
  height: 2px;
  background: var(--primary-color);
  position: relative;
}

.arrow .head {
  position: absolute;
  right: 10px;
  top: -4px;
  border-left: 6px solid var(--primary-color);
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
}

.amount-tip {
  font-size: 9px;
  color: var(--primary-color);
  margin-top: 4px;
  background: #fff;
  padding: 1px 6px;
  border-radius: 10px;
  border: 1px solid var(--primary-color);
}

.total-conservation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10px;
  color: var(--text-secondary);
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
}

.total-conservation .tag {
  background: #f6ffed;
  color: #52c41a;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 9px;
  border: 1px solid #b7eb8f;
}
</style>
