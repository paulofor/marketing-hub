import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [
    react(),
    {
      name: "pde-material-access-local",
      configureServer(server) {
        // Mantém no ambiente local a mesma autorização aplicada pelo Nginx publicado.
        server.middlewares.use(async (request, response, next) => {
          if (!request.url?.startsWith("/materials/")) return next();
          try {
            const auth = await fetch(
              `${process.env.PDE_TEST_BACKEND_URL || "http://localhost:8096"}/api/pde/access/materials/authorize`,
              {
                headers: {
                  "X-PDE-Access-Token": String(
                    request.headers["x-pde-access-token"] || "",
                  ),
                },
              },
            );
            if (!auth.ok) {
              response.statusCode = 403;
              response.end("Acesso completo necessário");
              return;
            }
            next();
          } catch (error) {
            server.config.logger.error(
              `Autorização local de materiais indisponível: ${error}`,
            );
            response.statusCode = 503;
            response.end("Indisponível");
          }
        });
      },
    },
  ],
  server: {
    port: 5176,
    allowedHosts: [
      "v5.clubemusa.com.br",
      "v6.clubemusa.com.br",
      "v7.clubemusa.com.br",
    ],
    proxy: {
      "/api": {
        target: process.env.PDE_TEST_BACKEND_URL || "http://localhost:8096",
        changeOrigin: true,
      },
    },
  },
  preview: {
    allowedHosts: [
      "v5.clubemusa.com.br",
      "v6.clubemusa.com.br",
      "v7.clubemusa.com.br",
    ],
  },
});
