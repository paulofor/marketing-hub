import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react({ fastRefresh: false })],
  server: {
    port: 5173,
    hmr: false,
    proxy: {
      // Backend now listens on port 8000
      "/api": "http://191.252.92.222:8000",
    },
  },
  test: {
    environment: "jsdom",
  },
});
