<template>
  <div class="landlord-houses-page">
    <div class="container">
      <div class="landlord-header card" v-if="landlord">
        <div class="card-body">
          <div class="landlord-profile">
            <div class="avatar">HOME</div>
            <div class="info">
              <h1>
                {{ landlord.username }}
                <span class="tag-success" v-if="landlord.role === 'LANDLORD'">认证房东</span>
              </h1>
              <p class="meta">
                <span>PHONE: {{ maskPhone(landlord.phone) }}</span>
                <span>·</span>
                <span>名下 {{ total }} 套房源</span>
              </p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>

      <div v-else>
        <div class="list-header">
          <span class="result-count">共 <strong>{{ total }}</strong> 套房源</span>
          <button class="btn-link" @click="goBack">返回全部房源</button>
        </div>

        <div v-if="houses.length === 0" class="empty card">
          <div class="card-body text-center"><p>该房东暂无房源</p></div>
        </div>

        <div v-else class="house-list">
          <div v-for="house in houses" :key="house.houseId" class="house-item card" @click="goToDetail(house.houseId)">
            <div class="house-cover">
              <img :src="house.coverImage || defaultImage" :alt="house.title" @error="handleImageError" />
            </div>
            <div class="house-content">
              <h3 class="house-title">{{ house.title }}</h3>
              <p class="house-location">LOC {{ house.city }} {{ house.district }}</p>
              <div class="house-tags">
                <span class="tag tag-primary">{{ house.rentType === 'WHOLE' ? '整租' : '合租' }}</span>
                <span class="tag">{{ house.roomCount }}室{{ house.hallCount }}厅</span>
                <span class="tag">{{ house.area }}㎡</span>
              </div>
              <div class="house-bottom">
                <span class="price">Y{{ house.price }}/月</span>
                <span class="text-secondary text-sm">{{ house.viewCount }}次浏览</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getHouseList } from '../../api/house'
import { batchUserInfo } from '../../api/user'
import { DEFAULT_IMAGE, handleImageError } from '../../utils/image'

export default {
  name: 'LandlordHouses',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const landlord = ref(null)
    const houses = ref([])
    const loading = ref(false)
    const total = ref(0)
    const defaultImage = DEFAULT_IMAGE

    const loadData = async () => {
      loading.value = true
      try {
        const landlordId = parseInt(route.params.landlordId)
        const res = await getHouseList({ pageNum: 1, pageSize: 50, landlordId: landlordId })
        houses.value = res.data?.records || []
        total.value = res.data?.total || 0

        const userRes = await batchUserInfo([landlordId])
        if (userRes.data && userRes.data[landlordId]) {
          landlord.value = userRes.data[landlordId]
        } else {
          landlord.value = { userId: landlordId, username: '房东#' + landlordId, phone: '***', role: 'LANDLORD' }
        }
      } catch (e) {
        console.error('加载房东房源失败', e)
      } finally {
        loading.value = false
      }
    }

    const goToDetail = (id) => router.push('/house/' + id)
    const goBack = () => router.push('/houses')
    const maskPhone = (phone) => {
      if (!phone || phone.length < 11) return phone
      return phone.substring(0, 3) + '****' + phone.substring(7)
    }

    onMounted(loadData)

    return { landlord, houses, loading, total, defaultImage, goToDetail, goBack, handleImageError, maskPhone }
  }
}
</script>

<style scoped>
.landlord-houses-page { padding: 20px 0; }
.landlord-header {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px;
  padding: 20px;
}
.landlord-profile { display: flex; align-items: center; gap: 16px; }
.avatar {
  width: 64px; height: 64px;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: bold;
}
.info h1 { font-size: 22px; margin: 0 0 6px; display: flex; align-items: center; gap: 8px; }
.info .meta { display: flex; gap: 8px; font-size: 13px; opacity: 0.9; margin: 0; }
.tag-success {
  background: rgba(82,196,26,0.25);
  color: #d4f106;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}
.list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.result-count { color: var(--text-secondary); font-size: 14px; }
.btn-link {
  background: none; border: none;
  color: var(--primary-color, #1890ff);
  cursor: pointer; font-size: 13px;
}
.btn-link:hover { text-decoration: underline; }
.house-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.house-item {
  cursor: pointer; overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  background: #fff;
  border-radius: 8px;
}
.house-item:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.1); }
.house-cover { height: 180px; overflow: hidden; background: #f5f5f5; }
.house-cover img { width: 100%; height: 100%; object-fit: cover; }
.house-content { padding: 14px; }
.house-title {
  font-size: 16px; margin: 0 0 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.house-location { color: var(--text-secondary); font-size: 13px; margin: 0 0 8px; }
.house-tags { display: flex; gap: 6px; margin-bottom: 10px; }
.tag { padding: 2px 8px; border-radius: 4px; font-size: 11px; background: #f0f0f0; color: #666; }
.tag-primary { background: #e6f7ff; color: #1890ff; }
.house-bottom { display: flex; justify-content: space-between; align-items: flex-end; }
.price { color: #ff4d4f; font-weight: 600; font-size: 18px; }
.text-secondary { color: var(--text-secondary); }
.text-sm { font-size: 12px; }
.loading { text-align: center; padding: 60px; color: var(--text-secondary); }
.empty { margin-top: 20px; padding: 40px; text-align: center; }
</style>
