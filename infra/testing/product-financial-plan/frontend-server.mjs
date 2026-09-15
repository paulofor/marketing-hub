import { createRequire } from "node:module";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { preview } = require("vite");
const server = await preview({
  configFile: false,
  root: "frontend",
  preview: {
    host: "127.0.0.1",
    port: 15175,
    strictPort: true,
    proxy: { "/api": "http://127.0.0.1:18095" },
  },
});
const stop = () => server.httpServer.close(() => process.exit(0));
process.on("SIGTERM", stop);
process.on("SIGINT", stop);
