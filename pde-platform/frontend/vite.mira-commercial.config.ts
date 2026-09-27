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

/** Replica localmente as entradas SPA que o Nginx comercial entrega pelo mesmo HTML. */
function miraCommercialRoutes() {
  return {
    name: "mira-commercial-local-routes",
    apply: "serve" as const,
    configureServer(server: LocalServer) {
      server.middlewares.use((request, _response, next) => {
        const original = request.url || "";
        const [path, query = ""] = original.split("?", 2);
        if (
          ["/", "/access", "/terms", "/privacy", "/refund-policy"].includes(
            path,
          )
        ) {
          request.url = `/mira-commercial.html${query ? `?${query}` : ""}`;
        }
        next();
      });
    },
  };
}

/** Empacota a superfície comercial de Mira sem importar outras experiências PDE. */
export default defineConfig(() => ({
  base: "/",
  publicDir: "public-mira-commercial",
  plugins: [miraCommercialRoutes(), react()],
  build: {
    outDir: "dist-mira-commercial",
    rollupOptions: {
      input: fileURLToPath(new URL("./mira-commercial.html", import.meta.url)),
    },
  },
}));
