import request from './request'

// 获取房源列表
export function getHouseList(params) {
    return request({
        url: '/api/house/list',
        method: 'get',
        params
    })
}

// 获取房源详情
export function getHouseDetail(id) {
    return request({
        url: `/api/house/${id}`,
        method: 'get'
    })
}

// 发布房源
export function publishHouse(data) {
    return request({
        url: '/api/house',
        method: 'post',
        data
    })
}

// 更新房源
export function updateHouse(id, data) {
    return request({
        url: `/api/house/${id}`,
        method: 'put',
        data
    })
}

// 删除房源
export function deleteHouse(id) {
    return request({
        url: `/api/house/${id}`,
        method: 'delete'
    })
}

// 下架房源
export function offlineHouse(id) {
    return request({
        url: `/api/house/${id}/offline`,
        method: 'put'
    })
}

// 上架房源
export function onlineHouse(id) {
    return request({
        url: `/api/house/${id}/online`,
        method: 'put'
    })
}

// ============================================================
// 推荐系统 API (TensorFlow FM)
// ============================================================

// 获取个性化推荐
export function getRecommendations(params) {
    return request({
        url: '/api/house/recommend',
        method: 'get',
        params
    })
}

// 上报用户行为 (VIEW/CLICK/FAVORITE/ORDER/PAY)
export function trackBehavior(data) {
    return request({
        url: '/api/house/behavior/track',
        method: 'post',
        data
    })
}

// 保存/更新用户偏好
export function savePreference(data) {
    return request({
        url: '/api/house/preference',
        method: 'post',
        data
    })
}

// 查询用户偏好
export function getPreference() {
    return request({
        url: '/api/house/preference',
        method: 'get'
    })
}

// 获取 FM 模型训练信息 (TensorFlow 元数据)
export function getRecommendModelInfo() {
    return request({
        url: '/api/house/recommend/model-info',
        method: 'get'
    })
}
