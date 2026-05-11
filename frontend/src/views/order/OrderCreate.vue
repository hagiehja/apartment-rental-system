<template>
  <div class="order-create-page">
    <div class="container">
      <div class="page-header">
        <h1>确认订单</h1>
        <p class="text-secondary">请确认租赁信息并提交订单</p>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
      </div>
      
      <template v-else-if="house">
        <div class="order-layout">
          <!-- 左侧：房源信息 -->
          <div class="order-main">
            <!-- 房源卡片 -->
            <div class="house-card card">
              <div class="card-header">房源信息</div>
              <div class="card-body flex gap-md">
                <div class="house-cover">
                  <img :src="house.images?.[0]?.imageUrl || defaultImage" :alt="house.title" @error="handleImageError" />
                </div>
                <div class="house-info">
                  <h3>{{ house.title }}</h3>
                  <p class="text-secondary">📍 {{ house.province }}{{ house.city }}{{ house.district }}{{ house.address }}</p>
                  <div class="house-tags mt-sm">
                    <span class="tag tag-primary">{{ house.rentType === 'WHOLE' ? '整租' : '合租' }}</span>
                    <span class="tag tag-default">{{ house.roomCount }}室{{ house.hallCount }}厅</span>
                    <span class="tag tag-default">{{ house.area }}㎡</span>
                  </div>
                  <p class="price mt-md">¥{{ house.price }}<span class="price-unit">/月</span></p>
                </div>
              </div>
            </div>
            
            <!-- 租期设置 -->
            <div class="rent-form card">
              <div class="card-header">租期设置</div>
              <div class="card-body">
                <div class="form-row">
                  <div class="form-group">
                    <label class="form-label">起租日期 *</label>
                    <input 
                      type="date" 
                      class="form-input"
                      v-model="form.rentStartDate"
                      :min="minDate"
                      required
                    />
                  </div>
                  <div class="form-group">
                    <label class="form-label">租赁月数 *</label>
                    <select class="form-select" v-model.number="form.rentMonths">
                      <option v-for="n in 24" :key="n" :value="n">{{ n }}个月</option>
                    </select>
                  </div>
                </div>
                
                <div class="rent-info mt-md">
                  <p>📅 租期：{{ form.rentStartDate }} 至 {{ rentEndDate }}</p>
                </div>
                
                <div class="form-group mt-md">
                  <label class="form-label">
                    <input type="checkbox" v-model="form.installmentEnabled" />
                    开启分期付款（每月支付一次）
                  </label>
                  <p class="text-secondary text-sm mt-xs" v-if="form.installmentEnabled">
                    分期付款将在每月起租日前生成账单
                  </p>
                </div>
                
                <div class="form-group mt-md">
                  <label class="form-label">备注</label>
                  <textarea 
                    class="form-textarea" 
                    v-model="form.remark"
                    placeholder="填写备注信息（可选）"
                    rows="2"
                  ></textarea>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 右侧：费用明细 -->
          <div class="order-sidebar">
            <div class="price-card card">
              <div class="card-header">费用明细</div>
              <div class="card-body">
                <div class="price-item">
                  <span>月租金</span>
                  <span>¥{{ house.price }}</span>
                </div>
                <div class="price-item">
                  <span>租赁月数</span>
                  <span>{{ form.rentMonths }}个月</span>
                </div>
                <div class="price-item">
                  <span>押金（一个月租金）</span>
                  <span>¥{{ house.price }}</span>
                </div>
                <div class="divider"></div>
                <div class="price-item total">
                  <span>订单总额</span>
                  <span class="price price-lg">¥{{ totalAmount }}</span>
                </div>
                <div class="price-item" v-if="form.installmentEnabled">
                  <span>首期应付</span>
                  <span class="price">¥{{ firstPayment }}</span>
                </div>
                <div class="price-item" v-else>
                  <span>应付金额</span>
                  <span class="price">¥{{ totalAmount }}</span>
                </div>
              </div>
              <div class="card-footer">
                <button 
                  class="btn btn-primary btn-lg btn-block"
                  :disabled="submitting"
                  @click="handleSubmit"
                >
                  {{ submitting ? '提交中...' : '提交订单' }}
                </button>
                <p class="text-secondary text-sm text-center mt-sm">
                  提交后请在30分钟内完成支付
                </p>
              </div>
            </div>
          </div>
        </div>
      </template>
      
      <div v-else class="empty card">
        <div class="card-body text-center">
          <p>😕 房源信息加载失败</p>
          <router-link to="/houses" class="btn btn-primary mt-md">返回房源列表</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getHouseDetail } from '../../api/house'
import { createOrder } from '../../api/order'

import { DEFAULT_IMAGE, handleImageError } from '../../utils/image'

export default {
  name: 'OrderCreate',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const house = ref(null)
    const loading = ref(false)
    const submitting = ref(false)
    
    const defaultImage = DEFAULT_IMAGE
    
    // 最小日期（明天）
    const minDate = computed(() => {
      const tomorrow = new Date()
      tomorrow.setDate(tomorrow.getDate() + 1)
      const year = tomorrow.getFullYear()
      const month = String(tomorrow.getMonth() + 1).padStart(2, '0')
      const day = String(tomorrow.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    })
    
    const form = reactive({
      rentStartDate: minDate.value,
      rentMonths: 6,
      installmentEnabled: false,
      remark: ''
    })
    
    // 租期结束日期
    const rentEndDate = computed(() => {
      if (!form.rentStartDate) return ''
      const start = new Date(form.rentStartDate)
      start.setMonth(start.getMonth() + form.rentMonths)
      start.setDate(start.getDate() - 1)
      return start.toISOString().split('T')[0]
    })
    
    // 总金额
    const totalAmount = computed(() => {
      if (!house.value) return 0
      return house.value.price * form.rentMonths + house.value.price // 租金 + 押金
    })
    
    // 首期付款（押金 + 首月租金）
    const firstPayment = computed(() => {
      if (!house.value) return 0
      return house.value.price * 2 // 押金 + 首月
    })
    
    // 加载房源信息
    const loadHouse = async () => {
      const houseId = route.params.houseId
      loading.value = true
      try {
        const res = await getHouseDetail(houseId)
        house.value = res.data
        
        if (house.value.status !== 'AVAILABLE') {
          alert('该房源已不可租')
          router.push('/houses')
        }
      } catch (error) {
        console.error('加载房源失败', error)
      } finally {
        loading.value = false
      }
    }
    
    // 提交订单
    const handleSubmit = async () => {
      if (!form.rentStartDate) {
        alert('请选择起租日期')
        return
      }
      
      submitting.value = true
      try {
        const res = await createOrder({
          houseId: house.value.houseId,
          rentStartDate: form.rentStartDate,
          rentMonths: form.rentMonths,
          installmentEnabled: form.installmentEnabled,
          remark: form.remark
        })
        
        alert('订单创建成功！')
        // 跳转到支付页面
        router.push(`/payment/${res.data.orderNo}`)
      } catch (error) {
        alert(error.message || '订单创建失败')
      } finally {
        submitting.value = false
      }
    }
    
    onMounted(() => {
      loadHouse()
    })
    
    return {
      house,
      loading,
      submitting,
      form,
      minDate,
      rentEndDate,
      totalAmount,
      firstPayment,
      defaultImage,
      handleSubmit,
      handleImageError
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

.order-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: var(--spacing-lg);
  align-items: start;
}

@media (max-width: 992px) {
  .order-layout {
    grid-template-columns: 1fr;
  }
}

.order-main {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.house-cover {
  width: 180px;
  min-width: 180px;
  height: 120px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.house-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.house-info h3 {
  font-size: 18px;
  margin-bottom: var(--spacing-xs);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.rent-info {
  padding: var(--spacing-md);
  background: #f0f7ff;
  border-radius: var(--radius-sm);
}

.order-sidebar {
  position: sticky;
  top: 80px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-sm) 0;
}

.price-item.total {
  font-size: 16px;
  font-weight: 600;
}

.divider {
  height: 1px;
  background: var(--border-color);
  margin: var(--spacing-sm) 0;
}
</style>
