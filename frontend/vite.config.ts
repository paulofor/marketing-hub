import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { Plugin } from "vite";

const debug = process.env.REACT_APP_DEBUG_PROD === "true";

export default defineConfig(async () => {
  const { default: checker } = await import("vite-plugin-checker");
  return {
    plugins: [react({ fastRefresh: debug }), checker({ typescript: true })],
    resolve: {
      dedupe: ["react", "react-dom"],
    },
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
      sourcemap: true,
    },
    test: {
      environment: "jsdom",
    },
  };
});
