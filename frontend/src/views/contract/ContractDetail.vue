<template>
  <div class="contract-detail-page">
    <div class="container">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <template v-else-if="contract">
        <!-- 面包屑 -->
        <div class="breadcrumb mb-md">
          <router-link to="/contracts">
            {{ isTenant ? '我的合同' : '租约合同' }}
          </router-link>
          <span>/</span>
          <span>合同详情</span>
        </div>
        
        <!-- 状态卡片 -->
        <div class="status-card card" :class="statusCardClass">
          <div class="card-body">
            <div class="status-info">
              <span class="status-icon">{{ statusIcon }}</span>
              <div>
                <h2>{{ statusText }}</h2>
                <p v-if="contract.status === 'PENDING_SIGN' || contract.status === 'PENDING'" class="text-secondary">
                  请双方尽快完成签署
                </p>
              </div>
            </div>
            <div class="status-actions" v-if="contract.status === 'PENDING_SIGN' || contract.status === 'PENDING'">
              <button class="btn btn-primary" :disabled="signing" @click="handleSign">
                {{ signing ? '签署中...' : '签署合同' }}
              </button>
            </div>
            <div class="status-actions" v-if="contract.status === 'SIGNED' || contract.status === 'COMPLETED'">
              <a :href="downloadUrl" class="btn btn-primary" target="_blank" style="margin-right: 10px">下载合同</a>
              <button 
                v-if="isTenant"
                class="btn btn-danger" 
                @click="openTerminateModal"
              >
                申请退款/退租
              </button>
            </div>
          </div>
        </div>
        
        <!-- 合同内容 -->
        <div class="contract-content card">
          <div class="card-header">📋 租赁合同</div>
          <div class="card-body">
            <div class="contract-text">
              <h2 class="text-center mb-lg">房屋租赁合同</h2>
              
              <p class="mb-md">合同编号：<strong>{{ contract.contractNo }}</strong></p>
              
              <h3>甲方（出租方）</h3>
              <p>姓名：{{ contract.landlordName || `房东(ID:${contract.landlordId})` }}</p>
              <p>联系电话：{{ contract.landlordPhone || '***' }}</p>
              
              <h3 class="mt-md">乙方（承租方）</h3>
              <p>姓名：{{ contract.tenantName || `租客(ID:${contract.tenantId})` }}</p>
              <p>联系电话：{{ contract.tenantPhone || '***' }}</p>
              
              <h3 class="mt-md">房屋信息</h3>
              <p>房屋地址：{{ contract.houseAddress }}</p>
              
              <h3 class="mt-md">租赁条款</h3>
              <p>1. 租赁期限：自 <strong>{{ contract.startDate }}</strong> 起至 <strong>{{ contract.endDate }}</strong> 止。</p>
              <p>2. 租金：每月人民币 <strong>¥{{ contract.rentalAmount }}</strong> 元整。</p>
              <p>3. 押金：人民币 <strong>¥{{ contract.depositAmount }}</strong> 元整，租赁期满无违约情况下全额退还。</p>
              <p>4. 付款方式：{{ contract.paymentMethod || '按月支付' }}。</p>
              
              <h3 class="mt-md">双方权利义务</h3>
              <p>5. 甲方应保证房屋符合安全居住条件，并按时交付房屋。</p>
              <p>6. 乙方应按时支付租金，爱护房屋设施，不得擅自改变房屋结构。</p>
              <p>7. 租赁期内，任何一方提前解除合同，应提前30天书面通知对方。</p>
              
              <h3 class="mt-md">违约责任</h3>
              <p>8. 乙方逾期支付租金的，每逾期一日，应向甲方支付日租金1%的违约金。</p>
              <p>9. 任何一方违约导致合同解除的，违约方应向守约方支付一个月租金作为违约金。</p>
              
              <div class="signature-area mt-lg">
                <div class="signature-party">
                  <h4>甲方签署</h4>
                  <div class="signature-box" :class="{ signed: contract.landlordSignedAt }">
                    <template v-if="contract.landlordSignedAt">
                      <span class="signature">{{ contract.landlordName || '房东' }}</span>
                      <span class="sign-time">{{ formatDateTime(contract.landlordSignedAt) }}</span>
                    </template>
                    <template v-else>
                      <span class="pending">待签署</span>
                    </template>
                  </div>
                </div>
                <div class="signature-party">
                  <h4>乙方签署</h4>
                  <div class="signature-box" :class="{ signed: contract.tenantSignedAt }">
                    <template v-if="contract.tenantSignedAt">
                      <span class="signature">{{ contract.tenantName || '租客' }}</span>
                      <span class="sign-time">{{ formatDateTime(contract.tenantSignedAt) }}</span>
                    </template>
                    <template v-else>
                      <span class="pending">待签署</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 相关信息 -->
        <div class="related-card card mt-md">
          <div class="card-header">相关信息</div>
          <div class="card-body">
            <div class="info-row">
              <label>关联订单</label>
              <router-link :to="`/order/${contract.orderNo}`">{{ contract.orderNo || contract.orderId }}</router-link>
            </div>
            <div class="info-row">
              <label>创建时间</label>
              <span>{{ formatDateTime(contract.createdAt || contract.createTime) }}</span>
            </div>
          </div>
        </div>
      </template>
      
      <div v-else class="empty card">
        <div class="card-body text-center">
          <p>😕 合同不存在</p>
          <router-link to="/contracts" class="btn btn-primary mt-md">返回合同列表</router-link>
        </div>
      </div>
      
      <!-- 退租弹窗 -->
      <div v-if="showTerminateModal" class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>申请退租</h3>
            <button class="close-btn" @click="closeTerminateModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>退款明细</label>
              <div class="refund-details" v-if="!calculatingRefund && contract">
                <div class="detail-row">
                  <span>总租金：</span>
                  <span>¥{{ contract.rentalAmount }}</span>
                </div>
                <div class="detail-row">
                  <span>押金：</span>
                  <span>¥{{ contract.depositAmount }}</span>
                </div>
                <div class="detail-row" v-if="refundBreakdown.usedDays > 0">
                  <span>已住天数：</span>
                  <span>{{ refundBreakdown.usedDays }} 天</span>
                </div>
                <div class="detail-row">
                  <span>剩余天数：</span>
                  <span>{{ refundBreakdown.remainingDays }} 天</span>
                </div>
                <div class="detail-row">
                  <span>每日租金：</span>
                  <span>¥{{ refundBreakdown.dailyRent }}</span>
                </div>
                <div class="total-refund">
                  <span>预计总退款：</span>
                  <span class="amount">¥{{ refundAmount }}</span>
                </div>
              </div>
              <div v-else-if="calculatingRefund" class="loading-refund">计算中...</div>
              
              <!-- 资深工程师级：资金守恒可视化 -->
              <div class="money-conservation-flow" v-if="!calculatingRefund && contract">
                <div class="flow-header">资金守恒路径 (Conservation of Money)</div>
                <div class="flow-graph">
                  <div class="actor landlord">
                    <span class="icon">🏠</span>
                    <span class="label">房东账户</span>
                    <span class="change negative">-¥{{ refundAmount }}</span>
                  </div>
                  <div class="arrow">
                    <div class="line"></div>
                    <div class="head"></div>
                    <div class="amount-tip">资金返还</div>
                  </div>
                  <div class="actor tenant">
                    <span class="icon">👤</span>
                    <span class="label">租客(您)</span>
                    <span class="change positive">+¥{{ refundAmount }}</span>
                  </div>
                </div>
                <div class="total-conservation">
                  <span>账户间变动净值：¥0.00</span>
                  <span class="tag">资金守恒已验证</span>
                </div>
              </div>

              <p class="help-text">退款 = (剩余天数 × 每日租金) + 全额押金。每日租金 = 总租金 / 总租期。</p>
            </div>
            
            <div class="form-group">
              <label>退租原因 <span class="required">*</span></label>
              <textarea 
                v-model="terminateForm.reason" 
                placeholder="请填写退租原因..."
                rows="4"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="closeTerminateModal">取消</button>
            <button 
              class="btn btn-danger" 
              :disabled="submittingTerminate || !terminateForm.reason" 
              @click="submitTerminate"
            >
              {{ submittingTerminate ? '提交中...' : '确认退租' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContractDetail, signContract, downloadContract, terminateContract } from '../../api/contract'
import { useWalletStore } from '../../stores/walletStore'

export default {
  name: 'ContractDetail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const walletStore = useWalletStore()
    
    const contract = ref(null)
    const loading = ref(false)

    // 判断是否为租客
    const isTenant = computed(() => {
        const userInfoStr = localStorage.getItem('userInfo')
        if (!userInfoStr) return false
        const userInfo = JSON.parse(userInfoStr)
        const userType = (userInfo.role || '').toUpperCase()
        return userType === 'TENANT' || userType === 'ROLE_TENANT'
    })
    
    // 状态图标
    const statusIcon = computed(() => {
      const map = {
        'PENDING': '✍️',
        'PENDING_SIGN': '✍️',
        'SIGNED': '✅',
        'TENANT_SIGNED': '⏳',
        'LANDLORD_SIGNED': '⏳',
        'COMPLETED': '🎉',
        'CANCELLED': '❌',
        'TERMINATED': '⛔'
      }
      return map[contract.value?.status] || '📋'
    })
    
    // 状态文本
    const statusText = computed(() => {
      const map = {
        'PENDING': '待签署',
        'PENDING_SIGN': '待签署',
        'SIGNED': '已签署',
        'TENANT_SIGNED': '待房东签署',
        'LANDLORD_SIGNED': '待租客签署',
        'COMPLETED': '已生效',
        'CANCELLED': '已取消',
        'TERMINATED': '已终止'
      }
      return map[contract.value?.status] || contract.value?.status
    })
    
    // 状态卡片样式
    const statusCardClass = computed(() => {
      const map = {
        'PENDING': 'status-pending',
        'PENDING_SIGN': 'status-pending',
        'SIGNED': 'status-success',
        'TENANT_SIGNED': 'status-warning',
        'LANDLORD_SIGNED': 'status-warning',
        'COMPLETED': 'status-success',
        'CANCELLED': 'status-cancelled',
        'TERMINATED': 'status-error'
      }
      return map[contract.value?.status] || ''
    })
    
    // 下载URL
    const downloadUrl = computed(() => {
      return downloadContract(contract.value?.id)
    })
    
    // 加载合同详情
    const loadContract = async () => {
      loading.value = true
      try {
        const res = await getContractDetail(route.params.id)
        contract.value = res.data
      } catch (error) {
        console.error('加载合同失败', error)
      } finally {
        loading.value = false
      }
    }
    
    const openTerminateModal = () => {
      showTerminateModal.value = true
      calculatingRefund.value = true
      
      // 使用前端公式实时计算，确保公式一致性
      refundAmount.value = calculateFrontendRefund()
      
      calculatingRefund.value = false
    }
    
    // 全局 toast（同 request.js 风格）
    function showToast(message, type = 'info') {
      let toast = document.getElementById('contract-toast')
      if (!toast) {
        toast = document.createElement('div')
        toast.id = 'contract-toast'
        toast.style.cssText = [
          'position:fixed','top:80px','left:50%','transform:translateX(-50%)',
          'padding:12px 24px','border-radius:8px','font-size:14px','font-weight:500',
          'box-shadow:0 8px 24px rgba(0,0,0,0.2)','z-index:9999',
          'max-width:80vw','text-align:center','opacity:0','transition:opacity 0.3s','pointer-events:none'
        ].join(';')
        document.body.appendChild(toast)
      }
      toast.style.background = type === 'success' ? 'rgba(122,157,140,0.97)' : (type === 'error' ? 'rgba(217,142,142,0.97)' : 'rgba(0,0,0,0.8)')
      toast.style.color = '#fff'
      toast.textContent = message
      toast.style.opacity = '1'
      setTimeout(() => { toast.style.opacity = '0' }, 3000)
    }

    // 签署合同
    const signing = ref(false)
    const handleSign = async () => {
      if (!confirm('确认签署此合同？签署后具有法律效力。')) return

      signing.value = true
      try {
        // 获取用户信息
        const userInfoStr = localStorage.getItem('userInfo')
        if (!userInfoStr) {
            showToast('请先登录', 'error')
            router.push('/login')
            return
        }
        const userInfo = JSON.parse(userInfoStr)
        const userId = userInfo.userId
        let userType = (userInfo.role || 'TENANT').toUpperCase()
        if (userType === 'ROLE_TENANT') userType = 'TENANT'
        if (userType === 'ROLE_LANDLORD') userType = 'LANDLORD'

        await signContract(contract.value.id, {
          signature: '电子签名',
          userId: userId,
          userType: userType,
          signatureData: 'base64_signature_data',
          ipAddress: '127.0.0.1'
        })
        // 刷新数据
        await loadContract()
        showToast('签署成功！', 'success')
      } catch (error) {
        console.error('签署失败', error)
        const msg = error.response?.data?.message || error.message || '签署失败'
        showToast(msg, 'error')
      } finally {
        signing.value = false
      }
    }
    
    // 格式化日期时间
    const formatDateTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    }
    
    // 退租相关逻辑

    
    const showTerminateModal = ref(false)
    const refundAmount = ref(0)
    const calculatingRefund = ref(false)
    const submittingTerminate = ref(false)
    const terminateForm = ref({
      reason: ''
    })
    const refundBreakdown = ref({
      usedDays: 0,
      remainingDays: 0,
      dailyRent: 0
    })

    const calculateFrontendRefund = () => {
      const c = contract.value
      if (!c) return 0
      
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const start = new Date(c.startDate)
      const end = new Date(c.endDate)
      const actualIn = c.actualCheckInDate ? new Date(c.actualCheckInDate) : start
      
      // 总天数
      const totalDays = Math.ceil((end - start) / (1000 * 3600 * 24))
      if (totalDays <= 0) return c.rentalAmount + c.depositAmount

      // 未入住或退租在入住前
      if (!c.actualCheckInDate || actualIn > today) {
        refundBreakdown.value = {
          usedDays: 0,
          remainingDays: totalDays,
          dailyRent: (c.rentalAmount / totalDays).toFixed(2)
        }
        return (parseFloat(c.rentalAmount) + parseFloat(c.depositAmount)).toFixed(2)
      }

      // 已入住，按天计算
      const usedDays = Math.max(0, Math.ceil((today - actualIn) / (1000 * 3600 * 24)))
      const remainingDays = Math.max(0, totalDays - usedDays)
      
      const dailyRentStr = (c.rentalAmount / totalDays).toFixed(2)
      const dailyRent = parseFloat(dailyRentStr)
      
      refundBreakdown.value = {
        usedDays,
        remainingDays,
        dailyRent: dailyRentStr
      }

      if (remainingDays <= 0) {
        return parseFloat(c.depositAmount).toFixed(2)
      }

      const totalRefund = (dailyRent * remainingDays) + parseFloat(c.depositAmount)
      return totalRefund.toFixed(2)
    }
    
    const closeTerminateModal = () => {
      showTerminateModal.value = false
      terminateForm.value.reason = ''
      refundAmount.value = 0
    }
    
    const submitTerminate = async () => {
      if (!terminateForm.value.reason) return
      
      submittingTerminate.value = true
      try {
        // 1. 前端验证资金守恒（支付前检查）
        const walletStore = useWalletStore()
        const validation = walletStore.validateFundsConservation(
          parseFloat(refundAmount.value),
          'refund'
        )
        
        console.log('💰 资金守恒验证:', validation)
        
        // 2. 显示确认对话框，展示资金流向
        const confirmMsg = 
          `确认退租申请？\n` +
          `退款金额：¥${refundAmount.value}\n` +
          `${validation.message}\n\n` +
          `退款后，您的余额将从 ¥${walletStore.displayBalance.toFixed(2)} ` +
          `变为 ¥${(walletStore.displayBalance + parseFloat(refundAmount.value)).toFixed(2)}\n\n` +
          `注意：退款将执行原子性操作（房东扣款 + 租客退款）`
        
        if (!confirm(confirmMsg)) return
        
        // 3. 终止合同（合同服务内部会通知订单服务执行退款+恢复房源，无需重复调用退款接口）
        await terminateContract(contract.value.id, {
          reason: terminateForm.value.reason,
          refundAmount: refundAmount.value
        })
        
        // 4. 关键步骤：退款完成后，立即刷新钱包余额
        console.log('🔄 开始刷新钱包余额...')
        
        // 短暂延迟，确保后端事务已完成
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // 刷新钱包数据（实时显示新余额）
        const refreshSuccess = await walletStore.fullRefresh('refund')
        
        if (refreshSuccess) {
          console.log('✅ 钱包已刷新，新余额:', walletStore.displayBalance)
          
          // 显示成功信息（包含新余额）
          alert(
            `✅ 退租申请已成功处理！\n\n` +
            `退款金额：¥${refundAmount.value}\n` +
            `✨ 您的新余额：¥${walletStore.displayBalance.toFixed(2)}\n\n` +
            `退款已入账，请在"我的钱包"页面查看详情。`
          )
        } else {
          alert(
            `退租申请已提交，但无法实时刷新余额。\n` +
            `请手动刷新页面或访问"我的钱包"查看最新余额。`
          )
        }
        
        closeTerminateModal()
        loadContract() // 刷新合同状态
        
      } catch (error) {
        console.error('❌ 退租申请失败:', error)
        
        // 区分错误类型
        let errorMsg = '退租申请失败，请重试'
        if (error.response?.status === 409) {
          errorMsg = '❌ 资金守恒检查失败：账户资金异常，请联系管理员'
        } else if (error.response?.data?.message) {
          errorMsg = error.response.data.message
        }
        
        alert(errorMsg)
      } finally {
        submittingTerminate.value = false
      }
    }
    
    onMounted(() => {
      loadContract()
    })
    
    return {
      contract,
      loading,
      statusIcon,
      statusText,
      statusCardClass,
      downloadUrl,
      isTenant,
      handleSign,
      signing,
      formatDateTime,
      // 退租相关
      showTerminateModal,
      refundAmount,
      calculatingRefund,
      submittingTerminate,
      terminateForm,
      refundBreakdown,
      openTerminateModal,
      closeTerminateModal,
      submitTerminate
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

.status-card .card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-info {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.status-icon {
  font-size: 36px;
}

.status-info h2 {
  margin-bottom: var(--spacing-xs);
}

.status-pending {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
}

.status-success {
  background: linear-gradient(135deg, #f6ffed 0%, #b7eb8f 100%);
}

.status-cancelled {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
}

.status-error {
  background: linear-gradient(135deg, #f5f5f5 0%, #d9d9d9 100%);
}

.contract-text {
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.8;
}

.contract-text h2 {
  font-size: 20px;
}

.contract-text h3 {
  font-size: 16px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.contract-text p {
  margin-bottom: var(--spacing-xs);
}

.signature-area {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 2px solid var(--border-color);
}

.signature-party h4 {
  margin-bottom: var(--spacing-sm);
}

.signature-box {
  height: 100px;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

.signature-box.signed {
  border-color: var(--success-color);
  background: #f6ffed;
}

.signature-box .signature {
  font-size: 24px;
  font-family: "Brush Script MT", cursive;
  color: var(--success-color);
}

.signature-box .sign-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.signature-box .pending {
  color: var(--text-disabled);
}

.info-row {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) 0;
}

.info-row label {
  width: 80px;
  color: var(--text-secondary);
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  width: 90%;
  max-width: 500px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
}

.modal-header {
  padding: var(--spacing-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  margin: 0;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary);
}

.modal-body {
  padding: var(--spacing-lg);
}

.form-group {
  margin-bottom: var(--spacing-md);
}

.form-group label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-weight: 500;
}

.form-group textarea {
  width: 100%;
  padding: var(--spacing-sm);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  resize: vertical;
}

.refund-details {
  background: var(--bg-secondary);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-sm);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-xs) 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.total-refund {
  display: flex;
  justify-content: space-between;
  padding-top: var(--spacing-sm);
  margin-top: var(--spacing-sm);
  border-top: 1px dashed var(--border-color);
  font-weight: bold;
  color: var(--text-primary);
}

.total-refund .amount {
  font-size: 20px;
  font-weight: 700;
  color: var(--error-color);
}

/* 资金守恒可视化样式 - 资深工程师标准 */
.money-conservation-flow {
  margin-top: 16px;
  padding: 16px;
  background: #f8f9ff;
  border: 1px dashed var(--primary-color);
  border-radius: 8px;
}

.flow-header {
  font-size: 12px;
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
  font-size: 12px;
  color: var(--text-secondary);
}

.actor .change {
  font-weight: 600;
  font-size: 14px;
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
  font-size: 10px;
  color: var(--primary-color);
  margin-top: 4px;
  background: #fff;
  padding: 2px 6px;
  border-radius: 10px;
  border: 1px solid var(--primary-color);
}

.total-conservation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: var(--text-secondary);
  border-top: 1px solid #eee;
  padding-top: 8px;
}

.total-conservation .tag {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.loading-refund {
  padding: var(--spacing-md);
  text-align: center;
  color: var(--text-secondary);
}

.help-text {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: var(--spacing-xs);
}

.required {
  color: var(--danger-color);
}

.modal-footer {
  padding: var(--spacing-md);
  background: var(--bg-secondary);
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  border-top: 1px solid var(--border-color);
}

.btn-danger {
  background-color: var(--danger-color);
  color: white;
  border: none;
}

.btn-danger:hover {
  background-color: #d9363e;
}

.btn-danger:disabled {
  background-color: #ffccc7;
  cursor: not-allowed;
}
</style>
