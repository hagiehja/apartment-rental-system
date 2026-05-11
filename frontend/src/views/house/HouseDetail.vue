<template>
  <div class="house-detail-page">
    <div class="container">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <template v-else-if="house">
        <!-- 面包屑导航 -->
        <div class="breadcrumb">
          <router-link to="/">首页</router-link>
          <span>/</span>
          <router-link to="/houses">房源列表</router-link>
          <span>/</span>
          <span>{{ house.title }}</span>
        </div>
        
        <div class="detail-layout">
          <!-- 左侧：图片和详情 -->
          <div class="detail-main">
            <!-- 图片展示 -->
            <div class="image-gallery card">
              <div class="main-image">
                <img :src="currentImage || defaultImage" :alt="house.title" @error="handleImageError" />
              </div>
              <div class="image-thumbs" v-if="house.images && house.images.length > 0">
                <div 
                  v-for="(img, index) in house.images" 
                  :key="img.imageId"
                  class="thumb-item"
                  :class="{ active: currentImageIndex === index }"
                  @click="currentImageIndex = index"
                >
                  <img :src="img.imageUrl" :alt="`图片${index + 1}`" @error="handleImageError" />
                </div>
              </div>
            </div>
            
            <!-- 房源信息 -->
            <div class="info-card card">
              <div class="card-header">房源信息</div>
              <div class="card-body">
                <div class="info-grid">
                  <div class="info-item">
                    <label>面积</label>
                    <span>{{ house.area }}㎡</span>
                  </div>
                  <div class="info-item">
                    <label>户型</label>
                    <span>{{ house.roomCount }}室{{ house.hallCount }}厅{{ house.bathroomCount }}卫</span>
                  </div>
                  <div class="info-item">
                    <label>楼层</label>
                    <span>{{ house.floor }}/{{ house.totalFloor }}层</span>
                  </div>
                  <div class="info-item">
                    <label>朝向</label>
                    <span>{{ house.orientation || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>装修</label>
                    <span>{{ decorationText }}</span>
                  </div>
                  <div class="info-item">
                    <label>看房时间</label>
                    <span>随时看房</span>
                  </div>
                  <div class="info-item">
                    <label>车位情况</label>
                    <span>暂无数据 / 视小区而定</span>
                  </div>
                  <div class="info-item">
                    <label>用水用电</label>
                    <span>民水民电</span>
                  </div>
                  <div class="info-item">
                    <label>配备电梯</label>
                    <span>{{ hasElevator ? '有' : '无' }}</span>
                  </div>
                  <div class="info-item">
                    <label>出租方式</label>
                    <span>{{ house.rentType === 'WHOLE' ? '整租' : '合租' }}</span>
                  </div>
                  <div class="info-item">
                    <label>付款方式</label>
                    <span>{{ house.paymentMethod || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>状态</label>
                    <span class="tag" :class="statusClass">{{ statusText }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 配套设施 -->
            <div class="facility-card card" v-if="house.facilities && house.facilities.length > 0">
              <div class="card-header">配套设施</div>
              <div class="card-body">
                <div class="facility-list">
                  <span v-for="facility in house.facilities" :key="facility" class="facility-item">
                    ✓ {{ facility }}
                  </span>
                </div>
              </div>
            </div>
            
            <!-- 房源描述 -->
            <div class="desc-card card" v-if="house.description">
              <div class="card-header">房源描述</div>
              <div class="card-body">
                <p>{{ house.description }}</p>
              </div>
            </div>
          </div>
          
          <!-- 右侧：价格和操作 -->
          <div class="detail-sidebar">
            <div class="price-card card">
              <div class="card-body">
                <h1 class="house-title">{{ house.title }}</h1>
                <p class="house-address">📍 {{ house.province }}{{ house.city }}{{ house.district }}{{ house.address }}</p>
                
                <div class="price-box">
                  <span class="price price-lg">¥{{ house.price }}</span>
                  <span class="price-unit">/月</span>
                </div>
                
                <div class="action-buttons">
                  <button 
                    v-if="house.status === 'AVAILABLE'"
                    class="btn btn-primary btn-lg btn-block"
                    @click="handleRent"
                  >
                    立即租房
                  </button>
                  <button 
                    v-else
                    class="btn btn-default btn-lg btn-block"
                    disabled
                  >
                    {{ house.status === 'RENTED' ? '已出租' : '已下架' }}
                  </button>
                </div>
                
                <div class="meta-info">
                  <span>👁 {{ house.viewCount }} 次浏览</span>
                  <span>📅 发布于 {{ formatDate(house.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
      
      <div v-else class="empty card">
        <div class="card-body text-center">
          <p>😕 房源不存在或已下架</p>
          <router-link to="/houses" class="btn btn-primary mt-md">返回房源列表</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getHouseDetail } from '../../api/house'
import { isLoggedIn, getCurrentUser } from '../../api/user'

import { DEFAULT_IMAGE, handleImageError } from '../../utils/image'

export default {
  name: 'HouseDetail',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const house = ref(null)
    const loading = ref(false)
    const currentImageIndex = ref(0)
    
    const defaultImage = DEFAULT_IMAGE
    
    // 当前显示的图片
    const currentImage = computed(() => {
      if (house.value?.images && house.value.images.length > 0) {
        return house.value.images[currentImageIndex.value]?.imageUrl
      }
      return defaultImage
    })
    
    // 装修文本
    const decorationText = computed(() => {
      const map = {
        'ROUGH': '毛坯',
        'SIMPLE': '简装',
        'FINE': '精装',
        'LUXURY': '豪装'
      }
      return map[house.value?.decoration] || '-'
    })
    
    // 是否有电梯
    const hasElevator = computed(() => {
      if (house.value?.facilities && house.value.facilities.length > 0) {
        return house.value.facilities.includes('电梯')
      }
      return false
    })
    
    // 状态文本
    const statusText = computed(() => {
      const map = {
        'AVAILABLE': '可租',
        'RENTED': '已出租',
        'OFFLINE': '已下架'
      }
      return map[house.value?.status] || house.value?.status
    })
    
    // 状态样式
    const statusClass = computed(() => {
      const map = {
        'AVAILABLE': 'tag-success',
        'RENTED': 'tag-warning',
        'OFFLINE': 'tag-default'
      }
      return map[house.value?.status] || 'tag-default'
    })
    
    // 加载房源详情
    const loadHouse = async () => {
      const id = route.params.id
      loading.value = true
      try {
        const res = await getHouseDetail(id)
        house.value = res.data
      } catch (error) {
        console.error('加载房源详情失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 立即租房
    const handleRent = () => {
      if (!isLoggedIn()) {
        router.push({ name: 'Login', query: { redirect: route.fullPath } })
        return
      }
      
      const user = getCurrentUser()
      if (user.role === 'LANDLORD') {
        alert('房东账号不能租房')
        return
      }
      
      router.push(`/order/create/${house.value.houseId}`)
    }
    
    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    }
    
    onMounted(() => {
      loadHouse()
    })
    
    return {
      house,
      loading,
      currentImageIndex,
      currentImage,
      defaultImage,
      decorationText,
      hasElevator,
      statusText,
      statusClass,
      handleRent,
      formatDate,
      handleImageError
    }
  }
}
</script>

<style scoped>
.breadcrumb {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
  color: var(--text-secondary);
  font-size: 13px;
}

.breadcrumb a {
  color: var(--text-secondary);
}

.breadcrumb a:hover {
  color: var(--primary-color);
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: var(--spacing-lg);
}

@media (max-width: 992px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.main-image {
  height: 400px;
  background: #f0f0f0;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-thumbs {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  overflow-x: auto;
}

.thumb-item {
  width: 80px;
  height: 60px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.2s;
  flex-shrink: 0;
}

.thumb-item:hover,
.thumb-item.active {
  opacity: 1;
  box-shadow: 0 0 0 2px var(--primary-color);
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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

.info-item span {
  font-size: 15px;
  font-weight: 500;
}

.facility-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.facility-item {
  padding: 4px 12px;
  background: #f0f7ff;
  color: var(--primary-color);
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.detail-sidebar {
  position: sticky;
  top: 80px;
  height: fit-content;
}

.price-card .house-title {
  font-size: 20px;
  margin-bottom: var(--spacing-sm);
}

.price-card .house-address {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: var(--spacing-lg);
}

.price-box {
  padding: var(--spacing-md);
  background: #fff7e6;
  border-radius: var(--radius-md);
  text-align: center;
  margin-bottom: var(--spacing-lg);
}

.price-box .price {
  font-size: 32px;
}

.action-buttons {
  margin-bottom: var(--spacing-lg);
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
