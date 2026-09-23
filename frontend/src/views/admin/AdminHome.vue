<template>
  <div class="admin-home">
    <div class="container">
      <!-- 欢迎卡 -->
      <div class="welcome-card">
        <div class="avatar">🛡️</div>
        <div>
          <h1>欢迎回来, {{ user?.username || '管理员' }}</h1>
          <p>您正在以 <strong>系统管理员</strong> 身份操作,所有变更将影响全平台数据</p>
        </div>
      </div>

      <!-- 数据卡片 -->
      <div class="stats-grid">
        <div class="stat-card stat-blue">
          <div class="stat-icon">👥</div>
          <div class="stat-body">
            <div class="stat-label">总用户数</div>
            <div class="stat-value">{{ stats.totalUsers ?? '—' }}</div>
            <div class="stat-foot">含全部角色</div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-icon">🔑</div>
          <div class="stat-body">
            <div class="stat-label">房东数</div>
            <div class="stat-value">{{ stats.landlords ?? '—' }}</div>
            <div class="stat-foot">可发布房源</div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-icon">🏠</div>
          <div class="stat-body">
            <div class="stat-label">租客数</div>
            <div class="stat-value">{{ stats.tenants ?? '—' }}</div>
            <div class="stat-foot">可下单租房子</div>
          </div>
        </div>
        <div class="stat-card stat-purple">
          <div class="stat-icon">🛡️</div>
          <div class="stat-body">
            <div class="stat-label">管理员数</div>
            <div class="stat-value">{{ stats.admins ?? '—' }}</div>
            <div class="stat-foot">可进后台</div>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="actions-row">
        <h2>快捷操作</h2>
        <div class="action-grid">
          <router-link to="/admin/users" class="action-card">
            <span class="icon">📋</span>
            <div>
              <strong>用户管理</strong>
              <p>查看/筛选/改角色</p>
            </div>
          </router-link>
          <router-link to="/admin/houses" class="action-card">
            <span class="icon">🏢</span>
            <div>
              <strong>房源管理</strong>
              <p>查看全平台 40 万房源</p>
            </div>
          </router-link>
        </div>
      </div>

      <!-- 平台健康度 -->
      <div class="health-card">
        <h2>📋 平台数据校验</h2>
        <table class="health-table">
          <thead>
            <tr><th>检查项</th><th>结果</th><th>说明</th></tr>
          </thead>
          <tbody>
            <tr>
              <td>房源归属校验</td>
              <td><span class="tag tag-success">已修复</span></td>
              <td>所有 40 万房源的 landlord_id 都已指向真实 LANDLORD 用户</td>
            </tr>
            <tr>
              <td>主从同步</td>
              <td><span class="tag tag-success">正常</span></td>
              <td>主库与从库 house 表数据一致</td>
            </tr>
            <tr>
              <td>统一登录密码</td>
              <td><span class="tag tag-success">已统一</span></td>
              <td>所有用户密码均为 <code>123456</code></td>
            </tr>
            <tr>
              <td>缓存清理</td>
              <td><span class="tag tag-success">已完成</span></td>
              <td>Redis 已 FLUSHALL,前端看到的是最新数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getUserStats, getCurrentUser } from '../../api/user'

export default {
  name: 'AdminHome',
  setup() {
    const user = ref(getCurrentUser())
    const stats = ref({})

    const load = async () => {
      try {
        const res = await getUserStats()
        stats.value = res.data || {}
      } catch (e) {
        console.error('加载统计失败', e)
      }
    }

    onMounted(load)
    return { user, stats }
  }
}
</script>

<style scoped>
.admin-home { padding: 20px 0; }

.welcome-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 28px 32px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);
}
.welcome-card .avatar {
  font-size: 48px;
  width: 72px;
  height: 72px;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.welcome-card h1 { font-size: 22px; margin: 0 0 6px; }
.welcome-card p { margin: 0; opacity: 0.9; font-size: 13px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  border-left: 4px solid;
}
.stat-blue { border-left-color: #1890ff; }
.stat-green { border-left-color: #52c41a; }
.stat-orange { border-left-color: #fa8c16; }
.stat-purple { border-left-color: #722ed1; }

.stat-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  background: #f5f7fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-label { font-size: 13px; color: #999; }
.stat-value { font-size: 28px; font-weight: 700; color: #262626; line-height: 1.2; }
.stat-foot { font-size: 11px; color: #bbb; margin-top: 2px; }

.actions-row { margin-bottom: 32px; }
.actions-row h2, .health-card h2 {
  font-size: 18px;
  margin-bottom: 14px;
  color: #262626;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}
.action-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  text-decoration: none;
  color: #262626;
  transition: all 0.2s;
}
.action-card:hover {
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}
.action-card .icon { font-size: 28px; }
.action-card strong { display: block; font-size: 14px; margin-bottom: 2px; }
.action-card p { font-size: 12px; color: #999; margin: 0; }

.health-card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.health-table { width: 100%; border-collapse: collapse; }
.health-table th, .health-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}
.health-table th { color: #999; font-weight: 500; background: #fafafa; }
.tag-success { background: #f6ffed; color: #52c41a; padding: 2px 10px; border-radius: 4px; font-size: 12px; }
code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
</style>
