<template>
  <div class="order-list-page">
    <div class="container">
      <div class="page-header">
        <h1>我的订单</h1>
        <p class="text-secondary">查看和管理您的租赁订单</p>
      </div>
      
      <!-- 状态筛选 -->
      <div class="filter-tabs">
        <button 
          v-for="tab in statusTabs" 
          :key="tab.key"
          class="tab-item"
          :class="{ active: currentTab === tab.key }"
          @click="filterByStatus(tab)"
        >
          {{ tab.label }}
        </button>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <div v-else-if="orders.length === 0" class="empty card">
        <div class="card-body text-center">
          <p>😊 暂无订单</p>
          <router-link to="/houses" class="btn btn-primary mt-md">去看看房源</router-link>
        </div>
      </div>
      
      <div v-else class="order-list">
        <div v-for="order in orders" :key="order.orderNo" class="order-item card">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ formatDateTime(order.createTime) }}</span>
            <span class="tag" :class="statusClass(order.orderStatus)">{{ statusText(order.orderStatus) }}</span>
          </div>
          
          <div class="order-content" @click="viewDetail(order.orderNo)">
            <div class="house-info">
              <h3>{{ order.houseTitle }}</h3>
              <p class="text-secondary">{{ order.houseAddress }}</p>
              <div class="rent-period mt-sm">
                📅 {{ order.rentStartDate }} 至 {{ order.rentEndDate }}（{{ order.rentMonths }}个月）
              </div>
            </div>
            <div class="order-amount">
              <span class="text-secondary">订单金额</span>
              <span class="price price-lg">¥{{ order.totalAmount }}</span>
            </div>
          </div>
          
          <div class="order-footer">
            <div class="order-tags">
              <span class="tag" :class="paymentStatusClass(order.paymentStatus)">
                {{ paymentStatusText(order.paymentStatus) }}
              </span>
              <span v-if="order.installmentEnabled" class="tag tag-primary">分期付款</span>
            </div>
            <div class="order-actions">
              <button class="btn btn-default btn-sm" @click="viewDetail(order.orderNo)">查看详情</button>
              <button 
                v-if="order.orderStatus === 'PENDING_PAYMENT'"
                class="btn btn-primary btn-sm"
                @click="goPay(order.orderNo)"
              >去支付</button>
              <button 
                v-if="order.orderStatus === 'PENDING_PAYMENT'"
                class="btn btn-default btn-sm"
                @click="handleCancel(order)"
              >取消订单</button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination">
        <button 
          class="page-item" 
          :disabled="currentPage <= 1"
          @click="changePage(currentPage - 1)"
        >上一页</button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button 
          class="page-item" 
          :disabled="currentPage >= totalPages"
          @click="changePage(currentPage + 1)"
        >下一页</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders, cancelOrder } from '../../api/order'

export default {
  name: 'OrderList',
  setup() {
    const router = useRouter()
    
    const orders = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const currentTab = ref('ALL')
    const pageSize = 10
    
    const statusTabs = [
      { label: '全部', key: 'ALL', value: [] },
      { label: '待支付', key: 'PENDING', value: ['PENDING_PAYMENT'] },
      { label: '已支付', key: 'PAID', value: ['PAID', 'RENTING'] },
      { label: '已退款/结束', key: 'REFUND', value: ['REFUNDED', 'CANCELLED', 'COMPLETED'] }
    ]
    
    const totalPages = computed(() => Math.ceil(total.value / pageSize))
    
    // 订单状态文本
    const statusText = (status) => {
      const map = {
        'PENDING_PAYMENT': '待支付',
        'PAID': '已支付',
        'RENTING': '已支付',
        'COMPLETED': '已结束',
        'CANCELLED': '已取消',
        'REFUNDED': '已退款'
      }
      return map[status] || status
    }
    
    // 订单状态样式
    const statusClass = (status) => {
      const map = {
        'PENDING_PAYMENT': 'tag-warning',
        'PAID': 'tag-success',
        'RENTING': 'tag-success',
        'COMPLETED': 'tag-success',
        'CANCELLED': 'tag-default',
        'REFUNDED': 'tag-default'
      }
      return map[status] || 'tag-default'
    }
    
    // 支付状态文本
    const paymentStatusText = (status) => {
      const map = {
        'UNPAID': '未支付',
        'PAID': '已支付',
        'REFUNDED': '已退款'
      }
      return map[status] || status
    }
    
    // 支付状态样式
    const paymentStatusClass = (status) => {
      const map = {
        'UNPAID': 'tag-warning',
        'PAID': 'tag-success',
        'REFUNDED': 'tag-default'
      }
      return map[status] || 'tag-default'
    }
    
    // 加载订单
    const loadOrders = async () => {
      loading.value = true
      try {
        const params = {
          pageNum: currentPage.value,
          pageSize: pageSize
        }
        
        const selectedTab = statusTabs.find(t => t.key === currentTab.value)
        if (selectedTab && selectedTab.value.length > 0) {
          params.orderStatusList = selectedTab.value.join(',')
        }
        
        const res = await getMyOrders(params)
        orders.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (error) {
        console.error('加载订单失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 状态筛选
    const filterByStatus = (tab) => {
      currentTab.value = tab.key
      currentPage.value = 1
      loadOrders()
    }
    
    // 查看详情
    const viewDetail = (orderNo) => {
      router.push(`/order/${orderNo}`)
    }
    
    // 去支付
    const goPay = (orderNo) => {
      router.push(`/payment/${orderNo}`)
    }
    
    // 取消订单
    const handleCancel = async (order) => {
      const reason = prompt('请输入取消原因（可选）：')
      if (reason === null) return // 用户点击取消
      
      try {
        await cancelOrder(order.orderNo, reason || '用户取消')
        order.orderStatus = 'CANCELLED'
        alert('订单已取消')
      } catch (error) {
        alert(error.message || '取消失败')
      }
    }
    
    // 翻页
    const changePage = (page) => {
      currentPage.value = page
      loadOrders()
    }
    
    // 格式化日期时间
    const formatDateTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    }
    
    onMounted(() => {
      loadOrders()
    })
    
    return {
      orders,
      loading,
      total,
      currentPage,
      currentTab,
      pageSize,
      totalPages,
      statusTabs,
      statusText,
      statusClass,
      paymentStatusText,
      paymentStatusClass,
      filterByStatus,
      viewDetail,
      goPay,
      handleCancel,
      changePage,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-color);
}

.tab-item {
  padding: 8px 20px;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.tab-item.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.order-item {
  overflow: hidden;
}

.order-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  background: #fafafa;
  border-bottom: 1px solid var(--border-color);
}

.order-no {
  font-weight: 500;
}

.order-time {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: auto;
}

.order-content {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-md);
  cursor: pointer;
  transition: background 0.2s;
}

.order-content:hover {
  background: #f9f9f9;
}

.house-info h3 {
  font-size: 16px;
  margin-bottom: var(--spacing-xs);
}

.rent-period {
  color: var(--text-secondary);
  font-size: 13px;
}

.order-amount {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  border-top: 1px solid var(--border-color);
  background: #fafafa;
}

.order-tags {
  display: flex;
  gap: var(--spacing-sm);
}

.order-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.page-info {
  padding: 0 var(--spacing-md);
  color: var(--text-secondary);
}
</style>
