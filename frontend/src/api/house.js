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
