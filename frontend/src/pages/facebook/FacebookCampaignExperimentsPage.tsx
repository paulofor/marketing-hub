import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, CheckCircle2 } from "lucide-react";
import { useFacebookCampaignExperiments } from "../../api/useFacebookCampaignExperiments";
import PageTitle from "../../components/PageTitle";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import { MissingConfigurationList } from "./MissingConfigurationList";

export default function FacebookCampaignExperimentsPage() {
  const [status, setStatus] = useState("PLANNED");
  const { data, isLoading } = useFacebookCampaignExperiments(status);
  const { data: configuration } = useFacebookConfigurationStatus();
  const experiments = Array.isArray(data) ? data : [];
  const requiresPageSetup = configuration && !configuration.hasConfiguredPages;
  const numberFormatter = useMemo(() => new Intl.NumberFormat("pt-BR"), []);
  const currencyFormatter = useMemo(
    () => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }),
    [],
  );
  const formatNumber = (value?: number | null) =>
    typeof value === "number" ? numberFormatter.format(value) : "--";
  const formatCurrency = (value?: number | null) =>
    typeof value === "number" ? currencyFormatter.format(value) : "--";
  const formatDateTime = (value?: string | null) =>
    value ? new Date(value).toLocaleString("pt-BR") : "--";

  const renderMetrics = (experiment: (typeof experiments)[number]) => {
    if (status !== "RUNNING") {
      return <span className="text-muted">--</span>;
    }
    const metrics = experiment.metrics;
    if (!metrics) {
      return <span className="text-muted">Aguardando dados</span>;
    }
    return (
      <div className="small">
        <div className="d-flex flex-wrap gap-3">
          <span>
            <strong>Impr.</strong> {formatNumber(metrics.impressions)}
          </span>
          <span>
            <strong>Cliques</strong> {formatNumber(metrics.clicks)}
          </span>
          <span>
            <strong>Leads</strong> {formatNumber(metrics.leads)}
          </span>
          <span>
            <strong>Custo</strong> {formatCurrency(metrics.spend)}
          </span>
          <span>
            <strong>CPL</strong> {formatCurrency(metrics.cpl)}
          </span>
        </div>
        <div className="text-muted mt-1">
          {metrics.lastSyncedAt
            ? `Atualizado em ${formatDateTime(metrics.lastSyncedAt)}`
            : "Última atualização indisponível"}
          {metrics.lastSyncError ? (
            <span className="text-danger ms-2">Erro: {metrics.lastSyncError}</span>
          ) : null}
        </div>
      </div>
    );
  };
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
                <th>Pendências</th>
                <th>Desempenho</th>
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
                  <td>
                    {e.missingConfiguration.length > 0 ? (
                      <div>
                        <span className="badge text-bg-warning d-inline-flex align-items-center gap-1 mb-1">
                          <AlertTriangle size={14} aria-hidden="true" />
                          Pendências
                        </span>
                        <MissingConfigurationList
                          items={e.missingConfiguration}
                          className="mb-0 ps-3 small"
                        />
                      </div>
                    ) : (
                      <span className="badge text-bg-success d-inline-flex align-items-center gap-1">
                        <CheckCircle2 size={14} aria-hidden="true" />
                        Em dia
                      </span>
                    )}
                  </td>
                  <td>{renderMetrics(e)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
