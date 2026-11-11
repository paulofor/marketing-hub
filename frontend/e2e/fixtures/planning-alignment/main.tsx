import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import CyclePlanningAlignment from "/src/pages/learningCycle/CyclePlanningAlignment.tsx";
import "/node_modules/bootstrap/dist/css/bootstrap.min.css";
const client = new QueryClient();
function App() {
  const [cycle, setCycle] = useState({
    id: 204,
    productId: 4,
    experimentId: 304,
    revision: 1,
    status: "OPEN",
    stage: "PLANNING",
  });
  return (
    <QueryClientProvider client={client}>
      <main className="container p-4">
        <h1>Planejamento privado — teste isolado</h1>
        <CyclePlanningAlignment
          key={cycle.revision}
          cycle={cycle}
          onUpdated={setCycle}
        />
        <p role="status">Revisão {cycle.revision}</p>
      </main>
    </QueryClientProvider>
  );
}
createRoot(document.getElementById("root")).render(<App />);
