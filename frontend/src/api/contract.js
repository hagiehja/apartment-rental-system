import request from './request'

// 获取合同详情
export function getContractDetail(contractId) {
    return request({
        url: `/api/contract/${contractId}`,
        method: 'get'
    })
}

// 根据订单ID获取合同
export function getContractByOrderId(orderId) {
    return request({
        url: `/api/contract/order/${orderId}`,
        method: 'get'
    })
}

// 签署合同
export function signContract(contractId, data) {
    return request({
        url: `/api/contract/sign/${contractId}`,
        method: 'post',
        data
    })
}

// 获取合同列表
export function getContractList(params) {
    return request({
        url: '/api/contract/list',
        method: 'get',
        params
    })
}

// 下载合同
export function downloadContract(contractId) {
    return `http://localhost:8080/api/contract/download/${contractId}`
}

// 申请退租
export function terminateContract(contractId, data) {
    return request({
        url: `/api/contract/${contractId}/terminate`,
        method: 'post',
        data
    })
}

// 根据订单号退款（调用payment服务）
export function refundByOrderNo(orderNo) {
    return request({
        url: `/api/payment/refund/order/${orderNo}`,
        method: 'post'
    })
}

// 计算退款金额
export function calculateRefund(contractId) {
    return request({
        url: `/api/contract/${contractId}/calculate-refund`,
        method: 'get'
    })
}
