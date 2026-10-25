import { defineConfig } from '../../../frontend/node_modules/vite/dist/node/index.js';
import react from '../../../frontend/node_modules/@vitejs/plugin-react/dist/index.mjs';
import { fileURLToPath } from 'node:url';
export default defineConfig({
  root: fileURLToPath(new URL('../../../frontend', import.meta.url)),
  plugins: [react()],
  server: { host: '127.0.0.1', port: 5179, strictPort: true,
    proxy: { '/api': 'http://127.0.0.1:18101', '/fixture': 'http://127.0.0.1:18101' } },
});
