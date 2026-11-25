import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Servidor local sem proxy de produção; TypeScript é validado separadamente no aceite.
export default defineConfig({
  plugins: [react()],
  server: { host: "127.0.0.1", port: 5199, strictPort: true },
});
