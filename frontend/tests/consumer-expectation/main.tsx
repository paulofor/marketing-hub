import React from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import ConsumerExpectationPanel from "../../src/pages/audioVideoStudio/ConsumerExpectationPanel";
createRoot(document.getElementById("root")!).render(
  <QueryClientProvider
    client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
  >
    <BrowserRouter>
      <main className="container p-3">
        <ConsumerExpectationPanel />
      </main>
    </BrowserRouter>
  </QueryClientProvider>,
);
