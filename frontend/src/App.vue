<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <header class="header" v-if="!isLoginPage">
      <div class="container header-inner">
        <router-link to="/" class="logo">🏠 寓见云 · SmartLiving</router-link>
        
        <nav class="nav">
          <router-link to="/houses" class="nav-link">房源列表</router-link>
          <template v-if="user">
            <router-link v-if="user.role === 'LANDLORD'" to="/my-houses" class="nav-link">我的房源</router-link>
            <router-link :to="user.role === 'LANDLORD' ? '/landlord/orders' : '/orders'" class="nav-link">
              {{ user.role === 'LANDLORD' ? '我的租客订单' : '我的订单' }}
            </router-link>
            <router-link to="/contracts" class="nav-link">
              {{ user.role === 'LANDLORD' ? '租约合同' : '我的合同' }}
            </router-link>
            <router-link to="/wallet" class="nav-link">钱包</router-link>
            <router-link v-if="user.role !== 'TENANT'" to="/notifications" class="nav-link notification-link">
              消息
              <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </router-link>
          </template>
        </nav>
        
        <div class="header-right">
          <template v-if="user">
            <span class="user-info">
              {{ user.username }}
              <span class="tag" :class="roleTagClass">{{ roleText }}</span>
            </span>
            <button class="btn btn-default btn-sm" @click="handleLogout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn btn-primary btn-sm">登录</router-link>
          </template>
        </div>
      </div>
    </header>
    
    <!-- 主内容区 -->
    <main class="main" :class="{ 'no-padding': isLoginPage }">
      <router-view />
    </main>
    
    <!-- 底部 -->
    <footer class="footer" v-if="!isLoginPage">
      <div class="container">
        <p>© 2026 公寓租赁系统 - 基于Vue 3 + Java微服务架构</p>
      </div>
    </footer>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getCurrentUser, logout } from './api/user'
import { getUnreadCount } from './api/notification'

export default {
  name: 'App',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const user = ref(null)
    const unreadCount = ref(0)
    
    const isLoginPage = computed(() => route.path === '/login')
    
    // 获取用户信息
    const loadUser = () => {
      user.value = getCurrentUser()
    }
    
    const loadUnreadCount = async () => {
      if (user.value && user.value.role !== 'TENANT') {
        try {
          const res = await getUnreadCount()
          unreadCount.value = res.data || 0
        } catch (e) {
          console.error('获取未读消息数失败', e)
        }
      }
    }
    
    // 角色文本
    const roleText = computed(() => {
      if (!user.value) return ''
      const roleMap = {
        'TENANT': '租客',
        'LANDLORD': '房东',
        'ADMIN': '管理员'
      }
      return roleMap[user.value.role] || user.value.role
    })
    
    // 角色标签样式
    const roleTagClass = computed(() => {
      if (!user.value) return ''
      const classMap = {
        'TENANT': 'tag-primary',
        'LANDLORD': 'tag-success',
        'ADMIN': 'tag-warning'
      }
      return classMap[user.value.role] || 'tag-default'
    })
    
    // 退出登录
    const handleLogout = () => {
      logout()
      user.value = null
      unreadCount.value = 0
      router.push('/login')
    }
    
    // 监听路由变化，刷新用户信息
    watch(() => router.currentRoute.value.path, () => {
      loadUser()
      loadUnreadCount()
    })
    
    onMounted(() => {
      loadUser()
      loadUnreadCount()
      // 定时刷新未读消息
      setInterval(loadUnreadCount, 30000)
    })
    
    return {
      user,
      unreadCount,
      roleText,
      roleTagClass,
      handleLogout,
      isLoginPage
    }
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 60px;
  gap: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary-color);
}

.nav {
  display: flex;
  gap: 16px;
  flex: 1;
}

.nav-link {
  padding: 8px 12px;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: var(--primary-color);
  background: var(--primary-light);
}

.notification-link {
  position: relative;
}

.badge {
  position: absolute;
  top: 0;
  right: -8px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  color: #fff;
  background: var(--error-color);
  border-radius: 9px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}

.main {
  min-height: calc(100vh - 120px);
  padding: var(--spacing-lg) 0;
}

.main.no-padding {
  padding: 0;
  min-height: 100vh;
}

.footer {
  background: #262626;
  color: #999;
  padding: var(--spacing-lg);
  text-align: center;
}
</style>
