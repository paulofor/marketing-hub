import { Link } from "react-router-dom";
import { useExperiments } from "../../api/experiment/useExperiments";
import { useNiches } from "../../api/niche/useNiches";
import { useUpdateExperimentStatus } from "../../api/experiment/useUpdateExperimentStatus";
import { useCloseExperimentPipelineJobs } from "../../api/experiment/useCloseExperimentPipelineJobs";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

const PAGE_SIZE = 25;

function parseDate(date?: string | null) {
  if (!date) return 0;
  const timestamp = new Date(date).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
  }).format(date);
}

function formatCurrency(value?: number | null) {
  if (value === null || value === undefined) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
  }).format(value);
}

function resolveExperimentCost(experiment: {
  cost?: number | null;
  totalCost?: number | null;
  expense?: number | null;
  campaignMetric?: { spend?: number | null } | null;
}) {
  return (
    experiment.cost ??
    experiment.totalCost ??
    experiment.expense ??
    experiment.campaignMetric?.spend ??
    null
  );
}

export default function ExperimentListPage() {
  const { data, isLoading } = useExperiments();
  const { data: niches } = useNiches();
  const updateStatus = useUpdateExperimentStatus();
  const closePipelineJobs = useCloseExperimentPipelineJobs();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [niche, setNiche] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [stoppingExperimentId, setStoppingExperimentId] = useState<
    string | null
  >(null);
  const [retryingExperimentId, setRetryingExperimentId] = useState<
    string | null
  >(null);
  const retryRelease = useMutation({
    mutationFn: async (experimentId: string) => {
      const { data } = await axios.post(
        `/api/experiments/${experimentId}/facebook-release`,
      );
      return data;
    },
    onSuccess: async (_, experimentId) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["experiments"] }),
        queryClient.invalidateQueries({
          queryKey: ["experiment", experimentId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["experiment-readiness", experimentId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["experiment", experimentId, "funnel"],
        }),
      ]);
    },
  });
  const experiments = Array.isArray(data) ? data : [];
  const nicheNameById = useMemo(() => {
    if (!Array.isArray(niches)) return new Map<number, string>();
    return new Map(niches.map((item) => [item.id, item.name]));
  }, [niches]);

  const nicheTotalCostMap = useMemo(() => {
    return experiments.reduce<Record<number, number>>((acc, experiment) => {
      if (typeof experiment.nicheId !== "number") return acc;
      const experimentCost = resolveExperimentCost(experiment);
      if (typeof experimentCost !== "number" || Number.isNaN(experimentCost))
        return acc;
      acc[experiment.nicheId] = (acc[experiment.nicheId] ?? 0) + experimentCost;
      return acc;
    }, {});
  }, [experiments]);

  const filtered = useMemo(() => {
    return experiments.filter(
      (e) =>
        (!search || e.name.toLowerCase().includes(search.toLowerCase())) &&
        (!status || e.status === status) &&
        (!niche || e.nicheId === Number(niche)),
    );
  }, [experiments, search, status, niche]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const bDate = parseDate(b.startDate ?? b.createdAt);
      const aDate = parseDate(a.startDate ?? a.createdAt);
      const dateComparison = bDate - aDate;
      if (dateComparison !== 0) return dateComparison;
      return Number(b.id) - Number(a.id);
    });
  }, [filtered]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const pageStart = (currentPage - 1) * PAGE_SIZE;
  const paginated = sorted.slice(pageStart, pageStart + PAGE_SIZE);
  const visibleStart = sorted.length === 0 ? 0 : pageStart + 1;
  const visibleEnd = Math.min(pageStart + PAGE_SIZE, sorted.length);
  const paginationItems = useMemo(() => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    const middleStart = Math.max(2, currentPage - 1);
    const middleEnd = Math.min(totalPages - 1, currentPage + 1);
    const items: Array<number | string> = [1];

    if (middleStart > 2) items.push("start-ellipsis");
    for (let page = middleStart; page <= middleEnd; page += 1) {
      items.push(page);
    }
    if (middleEnd < totalPages - 1) items.push("end-ellipsis");
    items.push(totalPages);

    return items;
  }, [currentPage, totalPages]);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, status, niche]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  async function handleUserStop(experimentId: string) {
    setStoppingExperimentId(experimentId);
    try {
      await updateStatus.mutateAsync({
        id: experimentId,
        status: "USER_STOPPED",
      });
      await closePipelineJobs.mutateAsync({
        experimentId,
        reason:
          "Encerrado pela ação de parada do usuário na tela de experimentos",
      });
      toast.success(
        "Experimento encerrado e jobs de pipeline abertos foram finalizados.",
      );
    } catch {
      toast.error("Não foi possível concluir a parada do usuário.");
    } finally {
      setStoppingExperimentId(null);
    }
  }

  async function handleRetry(experimentId: string) {
    setRetryingExperimentId(experimentId);
    try {
      await retryRelease.mutateAsync(experimentId);
      toast.success(
        "Experimento reenviado para a fila do Facebook Ads Worker.",
      );
    } catch {
      toast.error("Não foi possível reenviar o experimento para a fila.");
    } finally {
      setRetryingExperimentId(null);
    }
  }

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle icon={experimentIcon}>Testes de Nicho</PageTitle>
      <Link className="btn btn-primary mb-3" to="/experiments/new">
        Novo Teste
      </Link>
      <div className="row g-2 mb-3">
        <div className="col">
          <input
            className="form-control"
            placeholder="Buscar"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="col">
          <select
            className="form-select"
            value={niche}
            onChange={(e) => setNiche(e.target.value)}
          >
            <option value="">Todos Nichos</option>
            {Array.isArray(niches) &&
              niches.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.name} ({formatCurrency(nicheTotalCostMap[n.id] ?? 0)})
                </option>
              ))}
          </select>
        </div>
        <div className="col">
          <select
            className="form-select"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">Todos Status</option>
            <option value="PLANNED">PLANNED</option>
            <option value="RUNNING">RUNNING</option>
            <option value="PAUSED">PAUSED</option>
            <option value="USER_STOPPED">USER_STOPPED</option>
            <option value="FINISHED">FINISHED</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mb-2">
        <span className="text-muted small">
          Exibindo {visibleStart}-{visibleEnd} de {sorted.length} experimentos,
          com {PAGE_SIZE} por página.
        </span>
        <span className="badge text-bg-light align-self-start align-self-md-center">
          Mais recentes primeiro
        </span>
      </div>
      <div className="table-responsive">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>ID do experimento</th>
              <th>Nome do experimento</th>
              <th>Data de criação</th>
              <th>Nicho</th>
              <th>Hipótese</th>
              <th>Custo</th>
              <th>Status</th>
              <th>Botões/Ações</th>
            </tr>
          </thead>
          <tbody>
            {paginated.map((e) => {
              const canStop = e.status === "RUNNING";
              const isStopping = stoppingExperimentId === String(e.id);
              const isRetrying = retryingExperimentId === String(e.id);
              return (
                <tr key={e.id}>
                  <td className="text-nowrap">{e.id}</td>
                  <td>{e.name}</td>
                  <td className="text-nowrap">{formatDate(e.createdAt)}</td>
                  <td>
                    {nicheNameById.get(e.nicheId) || `Nicho #${e.nicheId}`}
                  </td>
                  <td>{e.hypothesis || "—"}</td>
                  <td>
                    {formatCurrency(nicheTotalCostMap[e.nicheId] ?? 0)}
                  </td>
                  <td>{e.status}</td>
                  <td>
                    <Link
                      className="btn btn-sm btn-outline-primary"
                      to={`/experiments/${e.id}`}
                    >
                      Visualizar
                    </Link>
                    {canStop && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-warning ms-1"
                        disabled={isStopping}
                        onClick={() => handleUserStop(String(e.id))}
                      >
                        {isStopping && (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            role="status"
                            aria-hidden="true"
                          />
                        )}
                        Parada do usuário
                      </button>
                    )}
                    {e.status === "FAILED" && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-danger ms-1"
                        disabled={isRetrying}
                        onClick={() => handleRetry(String(e.id))}
                      >
                        {isRetrying && (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            role="status"
                            aria-hidden="true"
                          />
                        )}
                        Retry
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {sorted.length === 0 && (
        <div className="alert alert-light border" role="status">
          Nenhum experimento encontrado para os filtros selecionados.
        </div>
      )}
      {totalPages > 1 && (
        <nav
          className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2"
          aria-label="Paginação dos experimentos"
        >
          <span className="text-muted small">
            Página {currentPage} de {totalPages}
          </span>
          <ul className="pagination mb-0">
            <li className={`page-item${currentPage === 1 ? " disabled" : ""}`}>
              <button
                type="button"
                className="page-link"
                disabled={currentPage === 1}
                onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              >
                Anterior
              </button>
            </li>
            {paginationItems.map((page) =>
              typeof page === "number" ? (
                <li
                  key={page}
                  className={`page-item${page === currentPage ? " active" : ""}`}
                >
                  <button
                    type="button"
                    className="page-link"
                    aria-current={page === currentPage ? "page" : undefined}
                    onClick={() => setCurrentPage(page)}
                  >
                    {page}
                  </button>
                </li>
              ) : (
                <li
                  key={page}
                  className="page-item disabled"
                  aria-hidden="true"
                >
                  <span className="page-link">…</span>
                </li>
              ),
            )}
            <li
              className={`page-item${currentPage === totalPages ? " disabled" : ""}`}
            >
              <button
                type="button"
                className="page-link"
                disabled={currentPage === totalPages}
                onClick={() =>
                  setCurrentPage((page) => Math.min(totalPages, page + 1))
                }
              >
                Próxima
              </button>
            </li>
          </ul>
        </nav>
      )}
    </div>
  );
}
