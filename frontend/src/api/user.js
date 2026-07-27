import request from './request'

// 用户登录
export function login(data) {
    return request({
        url: '/api/user/login',
        method: 'post',
        data
    })
}

// 用户注册
export function register(data) {
    return request({
        url: '/api/user/register',
        method: 'post',
        data
    })
}

// 批量查询用户信息
export function batchUserInfo(ids) {
    return request({
        url: '/api/user/batch',
        method: 'get',
        params: { ids: ids.join(',') }
    })
}

// 用户角色统计
export function getUserStats() {
    return request({
        url: '/api/user/stats',
        method: 'get'
    })
}

// 管理员: 分页查询用户列表
export function adminListUsers(params) {
    return request({
        url: '/api/user/admin/list',
        method: 'get',
        params
    })
}

// 管理员: 修改用户角色
export function adminUpdateUserRole(targetUserId, role) {
    return request({
        url: `/api/user/admin/${targetUserId}/role`,
        method: 'put',
        data: { role }
    })
}

// 获取当前用户信息（从localStorage）
export function getCurrentUser() {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo) : null
}

// 退出登录
export function logout() {
    localStorage.removeItem('userInfo')
}

// 检查是否登录
export function isLoggedIn() {
    return !!localStorage.getItem('userInfo')
}

// 检查用户角色
export function hasRole(role) {
    const user = getCurrentUser()
    return user && user.role === role
}
