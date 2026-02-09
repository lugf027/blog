import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 80,
    host: '0.0.0.0', // 允许任意IP访问
    allowedHosts: [
      'luguangfeng.com',
      '.luguangfeng.com', // 允许所有子域名
      'localhost'
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  preview: {
    port: 80,
    host: '0.0.0.0' // 预览模式也允许任意IP访问
  }
})
