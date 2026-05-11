<template>
  <div class="house-publish-page">
    <div class="container">
      <div class="page-header">
        <h1>{{ isEdit ? '编辑房源' : '发布房源' }}</h1>
        <p class="text-secondary">请填写房源信息，发布后即可在平台展示</p>
      </div>
      
      <form @submit.prevent="handleSubmit" class="publish-form card">
        <div class="card-body">
          <!-- 基本信息 -->
          <div class="form-section">
            <h3>基本信息</h3>
            
            <div class="form-group">
              <label class="form-label">房源标题 *</label>
              <input 
                type="text" 
                class="form-input" 
                v-model="form.title"
                placeholder="如：精装两室一厅 地铁口 拎包入住"
                required
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">房源描述</label>
              <textarea 
                class="form-textarea" 
                v-model="form.description"
                placeholder="详细描述房源特点、周边配套等"
                rows="4"
              ></textarea>
            </div>
          </div>
          
          <!-- 位置信息 -->
          <div class="form-section">
            <h3>位置信息</h3>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">省份</label>
                <input type="text" class="form-input" v-model="form.province" placeholder="如：广东省" />
              </div>
              <div class="form-group">
                <label class="form-label">城市 *</label>
                <input type="text" class="form-input" v-model="form.city" placeholder="如：深圳市" required />
              </div>
              <div class="form-group">
                <label class="form-label">区县</label>
                <input type="text" class="form-input" v-model="form.district" placeholder="如：南山区" />
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">详细地址 *</label>
              <input 
                type="text" 
                class="form-input" 
                v-model="form.address"
                placeholder="如：科技园南路88号"
                required
              />
            </div>
          </div>
          
          <!-- 房屋信息 -->
          <div class="form-section">
            <h3>房屋信息</h3>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">面积(㎡)</label>
                <input type="number" step="0.01" class="form-input" v-model.number="form.area" placeholder="如：85" />
              </div>
              <div class="form-group">
                <label class="form-label">房间数</label>
                <select class="form-select" v-model.number="form.roomCount">
                  <option v-for="n in 10" :key="n" :value="n">{{ n }}室</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">厅数</label>
                <select class="form-select" v-model.number="form.hallCount">
                  <option v-for="n in 5" :key="n" :value="n - 1">{{ n - 1 }}厅</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">卫生间数</label>
                <select class="form-select" v-model.number="form.bathroomCount">
                  <option v-for="n in 5" :key="n" :value="n">{{ n }}卫</option>
                </select>
              </div>
            </div>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">楼层</label>
                <input type="number" class="form-input" v-model.number="form.floor" placeholder="如：15" />
              </div>
              <div class="form-group">
                <label class="form-label">总楼层</label>
                <input type="number" class="form-input" v-model.number="form.totalFloor" placeholder="如：30" />
              </div>
              <div class="form-group">
                <label class="form-label">朝向</label>
                <select class="form-select" v-model="form.orientation">
                  <option value="">请选择</option>
                  <option value="东">东</option>
                  <option value="南">南</option>
                  <option value="西">西</option>
                  <option value="北">北</option>
                  <option value="南北">南北</option>
                  <option value="东西">东西</option>
                  <option value="东南">东南</option>
                  <option value="西南">西南</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">装修</label>
                <select class="form-select" v-model="form.decoration">
                  <option value="">请选择</option>
                  <option value="ROUGH">毛坯</option>
                  <option value="SIMPLE">简装</option>
                  <option value="FINE">精装</option>
                  <option value="LUXURY">豪装</option>
                </select>
              </div>
            </div>
          </div>
          
          <!-- 租赁信息 -->
          <div class="form-section">
            <h3>租赁信息</h3>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">出租类型 *</label>
                <select class="form-select" v-model="form.rentType" required>
                  <option value="WHOLE">整租</option>
                  <option value="SHARED">合租</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">月租金(元) *</label>
                <input type="number" class="form-input" v-model.number="form.price" placeholder="如：5500" required />
              </div>
              <div class="form-group">
                <label class="form-label">付款方式</label>
                <select class="form-select" v-model="form.paymentMethod">
                  <option value="押一付一">押一付一</option>
                  <option value="押一付三">押一付三</option>
                  <option value="押二付一">押二付一</option>
                  <option value="半年付">半年付</option>
                  <option value="年付">年付</option>
                </select>
              </div>
            </div>
          </div>
          
          <!-- 配套设施 -->
          <div class="form-section">
            <h3>配套设施</h3>
            <div class="facility-options">
              <label 
                v-for="facility in facilityOptions" 
                :key="facility"
                class="facility-checkbox"
              >
                <input 
                  type="checkbox" 
                  :value="facility" 
                  v-model="form.facilities"
                />
                {{ facility }}
              </label>
            </div>
          </div>
          
          <!-- 房源图片 -->
          <div class="form-section">
            <h3>房源图片</h3>
            <p class="text-secondary text-sm mb-md">请输入图片URL，多个URL用换行分隔</p>
            <textarea 
              class="form-textarea" 
              v-model="imageUrlsText"
              placeholder="https://example.com/image1.jpg&#10;https://example.com/image2.jpg"
              rows="4"
            ></textarea>
          </div>
        </div>
        
        <div class="card-footer">
          <button type="button" class="btn btn-default" @click="$router.back()">取消</button>
          <button type="submit" class="btn btn-primary btn-lg" :disabled="submitting">
            {{ submitting ? '提交中...' : (isEdit ? '保存修改' : '发布房源') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { publishHouse, updateHouse, getHouseDetail } from '../../api/house'

export default {
  name: 'HousePublish',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const isEdit = computed(() => !!route.query.id)
    const submitting = ref(false)
    const imageUrlsText = ref('')
    
    const form = reactive({
      title: '',
      description: '',
      province: '',
      city: '',
      district: '',
      address: '',
      area: null,
      roomCount: 2,
      hallCount: 1,
      bathroomCount: 1,
      floor: null,
      totalFloor: null,
      orientation: '',
      decoration: '',
      rentType: 'WHOLE',
      price: null,
      paymentMethod: '押一付三',
      facilities: []
    })
    
    const facilityOptions = [
      '空调', '冰箱', '洗衣机', '热水器', '宽带', 
      '衣柜', '床', '沙发', '电视', '微波炉',
      '燃气灶', '抽油烟机', '独立卫生间', '阳台', '电梯'
    ]
    
    // 加载编辑数据
    const loadHouseData = async () => {
      if (!isEdit.value) return
      
      try {
        const res = await getHouseDetail(route.query.id)
        const house = res.data
        Object.keys(form).forEach(key => {
          if (house[key] !== undefined) {
            form[key] = house[key]
          }
        })
        if (house.images && house.images.length > 0) {
          imageUrlsText.value = house.images.map(img => img.imageUrl).join('\n')
        }
      } catch (error) {
        console.error('加载房源数据失败', error)
        alert('加载房源数据失败')
      }
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!form.title || !form.city || !form.address || !form.price) {
        alert('请填写必填项')
        return
      }
      
      submitting.value = true
      try {
        const data = { ...form }
        // 处理图片URL
        if (imageUrlsText.value.trim()) {
          data.imageUrls = imageUrlsText.value.split('\n').filter(url => url.trim())
        }
        
        if (isEdit.value) {
          await updateHouse(route.query.id, data)
          alert('房源更新成功！')
        } else {
          await publishHouse(data)
          alert('房源发布成功！')
        }
        router.push('/my-houses')
      } catch (error) {
        alert(error.message || '操作失败')
      } finally {
        submitting.value = false
      }
    }
    
    onMounted(() => {
      loadHouseData()
    })
    
    return {
      form,
      isEdit,
      submitting,
      imageUrlsText,
      facilityOptions,
      handleSubmit
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

.publish-form {
  max-width: 900px;
}

.form-section {
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.form-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.form-section h3 {
  font-size: 16px;
  margin-bottom: var(--spacing-md);
  color: var(--text-primary);
}

.form-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.facility-options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.facility-checkbox {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: 8px 16px;
  background: #fafafa;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.facility-checkbox:hover {
  background: #e6f7ff;
}

.facility-checkbox input:checked + span,
.facility-checkbox:has(input:checked) {
  background: #e6f7ff;
  color: var(--primary-color);
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
}
</style>
