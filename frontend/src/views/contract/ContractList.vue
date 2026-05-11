<template>
  <div class="contract-list-page">
    <div class="container">
      <div class="page-header">
        <h1>{{ currentUserRole === 'LANDLORD' ? '租约合同' : '我的合同' }}</h1>
        <p class="text-secondary">查看和管理您的租赁合同</p>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <div v-else-if="contracts.length === 0" class="empty card">
        <div class="card-body text-center">
          <p>📋 暂无合同</p>
          <p class="text-secondary text-sm mt-sm">完成订单支付后将自动生成合同</p>
        </div>
      </div>
      
      <div v-else class="contract-list">
        <div v-for="contract in contracts" :key="contract.id" class="contract-item card">
          <div class="contract-header">
            <span class="contract-no">合同编号：{{ contract.contractNo }}</span>
            <span class="tag" :class="statusClass(contract.status)">{{ statusText(contract.status) }}</span>
          </div>
          
          <div class="contract-content" @click="viewDetail(contract.id)">
            <div class="contract-info">
              <h3>{{ contract.houseAddress }}</h3>
              <div class="contract-meta mt-sm">
                <p>📅 租期：{{ contract.startDate }} 至 {{ contract.endDate }}</p>
                <p>💰 月租金：¥{{ contract.rentalAmount }}</p>
                <p>👤 {{ contract.landlordId ? `房东ID：${contract.landlordId}` : `租客ID：${contract.tenantId}` }}</p>
              </div>
            </div>
          </div>
          
          <div class="contract-footer">
            <span class="text-secondary text-sm">创建于 {{ formatDate(contract.createdAt) }}</span>
            <div class="contract-actions">
              <button class="btn btn-default btn-sm" @click="viewDetail(contract.id)">查看详情</button>
              <button 
                v-if="shouldShowSignButton(contract)"
                class="btn btn-primary btn-sm"
                @click="handleSign(contract)"
              >签署合同</button>
              <a 
                v-if="contract.status === 'SIGNED'" 
                :href="downloadUrl(contract.id)"
                class="btn btn-default btn-sm"
                target="_blank"
              >下载合同</a>
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getContractList, getContractByOrderId, signContract, downloadContract } from '../../api/contract'

export default {
  name: 'ContractList',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const contracts = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = 10
    
    const totalPages = computed(() => Math.ceil(total.value / pageSize))
    
    // 状态文本
    const statusText = (status) => {
      const map = {
        'PENDING': '待签署', // 兼容旧数据
        'PENDING_SIGN': '待签署',
        'SIGNED': '已签署',
        'TENANT_SIGNED': '待房东签署',
        'LANDLORD_SIGNED': '待租客签署',
        'COMPLETED': '已生效',
        'CANCELLED': '已取消',
        'TERMINATED': '已终止'
      }
      return map[status] || status
    }
    
    // 状态样式
    const statusClass = (status) => {
      const map = {
        'PENDING': 'tag-warning', // 兼容旧数据
        'PENDING_SIGN': 'tag-warning',
        'SIGNED': 'tag-success',
        'TENANT_SIGNED': 'tag-warning',
        'LANDLORD_SIGNED': 'tag-warning',
        'COMPLETED': 'tag-success',
        'CANCELLED': 'tag-default',
        'TERMINATED': 'tag-error'
      }
      return map[status] || 'tag-default'
    }
    
    const currentUserRole = ref('')
    const currentUserId = ref('')

    // 检查是否需要我签署
    const shouldShowSignButton = (contract) => {
      // 1. 如果完全未签署，双方都能看到（或根据业务逻辑，通常租客先签，但也可能允许房东先签）
      // 这里为了简单，PENDING状态双方可见
      if (contract.status === 'PENDING' || contract.status === 'PENDING_SIGN') return true
      
      // 2. 如果是待房东签署，且我是房东 -> 显示
      if (contract.status === 'TENANT_SIGNED' && currentUserRole.value === 'LANDLORD') return true
      
      // 3. 如果是待租客签署，且我是租客 -> 显示
      if (contract.status === 'LANDLORD_SIGNED' && currentUserRole.value === 'TENANT') return true
      
      return false
    }

    // 加载合同列表
    const loadContracts = async () => {
      loading.value = true
      try {
        // 从localStorage获取用户信息
        const userInfoStr = localStorage.getItem('userInfo')
        if (!userInfoStr) {
            router.push('/login')
            return
        }
        const userInfo = JSON.parse(userInfoStr)
        const userId = userInfo.userId
        
        // 存储当前用户信息供模板使用
        currentUserId.value = userId
        let userType = (userInfo.role || 'TENANT').toUpperCase() // 强制大写
        
        // 兼容性处理
        if (userType === 'ROLE_TENANT') userType = 'TENANT'
        if (userType === 'ROLE_LANDLORD') userType = 'LANDLORD'
        
        currentUserRole.value = userType

        // 检查是否有订单ID参数
        const orderId = route.query.orderId
        if (orderId) {
          // 根据订单ID查询合同
          const res = await getContractByOrderId(orderId)
          if (res.data) {
            contracts.value = [res.data]
            total.value = 1
          } else {
            contracts.value = []
            total.value = 0
          }
        } else {
          // 常规查询：根据用户ID和用户类型
          const res = await getContractList({
            userId,
            userType,
            page: currentPage.value,
            size: pageSize
          })
          contracts.value = res.data?.records || []
          total.value = res.data?.total || 0
        }
      } catch (error) {
        console.error('加载合同失败', error)
        contracts.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }
    
    // 查看详情
    const viewDetail = (id) => {
      if (id) {
        router.push(`/contract/${id}`)
      }
    }
    
    // 签署合同
    const handleSign = async (contract) => {
      if (!confirm('确认签署此合同？签署后具有法律效力。')) return
      
      try {
        await signContract(contract.id, {
          signature: '电子签名', 
          userId: currentUserId.value,
          userType: currentUserRole.value,
          ipAddress: '127.0.0.1' 
        })
        
        // 重新加载列表以获取最新状态
        await loadContracts()
        
        alert('合同签署成功！')
      } catch (error) {
        console.error('签署失败', error)
        alert(error.response?.data?.message || error.message || '签署失败')
      }
    }
    
    // 下载合同URL
    const downloadUrl = (id) => {
      return downloadContract(id)
    }
    
    // 格式化时间
    const formatDate = (dateStr) => {
      if (!dateStr) return '未知时间'
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    }
    
    // 翻页
    const changePage = (page) => {
      currentPage.value = page
      loadContracts()
    }
    
    onMounted(() => {
      loadContracts()
    })
    
    // 监听路由参数变化
    watch(() => route.query.orderId, () => {
      loadContracts()
    })
    
    return {
      contracts,
      loading,
      total,
      currentPage,
      pageSize,
      totalPages,
      statusText,
      statusClass,
      viewDetail,
      handleSign,
      downloadUrl,
      formatDate,
      changePage,
      shouldShowSignButton,
      currentUserRole
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

.contract-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.contract-item {
  overflow: hidden;
}

.contract-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  background: #fafafa;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
}

.contract-no {
  font-weight: 500;
}

.contract-content {
  padding: var(--spacing-md);
  cursor: pointer;
  transition: background 0.2s;
}

.contract-content:hover {
  background: #f9f9f9;
}

.contract-info h3 {
  font-size: 16px;
  margin-bottom: var(--spacing-sm);
}

.contract-meta {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  color: var(--text-secondary);
  font-size: 14px;
}

.contract-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  border-top: 1px solid var(--border-color);
  background: #fafafa;
}

.contract-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.page-info {
  padding: 0 var(--spacing-md);
  color: var(--text-secondary);
}
</style>
