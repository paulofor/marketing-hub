import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useFacebookCampaignExperiments } from "../../api/useFacebookCampaignExperiments";
import PageTitle from "../../components/PageTitle";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import { MissingConfigurationList } from "./MissingConfigurationList";

const EXPERIMENTS_PER_PAGE = 25;

export default function FacebookCampaignExperimentsPage() {
  const [status, setStatus] = useState("PLANNED");
  const [currentPage, setCurrentPage] = useState(1);
  const { data, isLoading } = useFacebookCampaignExperiments(status);
  const { data: configuration } = useFacebookConfigurationStatus();
  const experiments = useMemo(
    () =>
      (Array.isArray(data) ? [...data] : []).sort(
        (current, next) => next.id - current.id,
      ),
    [data],
  );
  const totalPages = Math.max(
    1,
    Math.ceil(experiments.length / EXPERIMENTS_PER_PAGE),
  );
  const firstExperimentIndex = (currentPage - 1) * EXPERIMENTS_PER_PAGE;
  const paginatedExperiments = experiments.slice(
    firstExperimentIndex,
    firstExperimentIndex + EXPERIMENTS_PER_PAGE,
  );
  const pageStart = experiments.length === 0 ? 0 : firstExperimentIndex + 1;
  const pageEnd = Math.min(
    firstExperimentIndex + EXPERIMENTS_PER_PAGE,
    experiments.length,
  );
  const requiresPageSetup = configuration && !configuration.hasConfiguredPages;
  const numberFormatter = useMemo(() => new Intl.NumberFormat("pt-BR"), []);
  const currencyFormatter = useMemo(
    () =>
      new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }),
    [],
  );
  const formatNumber = (value?: number | null) =>
    typeof value === "number" ? numberFormatter.format(value) : "--";
  const formatCurrency = (value?: number | null) =>
    typeof value === "number" ? currencyFormatter.format(value) : "--";
  const formatDateTime = (value?: string | null) =>
    value ? new Date(value).toLocaleString("pt-BR") : "--";

  useEffect(() => {
    setCurrentPage(1);
  }, [status]);

  useEffect(() => {
    setCurrentPage((page) => Math.min(page, totalPages));
  }, [totalPages]);

  const renderMetrics = (experiment: (typeof experiments)[number]) => {
    if (status !== "RUNNING") {
      return <span className="text-muted">--</span>;
    }
    const metrics = experiment.metrics;
    const leadPortalFunnel = experiment.leadPortalFunnel;
    if (!metrics && !leadPortalFunnel) {
      return <span className="text-muted">Aguardando dados</span>;
    }
    return (
      <div className="small">
        <div className="d-flex flex-wrap gap-3">
          {metrics ? (
            <>
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
            </>
          ) : null}
          {leadPortalFunnel ? (
            <>
              <span>
                <strong>Form visto</strong>{" "}
                {formatNumber(leadPortalFunnel.formAccesses)}
              </span>
              <span>
                <strong>Form enviado</strong>{" "}
                {formatNumber(leadPortalFunnel.formSubmissions)}
              </span>
            </>
          ) : null}
        </div>
        {metrics ? (
          <div className="text-muted mt-1">
            {metrics.lastSyncedAt
              ? `Atualizado em ${formatDateTime(metrics.lastSyncedAt)}`
              : "Última atualização indisponível"}
            {metrics.lastSyncError ? (
              <span className="text-danger ms-2">
                Erro: {metrics.lastSyncError}
              </span>
            ) : null}
          </div>
        ) : null}
      </div>
    );
  };
  return (
    <div>
      <PageTitle>Experimentos para Campanha</PageTitle>
      {requiresPageSetup ? (
        <div
          className="alert alert-warning d-flex align-items-center gap-2"
          role="alert"
        >
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
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
            <span className="text-muted small">
              Mais recentes primeiro · exibindo {pageStart}-{pageEnd} de{" "}
              {experiments.length}
            </span>
            <span className="badge text-bg-light border">25 por página</span>
          </div>
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
              {paginatedExperiments.map((e) => (
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
          {totalPages > 1 ? (
            <nav aria-label="Paginação de experimentos para campanha">
              <ul className="pagination justify-content-end mb-0">
                <li
                  className={`page-item${currentPage === 1 ? " disabled" : ""}`}
                >
                  <button
                    type="button"
                    className="page-link d-inline-flex align-items-center gap-1"
                    onClick={() =>
                      setCurrentPage((page) => Math.max(1, page - 1))
                    }
                    disabled={currentPage === 1}
                  >
                    <ChevronLeft size={16} aria-hidden="true" />
                    Anterior
                  </button>
                </li>
                {Array.from(
                  { length: totalPages },
                  (_, index) => index + 1,
                ).map((page) => (
                  <li
                    key={page}
                    className={`page-item${page === currentPage ? " active" : ""}`}
                  >
                    <button
                      type="button"
                      className="page-link"
                      onClick={() => setCurrentPage(page)}
                      aria-current={page === currentPage ? "page" : undefined}
                    >
                      {page}
                    </button>
                  </li>
                ))}
                <li
                  className={`page-item${currentPage === totalPages ? " disabled" : ""}`}
                >
                  <button
                    type="button"
                    className="page-link d-inline-flex align-items-center gap-1"
                    onClick={() =>
                      setCurrentPage((page) => Math.min(totalPages, page + 1))
                    }
                    disabled={currentPage === totalPages}
                  >
                    Próxima
                    <ChevronRight size={16} aria-hidden="true" />
                  </button>
                </li>
              </ul>
            </nav>
          ) : null}
        </div>
      )}
    </div>
  );
}
