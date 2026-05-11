import request from './request'

// 创建订单
export function createOrder(data) {
    return request({
        url: '/api/order',
        method: 'post',
        data
    })
}

// 获取订单详情
export function getOrderDetail(orderNo) {
    return request({
        url: `/api/order/${orderNo}`,
        method: 'get'
    })
}

// 获取我的订单列表
export function getMyOrders(params) {
    return request({
        url: '/api/order/my/list',
        method: 'get',
        params
    })
}

// 取消订单
export function cancelOrder(orderNo, cancelReason) {
    return request({
        url: `/api/order/${orderNo}/cancel`,
        method: 'put',
        data: { cancelReason }
    })
}

// 获取分期计划
export function getInstallmentPlan(orderNo) {
    return request({
        url: `/api/order/${orderNo}/installments`,
        method: 'get'
    })
}

// 房东获取租客订单列表
export function getLandlordOrders(params) {
    return request({
        url: '/api/order/landlord/orders',
        method: 'get',
        params
    })
}

