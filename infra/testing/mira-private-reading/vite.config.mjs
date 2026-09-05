import { defineConfig } from "../../../frontend/node_modules/vite/dist/node/index.js";
import react from "../../../frontend/node_modules/@vitejs/plugin-react/dist/index.mjs";

export default defineConfig({
  root: new URL("../../../frontend", import.meta.url).pathname,
  plugins: [react()],
  server: { host: "127.0.0.1", port: 15173, strictPort: true, proxy: { "/api": "http://127.0.0.1:18090" } },
});
