import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Tauri dev server + stable HMR host inside the webview.
const host = process.env.TAURI_DEV_HOST

export default defineConfig({
  plugins: [react()],
  clearScreen: false,
  server: {
    port: 5174,
    strictPort: true,
    host: host || false,
    hmr: host ? { protocol: 'ws', host, port: 5180 } : undefined,
  },
  envPrefix: ['VITE_'],
  build: {
    outDir: 'dist',
  },
})
