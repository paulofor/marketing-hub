import { useState } from "react";
import LeadForm from "./components/LeadForm";
import ResultCard from "./components/ResultCard";
import { LeadDetails } from "./types";
import { useLeadResultPolling } from "./hooks/useLeadResultPolling";

function App() {
  const [lead, setLead] = useState<LeadDetails | null>(null);
  useLeadResultPolling(lead, setLead);

  return (
    <div className="app-container">
      <header>
        <h1>Portal de Leads</h1>
        <p>
          Envie uma imagem de referência e acompanhe o processamento automático do seu lead.
        </p>
      </header>
      <main>
        <LeadForm onLeadCreated={setLead} />
        {lead && <ResultCard lead={lead} />}
      </main>
      <footer>
        <small>Marketing Hub &copy; {new Date().getFullYear()}</small>
      </footer>
    </div>
  );
}

export default App;
