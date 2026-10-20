import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'https://207-180-227-65.sslip.io',
        changeOrigin: true,
        secure: true,
      },
    },
  },
})