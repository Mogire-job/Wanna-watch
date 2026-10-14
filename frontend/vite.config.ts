import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development, /api calls go to the Spring Boot backend.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
})
