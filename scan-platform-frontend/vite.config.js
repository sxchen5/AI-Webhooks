import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

/** 开发时代理到 Spring Boot，避免跨域 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
