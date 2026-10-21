import { defineConfig, mergeConfig } from "vite";
import base from "./vite.alcyone.config";
/** Encaminha somente para o backend local de homologação; não é usado no build de produção. */
export default mergeConfig(
  base,
  defineConfig({
    server: {
      host: "127.0.0.1",
      port: 5189,
      proxy: { "/api/pde": "http://127.0.0.1:8189" },
    },
  }),
);
