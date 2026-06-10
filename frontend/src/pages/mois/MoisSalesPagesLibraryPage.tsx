import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  getSalesLibraryJobBadgeClass,
  useMoisSalesLibraryPages,
  useMoisSalesLibraryPageSummary,
} from "../../api/mois/useMoisSalesLibrary";
import type { MoisSalesLibraryPage } from "../../api/mois/types";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 20;

const warmupTemperatureLabels: Record<string, string> = {
  HOT: "Quente",
  PROMISING: "Promissor",
  WARM: "Morno",
  COLD: "Frio",
  SATURATED: "Saturado",
};

const warmupStatusLabels: Record<string, string> = {
  PENDING: "Pendente",
  FETCHING: "Em pesquisa",
  DONE: "Com dossiê",
  FAILED: "Falhou",
};

function formatWarmupScore(value?: number | null) {
  return value == null ? "—" : `${Math.round(value)}/100`;
}

function getWarmupBadgeClass(value?: string | null) {
  switch (value) {
    case "HOT":
      return "bg-success-subtle text-success-emphasis";
    case "PROMISING":
      return "bg-primary-subtle text-primary-emphasis";
    case "WARM":
      return "bg-warning-subtle text-warning-emphasis";
    case "SATURATED":
    case "FAILED":
      return "bg-danger-subtle text-danger-emphasis";
    case "FETCHING":
      return "bg-info-subtle text-info-emphasis";
    case "PENDING":
      return "bg-warning-subtle text-warning-emphasis";
    case "DONE":
      return "bg-success-subtle text-success-emphasis";
    case "COLD":
    default:
      return "bg-secondary-subtle text-secondary-emphasis";
  }
}

function matchesWarmupFilter(item: MoisSalesLibraryPage, filter: string) {
  switch (filter) {
    case "WITH_DOSSIER":
      return item.marketWarmupStatus === "DONE";
    case "PENDING_OR_RUNNING":
      return ["PENDING", "FETCHING"].includes(item.marketWarmupStatus || "");
    case "FAILED":
      return item.marketWarmupStatus === "FAILED";
    case "HOT_OR_PROMISING":
      return ["HOT", "PROMISING"].includes(item.marketWarmupTemperature || "");
    case "WITHOUT_DOSSIER":
      return !item.marketWarmupStatus;
    default:
      return true;
  }
}

function getPipelinePhase(stage?: string | null, status?: string | null) {
  if (stage === "CAPTURE" && status === "FAILED") {
    return "Captura falhou — revisar URL ou cooldown";
  }
  if (stage === "CAPTURE" && status === "CAPTURED") {
    return "HTML capturado — pronto para análise";
  }
  if (stage === "ANALYSIS" && status === "DONE") {
    return "Análise concluída — priorizar ofertas vencedoras";
  }
  if (stage === "ANALYSIS" && status === "PENDING") {
    return "Aguardando análise comercial";
  }
  if (status === "BLOCKED_COOLDOWN") {
    return "Bloqueada por cooldown";
  }
  return stage && status ? `${stage} — ${status}` : "Sem fase definida";
}

function formatAnalysisDate(value?: string | null) {
  if (!value) {
    return "—";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function MoisSalesPagesLibraryPage() {
  const [searchParams] = useSearchParams();
  const initialWarmupFilter = searchParams.get("warmupFilter") || "ALL";
  const initialSort = searchParams.get("sort") || "MARKET_WARMUP_SCORE";
  const [warmupFilter, setWarmupFilter] = useState(initialWarmupFilter);
  const [sort, setSort] = useState(initialSort);
  const pagesQuery = useMoisSalesLibraryPages(
    WORKSPACE_ID,
    1,
    PAGE_SIZE,
    warmupFilter,
    sort,
  );
  const summaryQuery = useMoisSalesLibraryPageSummary(WORKSPACE_ID);
  const summary = summaryQuery.data;
  const visiblePages = useMemo(() => {
    return (pagesQuery.data?.items ?? []).filter((item) =>
      matchesWarmupFilter(item, warmupFilter),
    );
  }, [pagesQuery.data?.items, warmupFilter]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Tabela consolidada com cada produto coletado e a fase atual no fluxo
            canônico.
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link
            className="btn btn-primary"
            to="/mois/sales-pages-library/pipeline"
          >
            Pipeline
          </Link>
          <Link className="btn btn-outline-secondary" to="/mois">
            Voltar ao workspace
          </Link>
        </div>
      </header>

      {summary ? (
        <section className="row g-3">
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Total de páginas</p>
                <h3 className="mb-0">{summary.total}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">
                  Elegíveis para aquecimento
                </p>
                <h3 className="mb-0">{summary.marketWarmupEligible}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Com dossiê concluído</p>
                <h3 className="mb-0">{summary.marketWarmupCompleted}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Quentes/promissores</p>
                <h3 className="mb-0">
                  {summary.marketWarmupHot + summary.marketWarmupPromising}
                </h3>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap align-items-end justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-1">Priorização por aquecimento</h2>
              <p className="text-secondary mb-0">
                Ordene a listagem diretamente no backend pelo score de
                aquecimento para decidir quais mercados merecem ação primeiro.
              </p>
            </div>
            <div className="d-flex flex-wrap gap-3">
              <div>
                <label className="form-label small" htmlFor="warmupFilter">
                  Filtro de aquecimento
                </label>
                <select
                  id="warmupFilter"
                  className="form-select form-select-sm"
                  value={warmupFilter}
                  onChange={(event) => setWarmupFilter(event.target.value)}
                >
                  <option value="ALL">Todos</option>
                  <option value="WITH_DOSSIER">Com dossiê</option>
                  <option value="HOT_OR_PROMISING">Quentes/promissores</option>
                  <option value="PENDING_OR_RUNNING">
                    Pendentes/em pesquisa
                  </option>
                  <option value="FAILED">Falharam</option>
                  <option value="WITHOUT_DOSSIER">Sem dossiê</option>
                </select>
              </div>
              <div>
                <label className="form-label small" htmlFor="warmupSort">
                  Ordenação
                </label>
                <select
                  id="warmupSort"
                  className="form-select form-select-sm"
                  value={sort}
                  onChange={(event) => setSort(event.target.value)}
                >
                  <option value="MARKET_WARMUP_SCORE">
                    Maior score de aquecimento
                  </option>
                  <option value="RECENT_ANALYSIS">Análise mais recente</option>
                </select>
              </div>
            </div>
          </div>
          <div className="table-responsive">
            {pagesQuery.isLoading ? (
              <p className="text-secondary mb-0">
                Carregando produtos coletados...
              </p>
            ) : null}
            {pagesQuery.isError ? (
              <div className="alert alert-danger mb-0">
                Falha ao carregar produtos da biblioteca.
              </div>
            ) : null}
            {pagesQuery.data ? (
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Produto</th>
                    <th>Origem</th>
                    <th>Status</th>
                    <th>Aquecimento</th>
                    <th>Score aquecimento</th>
                    <th>Data da análise</th>
                    <th>Fase no diagrama</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {visiblePages.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-secondary">
                        Nenhum produto coletado encontrado para o filtro atual.
                      </td>
                    </tr>
                  ) : (
                    visiblePages.map((item) => (
                      <tr key={item.pageId}>
                        <td className="text-break">
                          {item.title || item.urlCanonical}
                        </td>
                        <td>{item.source || "—"}</td>
                        <td>
                          <span
                            className={`badge ${getSalesLibraryJobBadgeClass({ status: item.currentStatus })}`}
                          >
                            {item.currentStatus ||
                              item.analysisStatus ||
                              "SEM STATUS"}
                          </span>
                        </td>
                        <td>
                          <span
                            className={`badge ${getWarmupBadgeClass(item.marketWarmupTemperature || item.marketWarmupStatus)}`}
                          >
                            {item.marketWarmupTemperature
                              ? warmupTemperatureLabels[
                                  item.marketWarmupTemperature
                                ]
                              : item.marketWarmupStatus
                                ? warmupStatusLabels[item.marketWarmupStatus]
                                : "Sem dossiê"}
                          </span>
                        </td>
                        <td className="text-nowrap fw-semibold">
                          {formatWarmupScore(item.marketWarmupScoreTotal)}
                        </td>
                        <td className="text-nowrap">
                          {formatAnalysisDate(item.analyzedAt)}
                        </td>
                        <td>
                          {getPipelinePhase(
                            item.currentStage,
                            item.currentStatus,
                          )}
                        </td>
                        <td>
                          <div className="d-flex flex-wrap gap-2">
                            <Link
                              className="btn btn-outline-primary btn-sm"
                              to={`/mois/sales-pages-library/${item.pageId}`}
                            >
                              Ver detalhe
                            </Link>
                            <a
                              className="btn btn-outline-secondary btn-sm"
                              href={item.urlCanonical}
                              target="_blank"
                              rel="noreferrer"
                            >
                              Abrir página
                            </a>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  );
}
