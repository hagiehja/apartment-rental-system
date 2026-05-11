<template>
  <div class="order-detail-page">
    <div class="container">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <template v-else-if="order">
        <!-- 面包屑 -->
        <div class="breadcrumb mb-md">
          <router-link :to="userRole === 'LANDLORD' ? '/landlord/orders' : '/orders'">
            {{ userRole === 'LANDLORD' ? '我的租客订单' : '我的订单' }}
          </router-link>
          <span>/</span>
          <span>订单详情</span>
        </div>
        
        <!-- 订单状态卡片 -->
        <div class="status-card card" :class="statusCardClass">
          <div class="card-body">
            <div class="status-icon">{{ statusIcon }}</div>
            <div class="status-content">
              <h2>{{ statusText }}</h2>
              <p v-if="order.orderStatus === 'PENDING_PAYMENT'">
                请在 {{ formatDateTime(order.expireTime) }} 前完成支付
              </p>
              <p v-else-if="order.orderStatus === 'CANCELLED'">
                取消原因：{{ order.cancelReason || '无' }}
              </p>
            </div>
            <div class="status-actions" v-if="order.orderStatus === 'PENDING_PAYMENT'">
              <button class="btn btn-primary btn-lg" @click="goPay">立即支付</button>
              <button class="btn btn-default" :disabled="canceling" @click="handleCancel">
                {{ canceling ? '取消中...' : '取消订单' }}
              </button>
            </div>
          </div>
        </div>
        
        <div class="detail-grid">
          <!-- 房源信息 -->
          <div class="info-card card">
            <div class="card-header">房源信息</div>
            <div class="card-body">
              <h3 @click="viewHouse" class="house-title link">{{ order.houseTitle }}</h3>
              <p class="text-secondary">📍 {{ order.houseAddress }}</p>
              <div class="info-row mt-md">
                <span>房东：{{ order.landlordName || '-' }}</span>
              </div>
            </div>
          </div>
          
          <!-- 租期信息 -->
          <div class="info-card card">
            <div class="card-header">租期信息</div>
            <div class="card-body">
              <div class="info-grid">
                <div class="info-item">
                  <label>起租日期</label>
                  <span>{{ order.rentStartDate }}</span>
                </div>
                <div class="info-item">
                  <label>结束日期</label>
                  <span>{{ order.rentEndDate }}</span>
                </div>
                <div class="info-item">
                  <label>租赁时长</label>
                  <span>{{ order.rentMonths }}个月</span>
                </div>
                <div class="info-item">
                  <label>月租金</label>
                  <span class="price">¥{{ order.monthlyRent }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 费用明细 -->
          <div class="info-card card">
            <div class="card-header">费用明细</div>
            <div class="card-body">
              <div class="price-list">
                <div class="price-item">
                  <span>租金（{{ order.rentMonths }}个月）</span>
                  <span>¥{{ order.monthlyRent * order.rentMonths }}</span>
                </div>
                <div class="price-item">
                  <span>押金</span>
                  <span>¥{{ order.deposit }}</span>
                </div>
                <div class="divider"></div>
                <div class="price-item total">
                  <span>订单总额</span>
                  <span class="price price-lg">¥{{ order.totalAmount }}</span>
                </div>
                <div class="price-item" v-if="order.installmentEnabled">
                  <span>首期应付</span>
                  <span class="price">¥{{ order.firstPaymentAmount }}</span>
                </div>
              </div>
              <div class="mt-md" v-if="order.installmentEnabled">
                <span class="tag tag-primary">分期付款</span>
              </div>
            </div>
          </div>
          
          <!-- 订单信息 -->
          <div class="info-card card">
            <div class="card-header">订单信息</div>
            <div class="card-body">
              <div class="info-list">
                <div class="info-row">
                  <label>订单编号</label>
                  <span>{{ order.orderNo }}</span>
                </div>
                <div class="info-row">
                  <label>创建时间</label>
                  <span>{{ formatDateTime(order.createTime) }}</span>
                </div>
                <div class="info-row" v-if="order.payTime">
                  <label>支付时间</label>
                  <span>{{ formatDateTime(order.payTime) }}</span>
                </div>
                <div class="info-row" v-if="order.cancelTime">
                  <label>取消时间</label>
                  <span>{{ formatDateTime(order.cancelTime) }}</span>
                </div>
                <div class="info-row" v-if="order.remark">
                  <label>备注</label>
                  <span>{{ order.remark }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 分期计划 -->
        <div class="installment-card card" v-if="order.installmentEnabled && installments.length > 0">
          <div class="card-header">分期付款计划</div>
          <div class="card-body">
            <table class="installment-table">
              <thead>
                <tr>
                  <th>期数</th>
                  <th>应付金额</th>
                  <th>应付日期</th>
                  <th>支付状态</th>
                  <th>支付时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in installments" :key="item.installmentId">
                  <td>第{{ item.periodNo }}期{{ item.periodNo === 1 ? '（首付）' : '' }}</td>
                  <td class="price">¥{{ item.amount }}</td>
                  <td>{{ item.dueDate }}</td>
                  <td>
                    <span class="tag" :class="item.paymentStatus === 'PAID' ? 'tag-success' : 'tag-warning'">
                      {{ item.paymentStatus === 'PAID' ? '已支付' : '待支付' }}
                    </span>
                  </td>
                  <td>{{ item.paymentTime || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        
        <!-- 相关操作 -->
        <div class="action-card card" v-if="['PAID', 'RENTING', 'COMPLETED'].includes(order.orderStatus)">
          <div class="card-body flex gap-md">
            <router-link :to="`/contracts?orderId=${order.orderId}`" class="btn btn-primary">
              查看合同
            </router-link>
          </div>
        </div>
      </template>
      
      <div v-else class="empty card">
        <div class="card-body text-center">
          <p>😕 订单不存在</p>
          <router-link :to="userRole === 'LANDLORD' ? '/landlord/orders' : '/orders'" class="btn btn-primary mt-md">返回订单列表</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getOrderDetail, getInstallmentPlan, cancelOrder } from '../../api/order'

export default {
  name: 'OrderDetail',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const order = ref(null)
    const installments = ref([])
    const loading = ref(false)

    const userRole = computed(() => {
      const userInfoStr = localStorage.getItem('userInfo')
      if (!userInfoStr) return ''
      return JSON.parse(userInfoStr).role || ''
    })
    
    // 状态图标
    const statusIcon = computed(() => {
      const map = {
        'PENDING_PAYMENT': '⏳',
        'PAID': '✅',
        'CANCELLED': '❌',
        'REFUNDED': '↩️'
      }
      return map[order.value?.orderStatus] || '📋'
    })
    
    // 状态文本
    const statusText = computed(() => {
      const map = {
        'PENDING_PAYMENT': '待支付',
        'PAID': '已支付',
        'RENTING': '已支付',
        'COMPLETED': '已支付',
        'CANCELLED': '已取消',
        'REFUNDED': '已退款'
      }
      return map[order.value?.orderStatus] || order.value?.orderStatus
    })
    
    // 状态卡片样式
    const statusCardClass = computed(() => {
      const map = {
        'PENDING_PAYMENT': 'status-pending',
        'PAID': 'status-success',
        'CANCELLED': 'status-cancelled',
        'REFUNDED': 'status-refunded'
      }
      return map[order.value?.orderStatus] || ''
    })
    
    // 加载订单详情
    const loadOrder = async () => {
      const orderNo = route.params.orderNo
      loading.value = true
      try {
        const res = await getOrderDetail(orderNo)
        order.value = res.data
        
        // 如果开启分期，加载分期计划
        if (order.value.installmentEnabled) {
          const installRes = await getInstallmentPlan(orderNo)
          installments.value = installRes.data || []
        }
      } catch (error) {
        console.error('加载订单失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 去支付
    const goPay = () => {
      router.push(`/payment/${order.value.orderNo}`)
    }
    
    // 查看房源
    const viewHouse = () => {
      router.push(`/house/${order.value.houseId}`)
    }
    
    // 取消订单
    const canceling = ref(false)
    const handleCancel = async () => {
      const reason = prompt('请输入取消原因（可选）：')
      if (reason === null) return
      
      canceling.value = true
      try {
        await cancelOrder(order.value.orderNo, reason || '用户取消')
        order.value.orderStatus = 'CANCELLED'
        order.value.cancelReason = reason || '用户取消'
        alert('订单已取消')
      } catch (error) {
        // 错误信息已由 request.js 处理或这里再次 alert
        if (!error.response || error.response.status !== 500) {
           alert(error.message || '取消失败')
        }
      } finally {
        canceling.value = false
      }
    }
    
    // 格式化日期时间
    const formatDateTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    }
    
    onMounted(() => {
      loadOrder()
    })
    
    return {
      order,
      installments,
      loading,
      userRole,
      statusIcon,
      statusText,
      statusCardClass,
      goPay,
      viewHouse,
      handleCancel,
      canceling,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.breadcrumb {
  display: flex;
  gap: var(--spacing-sm);
  color: var(--text-secondary);
  font-size: 13px;
}

.breadcrumb a {
  color: var(--text-secondary);
}

.breadcrumb a:hover {
  color: var(--primary-color);
}

.status-card {
  margin-bottom: var(--spacing-lg);
}

.status-card .card-body {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.status-icon {
  font-size: 48px;
}

.status-content {
  flex: 1;
}

.status-content h2 {
  font-size: 24px;
  margin-bottom: var(--spacing-xs);
}

.status-content p {
  color: var(--text-secondary);
}

.status-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.status-pending {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
}

.status-success {
  background: linear-gradient(135deg, #f6ffed 0%, #b7eb8f 100%);
}

.status-cancelled {
  background: #f5f5f5;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

.house-title {
  cursor: pointer;
  margin-bottom: var(--spacing-xs);
}

.house-title:hover {
  color: var(--primary-color);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.info-item label {
  font-size: 13px;
  color: var(--text-secondary);
}

.price-list {
  display: flex;
  flex-direction: column;
}

.price-item {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-sm) 0;
}

.price-item.total {
  font-weight: 600;
}

.divider {
  height: 1px;
  background: var(--border-color);
  margin: var(--spacing-sm) 0;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.info-row {
  display: flex;
  gap: var(--spacing-md);
}

.info-row label {
  width: 80px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.installment-table {
  width: 100%;
  border-collapse: collapse;
}

.installment-table th,
.installment-table td {
  padding: var(--spacing-sm) var(--spacing-md);
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.installment-table th {
  background: #fafafa;
  font-weight: 500;
}
</style>
