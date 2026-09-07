import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

/** Empacota Mira sem importar a entrada pública do Vega/Método MUSA. */
export default defineConfig({
  base: "/mira-private-assets/",
  publicDir: false,
  plugins: [react()],
  build: {
    outDir: "dist-mira",
    rollupOptions: {
      input: fileURLToPath(new URL("./mira.html", import.meta.url)),
    },
  },
});
