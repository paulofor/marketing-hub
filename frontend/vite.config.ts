import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { Plugin } from "vite";

const debug = process.env.REACT_APP_DEBUG_PROD === "true";
const localTestBackend = process.env.MH_LOCAL_TEST_BACKEND;
if (
  localTestBackend &&
  !["127.0.0.1", "localhost"].includes(new URL(localTestBackend).hostname)
) {
  throw new Error("MH_LOCAL_TEST_BACKEND aceita somente backend local da sandbox.");
}

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
        "/api/tiktok": "http://127.0.0.1:8097",
        // Codex/dev access should use the backend through port 80; port 8000 can be unavailable behind the proxy.
        "/api": localTestBackend || "http://191.252.181.168",
      },
    },
    build: {
      minify: debug ? false : "esbuild",
      sourcemap: true,
    },
    test: {
      environment: "jsdom",
      setupFiles: "./src/test-setup.ts",
    },
  };
});
