import request from './request'
import { getCurrentUser } from './user'

// 获取未读消息数量
export function getUnreadCount() {
    const user = getCurrentUser()
    if (!user) return Promise.resolve({ data: 0 })
    return request({
        url: '/api/notification/unread/count',
        method: 'get',
        params: { userId: user.userId }
    })
}

// 获取未读消息列表
export function getUnreadMessages() {
    const user = getCurrentUser()
    if (!user) return Promise.resolve({ data: [] })
    return request({
        url: '/api/notification/unread',
        method: 'get',
        params: { userId: user.userId }
    })
}

// 分页获取消息列表
export function getMessageList(params) {
    const user = getCurrentUser()
    if (!user) return Promise.resolve({ data: { records: [], total: 0 } })
    return request({
        url: '/api/notification/list',
        method: 'get',
        params: { ...params, userId: user.userId }
    })
}

// 获取消息详情
export function getMessageDetail(messageId) {
    const user = getCurrentUser()
    if (!user) return Promise.reject(new Error('未登录'))
    return request({
        url: `/api/notification/${messageId}`,
        method: 'get',
        params: { userId: user.userId }
    })
}

// 标记消息已读
export function markAsRead(messageId) {
    const user = getCurrentUser()
    if (!user) return Promise.reject(new Error('未登录'))
    return request({
        url: `/api/notification/read/${messageId}`,
        method: 'post',
        params: { userId: user.userId }
    })
}

// 标记所有消息已读
export function markAllAsRead() {
    const user = getCurrentUser()
    if (!user) return Promise.reject(new Error('未登录'))
    return request({
        url: '/api/notification/read/all',
        method: 'post',
        params: { userId: user.userId }
    })
}
