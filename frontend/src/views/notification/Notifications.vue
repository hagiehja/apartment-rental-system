<template>
  <div class="notifications-page">
    <div class="container">
      <div class="page-header flex justify-between items-center">
        <div>
          <h1>消息中心</h1>
          <p class="text-secondary">查看系统通知</p>
        </div>
      </div>
      
      <!-- 系统通知内容 -->
      <div class="tab-content">
        <div class="action-bar mb-md text-right">
           <button 
            v-if="messages.length > 0" 
            class="btn btn-default"
            @click="handleMarkAllRead"
          >
            全部标为已读
          </button>
        </div>
      
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
        </div>
        
        <div v-else-if="messages.length === 0" class="empty card">
          <div class="card-body text-center">
            <p>🔔 暂无通知</p>
          </div>
        </div>
        
        <div v-else class="message-list">
          <div 
            v-for="msg in messages" 
            :key="msg.id" 
            class="message-item card"
            :class="{ unread: msg.isRead === 0 }"
            @click="handleRead(msg)"
          >
            <div class="message-icon">{{ getTypeIcon(msg.type) }}</div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-title">{{ msg.title }}</span>
                <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
              </div>
              <p class="message-body">{{ msg.content }}</p>
              <div class="message-meta" v-if="msg.bizId">
                <router-link 
                  v-if="msg.type === 'ORDER'" 
                  :to="`/order/${msg.bizId}`"
                  class="link"
                  @click.stop
                >
                  查看订单
                </router-link>
                <router-link 
                  v-else-if="msg.type === 'CONTRACT'" 
                  :to="`/contract/${msg.bizId}`"
                  class="link"
                  @click.stop
                >
                  查看合同
                </router-link>
              </div>
            </div>
            <span v-if="msg.isRead === 0" class="unread-dot"></span>
          </div>
        </div>
        
        <!-- 分页 -->
        <div v-if="total > pageSize" class="pagination">
          <button class="page-item" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-item" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">下一页</button>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { getMessageList, markAsRead, markAllAsRead } from '../../api/notification'
import { useRouter } from 'vue-router'

export default {
  name: 'Notifications',
  setup() {
    const router = useRouter()
    
    // 系统通知逻辑
    const messages = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(20)
    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

    const getTypeIcon = (type) => {
      const map = { 'ORDER': '📋', 'PAYMENT': '💰', 'CONTRACT': '📝', 'HOUSE': '🏠' }
      return map[type] || '📬'
    }
    
    const loadMessages = async () => {
      loading.value = true
      try {
        const res = await getMessageList({ page: currentPage.value, size: pageSize.value })
        messages.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (error) {
        console.error('加载系统通知失败', error)
      } finally {
        loading.value = false
      }
    }
    
    const handleRead = async (msg) => {
      if (msg.isRead === 1) return
      try {
        await markAsRead(msg.id)
        msg.isRead = 1
      } catch (e) { console.error(e) }
    }
    
    const handleMarkAllRead = async () => {
      try {
        await markAllAsRead()
        messages.value.forEach(m => m.isRead = 1)
        alert('已标为已读')
      } catch (e) { alert(e.message) }
    }
    
    const changePage = (page) => {
      currentPage.value = page
      loadMessages()
    }
    
    // 通用格式化时间
    const formatTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const now = new Date()
      const diff = now - date
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
    }

    onMounted(() => {
      loadMessages()
    })
    
    return {
      messages, loading, total, currentPage, totalPages,
      getTypeIcon, handleRead, handleMarkAllRead, changePage,
      formatTime
    }
  }
}
</script>

<style scoped>
.page-header { margin-bottom: var(--spacing-md); }

/* 系统通知样式 */
.message-list { display: flex; flex-direction: column; gap: var(--spacing-sm); }
.message-item { 
  display: flex; padding: var(--spacing-md); gap: var(--spacing-md); 
  cursor: pointer; transition: all 0.2s; position: relative; 
}
.message-item:hover { box-shadow: var(--shadow-md); }
.message-item.unread { background: #f0f7ff; border-left: 3px solid var(--primary-color); }
.message-icon { font-size: 28px; flex-shrink: 0; }
.message-content { flex: 1; min-width: 0; }
.message-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--spacing-xs); }
.message-title { font-weight: 500; }
.message-time { color: var(--text-secondary); font-size: 13px; }
.message-body { color: var(--text-secondary); font-size: 14px; margin-bottom: var(--spacing-xs); }
.link { color: var(--primary-color); margin-right: 10px; }
.unread-dot { width: 8px; height: 8px; background: var(--primary-color); border-radius: 50%; position: absolute; top: 10px; right: 10px; }
</style>