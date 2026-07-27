<template>
  <div class="admin-houses">
    <div class="container">
      <div class="page-header">
        <div>
          <h1>房源管理</h1>
          <p>全平台共 <strong>{{ total }}</strong> 套房源 · 第 {{ pageNum }} / {{ totalPages }} 页</p>
        </div>
        <div class="header-actions">
          <select v-model="filter.status" @change="resetAndLoad" class="filter-select">
            <option value="">全部状态</option>
            <option value="AVAILABLE">可租</option>
            <option value="RENTED">已出租</option>
            <option value="OFFLINE">已下架</option>
          </select>
          <select v-model="filter.rentType" @change="resetAndLoad" class="filter-select">
            <option value="">全部类型</option>
            <option value="WHOLE">整租</option>
            <option value="SHARED">合租</option>
          </select>
          <select v-model="pageSize" @change="resetAndLoad" class="filter-select">
            <option :value="10">10/页</option>
            <option :value="20">20/页</option>
            <option :value="50">50/页</option>
          </select>
        </div>
      </div>

      <!-- 房源表 -->
      <div class="house-table card">
        <table>
          <thead>
            <tr>
              <th width="80">ID</th>
              <th>标题</th>
              <th width="120">房东</th>
              <th width="100">城市</th>
              <th width="80">户型</th>
              <th width="90">租金/月</th>
              <th width="90">状态</th>
              <th width="80">浏览</th>
              <th width="100">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="9" class="text-center text-secondary" style="padding:40px">加载中...</td>
            </tr>
            <tr v-else-if="houses.length === 0">
              <td colspan="9" class="text-center text-secondary" style="padding:40px">没有匹配的房源</td>
            </tr>
            <tr v-for="h in houses" :key="h.houseId">
              <td>{{ h.houseId }}</td>
              <td class="title-cell" :title="h.title">{{ h.title }}</td>
              <td>
                <router-link :to="`/landlord/${h.landlordId}/houses`" class="landlord-link">
                  {{ h.landlordName || '未知' }}
                </router-link>
              </td>
              <td>{{ h.city }}{{ h.district ? ' '+h.district : '' }}</td>
              <td>{{ h.roomCount }}室{{ h.hallCount }}厅</td>
              <td class="price">¥{{ formatPrice(h.price) }}</td>
              <td>
                <span class="status-tag" :class="'status-' + (h.status||'').toLowerCase()">
                  {{ statusText(h.status) }}
                </span>
              </td>
              <td class="text-secondary">{{ h.viewCount }}</td>
              <td>
                <router-link :to="`/house/${h.houseId}`" class="btn-link">查看</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination" v-if="total > pageSize">
        <button class="btn btn-default" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button>
        <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
        <button class="btn btn-default" :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { getHouseList } from '../../api/house'

export default {
  name: 'AdminHouses',
  setup() {
    const houses = ref([])
    const total = ref(0)
    const pageNum = ref(1)
    const pageSize = ref(20)
    const loading = ref(false)
    const filter = ref({ status: '', rentType: '' })

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

    const load = async () => {
      loading.value = true
      try {
        const res = await getHouseList({
          pageNum: pageNum.value,
          pageSize: pageSize.value,
          status: filter.value.status || undefined,
          rentType: filter.value.rentType || undefined
        })
        houses.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (e) {
        console.error('加载房源失败', e)
      } finally {
        loading.value = false
      }
    }

    const resetAndLoad = () => { pageNum.value = 1; load() }
    const goPage = (p) => {
      if (!p || p < 1 || p > totalPages.value) return
      pageNum.value = p
      load()
    }

    const statusText = (s) => ({ AVAILABLE: '可租', RENTED: '已出租', OFFLINE: '已下架' }[s] || s)
    const formatPrice = (p) => p != null ? Number(p).toLocaleString('zh-CN') : '—'

    onMounted(load)
    return { houses, total, pageNum, pageSize, totalPages, loading, filter,
             resetAndLoad, goPage, statusText, formatPrice }
  }
}
</script>

<style scoped>
.admin-houses { padding: 20px 0; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 14px;
}
.page-header h1 { font-size: 24px; margin: 0 0 4px; }
.page-header p { color: #999; margin: 0; font-size: 13px; }
.header-actions { display: flex; gap: 8px; }
.filter-select {
  height: 32px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13px;
  background: #fff;
}

.house-table {
  overflow-x: auto;
  margin-bottom: 18px;
}
.house-table table { width: 100%; border-collapse: collapse; font-size: 13px; }
.house-table th, .house-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: middle;
}
.house-table th { background: #fafafa; color: #666; font-weight: 500; }
.house-table tr:hover { background: #fafafa; }

.title-cell {
  max-width: 240px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.landlord-link { color: #1890ff; text-decoration: none; }
.landlord-link:hover { text-decoration: underline; }

.price { color: #ff4d4f; font-weight: 600; }

.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
}
.status-available { background: #f6ffed; color: #52c41a; }
.status-rented { background: #fff7e6; color: #fa8c16; }
.status-offline { background: #f5f5f5; color: #999; }

.btn-link {
  background: none;
  border: none;
  color: #1890ff;
  cursor: pointer;
  text-decoration: none;
  font-size: 13px;
}
.btn-link:hover { text-decoration: underline; }

.text-secondary { color: #999; }
.text-center { text-align: center; }

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}
.page-info { font-size: 13px; color: #666; min-width: 80px; text-align: center; }
</style>
