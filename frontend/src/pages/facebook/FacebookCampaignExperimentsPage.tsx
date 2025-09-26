import { useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle } from "lucide-react";
import { useFacebookCampaignExperiments } from "../../api/useFacebookCampaignExperiments";
import PageTitle from "../../components/PageTitle";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";

export default function FacebookCampaignExperimentsPage() {
  const [status, setStatus] = useState("PLANNED");
  const { data, isLoading } = useFacebookCampaignExperiments(status);
  const { data: configuration } = useFacebookConfigurationStatus();
  const experiments = Array.isArray(data) ? data : [];
  const requiresPageSetup = configuration && !configuration.hasConfiguredPages;
  return (
    <div>
      <PageTitle>Experimentos para Campanha</PageTitle>
      {requiresPageSetup ? (
        <div className="alert alert-warning d-flex align-items-center gap-2" role="alert">
          <AlertTriangle size={18} />
          <div>
            Configure ao menos uma página do Facebook para continuar publicando
            campanhas.
          </div>
        </div>
      ) : null}
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
        <div className="table-responsive">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Hipótese</th>
                <th>KPI alvo</th>
                <th>Início</th>
                <th>Término</th>
              </tr>
            </thead>
            <tbody>
              {experiments.map((e) => (
                <tr key={e.id}>
                  <td>
                    <Link to={`/experiments/${e.id}`}>{e.name}</Link>
                  </td>
                  <td>{e.hypothesis}</td>
                  <td>{e.kpiTargetCpl}</td>
                  <td>{e.startDate}</td>
                  <td>{e.endDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
