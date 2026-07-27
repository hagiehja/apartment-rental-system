<template>
  <div class="recommend-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <div>
          <h1>🤖 智能推荐</h1>
          <p class="subtitle">基于 TensorFlow FM 算法的个性化房源推荐</p>
        </div>
      </div>

      <!-- TensorFlow 模型信息卡片 -->
      <div class="model-card card" v-if="modelInfo">
        <div class="card-body">
          <div class="model-header">
            <div class="model-icon">🧠</div>
            <div class="model-meta">
              <div class="model-title">
                {{ modelInfo.version || 'FM Model' }}
                <span class="tag tag-primary">{{ modelInfo.tensorflowVersion }}</span>
              </div>
              <div class="model-desc">
                TensorFlow 训练的 Factorization Machines 模型,{{ modelInfo.nFeatures }} 维特征 × {{ modelInfo.kFactors }} 维隐向量
              </div>
            </div>
            <div class="model-metrics">
              <div class="metric">
                <div class="metric-value">{{ (modelInfo.valAuc * 100).toFixed(2) }}%</div>
                <div class="metric-label">AUC</div>
              </div>
              <div class="metric">
                <div class="metric-value">{{ (modelInfo.valAccuracy * 100).toFixed(1) }}%</div>
                <div class="metric-label">准确率</div>
              </div>
              <div class="metric">
                <div class="metric-value">{{ (modelInfo.nSamples / 1000).toFixed(0) }}k</div>
                <div class="metric-label">训练样本</div>
              </div>
            </div>
          </div>
          <div class="model-footer">
            训练时间: {{ formatTime(modelInfo.trainedAt) }}
          </div>
        </div>
      </div>

      <!-- 偏好设置 -->
      <div class="preference-card card">
        <div class="card-body">
          <h3 class="section-title">偏好设置</h3>
          <p class="section-desc">完善偏好,获得更精准的推荐</p>
          <div class="pref-grid">
            <div class="pref-item">
              <label>城市</label>
              <input type="text" class="form-input" v-model="pref.city" placeholder="如:北京市"/>
            </div>
            <div class="pref-item">
              <label>区域</label>
              <input type="text" class="form-input" v-model="pref.district" placeholder="如:朝阳区"/>
            </div>
            <div class="pref-item">
              <label>最低价(元/月)</label>
              <input type="number" class="form-input" v-model.number="pref.minPrice" placeholder="2000"/>
            </div>
            <div class="pref-item">
              <label>最高价(元/月)</label>
              <input type="number" class="form-input" v-model.number="pref.maxPrice" placeholder="6000"/>
            </div>
            <div class="pref-item">
              <label>居室数</label>
              <select class="form-select" v-model.number="pref.roomCount">
                <option :value="null">不限</option>
                <option :value="1">1室</option>
                <option :value="2">2室</option>
                <option :value="3">3室</option>
                <option :value="4">4室+</option>
              </select>
            </div>
            <div class="pref-item">
              <label>出租类型</label>
              <select class="form-select" v-model="pref.rentType">
                <option value="">不限</option>
                <option value="WHOLE">整租</option>
                <option value="SHARED">合租</option>
              </select>
            </div>
          </div>
          <div class="pref-actions">
            <button class="btn btn-primary" @click="savePref">保存偏好并刷新推荐</button>
            <button class="btn btn-default" @click="resetPref">重置</button>
          </div>
        </div>
      </div>

      <!-- 推荐结果 -->
      <div class="recommend-list-header">
        <h3>为你推荐的房源 <span class="count">共 {{ total }} 套</span></h3>
        <button class="btn btn-default btn-sm" @click="loadRecommend">🔄 刷新</button>
      </div>

      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>

      <div v-else-if="recommendList.length === 0" class="empty card">
        <div class="card-body">
          <p>📝 暂无推荐结果,请先完善偏好</p>
        </div>
      </div>

      <div v-else class="recommend-grid">
        <div
          v-for="house in recommendList"
          :key="house.houseId"
          class="recommend-card card"
          @click="goToDetail(house.houseId)"
        >
          <div class="rec-cover">
            <img :src="house.coverImage || defaultImage" :alt="house.title" @error="handleImageError"/>
            <span class="rec-tag" :class="house.rentType === 'WHOLE' ? 'tag-whole' : 'tag-share'">
              {{ house.rentType === 'WHOLE' ? '整租' : '合租' }}
            </span>
            <div class="score-badge">
              <div class="score-circle" :style="scoreStyle(house.recommendScore)">
                <span>{{ house.recommendScore.toFixed(1) }}</span>
              </div>
              <div class="score-label">推荐分</div>
            </div>
          </div>
          <div class="rec-info">
            <h4 class="rec-title">{{ house.title }}</h4>
            <div class="rec-meta">
              <span>📍 {{ house.city }} {{ house.district }}</span>
              <span>·</span>
              <span>{{ house.roomCount }}室{{ house.hallCount }}厅</span>
              <span>·</span>
              <span>{{ house.area }}㎡</span>
            </div>
            <div class="rec-reason">
              <span class="reason-label">推荐理由:</span>
              <span class="reason-text">{{ house.recommendReason }}</span>
            </div>
            <div class="rec-footer">
              <span class="rec-price">¥{{ house.price }}<span class="unit">/月</span></span>
              <span class="rec-views">👁 {{ house.viewCount }} 次浏览</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <button class="btn btn-default btn-sm" :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">上一页</button>
        <span class="page-info">{{ pageNum }} / {{ Math.ceil(total / pageSize) }}</span>
        <button class="btn btn-default btn-sm" :disabled="pageNum * pageSize >= total" @click="changePage(pageNum + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  getRecommendations, trackBehavior, savePreference, getPreference, getRecommendModelInfo
} from '../../api/house'
import { DEFAULT_IMAGE, handleImageError } from '../../utils/image'

export default {
  name: 'Recommend',
  setup() {
    const router = useRouter()
    const modelInfo = ref(null)
    const recommendList = ref([])
    const loading = ref(false)
    const total = ref(0)
    const pageNum = ref(1)
    const pageSize = ref(10)
    const defaultImage = DEFAULT_IMAGE

    const pref = ref({
      city: '', district: '', minPrice: null, maxPrice: null,
      roomCount: null, rentType: ''
    })

    const scoreColor = (score) => {
      if (score >= 70) return '#7CB342'
      if (score >= 50) return '#FB8C00'
      if (score >= 30) return '#F4511E'
      return '#999'
    }
    const scoreStyle = (score) => ({
      background: `conic-gradient(${scoreColor(score)} ${score * 3.6}deg, #eee ${score * 3.6}deg)`
    })

    const formatTime = (iso) => {
      if (!iso) return '-'
      try { return new Date(iso).toLocaleString('zh-CN') } catch { return iso }
    }

    const loadModelInfo = async () => {
      try {
        const res = await getRecommendModelInfo()
        modelInfo.value = res.data
      } catch (e) { console.error('模型信息加载失败', e) }
    }

    const loadPreference = async () => {
      try {
        const res = await getPreference()
        if (res.data) {
          pref.value.city = res.data.city || ''
          pref.value.district = res.data.district || ''
          pref.value.minPrice = res.data.minPrice
          pref.value.maxPrice = res.data.maxPrice
          pref.value.roomCount = res.data.roomCount
          pref.value.rentType = res.data.rentType || ''
        }
      } catch (e) { console.warn('偏好加载失败(可能未登录)', e) }
    }

    const loadRecommend = async () => {
      loading.value = true
      try {
        const res = await getRecommendations({ pageNum: pageNum.value, pageSize: pageSize.value })
        recommendList.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (e) {
        console.error('推荐加载失败', e)
        if (e?.response?.status === 401) {
          router.push({ name: 'Login', query: { redirect: '/recommend' } })
        }
      } finally {
        loading.value = false
      }
    }

    const savePref = async () => {
      try {
        await savePreference(pref.value)
        pageNum.value = 1
        await loadRecommend()
      } catch (e) {
        console.error('偏好保存失败', e)
        alert('保存失败,请先登录')
      }
    }

    const resetPref = () => {
      pref.value = { city: '', district: '', minPrice: null, maxPrice: null, roomCount: null, rentType: '' }
    }

    const changePage = (p) => {
      pageNum.value = p
      loadRecommend()
    }

    const goToDetail = async (id) => {
      // 埋点: 进入详情前上报 VIEW 行为(用于训练数据收集)
      try {
        await trackBehavior({ houseId: id, behaviorType: 'CLICK', source: 'recommend' })
      } catch (e) { /* 静默失败,不影响跳转 */ }
      router.push(`/house/${id}`)
    }

    onMounted(async () => {
      await Promise.all([loadModelInfo(), loadPreference()])
      await loadRecommend()
    })

    return {
      modelInfo, recommendList, loading, total, pageNum, pageSize, pref,
      defaultImage, formatTime, scoreStyle,
      loadRecommend, savePref, resetPref, changePage, goToDetail, handleImageError
    }
  }
}
</script>

<style scoped>
.recommend-page { padding-bottom: 40px; }

.page-header {
  margin-bottom: 24px;
}
.page-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
}
.page-header .subtitle {
  color: var(--text-secondary);
  margin-top: 4px;
  font-size: 14px;
}

/* Model Card - 算法可见性 */
.model-card {
  background: linear-gradient(135deg, #fff 0%, #f8fbff 100%);
  margin-bottom: 24px;
  border: 1px solid #e3e8ef;
}
.model-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.model-icon {
  font-size: 42px;
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #7CB342 0%, #558B2F 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.model-meta { flex: 1; }
.model-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}
.model-title .tag {
  margin-left: 8px;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  background: var(--primary-light);
  color: var(--primary-color);
  font-weight: 600;
}
.model-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}
.model-metrics {
  display: flex;
  gap: 24px;
}
.metric {
  text-align: center;
  padding: 0 12px;
}
.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--primary-color);
}
.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.model-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e3e8ef;
  font-size: 12px;
  color: var(--text-disabled);
}

/* Preference Card */
.preference-card { margin-bottom: 24px; }
.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}
.section-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}
.pref-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.pref-item { display: flex; flex-direction: column; gap: 6px; }
.pref-item label {
  font-size: 12px;
  color: var(--text-secondary);
}
.pref-actions {
  display: flex;
  gap: 12px;
}

/* Recommend List */
.recommend-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.recommend-list-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.count {
  font-size: 13px;
  font-weight: normal;
  color: var(--text-secondary);
  margin-left: 8px;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.recommend-card {
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
}
.recommend-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.rec-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
}
.rec-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rec-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  border-radius: 4px;
  z-index: 2;
}
.tag-whole { background: rgba(122, 157, 140, 0.9); }
.tag-share { background: rgba(238, 215, 154, 0.9); color: #5C4B29; }

/* Score Badge (推荐分可视化) */
.score-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  text-align: center;
}
.score-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.score-circle::after {
  content: '';
  position: absolute;
  inset: 4px;
  background: #fff;
  border-radius: 50%;
  z-index: 1;
}
.score-circle span {
  position: relative;
  z-index: 2;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.score-label {
  font-size: 10px;
  color: #fff;
  background: rgba(0,0,0,0.6);
  padding: 2px 6px;
  border-radius: 8px;
  margin-top: 4px;
}

.rec-info { padding: 16px; }
.rec-title {
  font-size: 15px;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rec-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 10px;
}
.rec-meta span:nth-child(even) { color: var(--text-disabled); }
.rec-reason {
  background: #f5f7fa;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
  margin-bottom: 12px;
}
.reason-label {
  color: var(--text-secondary);
  margin-right: 4px;
}
.reason-text {
  color: var(--text-primary);
  font-weight: 500;
}
.rec-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.rec-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary-color);
}
.rec-price .unit {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: normal;
}
.rec-views {
  font-size: 12px;
  color: var(--text-disabled);
}

/* Pagination */
.pagination {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}
.page-info {
  color: var(--text-secondary);
  font-size: 14px;
}

@media (max-width: 992px) {
  .pref-grid { grid-template-columns: repeat(2, 1fr); }
  .recommend-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .model-header { flex-direction: column; align-items: flex-start; }
  .model-metrics { width: 100%; justify-content: space-around; }
  .pref-grid { grid-template-columns: 1fr; }
}
</style>
