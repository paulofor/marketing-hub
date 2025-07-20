import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const debug = env.REACT_APP_DEBUG_PROD === "true";
  const isVitest = process.env.VITEST;
  const { default: checker } = await import("vite-plugin-checker");
  const plugins = [react()];
  if (debug && !isVitest) plugins.push(checker({ typescript: true }));
  return {
    plugins,
    resolve: { dedupe: ["react", "react-dom"] },
    server: {
      port: 5173,
      hmr: false,
      proxy: {
        // Backend now listens on port 8000
        "/api": "http://191.252.92.222:8000",
      },
    },
    build: {
      minify: debug ? false : "esbuild",
      sourcemap: debug,
    },
    test: {
      environment: "jsdom",
    },
  };
});
