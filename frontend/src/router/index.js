import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页' }
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/houses',
        name: 'HouseList',
        component: () => import('../views/house/HouseList.vue'),
        meta: { title: '房源列表' }
    },
    {
        path: '/house/:id',
        name: 'HouseDetail',
        component: () => import('../views/house/HouseDetail.vue'),
        meta: { title: '房源详情' }
    },
    {
        path: '/house/publish',
        name: 'HousePublish',
        component: () => import('../views/house/HousePublish.vue'),
        meta: { title: '发布房源', requiresAuth: true, roles: ['LANDLORD'] }
    },
    {
        path: '/my-houses',
        name: 'MyHouses',
        component: () => import('../views/house/MyHouses.vue'),
        meta: { title: '我的房源', requiresAuth: true, roles: ['LANDLORD'] }
    },
    {
        path: '/orders',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { title: '我的订单', requiresAuth: true }
    },
    {
        path: '/landlord/orders',
        name: 'LandlordOrders',
        component: () => import('../views/order/LandlordOrders.vue'),
        meta: { title: '租客订单', requiresAuth: true, roles: ['LANDLORD'] }
    },
    {
        path: '/order/create/:houseId',
        name: 'OrderCreate',
        component: () => import('../views/order/OrderCreate.vue'),
        meta: { title: '创建订单', requiresAuth: true }
    },
    {
        path: '/order/:orderNo',
        name: 'OrderDetail',
        component: () => import('../views/order/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
    },
    {
        path: '/payment/:orderNo',
        name: 'Payment',
        component: () => import('../views/payment/Payment.vue'),
        meta: { title: '支付', requiresAuth: true }
    },
    {
        path: '/wallet',
        name: 'Wallet',
        component: () => import('../views/payment/Wallet.vue'),
        meta: { title: '我的钱包', requiresAuth: true }
    },
    {
        path: '/contracts',
        name: 'ContractList',
        component: () => import('../views/contract/ContractList.vue'),
        meta: { title: '我的合同', requiresAuth: true }
    },
    {
        path: '/contract/:id',
        name: 'ContractDetail',
        component: () => import('../views/contract/ContractDetail.vue'),
        meta: { title: '合同详情', requiresAuth: true }
    },
    {
        path: '/notifications',
        name: 'Notifications',
        component: () => import('../views/notification/Notifications.vue'),
        meta: { title: '消息通知', requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    // 设置页面标题
    document.title = to.meta.title ? `${to.meta.title} - 公寓租赁系统` : '公寓租赁系统'

    // 检查是否需要登录
    if (to.meta.requiresAuth) {
        const userInfo = localStorage.getItem('userInfo')
        if (!userInfo) {
            next({ name: 'Login', query: { redirect: to.fullPath } })
            return
        }

        // 检查角色权限
        if (to.meta.roles) {
            const user = JSON.parse(userInfo)
            if (!to.meta.roles.includes(user.role)) {
                alert('您没有权限访问此页面')
                next({ name: 'Home' })
                return
            }
        }
    }

    next()
})

export default router
