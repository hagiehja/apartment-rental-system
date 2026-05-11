<template>
  <div class="house-list-page">
    <div class="container">
      <!-- 筛选区域 -->
      <div class="filter-card card">
        <div class="card-body">
          <div class="filter-row">
            <div class="filter-item">
              <label>城市</label>
              <input 
                type="text" 
                class="form-input" 
                v-model="filters.city"
                placeholder="如：深圳市"
              />
              <div class="hot-cities">
                <span class="hot-label">热门：</span>
                <span v-for="city in hotCities" :key="city" class="hot-city-tag" @click="selectHotCity(city)">
                  {{ city }}
                </span>
              </div>
            </div>
            <div class="filter-item">
              <label>区县</label>
              <input 
                type="text" 
                class="form-input" 
                v-model="filters.district"
                placeholder="如：南山区"
              />
            </div>
            <div class="filter-item">
              <label>价格范围</label>
              <select class="form-select" v-model="filters.priceRange">
                <option value="">不限</option>
                <option value="0-1000">1000元以下</option>
                <option value="1000-2000">1000-2000元</option>
                <option value="2000-3000">2000-3000元</option>
                <option value="3000-5000">3000-5000元</option>
                <option value="5000-8000">5000-8000元</option>
                <option value="8000-10000">8000-10000元</option>
                <option value="10000-">10000元以上</option>
              </select>
            </div>
            <div class="filter-item">
              <label>户型</label>
              <select class="form-select" v-model="filters.roomCount">
                <option value="">不限</option>
                <option value="1">一室</option>
                <option value="2">两室</option>
                <option value="3">三室</option>
                <option value="4">四室及以上</option>
              </select>
            </div>
            <div class="filter-item">
              <label>出租类型</label>
              <select class="form-select" v-model="filters.rentType">
                <option value="">不限</option>
                <option value="WHOLE">整租</option>
                <option value="SHARED">合租</option>
              </select>
            </div>
            <div class="filter-actions">
              <button class="btn btn-primary" @click="handleSearch">搜索</button>
              <button class="btn btn-default" @click="resetFilters">重置</button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 房源列表 -->
      <div class="list-header">
        <span class="result-count">共找到 <strong>{{ total }}</strong> 套房源</span>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <div v-else-if="houses.length === 0" class="empty card">
        <div class="card-body">
          <p>😕 暂无符合条件的房源</p>
          <button class="btn btn-primary mt-md" @click="resetFilters">清空筛选条件</button>
        </div>
      </div>
      
      <div v-else class="house-list">
        <div 
          v-for="house in houses" 
          :key="house.houseId" 
          class="house-item card"
          :class="{ 'house-rented': house.status === 'RENTED' }"
          @click="goToDetail(house.houseId)"
        >
          <div class="house-cover">
            <img :src="house.coverImage || defaultImage" :alt="house.title" @error="handleImageError" />
            <div v-if="house.status === 'RENTED'" class="rented-overlay">
              <span>已租赁</span>
            </div>
          </div>
          <div class="house-content">
            <h3 class="house-title">
              {{ house.title }}
              <span v-if="house.status === 'RENTED'" class="status-badge rented">已租赁</span>
            </h3>
            <p class="house-location">📍 {{ house.city }} {{ house.district }}</p>
            <div class="house-tags">
              <span class="tag tag-primary">{{ house.rentType === 'WHOLE' ? '整租' : '合租' }}</span>
              <span class="tag tag-default">{{ house.roomCount }}室{{ house.hallCount }}厅</span>
              <span class="tag tag-default">{{ house.area }}㎡</span>
            </div>
            <div class="house-bottom">
              <span class="price price-lg">¥{{ house.price }}<span class="price-unit">/月</span></span>
              <span class="text-secondary text-sm">{{ house.viewCount }}次浏览 · {{ formatDate(house.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getHouseList } from '../../api/house'

import { DEFAULT_IMAGE, handleImageError } from '../../utils/image'

export default {
  name: 'HouseList',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const houses = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = 10
    
    const defaultImage = DEFAULT_IMAGE
    
    const filters = reactive({
      city: '',
      district: '',
      priceRange: '',
      roomCount: '',
      rentType: ''
    })
    
    const hotCities = ['北京市', '上海市', '广州市', '深圳市', '杭州市', '成都市']
    
    const selectHotCity = (city) => {
      filters.city = city
      handleSearch()
    }
    
    const totalPages = computed(() => Math.ceil(total.value / pageSize))
    
    // 加载房源列表
    const loadHouses = async () => {
      loading.value = true
      try {
        const params = {
          pageNum: currentPage.value,
          pageSize: pageSize
        }
        // 添加筛选条件
        if (filters.city) params.city = filters.city
        if (filters.district) params.district = filters.district
        if (filters.rentType) params.rentType = filters.rentType
        
        // 处理价格范围筛选
        if (filters.priceRange) {
          const [min, max] = filters.priceRange.split('-')
          if (min) params.minPrice = Number(min)
          if (max) params.maxPrice = Number(max)
        }
        
        // 处理户型筛选
        if (filters.roomCount) {
          const count = parseInt(filters.roomCount)
          if (count >= 4) {
            params.minRoomCount = 4
          } else {
            params.minRoomCount = count
            params.maxRoomCount = count
          }
        }
        
        const res = await getHouseList(params)
        houses.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (error) {
        console.error('加载房源失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      currentPage.value = 1
      loadHouses()
    }
    
    // 重置筛选
    const resetFilters = () => {
      filters.city = ''
      filters.district = ''
      filters.priceRange = ''
      filters.roomCount = ''
      filters.rentType = ''
      handleSearch()
    }
    
    // 翻页
    const changePage = (page) => {
      currentPage.value = page
      loadHouses()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
    
    // 跳转详情
    const goToDetail = (id) => {
      router.push(`/house/${id}`)
    }
    
    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getMonth() + 1}月${date.getDate()}日`
    }
    
    onMounted(() => {
      // 从URL获取搜索参数
      if (route.query.city) {
        filters.city = route.query.city
      }
      loadHouses()
    })
    
    return {
      houses,
      loading,
      total,
      currentPage,
      totalPages,
      filters,
      defaultImage,
      hotCities,
      selectHotCity,
      handleSearch,
      resetFilters,
      changePage,
      goToDetail,
      formatDate,
      handleImageError
    }
  }
}
</script>

<style scoped>
.filter-card {
  margin-bottom: var(--spacing-lg);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  align-items: flex-end;
}

.filter-item {
  flex: 1;
  min-width: 150px;
}

.filter-item label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-size: 13px;
  color: var(--text-secondary);
}

.hot-cities {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.hot-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.hot-city-tag {
  font-size: 12px;
  color: var(--primary-color);
  background-color: #f0f7ff;
  padding: 2px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.hot-city-tag:hover {
  background-color: var(--primary-color);
  color: white;
}

.filter-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.list-header {
  margin-bottom: var(--spacing-md);
}

.result-count {
  color: var(--text-secondary);
}

.house-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.house-item {
  display: flex;
  cursor: pointer;
  transition: all 0.3s;
}

.house-item:hover {
  box-shadow: var(--shadow-md);
}

.house-cover {
  width: 280px;
  min-width: 280px;
  height: 180px;
  overflow: hidden;
  position: relative;
}

.rented-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rented-overlay span {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  padding: 6px 16px;
  border: 2px solid #fff;
  border-radius: 6px;
  letter-spacing: 2px;
}

.house-rented {
  opacity: 0.8;
}

.status-badge.rented {
  font-size: 12px;
  background: #ff4d4f;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 8px;
  font-weight: 500;
  vertical-align: middle;
}

.house-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.house-content {
  flex: 1;
  padding: var(--spacing-md);
  display: flex;
  flex-direction: column;
}

.house-title {
  font-size: 18px;
  margin-bottom: var(--spacing-sm);
}

.house-location {
  color: var(--text-secondary);
  margin-bottom: var(--spacing-sm);
}

.house-tags {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: auto;
}

.house-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

.page-info {
  padding: 0 var(--spacing-md);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .house-item {
    flex-direction: column;
  }
  
  .house-cover {
    width: 100%;
  }
}
</style>
