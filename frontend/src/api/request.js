import axios from 'axios'

// 创建axios实例
const request = axios.create({
    baseURL: '', // 开发模式使用Vite代理，生产环境需要配置实际地址
    timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        // 从localStorage获取用户信息
        const userInfo = localStorage.getItem('userInfo')
        console.log('🔍 从localStorage获取用户信息:', userInfo)
        if (userInfo) {
            try {
                const user = JSON.parse(userInfo)
                console.log('🔍 解析用户信息:', user)
                // 添加用户ID到请求头
                if (user.userId) {
                    config.headers['X-User-Id'] = user.userId
                    console.log('🔍 添加X-User-Id到请求头:', user.userId)
                } else {
                    console.error('❌ 用户信息中没有userId:', user)
                }
                // 添加JWT Token
                if (user.token) {
                    config.headers['Authorization'] = `Bearer ${user.token}`
                }
            } catch (error) {
                console.error('❌ 解析用户信息失败:', error)
            }
        } else {
            console.error('❌ localStorage中没有userInfo')
        }
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        // 统一处理响应
        if (res.code === 200) {
            return res
        } else if (res.code === 401) {
            // 未登录或token过期
            localStorage.removeItem('userInfo')
            window.location.href = '/login'
            return Promise.reject(new Error(res.message || '请重新登录'))
        } else {
            return Promise.reject(new Error(res.message || '请求失败'))
        }
    },
    error => {
        console.error('响应错误:', error)
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    localStorage.removeItem('userInfo')
                    window.location.href = '/login'
                    break
                case 403:
                    alert('没有权限访问')
                    break
                case 404:
                    alert('请求的资源不存在')
                    break
                case 500:
                    // 优先使用后端返回的错误信息
                    const msg = error.response.data && error.response.data.message
                        ? error.response.data.message
                        : '服务器错误'
                    alert(msg)
                    break
            }
        } else {
            alert('网络连接失败，请检查后端服务是否启动')
        }
        return Promise.reject(error)
    }
)

export default request
