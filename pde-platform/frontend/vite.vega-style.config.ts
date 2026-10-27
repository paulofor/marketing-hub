import { defineConfig, mergeConfig } from "vite";
import base from "./vite.config";

/** Usa o mesmo aplicativo com a fronteira contextual local e preserva a origem do navegador. */
export default mergeConfig(
  base,
  defineConfig({
    server: {
      port: 57188,
      proxy: {
        "/api": {
          target:
            process.env.VEGA_STYLE_TEST_BACKEND || "http://127.0.0.1:18086",
          changeOrigin: false,
        },
      },
    },
  }),
);
