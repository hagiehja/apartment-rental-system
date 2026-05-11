import request from './request'

// 获取账户余额
export function getAccountBalance() {
    return request({
        url: '/api/payment/account/balance',
        method: 'get'
    })
}

// 获取交易记录
export function getAccountTransactions() {
    return request({
        url: '/api/payment/account/transactions',
        method: 'get'
    })
}

// 创建支付单
export function createPayment(data) {
    return request({
        url: '/api/payment',
        method: 'post',
        data
    })
}

// 执行支付
export function executePayment(paymentNo) {
    return request({
        url: `/api/payment/${paymentNo}/pay`,
        method: 'post'
    })
}

// 申请退款
export function refundPayment(paymentNo) {
    return request({
        url: `/api/payment/${paymentNo}/refund`,
        method: 'post'
    })
}

/**
 * ========== 合同取消与退款相关接口 ==========
 */

// 执行原子性退款（合同取消）
// 资金守恒：租客余额 + X，房东余额 - X，总额不变
export function executeAtomicRefund(contractId, data) {
    return request({
        url: `/api/payment/contract/${contractId}/atomic-refund`,
        method: 'post',
        data
    })
}

// 获取退款状态（检查是否已完成）
export function getRefundStatus(contractId) {
    return request({
        url: `/api/payment/contract/${contractId}/refund-status`,
        method: 'get'
    })
}

// 查询账户间的资金转移历史（用于验证资金守恒）
export function getMoneyTransferHistory(startTime, endTime) {
    return request({
        url: '/api/payment/transfer-history',
        method: 'get',
        params: { startTime, endTime }
    })
}

// 模拟充值（使用退款接口实现）
export function mockRecharge(amount) {
    return request({
        url: '/api/payment/account/recharge',
        method: 'post',
        data: { amount }
    })
}
