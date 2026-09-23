/**
 * 钱包状态管理 (Pinia Store)
 * 核心职责：
 * 1. 管理账户余额、交易记录等全局状态
 * 2. 提供实时余额更新方法
 * 3. 支持跨页面的钱包数据同步
 * 4. 处理资金守恒验证和事务跟踪
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAccountBalance, getAccountTransactions } from '../api/payment'

export const useWalletStore = defineStore('wallet', () => {
  // 状态定义
  const account = ref({
    balance: 0,
    frozenAmount: 0,
    totalIncome: 0,
    totalExpense: 0,
    userId: null,
    userType: null
  })

  const transactions = ref([])
  const lastUpdateTime = ref(null)
  const isLoading = ref(false)

  // 模拟余额变化监听器（用于实时通知）
  const balanceChangeListeners = ref([])

  /**
   * 计算属性：总资产（守恒验证使用）
   */
  const totalAssets = computed(() => {
    return (account.value.balance || 0) + (account.value.frozenAmount || 0)
  })

  /**
   * 计算属性：显示的可用余额
   */
  const displayBalance = computed(() => {
    return account.value.balance || 0
  })

  /**
   * 加载完整钱包数据
   */
  const loadWalletData = async () => {
    isLoading.value = true
    try {
      const [balanceRes, transRes] = await Promise.all([
        getAccountBalance(),
        getAccountTransactions()
      ])

      account.value = {
        ...account.value,
        ...balanceRes.data
      }
      transactions.value = transRes.data || []
      lastUpdateTime.value = new Date().getTime()

      // 触发余额变化监听
      notifyBalanceChange('initial_load')

      return true
    } catch (error) {
      console.error('加载钱包数据失败:', error)
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 实时刷新余额（关键方法 - 用于支付/退款后）
   * @param {string} reason 刷新原因：'payment'(支付), 'refund'(退款), 'cancel'(取消), 'income'(收入)
   */
  const refreshBalance = async (reason = 'manual') => {
    try {
      const balanceRes = await getAccountBalance()

      // 记录旧余额，用于变动检测
      const oldBalance = displayBalance.value

      // 更新状态
      account.value = {
        ...account.value,
        ...balanceRes.data
      }

      const newBalance = displayBalance.value
      const changeAmount = newBalance - oldBalance

      // 如有余额变动，触发监听
      if (changeAmount !== 0) {
        notifyBalanceChange(reason, {
          oldBalance,
          newBalance,
          changeAmount,
          timestamp: new Date().getTime()
        })
      }

      lastUpdateTime.value = new Date().getTime()
      return true
    } catch (error) {
      console.error('刷新余额失败:', error)
      return false
    }
  }

  /**
   * 刷新交易记录
   */
  const refreshTransactions = async () => {
    try {
      const transRes = await getAccountTransactions()
      transactions.value = transRes.data || []
      return true
    } catch (error) {
      console.error('刷新交易记录失败:', error)
      return false
    }
  }

  /**
   * 完整刷新（支付/退款后必须调用）
   */
  const fullRefresh = async (reason = 'manual') => {
    return await Promise.all([
      refreshBalance(reason),
      refreshTransactions()
    ])
  }

  /**
   * 注册余额变化监听器
   * @param {Function} callback 回调函数 (reason, details) => {}
   * @returns {Function} 取消监听函数
   */
  const onBalanceChange = (callback) => {
    balanceChangeListeners.value.push(callback)

    // 返回取消监听函数
    return () => {
      const index = balanceChangeListeners.value.indexOf(callback)
      if (index > -1) {
        balanceChangeListeners.value.splice(index, 1)
      }
    }
  }

  /**
   * 触发余额变化通知（内部方法）
   */
  const notifyBalanceChange = (reason, details = {}) => {
    balanceChangeListeners.value.forEach(callback => {
      try {
        callback(reason, details)
      } catch (error) {
        console.error('余额变化监听器执行出错:', error)
      }
    })
  }

  /**
   * 资金守恒验证辅助函数
   * 用于前端验证操作的一致性
   * @param {number} amount 交易金额
   * @param {string} type 操作类型：'payment'(支付) 或 'refund'(退款)
   */
  const validateFundsConservation = (amount, type = 'payment') => {
    const validation = {
      isValid: false,
      message: '',
      details: {}
    }

    if (type === 'payment') {
      // 支付场景：检查余额是否充足
      if (displayBalance.value >= amount) {
        validation.isValid = true
        validation.message = '余额充足，可进行支付'
        validation.details = {
          beforeBalance: displayBalance.value,
          paymentAmount: amount,
          afterBalance: displayBalance.value - amount
        }
      } else {
        validation.message = `余额不足，还需 ¥${(amount - displayBalance.value).toFixed(2)}`
        validation.details = {
          currentBalance: displayBalance.value,
          requiredAmount: amount,
          shortage: amount - displayBalance.value
        }
      }
    } else if (type === 'refund') {
      // 退款场景：检查房东余额（这里只能检查当前用户的能力）
      validation.isValid = true
      validation.message = '退款将按原子性执行'
      validation.details = {
        refundAmount: amount,
        willReceive: displayBalance.value + amount
      }
    }

    return validation
  }

  /**
   * 清空钱包数据（退出登录时调用）
   */
  const clearWallet = () => {
    account.value = {
      balance: 0,
      frozenAmount: 0,
      totalIncome: 0,
      totalExpense: 0,
      userId: null,
      userType: null
    }
    transactions.value = []
    lastUpdateTime.value = null
    balanceChangeListeners.value = []
  }

  return {
    // 状态
    account,
    transactions,
    lastUpdateTime,
    isLoading,
    totalAssets,
    displayBalance,

    // 方法
    loadWalletData,
    refreshBalance,
    refreshTransactions,
    fullRefresh,
    onBalanceChange,
    validateFundsConservation,
    clearWallet
  }
})
