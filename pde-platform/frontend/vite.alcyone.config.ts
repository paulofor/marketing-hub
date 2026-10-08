import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

/** Empacota somente a superfície visual privada de Alcyone. */
export default defineConfig({
  base: "/",
  publicDir: "public-alcyone",
  plugins: [react()],
  build: {
    outDir: "dist-alcyone",
    rollupOptions: {
      input: {
        alcyone: fileURLToPath(new URL("./alcyone.html", import.meta.url)),
        personalization: fileURLToPath(new URL("./personalization.html", import.meta.url)),
      },
    },
  },
});
