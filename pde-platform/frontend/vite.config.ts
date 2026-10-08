import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5176,
    allowedHosts: [
      "v5.clubemusa.com.br",
      "v6.clubemusa.com.br",
      "v7.clubemusa.com.br",
    ],
    proxy: {
      "/api/pde/vega/private/v1": {
        target:
          process.env.PDE_PRIVATE_TEST_BACKEND_URL || "http://localhost:8000",
        changeOrigin: true,
      },
      "/api": {
        target: "http://localhost:8096",
        changeOrigin: true,
      },
    },
  },
  preview: {
    allowedHosts: [
      "v5.clubemusa.com.br",
      "v6.clubemusa.com.br",
      "v7.clubemusa.com.br",
    ],
  },
});
