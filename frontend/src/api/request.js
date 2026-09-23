import axios from 'axios'

// 创建axios实例
const request = axios.create({
    baseURL: '', // 开发模式使用Vite代理，生产环境需要配置实际地址
    timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        // 从localStorage获取用户信息
        const userInfo = localStorage.getItem('userInfo')
        if (userInfo) {
            try {
                const user = JSON.parse(userInfo)
                // 添加用户ID到请求头
                if (user.userId) {
                    config.headers['X-User-Id'] = user.userId
                }
                // 添加JWT Token
                if (user.token) {
                    config.headers['Authorization'] = `Bearer ${user.token}`
                }
            } catch (error) {
                console.error('解析用户信息失败:', error)
            }
        }
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 全局错误通知（非阻塞 toast）
let toastTimer = null
function showErrorToast(message) {
    // 避免重复 toast 堆积
    let toast = document.getElementById('global-error-toast')
    if (!toast) {
        toast = document.createElement('div')
        toast.id = 'global-error-toast'
        toast.style.cssText = [
            'position:fixed', 'top:80px', 'left:50%', 'transform:translateX(-50%)',
            'background:rgba(217,142,142,0.97)', 'color:#fff',
            'padding:12px 24px', 'border-radius:8px',
            'box-shadow:0 8px 24px rgba(0,0,0,0.2)',
            'font-size:14px', 'font-weight:500', 'z-index:9999',
            'max-width:80vw', 'text-align:center',
            'opacity:0', 'transition:opacity 0.3s', 'pointer-events:none'
        ].join(';')
        document.body.appendChild(toast)
    }
    toast.textContent = message
    toast.style.opacity = '1'
    if (toastTimer) clearTimeout(toastTimer)
    toastTimer = setTimeout(() => { toast.style.opacity = '0' }, 3500)
}

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
            showErrorToast('登录已过期，请重新登录')
            setTimeout(() => { window.location.href = '/login' }, 800)
            return Promise.reject(new Error(res.message || '请重新登录'))
        } else {
            showErrorToast(res.message || '请求失败')
            return Promise.reject(new Error(res.message || '请求失败'))
        }
    },
    error => {
        console.error('响应错误:', error)
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    localStorage.removeItem('userInfo')
                    showErrorToast('登录已过期，请重新登录')
                    setTimeout(() => { window.location.href = '/login' }, 800)
                    break
                case 403:
                    showErrorToast('没有权限访问')
                    break
                case 404:
                    showErrorToast('请求的资源不存在')
                    break
                case 500: {
                    // 优先使用后端返回的错误信息
                    const msg = error.response.data && error.response.data.message
                        ? error.response.data.message
                        : '服务器错误'
                    showErrorToast(msg)
                    break
                }
            }
        } else if (error.code === 'ECONNABORTED') {
            showErrorToast('请求超时，请稍后重试')
        } else {
            showErrorToast('网络连接失败，请检查后端服务')
        }
        return Promise.reject(error)
    }
)

export default request
