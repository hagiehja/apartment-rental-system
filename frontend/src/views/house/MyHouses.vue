<template>
  <div class="my-houses-page">
    <div class="container">
      <div class="page-header flex justify-between items-center">
        <div>
          <h1>我的房源</h1>
          <p class="text-secondary">管理您发布的所有房源</p>
        </div>
        <router-link to="/house/publish" class="btn btn-primary">+ 发布新房源</router-link>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <div v-else-if="houses.length === 0" class="empty card">
        <div class="card-body text-center">
          <p class="mb-md">😊 您还没有发布任何房源</p>
          <router-link to="/house/publish" class="btn btn-primary">立即发布</router-link>
        </div>
      </div>
      
      <div v-else class="house-list">
        <div v-for="house in houses" :key="house.houseId" class="house-item card">
          <div class="house-cover">
            <img :src="house.coverImage || defaultImage" :alt="house.title" @error="handleImageError" />
            <span class="status-tag" :class="statusClass(house.status)">{{ statusText(house.status) }}</span>
          </div>
          
          <div class="house-content">
            <h3 class="house-title">{{ house.title }}</h3>
            <p class="house-location">📍 {{ house.city }} {{ house.district }}</p>
            <div class="house-meta">
              <span>{{ house.area }}㎡</span>
              <span>{{ house.roomCount }}室{{ house.hallCount }}厅</span>
              <span>{{ house.rentType === 'WHOLE' ? '整租' : '合租' }}</span>
            </div>
            <div class="house-footer">
              <span class="price">¥{{ house.price }}<span class="price-unit">/月</span></span>
              <span class="text-secondary text-sm">{{ house.viewCount }}次浏览</span>
            </div>
          </div>
          
          <div class="house-actions">
            <button class="btn btn-default btn-sm" @click="viewDetail(house.houseId)">查看</button>
            <button class="btn btn-default btn-sm" @click="editHouse(house.houseId)">编辑</button>
            <button 
              v-if="house.status === 'AVAILABLE'"
              class="btn btn-default btn-sm"
              @click="handleOffline(house)"
            >下架</button>
            <button 
              v-else-if="house.status === 'OFFLINE'"
              class="btn btn-primary btn-sm"
              @click="handleOnline(house)"
            >上架</button>
            <button class="btn btn-danger btn-sm" @click="handleDelete(house)">删除</button>
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
import { getHouseList, offlineHouse, onlineHouse, deleteHouse } from '../../api/house'
import { getCurrentUser } from '../../api/user'

import { DEFAULT_IMAGE, handleImageError } from '../../utils/image'

export default {
  name: 'MyHouses',
  setup() {
    const router = useRouter()
    
    const houses = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = 10
    
    const defaultImage = DEFAULT_IMAGE
    
    const totalPages = computed(() => Math.ceil(total.value / pageSize))
    
    // 状态文本
    const statusText = (status) => {
      const map = { 'AVAILABLE': '可租', 'RENTED': '已出租', 'OFFLINE': '已下架' }
      return map[status] || status
    }
    
    // 状态样式
    const statusClass = (status) => {
      const map = { 'AVAILABLE': 'status-available', 'RENTED': 'status-rented', 'OFFLINE': 'status-offline' }
      return map[status] || ''
    }
    
    // 加载房源
    const loadHouses = async () => {
      loading.value = true
      try {
        // 获取当前用户的房源
        const user = getCurrentUser()
        const res = await getHouseList({ 
          page: currentPage.value, 
          size: pageSize,
          landlordId: user.userId 
        })
        houses.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (error) {
        console.error('加载房源失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 查看详情
    const viewDetail = (id) => {
      router.push(`/house/${id}`)
    }
    
    // 编辑
    const editHouse = (id) => {
      router.push({ path: '/house/publish', query: { id } })
    }
    
    // 下架
    const handleOffline = async (house) => {
      if (!confirm(`确定要下架房源"${house.title}"吗？`)) return
      try {
        await offlineHouse(house.houseId)
        house.status = 'OFFLINE'
        alert('下架成功')
      } catch (error) {
        alert(error.message || '操作失败')
      }
    }
    
    // 上架
    const handleOnline = async (house) => {
      try {
        await onlineHouse(house.houseId)
        house.status = 'AVAILABLE'
        alert('上架成功')
      } catch (error) {
        alert(error.message || '操作失败')
      }
    }
    
    // 删除
    const handleDelete = async (house) => {
      if (!confirm(`确定要删除房源"${house.title}"吗？此操作不可恢复！`)) return
      try {
        await deleteHouse(house.houseId)
        houses.value = houses.value.filter(h => h.houseId !== house.houseId)
        alert('删除成功')
      } catch (error) {
        alert(error.message || '删除失败')
      }
    }
    
    // 翻页
    const changePage = (page) => {
      currentPage.value = page
      loadHouses()
    }
    
    onMounted(() => {
      loadHouses()
    })
    
    return {
      houses,
      loading,
      total,
      currentPage,
      pageSize,
      totalPages,
      defaultImage,
      statusText,
      statusClass,
      viewDetail,
      editHouse,
      handleOffline,
      handleOnline,
      handleDelete,
      changePage,
      handleImageError
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

.page-header h1 {
  margin-bottom: var(--spacing-xs);
}

.house-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.house-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  gap: var(--spacing-md);
}

.house-cover {
  position: relative;
  width: 160px;
  min-width: 160px;
  height: 120px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.house-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  font-size: 12px;
  color: #fff;
  border-radius: var(--radius-sm);
}

.status-available { background: var(--success-color); }
.status-rented { background: var(--warning-color); }
.status-offline { background: var(--text-disabled); }

.house-content {
  flex: 1;
}

.house-title {
  font-size: 16px;
  margin-bottom: var(--spacing-xs);
}

.house-location {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: var(--spacing-xs);
}

.house-meta {
  display: flex;
  gap: var(--spacing-md);
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: var(--spacing-sm);
}

.house-footer {
  display: flex;
  gap: var(--spacing-lg);
  align-items: center;
}

.house-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.page-info {
  padding: 0 var(--spacing-md);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .house-item {
    flex-direction: column;
    align-items: stretch;
  }
  
  .house-cover {
    width: 100%;
    height: 180px;
  }
  
  .house-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
