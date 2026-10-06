import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

type LocalServer = {
  middlewares: {
    use(
      handler: (
        request: { url?: string },
        response: unknown,
        next: () => void,
      ) => void,
    ): void;
  };
};

/** Replica as duas entradas HTML independentes, tanto em desenvolvimento quanto no preview. */
function configureRoutes(server: LocalServer) {
  server.middlewares.use((request, _response, next) => {
    const original = request.url || "";
    const [path, query = ""] = original.split("?", 2);
    if (
      ["/", "/access", "/terms", "/privacy", "/refund-policy"].includes(path)
    ) {
      request.url = `/mira-commercial.html${query ? `?${query}` : ""}`;
    } else if (path === "/mira-candidate") {
      request.url = `/mira-candidate.html${query ? `?${query}` : ""}`;
    }
    next();
  });
}

/** Mantém a candidata separada do grafo carregado pela página comercial. */
function miraCommercialRoutes() {
  return {
    name: "mira-commercial-local-routes",
    apply: "serve" as const,
    configureServer: configureRoutes,
    configurePreviewServer: configureRoutes,
  };
}

/** Empacota a superfície comercial de Mira sem importar outras experiências PDE. */
export default defineConfig(() => ({
  base: "/",
  publicDir: "public-mira-commercial",
  plugins: [miraCommercialRoutes(), react()],
  preview: {
    proxy: { "/api/pde/mira/candidate/v1": "http://127.0.0.1:57182" },
  },
  server: { proxy: { "/api/pde/mira/candidate/v1": "http://127.0.0.1:57182" } },
  build: {
    outDir: "dist-mira-commercial",
    rollupOptions: {
      input: {
        commercial: fileURLToPath(
          new URL("./mira-commercial.html", import.meta.url),
        ),
        candidate: fileURLToPath(
          new URL("./mira-candidate.html", import.meta.url),
        ),
      },
    },
  },
}));
