import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import "bootstrap/dist/css/bootstrap.min.css";
import "../src/App.css";
import ProductProcessActivityExecutionPanel from "../src/pages/product/ProductProcessActivityExecutionPanel";

function Sandbox() {
  const [activity, setActivity] = useState<any>();
  const [pending, setPending] = useState(false);
  const [outcome, setOutcome] = useState("");
  const code =
    new URLSearchParams(location.search).get("activity") || "privateReading1";
  useEffect(() => {
    void axios
      .get(`/api/local/mira/activities/${code}`)
      .then((response) => setActivity(response.data));
  }, [code]);
  return (
    <main className="container py-4" style={{ maxWidth: 920 }}>
      <h1>Mira — homologação local</h1>
      <p>Dados sintéticos; nenhum cliente, receita ou gasto.</p>
      {activity && (
        <ProductProcessActivityExecutionPanel
          activity={activity}
          productId={10}
          pending={pending}
          pendingActivityId={pending ? code : undefined}
          onExecute={async (command) => {
            setPending(true);
            setOutcome("");
            try {
              const response = await axios.post(
                `/api/business-processes/68/products/10/activities/${code}/execution-requests`,
                command.decision,
              );
              setOutcome(response.data.operationalState);
            } catch (error: any) {
              setOutcome(error.response?.data?.message || "Erro local");
            } finally {
              setPending(false);
            }
          }}
        />
      )}
      {outcome && <p data-testid="bpm-outcome">{outcome}</p>}
    </main>
  );
}
createRoot(document.getElementById("root")!).render(
  <QueryClientProvider client={new QueryClient()}>
    <MemoryRouter>
      <Sandbox />
    </MemoryRouter>
  </QueryClientProvider>,
);
