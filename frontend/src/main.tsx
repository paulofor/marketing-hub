import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { io } from "socket.io-client";
import App from "./App";
import axios from "axios";
import { apiBaseUrl } from "./config/api";
import "./api/http";
import { ensureVisitorIdCookie } from "./utils/visitorCookie";
import { ErrorBoundary } from "./app/ErrorBoundary";

axios.defaults.baseURL = apiBaseUrl;
ensureVisitorIdCookie();

const queryClient = new QueryClient();
const socket = io({ path: "/ws" });
socket.on("asset.updated", () => {
  queryClient.invalidateQueries({ queryKey: ["assets"] });
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ErrorBoundary
          fallback={
            <div className="container-fluid py-4">
              <div className="alert alert-danger" role="alert">
                Não foi possível carregar esta tela. Recarregue a página ou
                verifique o console para identificar o erro de renderização.
              </div>
            </div>
          }
        >
          <App />
        </ErrorBoundary>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
