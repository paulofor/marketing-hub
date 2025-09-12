import { useState } from "react";
import { useFacebookCampaignExperiments } from "../../api/useFacebookCampaignExperiments";
import PageTitle from "../../components/PageTitle";

export default function FacebookCampaignExperimentsPage() {
  const [status, setStatus] = useState("PLANNED");
  const { data, isLoading } = useFacebookCampaignExperiments(status);
  const experiments = Array.isArray(data) ? data : [];
  return (
    <div>
      <PageTitle>Campanhas do Facebook</PageTitle>
      <div className="btn-group mb-3">
        <button
          className={`btn btn-outline-primary${status === "PLANNED" ? " active" : ""}`}
          onClick={() => setStatus("PLANNED")}
        >
          Planejadas
        </button>
        <button
          className={`btn btn-outline-primary${status === "RUNNING" ? " active" : ""}`}
          onClick={() => setStatus("RUNNING")}
        >
          Ativas
        </button>
        <button
          className={`btn btn-outline-primary${status === "FINISHED" ? " active" : ""}`}
          onClick={() => setStatus("FINISHED")}
        >
          Encerradas
        </button>
      </div>
      {isLoading ? (
        <p>Carregando...</p>
      ) : (
        <ul>
          {experiments.map((e) => (
            <li key={e.id}>{e.name}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
