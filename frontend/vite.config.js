import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端 Spring Boot 默认监听 9000，且未配置 CORS。
// 开发期通过 Vite 代理转发 /api -> http://localhost:9000，规避跨域；
// 生产部署时若前后端分离，需要后端补 CORS 配置或由 Nginx 反向代理。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
