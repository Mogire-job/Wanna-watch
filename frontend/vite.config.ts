import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_')
  return {
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8081',
      '/oauth2': env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8081',
      '/login': env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8081',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
  }
})
