import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Backend now listens on port 8000
      "/api": "http://191.252.92.222:8000",
    },
  },
  test: {
    environment: "jsdom",
  },
});
