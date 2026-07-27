<template>
  <div class="login-container">
    <!-- 左侧品牌展示区 (与 Login 完全一致) -->
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

        <div class="bg-circle circle-1"></div>
        <div class="bg-circle circle-2"></div>
      </div>
    </div>

    <!-- 右侧注册表单 -->
    <div class="form-side">
      <div class="login-wrapper">
        <div class="form-header">
          <h2>创建账号</h2>
          <p class="text-secondary">选择身份,开启专属服务</p>
        </div>

        <!-- 身份选择 -->
        <div class="role-selector">
          <div
            class="role-card"
            :class="{ active: form.role === 'TENANT' }"
            @click="form.role = 'TENANT'"
          >
            <span class="role-icon">🏠</span>
            <div class="role-text">
              <strong>我是租客</strong>
              <small>寻找理想住所</small>
            </div>
          </div>
          <div
            class="role-card"
            :class="{ active: form.role === 'LANDLORD' }"
            @click="form.role = 'LANDLORD'"
          >
            <span class="role-icon">🔑</span>
            <div class="role-text">
              <strong>我是房东</strong>
              <small>发布房源,收租金</small>
            </div>
          </div>
        </div>

        <form @submit.prevent="handleRegister" class="login-form">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div class="input-wrapper">
              <span class="input-icon">👤</span>
              <input
                type="text"
                class="form-input with-icon"
                v-model="form.username"
                placeholder="2-20位中英文/数字/下划线"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">手机号</label>
            <div class="input-wrapper">
              <span class="input-icon">📱</span>
              <input
                type="tel"
                class="form-input with-icon"
                v-model="form.phone"
                placeholder="11位手机号(用于登录)"
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
                placeholder="6-20位密码"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">确认密码</label>
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input
                type="password"
                class="form-input with-icon"
                v-model="form.confirmPassword"
                placeholder="再次输入密码"
                required
              />
            </div>
          </div>

          <button type="submit" class="btn btn-primary btn-block btn-xl" :disabled="loading">
            {{ loading ? '注册中...' : '立即注册' }}
          </button>
        </form>

        <div class="register-link">
          已有账号? <router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'

export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const form = ref({
      role: 'TENANT',
      username: '',
      phone: '',
      password: '123456',
      confirmPassword: '123456'
    })

    const handleRegister = async () => {
      // 客户端校验
      if (!form.value.role) {
        alert('请选择身份(租客/房东)')
        return
      }
      if (form.value.password !== form.value.confirmPassword) {
        alert('两次密码不一致')
        return
      }
      if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
        alert('手机号格式不正确')
        return
      }

      loading.value = true
      try {
        const res = await register({
          username: form.value.username,
          phone: form.value.phone,
          password: form.value.password,
          role: form.value.role
        })
        if (res && res.data) {
          localStorage.setItem('userInfo', JSON.stringify(res.data))
          alert(`注册成功! 欢迎您, ${res.data.username}`)
          router.push('/')
        } else {
          alert(res?.message || '注册失败')
        }
      } catch (error) {
        alert(error.message || '注册失败,请稍后再试')
      } finally {
        loading.value = false
      }
    }

    return { form, loading, handleRegister }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
  background: var(--background-color);
}

/* 左侧品牌区 - 与 Login 完全一致 */
.branding-side {
  width: 55%;
  min-width: 480px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  color: #fff;
  overflow: hidden;
  background: url('https://images.unsplash.com/photo-1613490493576-7fde63acd811?q=80&w=1600&auto=format&fit=crop') center/cover no-repeat;
}

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
  margin-top: -60px;
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

.card-1 { bottom: 120px; right: -20px; animation-delay: 0s; }
.card-2 { bottom: 40px; right: 40px; animation-delay: 2.5s; }

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

/* 右侧表单区 - 与 Login 完全一致 */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--background-color);
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
  margin-bottom: 24px;
  text-align: center;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 输入框样式 - 与 Login 完全一致 */
.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

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
  border-radius: 12px;
  width: 100%;
  box-sizing: border-box;
  font-size: 14px;
}

.form-input.with-icon:focus {
  outline: none;
  background: #fff;
  border-color: var(--primary-color);
}

/* === Register 特有: 身份选择 (复用 Login 的 pill 样式) === */
.role-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}

.role-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #F8F9FA;
  border: 2px solid #E9ECEF;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.role-card:hover {
  border-color: var(--primary-color);
  transform: translateY(-1px);
}

.role-card.active {
  background: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(122, 157, 140, 0.15);
}

.role-icon {
  font-size: 24px;
}

.role-text strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.role-text small {
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 按钮 - 与 Login 完全一致 */
.btn-xl {
  height: 52px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 12px;
  letter-spacing: 0.5px;
  width: 100%;
  margin-top: 8px;
}

/* 底部登录链接 */
.register-link {
  text-align: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed var(--border-color);
  font-size: 14px;
  color: var(--text-secondary);
}

.register-link a {
  color: var(--primary-color);
  font-weight: 600;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
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
