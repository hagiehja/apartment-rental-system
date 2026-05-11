import request from './request'

// 用户登录
export function login(data) {
    return request({
        url: '/api/user/login',
        method: 'post',
        data
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
