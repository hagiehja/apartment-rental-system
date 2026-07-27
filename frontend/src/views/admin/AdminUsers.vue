<template>
  <div class="admin-users">
    <div class="container">
      <div class="page-header">
        <div>
          <h1>用户管理</h1>
          <p>共 <strong>{{ total }}</strong> 个用户 · 当前第 {{ pageNum }} 页</p>
        </div>
      </div>

      <!-- 筛选条 -->
      <div class="filter-bar card">
        <div class="filter-group">
          <label>角色:</label>
          <select v-model="filter.role" @change="resetAndLoad">
            <option value="">全部</option>
            <option value="TENANT">租客</option>
            <option value="LANDLORD">房东</option>
            <option value="ADMIN">管理员</option>
          </select>
        </div>
        <div class="filter-group">
          <label>搜索:</label>
          <input type="text" v-model="filter.keyword" placeholder="用户名/手机号" @keyup.enter="resetAndLoad" />
          <button class="btn btn-primary btn-sm" @click="resetAndLoad">搜索</button>
          <button class="btn btn-default btn-sm" @click="clearFilter">重置</button>
        </div>
      </div>

      <!-- 用户表 -->
      <div class="user-table card">
        <table>
          <thead>
            <tr>
              <th width="80">ID</th>
              <th>用户名</th>
              <th>手机号</th>
              <th width="100">角色</th>
              <th width="170">注册时间</th>
              <th width="200">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="text-center text-secondary" style="padding:40px">加载中...</td>
            </tr>
            <tr v-else-if="users.length === 0">
              <td colspan="6" class="text-center text-secondary" style="padding:40px">没找到匹配的用户</td>
            </tr>
            <tr v-for="u in users" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td>
                <strong>{{ u.username }}</strong>
                <span v-if="u.userId === currentUserId" class="self-tag">本人</span>
              </td>
              <td class="text-mono">{{ u.phone }}</td>
              <td>
                <span class="role-tag" :class="'role-' + (u.role || '').toLowerCase()">
                  {{ roleText(u.role) }}
                </span>
              </td>
              <td class="text-secondary">{{ formatDate(u.createTime) }}</td>
              <td>
                <select
                  :value="u.role"
                  :disabled="u.userId === currentUserId"
                  @change="changeRole(u, $event.target.value)"
                  class="role-select"
                  :title="u.userId === currentUserId ? '不能改自己的角色' : ''"
                >
                  <option value="TENANT">租客</option>
                  <option value="LANDLORD">房东</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <button class="btn btn-default" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button>
        <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
        <button class="btn btn-default" :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
        <span class="page-jump">
          跳转到 <input type="number" v-model.number="jumpPage" min="1" :max="totalPages" style="width:70px" />
          <button class="btn btn-default btn-sm" @click="goPage(jumpPage)">Go</button>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { adminListUsers, adminUpdateUserRole, getCurrentUser } from '../../api/user'

export default {
  name: 'AdminUsers',
  setup() {
    const currentUserId = ref(getCurrentUser()?.userId)
    const users = ref([])
    const total = ref(0)
    const pageNum = ref(1)
    const pageSize = ref(20)
    const loading = ref(false)
    const filter = ref({ role: '', keyword: '' })
    const jumpPage = ref(1)

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

    const load = async () => {
      loading.value = true
      try {
        const res = await adminListUsers({
          pageNum: pageNum.value,
          pageSize: pageSize.value,
          role: filter.value.role || undefined,
          keyword: filter.value.keyword || undefined
        })
        users.value = res.data?.records || []
        total.value = res.data?.total || 0
      } catch (e) {
        console.error('加载用户列表失败', e)
      } finally {
        loading.value = false
      }
    }

    const resetAndLoad = () => { pageNum.value = 1; load() }
    const clearFilter = () => { filter.value = { role: '', keyword: '' }; resetAndLoad() }
    const goPage = (p) => {
      if (!p || p < 1 || p > totalPages.value) return
      pageNum.value = p
      load()
    }

    const changeRole = async (u, newRole) => {
      if (newRole === u.role) return
      if (!confirm(`确认将用户「${u.username}」的角色从 ${roleText(u.role)} 改为 ${roleText(newRole)} ?`)) {
        // 用户取消,还原 select 显示
        await load()
        return
      }
      try {
        await adminUpdateUserRole(u.userId, newRole)
        u.role = newRole
        alert('角色已更新')
      } catch (e) {
        alert('修改失败: ' + (e.message || ''))
        await load()
      }
    }

    const roleText = (r) => ({ TENANT: '租客', LANDLORD: '房东', ADMIN: '管理员' }[r] || r)
    const formatDate = (s) => s ? new Date(s).toLocaleString('zh-CN', { hour12: false }) : ''

    onMounted(load)
    return { currentUserId, users, total, pageNum, pageSize, totalPages, loading, filter, jumpPage,
             resetAndLoad, clearFilter, goPage, changeRole, roleText, formatDate }
  }
}
</script>

<style scoped>
.admin-users { padding: 20px 0; }
.page-header { margin-bottom: 18px; }
.page-header h1 { font-size: 24px; margin: 0 0 4px; }
.page-header p { color: #999; margin: 0; font-size: 13px; }

.filter-bar {
  display: flex;
  gap: 20px;
  align-items: center;
  padding: 14px 18px;
  margin-bottom: 14px;
}
.filter-group { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.filter-group label { color: #666; }
.filter-group select, .filter-group input {
  height: 32px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13px;
}
.filter-group input { min-width: 200px; }

.user-table {
  overflow-x: auto;
  margin-bottom: 18px;
}
.user-table table { width: 100%; border-collapse: collapse; font-size: 13px; }
.user-table th, .user-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}
.user-table th { background: #fafafa; color: #666; font-weight: 500; }
.user-table tr:hover { background: #fafafa; }

.text-mono { font-family: 'Consolas', monospace; }
.text-secondary { color: #999; }
.text-center { text-align: center; }

.self-tag {
  display: inline-block;
  background: #e6f7ff;
  color: #1890ff;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  margin-left: 6px;
}

.role-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.role-tenant { background: #fff7e6; color: #fa8c16; }
.role-landlord { background: #f6ffed; color: #52c41a; }
.role-admin { background: #f9f0ff; color: #722ed1; }

.role-select {
  height: 30px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  background: #fff;
  cursor: pointer;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}
.page-info { font-size: 13px; color: #666; min-width: 80px; text-align: center; }
.page-jump { font-size: 13px; color: #999; display: flex; align-items: center; gap: 6px; margin-left: 20px; }
</style>
