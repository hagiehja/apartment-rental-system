<template>
  <div class="home-page">
    <div class="container">
      <!-- Banner区域 -->
      <section class="hero bento-intro">
        <div class="hero-content">
          <h1>找到您理想的家</h1>
          <p>优质房源 · 便捷租房 · 安心入住</p>
          
          <!-- 搜索框 -->
          <div class="search-box">
            <div class="search-icon">🔍</div>
            <input 
              type="text" 
              class="search-input" 
              v-model="searchCity"
              placeholder="搜索城市、商圈或小区..."
              @keyup.enter="handleSearch"
            />
            <button class="btn btn-primary search-btn" @click="handleSearch">搜索</button>
          </div>
          
          <!-- 快捷筛选 -->
          <div class="quick-filters">
            <span class="filter-label">热门城市：</span>
            <div class="tags-wrapper">
              <span 
                v-for="city in hotCities" 
                :key="city"
                class="quick-tag"
                @click="quickSearch(city)"
              >
                {{ city }}
              </span>
            </div>
          </div>
        </div>
      </section>
      
      <!-- 热门房源 (Bento Grid Layout) -->
      <section class="section">
        <div class="section-header">
          <div class="title-group">
            <h2>精选房源</h2>
            <span class="subtitle">Discover our curated selection</span>
          </div>
          <router-link to="/houses" class="link-btn">
            全览房源 <span class="arrow">→</span>
          </router-link>
        </div>
        
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
        </div>
        
        <div v-else-if="houses.length === 0" class="empty">
          <p>暂无房源数据</p>
        </div>
        
        <div v-else class="house-grid bento-grid">
          <div 
            v-for="(house, index) in houses" 
            :key="house.houseId" 
            class="house-card card"
            :class="{ 'bento-large': index === 0, 'bento-medium': index === 1 }"
            @click="goToDetail(house.houseId)"
          >
            <div class="house-cover">
              <img :src="house.coverImage || defaultImage" :alt="house.title" @error="handleImageError" />
              <div class="blur-overlay"></div>
              <span class="house-tag" :class="house.rentType === 'WHOLE' ? 'tag-whole' : 'tag-share'">
                {{ house.rentType === 'WHOLE' ? '整租' : '合租' }}
              </span>
              <div class="price-badge">
                ¥{{ house.price }}<span class="unit">/月</span>
              </div>
            </div>
            <div class="house-info">
              <h3 class="house-title">{{ house.title }}</h3>
              <div class="house-meta-row">
                <span class="meta-item location">📍 {{ house.city }} {{ house.district }}</span>
                <span class="meta-dot">·</span>
                <span class="meta-item">{{ house.roomCount }}室{{ house.hallCount }}厅</span>
                <span class="meta-dot">·</span>
                <span class="meta-item">{{ house.area }}㎡</span>
              </div>
            </div>
          </div>
        </div>
      </section>
      
      <!-- 平台优势 (Feature Bento) -->
      <section class="section features">
        <div class="section-header text-center">
          <h2>为什么选择我们</h2>
          <p class="subtitle">安心租房的四大保障</p>
        </div>
        <div class="feature-grid">
          <div class="feature-item feature-1">
            <div class="feature-icon">🔍</div>
            <h3>海量房源</h3>
            <p>覆盖全城优质小区，满足多样需求</p>
          </div>
          <div class="feature-item feature-2">
            <div class="feature-icon">🛡️</div>
            <h3>真实可靠</h3>
            <p>房源实地核验，信息真实无虚假</p>
          </div>
          <div class="feature-item feature-3">
            <div class="feature-icon">💰</div>
            <h3>价格透明</h3>
            <p>无隐形消费，租金透明签约放心</p>
          </div>
          <div class="feature-item feature-4">
            <div class="feature-icon">📝</div>
            <h3>电子签约</h3>
            <p>在线签署合同，法律保障权益</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHouseList } from '../api/house'
import { DEFAULT_IMAGE, handleImageError } from '../utils/image'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()
    
    // 状态定义
    const searchCity = ref('')
    const houses = ref([])
    const loading = ref(false)
    
    const hotCities = ['深圳市', '广州市', '北京市', '上海市', '杭州市']
    const defaultImage = DEFAULT_IMAGE
    
    // 加载热门房源
    const loadHotHouses = async () => {
      loading.value = true
      try {
        const res = await getHouseList({ page: 1, size: 6 })
        houses.value = res.data?.records || []
      } catch (error) {
        console.error('加载房源失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      router.push({ 
        path: '/houses', 
        query: { city: searchCity.value } 
      })
    }
    
    // 快捷搜索
    const quickSearch = (city) => {
      searchCity.value = city
      handleSearch()
    }
    
    // 跳转详情
    const goToDetail = (id) => {
      router.push(`/house/${id}`)
    }
    
    onMounted(() => {
      loadHotHouses()
    })
    
    return {
      searchCity,
      houses,
      loading,
      hotCities,
      defaultImage,
      handleSearch,
      quickSearch,
      goToDetail,
      handleImageError
    }
  }
}
</script>

<style scoped>
/* Home Page Layout */
.home-page {
  padding-bottom: var(--spacing-xl);
}

/* Hero Section */
.hero {
  background: linear-gradient(120deg, #F3EFE7 0%, #E8F1EC 100%);
  padding: 80px 40px;
  border-radius: var(--radius-lg);
  text-align: center;
  margin-bottom: var(--spacing-xl);
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.8) 0%, transparent 60%);
  opacity: 0.6;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 700px;
  margin: 0 auto;
}

.hero h1 {
  font-size: 42px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
  letter-spacing: -1px;
}

.hero p {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 40px;
}

/* Search Box - Premium Style */
.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 8px;
  border-radius: var(--radius-full);
  box-shadow: 0 12px 36px rgba(122, 157, 140, 0.2);
  margin-bottom: var(--spacing-lg);
  transition: transform 0.3s;
}

.search-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 48px rgba(122, 157, 140, 0.25);
}

.search-icon {
  padding-left: 20px;
  font-size: 20px;
  color: var(--text-disabled);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 16px;
  font-size: 16px;
  color: var(--text-primary);
  box-shadow: none !important; /* Override default input focus */
}

.search-btn {
  padding: 12px 32px;
  border-radius: var(--radius-full);
  font-size: 16px;
}

/* Quick Filters */
.quick-filters {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.filter-label {
  color: var(--text-secondary);
  font-size: 14px;
}

.tags-wrapper {
  display: flex;
  gap: 8px;
}

.quick-tag {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: var(--text-primary);
}

.quick-tag:hover {
  background: #fff;
  color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

/* Section Headers */
.section {
  margin-bottom: 60px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 30px;
}

.section-header.text-center {
  justify-content: center;
  flex-direction: column;
  align-items: center;
}

.title-group h2 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 14px;
}

.link-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: var(--primary-color);
  padding: 8px 16px;
  background: var(--primary-light);
  border-radius: var(--radius-full);
  transition: all 0.2s;
}

.link-btn:hover {
  background: var(--primary-color);
  color: #fff;
}

/* Bento Grid House Layout */
.house-grid.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, auto);
  gap: 24px;
}

/* First item spans 2 cols, 2 rows (Large) */
.bento-large {
  grid-column: span 2;
  grid-row: span 2;
}

.bento-large .house-cover {
  height: 480px; /* Taller image */
}

/* Medium item (if needed, currently standard) */
.bento-medium {
  /* Standard size */
}

@media (max-width: 992px) {
  .house-grid.bento-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bento-large {
    grid-column: span 2;
    grid-row: auto; /* Reset row span */
  }
  .bento-large .house-cover { height: 300px; }
}

@media (max-width: 576px) {
  .house-grid.bento-grid {
    grid-template-columns: 1fr;
  }
  .bento-large { grid-column: span 1; }
}

/* House Card Premium Style */
.house-card {
  border: none;
  background: #fff;
  transition: all 0.4s ease;
}

.house-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
}

.house-cover {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.house-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}

.house-card:hover .house-cover img {
  transform: scale(1.05);
}

.house-tag {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  border-radius: var(--radius-sm);
  z-index: 2;
  backdrop-filter: blur(4px);
}

.tag-whole { background: rgba(122, 157, 140, 0.9); }
.tag-share { background: rgba(238, 215, 154, 0.9); color: #5C4B29; }

.price-badge {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: #fff;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-weight: 700;
  color: var(--primary-color);
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 2;
}

.price-badge .unit {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 2px;
}

.house-info {
  padding: 20px;
}

.house-title {
  font-size: 18px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-meta-row {
  display: flex;
  align-items: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.meta-dot {
  margin: 0 8px;
  color: var(--text-disabled);
}

.location {
  max-width: 50%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Features Bento */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.feature-item {
  background: #fff;
  padding: 32px 24px;
  border-radius: var(--radius-lg);
  text-align: center;
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.feature-item:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-md);
  border-color: transparent;
}

.feature-1:hover { background: #E8F1EC; }
.feature-2:hover { background: #E6F7FF; }
.feature-3:hover { background: #FFF7E6; }
.feature-4:hover { background: #FFF1F0; }

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-item h3 {
  font-size: 18px;
  margin-bottom: 8px;
}

.feature-item p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
