import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { io } from "socket.io-client";
import App from "./App";
import ErrorBoundary from "./app/ErrorBoundary";
import axios from "axios";

const defaultBaseURL = `${window.location.protocol}//${window.location.hostname}:8000`;
axios.defaults.baseURL = import.meta.env.VITE_API_URL || defaultBaseURL;

const queryClient = new QueryClient();
const socket = io({ path: "/ws" });
socket.on("asset.updated", () => {
  queryClient.invalidateQueries({ queryKey: ["assets"] });
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ErrorBoundary>
          <App />
        </ErrorBoundary>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
