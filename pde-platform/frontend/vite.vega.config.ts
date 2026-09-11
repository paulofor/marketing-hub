import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { resolve } from "node:path";
export default defineConfig({
  root: resolve(__dirname, "vega-private"),
  base: "/vega-private/",
  plugins: [react()],
  build: { outDir: resolve(__dirname, "dist-vega"), emptyOutDir: true },
  server: {
    host: "0.0.0.0",
    proxy: {
      "/api": {
        target: process.env.VEGA_BACKEND_URL || "http://127.0.0.1:18080",
        changeOrigin: true,
      },
    },
  },
});
