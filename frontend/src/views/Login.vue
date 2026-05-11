<template>
  <div class="login-container">
    <!-- 左侧品牌展示区 -->
    <div class="branding-side">
      <div class="branding-content">
        <div class="logo-area">
          <span class="logo-icon">🏠</span>
          <span class="logo-text">寓见云 · SmartLiving</span>
        </div>
        
        <div class="hero-text">
          <h1>遇见你的<br>理想生活</h1>
          <p>高品质长租公寓 · 智能管理 · 贴心服务</p>
        </div>
        
        <!-- 装饰性元素：浮动卡片 -->
        <div class="floating-card card-1">
          <span class="icon">✨</span>
          <div class="text">
            <h4>精选房源</h4>
            <small>100% 实地核验</small>
          </div>
        </div>
        
        <div class="floating-card card-2">
          <span class="icon">🛡️</span>
          <div class="text">
            <h4>安心签约</h4>
            <small>电子合同保障</small>
          </div>
        </div>
        
        <!-- 背景装饰 -->
        <div class="bg-circle circle-1"></div>
        <div class="bg-circle circle-2"></div>
      </div>
    </div>
    
    <!-- 右侧登录表单区 -->
    <div class="form-side">
      <div class="login-wrapper">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p class="text-secondary">请登录您的账号以继续</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label class="form-label">账号</label>
            <div class="input-wrapper">
              <span class="input-icon">👤</span>
              <input 
                type="text" 
                class="form-input with-icon" 
                v-model="form.account"
                placeholder="请输入用户名或手机号"
                required
              />
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">密码</label>
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input 
                type="password" 
                class="form-input with-icon" 
                v-model="form.password"
                placeholder="请输入密码"
                required
              />
            </div>
          </div>
          
          <div class="form-actions">
            <label class="checkbox-label">
              <input type="checkbox" checked />
              <span>记住我</span>
            </label>
            <a href="#" class="forgot-link">忘记密码？</a>
          </div>
          
          <button type="submit" class="btn btn-primary btn-block btn-xl" :disabled="loading">
            {{ loading ? '正在登录...' : '立即登录' }}
          </button>
        </form>
        
        <!-- 测试账号区域 -->
        <div class="test-accounts-section">
          <div class="divider">
            <span>快速测试通道</span>
          </div>
          
          <div class="account-grid">
            <div class="role-group">
              <span class="role-label">我是房东</span>
              <div class="pill-group">
                <button class="account-pill landlord" @click="fillAccount('landlord1')">Landlord A</button>
                <button class="account-pill landlord" @click="fillAccount('landlord2')">Landlord B</button>
              </div>
            </div>
            
            <div class="role-group">
              <span class="role-label">我是租客</span>
              <div class="pill-group">
                <button class="account-pill tenant" @click="fillAccount('tenant1')">Tenant A</button>
                <button class="account-pill tenant" @click="fillAccount('tenant2')">Tenant B</button>
              </div>
            </div>
          </div>
          <p class="pwd-hint">默认密码均为：123456</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../api/user'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)
    const form = ref({
      account: '',
      password: ''
    })
    
    // 快速填入测试账号
    const fillAccount = (account) => {
      form.value.account = account
      form.value.password = '123456'
    }
    
    // 登录处理
    const handleLogin = async () => {
      if (!form.value.account || !form.value.password) {
        // 使用简单的shake动画或toast更好，这里暂时保留alert
        return
      }
      
      loading.value = true
      try {
        const res = await login(form.value)
        localStorage.setItem('userInfo', JSON.stringify(res.data))
        
        const redirect = route.query.redirect || '/'
        router.push(redirect)
      } catch (error) {
        alert(error.message || '登录失败，请检查账号密码')
      } finally {
        loading.value = false
      }
    }
    
    return {
      form,
      loading,
      fillAccount,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
  background: var(--background-color); /* Premium beige background */
}

/* 左侧品牌区 - 优化比例和背景 */
.branding-side {
  width: 70%; /* Increased to 70% as requested */
  min-width: 600px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  color: #fff;
  overflow: hidden;
  background: url('https://images.unsplash.com/photo-1613490493576-7fde63acd811?q=80&w=1600&auto=format&fit=crop') center/cover no-repeat;
}

/* 渐变遮罩，确保文字可读 */
.branding-side::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(122, 157, 140, 0.9) 0%, rgba(95, 140, 118, 0.8) 100%);
  z-index: 1;
}

.branding-content {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.logo-area {
  position: absolute;
  top: 40px;
  left: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.hero-text {
  margin-top: -60px; /* Shift up slightly */
}

.hero-text h1 {
  font-size: 48px;
  line-height: 1.2;
  font-weight: 800;
  margin-bottom: 24px;
  letter-spacing: -1px;
  text-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.hero-text p {
  font-size: 18px;
  opacity: 0.95;
  font-weight: 400;
  max-width: 90%;
  line-height: 1.6;
}

/* 浮动装饰卡片 - 移到底部避免遮挡 */
.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-primary);
  padding: 14px 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  animation: float 6s ease-in-out infinite;
  z-index: 3;
}

.card-1 {
  bottom: 120px;
  right: -20px; /* Peeking out */
  animation-delay: 0s;
}

.card-2 {
  bottom: 40px;
  right: 40px;
  animation-delay: 2.5s;
}

.floating-card .icon {
  font-size: 20px;
  background: #E8F1EC;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.floating-card .text h4 {
  font-size: 14px;
  margin-bottom: 2px;
  font-weight: 700;
}

.floating-card .text small {
  font-size: 12px;
  color: var(--text-secondary);
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 右侧表单区 - 增加卡片感 */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--background-color); /* Light beige bg */
}

.login-wrapper {
  width: 100%;
  max-width: 480px;
  background: #fff;
  padding: 48px;
  border-radius: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s;
}

.login-wrapper:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.02);
}

.form-header {
  margin-bottom: 32px;
  text-align: center;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 输入框样式 */
.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: var(--text-disabled);
  transition: color 0.3s;
}

.form-input:focus ~ .input-wrapper .input-icon,
.input-wrapper:focus-within .input-icon {
  color: var(--primary-color);
}

.form-input.with-icon {
  padding-left: 48px;
  height: 52px;
  background: #F8F9FA;
  border: 2px solid transparent;
}

.form-input.with-icon:focus {
  background: #fff;
  border-color: var(--primary-color);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  font-size: 14px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  accent-color: var(--primary-color);
  width: 16px;
  height: 16px;
}

.forgot-link {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 13px;
}

.forgot-link:hover {
  text-decoration: underline;
}

.btn-xl {
  height: 52px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 12px;
  letter-spacing: 0.5px;
}

/* 测试账号区 */
.test-accounts-section {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px dashed var(--border-color);
}

.divider {
  display: none; /* Hide old divider */
}

.test-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-disabled);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 16px;
  text-align: center;
}

.account-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-group {
  display: flex;
  align-items: center;
  background: #F8F9FA;
  padding: 4px;
  border-radius: 99px;
  border: 1px solid #E9ECEF;
}

.role-label {
  font-size: 12px;
  color: var(--text-secondary);
  width: 70px;
  text-align: center;
  font-weight: 600;
}

.pill-group {
  display: flex;
  gap: 4px;
  flex: 1;
}

.account-pill {
  flex: 1;
  padding: 6px 0;
  border: none;
  background: #fff;
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.account-pill:hover {
  background: var(--primary-color);
  color: #fff;
  box-shadow: 0 4px 6px rgba(122, 157, 140, 0.2);
}

.pwd-hint {
  text-align: center;
  font-size: 12px;
  color: var(--text-disabled);
  margin-top: 12px;
  display: none; /* Simplify interface */
}

/* 响应式 */
@media (max-width: 900px) {
  .branding-side {
    display: none;
  }
  .form-side {
    padding: 20px;
    background: url('https://images.unsplash.com/photo-1613490493576-7fde63acd811?q=60&w=800&auto=format&fit=crop') center/cover;
  }
  .login-wrapper {
    max-width: 400px;
    padding: 32px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  }
}
</style>
