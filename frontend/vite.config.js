import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true,
    // 配置代理，解决跨域问题
    // Gateway 端口是 80，所有接口都以 /api/ 开头
    // /img/ 是图片代理服务 (image-proxy)
    proxy: {
      '/api': {
        target: 'http://192.168.24.129',
        changeOrigin: true
      },
      '/img': {
        target: 'http://192.168.24.129',
        changeOrigin: true
      }
    }
  }
})
