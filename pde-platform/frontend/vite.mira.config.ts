import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

type LocalServer = {
  middlewares: {
    use(
      handler: (
        request: { url?: string },
        response: { statusCode: number; end(body?: string): void },
        next: () => void,
      ) => void,
    ): void;
  };
};

/** Replica no servidor local as rotas exatas e isoladas do Nginx de Mira. */
function registerMiraRoutes(server: LocalServer) {
  server.middlewares.use((request, response, next) => {
    const originalUrl = request.url || "";
    const queryStart = originalUrl.indexOf("?");
    const path =
      queryStart >= 0 ? originalUrl.slice(0, queryStart) : originalUrl;
    const query = queryStart >= 0 ? originalUrl.slice(queryStart) : "";
    if (path === "/mira-private") {
      request.url = `/mira.html${query}`;
    } else if (path.startsWith("/mira-private/")) {
      response.statusCode = 404;
      response.end("Not Found");
      return;
    } else if (path.startsWith("/mira-private-assets/")) {
      request.url = `${path.slice("/mira-private-assets".length)}${query}`;
    }
    next();
  });
}

/** Mantém no servidor local a mesma rota privada entregue pelo Nginx da imagem. */
function miraPrivateLocalRoute() {
  return {
    name: "mira-private-local-route",
    apply: "serve" as const,
    configureServer(server: LocalServer) {
      registerMiraRoutes(server);
    },
    configurePreviewServer(server: LocalServer) {
      registerMiraRoutes(server);
    },
  };
}

/** Empacota Mira sem importar a entrada pública do Vega/Método MUSA. */
export default defineConfig(({ command }) => ({
  base: command === "serve" ? "/" : "/mira-private-assets/",
  publicDir: false,
  plugins: [miraPrivateLocalRoute(), react()],
  build: {
    outDir: "dist-mira",
    rollupOptions: {
      input: fileURLToPath(new URL("./mira.html", import.meta.url)),
    },
  },
}));
