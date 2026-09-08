import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Configuração exclusiva de homologação: todas as APIs ficam na aplicação local de testes.
export default defineConfig({
  plugins: [react()],
  preview: {
    host: "127.0.0.1",
    port: 15173,
    strictPort: true,
    proxy: { "/api": "http://127.0.0.1:18091" },
  },
  server: {
    host: "127.0.0.1",
    port: 15173,
    strictPort: true,
    hmr: false,
    proxy: { "/api": "http://127.0.0.1:18091" },
  },
});
