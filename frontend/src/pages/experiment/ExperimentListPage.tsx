import { Link } from "react-router-dom";
import { useExperimentSummary } from "../../api/experiment/useExperiments";
import { useNiches } from "../../api/niche/useNiches";
import {
  useReactivateExperiment,
  useUpdateExperimentStatus,
} from "../../api/experiment/useUpdateExperimentStatus";
import { useCloseExperimentPipelineJobs } from "../../api/experiment/useCloseExperimentPipelineJobs";
import { useReconcileExperimentTerminalState } from "../../api/experiment/useReconcileExperimentTerminalState";
import { useLatestPromiseOptionsDraft } from "../../api/experiment/useGeneratePromiseOptions";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

const PAGE_SIZE = 25;
const BRL_PER_USD = 5;
function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
  }).format(date);
}

function formatCurrency(
  value?: number | null,
  currency: "BRL" | "USD" = "BRL",
) {
  if (value === null || value === undefined) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(value);
}

function formatCurrencyPair(valueBrl?: number | null) {
  if (valueBrl === null || valueBrl === undefined) return "—";
  return `${formatCurrency(valueBrl, "BRL")} / ${formatCurrency(valueBrl / BRL_PER_USD, "USD")}`;
}

function formatDurationMs(value?: number | null) {
  if (!value || value <= 0) return "—";
  const totalSeconds = Math.round(value / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes <= 0) return `${seconds}s`;
  return `${minutes}m ${String(seconds).padStart(2, "0")}s`;
}

function resolveExperimentCost(experiment: {
  cost?: number | null;
  totalCost?: number | null;
  auditableTotalCost?: number | null;
  expense?: number | null;
  campaignMetric?: { spend?: number | null } | null;
}) {
  return (
    experiment.auditableTotalCost ??
    experiment.totalCost ??
    experiment.campaignMetric?.spend ??
    experiment.cost ??
    experiment.expense ??
    null
  );
}

function resolveExperimentRevenue(experiment: {
  revenue?: number | null;
  totalRevenue?: number | null;
  campaignMetric?: { revenue?: number | null } | null;
}) {
  return (
    experiment.revenue ??
    experiment.totalRevenue ??
    experiment.campaignMetric?.revenue ??
    null
  );
}

function isCommercialValidationExperiment(experiment: {
  status?: string | null;
}) {
  const normalizedStatus = experiment.status
    ?.normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
  return Boolean(
    normalizedStatus === "RUNNING" ||
    normalizedStatus === "VALIDACAO_COMERCIAL" ||
    normalizedStatus === "COMMERCIAL_VALIDATION" ||
    normalizedStatus === "COMMERCIAL_VALIDATING",
  );
}

export default function ExperimentListPage() {
  const { data: niches } = useNiches();
  const updateStatus = useUpdateExperimentStatus();
  const reactivateExperiment = useReactivateExperiment();
  const closePipelineJobs = useCloseExperimentPipelineJobs();
  const reconcileTerminalState = useReconcileExperimentTerminalState();
  const queryClient = useQueryClient();
  const latestPromiseOptionsDraft = useLatestPromiseOptionsDraft();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [niche, setNiche] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const deferredSearch = useDeferredValue(search.trim());
  const { data, isLoading } = useExperimentSummary(currentPage - 1, PAGE_SIZE, {
    search: deferredSearch || undefined,
    status: status || undefined,
    nicheId: niche ? Number(niche) : undefined,
  });
  const [stoppingExperimentId, setStoppingExperimentId] = useState<
    string | null
  >(null);
  const [retryingExperimentId, setRetryingExperimentId] = useState<
    string | null
  >(null);
  const [reconcilingExperimentId, setReconcilingExperimentId] = useState<
    string | null
  >(null);
  const [reactivationExperimentId, setReactivationExperimentId] = useState<
    string | null
  >(null);
  const [reactivationReason, setReactivationReason] = useState("");
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
  const experiments = data?.items ?? [];
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

  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const pageStart = (currentPage - 1) * PAGE_SIZE;
  const paginated = experiments;
  const totalElements = data?.totalElements ?? 0;
  const visibleStart = totalElements === 0 ? 0 : pageStart + 1;
  const visibleEnd = Math.min(pageStart + experiments.length, totalElements);
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
    if (data && currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, data, totalPages]);

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

  async function handleTerminalReconciliation(experimentId: string) {
    setReconcilingExperimentId(experimentId);
    try {
      const result = await reconcileTerminalState.mutateAsync(experimentId);
      if (result.invalidated) {
        toast.success(
          "Experimento encerrado pelo limite financeiro, com execução e tarefas reconciliadas.",
        );
      } else {
        toast.info(
          "O encerramento foi preservado sem evidência para invalidação financeira.",
        );
      }
    } catch {
      toast.error(
        "Não foi possível reconciliar o encerramento do experimento.",
      );
    } finally {
      setReconcilingExperimentId(null);
    }
  }

  async function handleReactivate() {
    if (!reactivationExperimentId) return;
    const reason = reactivationReason.trim();
    if (reason.length < 10) {
      toast.error("Informe um motivo com pelo menos 10 caracteres.");
      return;
    }
    try {
      await reactivateExperiment.mutateAsync({
        id: reactivationExperimentId,
        reason,
      });
      toast.success("Experimento reativado com motivo registrado.");
      setReactivationExperimentId(null);
      setReactivationReason("");
    } catch {
      toast.error("Não foi possível reativar o experimento.");
    }
  }

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle icon={experimentIcon}>Testes de Nicho</PageTitle>
      <div className="d-flex flex-wrap gap-2 mb-3">
        <Link className="btn btn-primary" to="/experiments/new">
          Novo Teste
        </Link>
        <Link className="btn btn-outline-primary" to="/experiments/manual/new">
          Novo experimento manual
        </Link>
        {latestPromiseOptionsDraft.data && (
          <Link className="btn btn-outline-primary" to="/experiments/new">
            Continuar teste em criação
          </Link>
        )}
      </div>
      <div className="row g-2 mb-3">
        <div className="col">
          <input
            className="form-control"
            placeholder="Buscar"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setCurrentPage(1);
            }}
          />
        </div>
        <div className="col">
          <select
            className="form-select"
            value={niche}
            onChange={(e) => {
              setNiche(e.target.value);
              setCurrentPage(1);
            }}
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
            onChange={(e) => {
              setStatus(e.target.value);
              setCurrentPage(1);
            }}
          >
            <option value="">Status não finalizados</option>
            <option value="PLANNED">PLANNED</option>
            <option value="RUNNING">RUNNING</option>
            <option value="PAUSED">PAUSED</option>
            <option value="STANDBY">STANDBY</option>
            <option value="USER_STOPPED">USER_STOPPED</option>
          </select>
        </div>
      </div>
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mb-2">
        <span className="text-muted small">
          Exibindo {visibleStart}-{visibleEnd} de {totalElements} experimentos
          não finalizados, com {PAGE_SIZE} por página.
        </span>
        <span className="badge text-bg-light align-self-start align-self-md-center">
          Validação comercial no topo · depois mais recentes
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
              <th>Tempo médio sessão</th>
              <th>Status</th>
              <th className="text-end">Custo e receita</th>
              <th>Botões/Ações</th>
            </tr>
          </thead>
          <tbody>
            {paginated.map((e) => {
              const canStop = e.status === "RUNNING";
              const canReactivate = e.reactivationAvailable === true;
              const isCommercialValidation =
                isCommercialValidationExperiment(e);
              const isStopping = stoppingExperimentId === String(e.id);
              const isRetrying = retryingExperimentId === String(e.id);
              return (
                <tr
                  key={e.id}
                  className={
                    isCommercialValidation ? "table-success" : undefined
                  }
                >
                  <td className="text-nowrap">{e.id}</td>
                  <td>
                    <div className="d-flex flex-column gap-1">
                      <span>{e.name}</span>
                      {e.creationSource === "MANUAL_FLOW" ? (
                        <span className="badge text-bg-warning align-self-start">
                          Fluxo manual
                        </span>
                      ) : null}
                      {isCommercialValidation ? (
                        <span className="badge text-bg-success align-self-start">
                          Validação comercial
                        </span>
                      ) : null}
                    </div>
                  </td>
                  <td className="text-nowrap">{formatDate(e.createdAt)}</td>
                  <td>
                    {nicheNameById.get(e.nicheId) || `Nicho #${e.nicheId}`}
                  </td>
                  <td>{e.hypothesis || "—"}</td>
                  <td>
                    <div className="d-flex flex-column gap-1 text-nowrap">
                      <span>
                        {formatDurationMs(
                          e.sessionDurationSummary?.averageVisibleMsPerSession,
                        )}
                      </span>
                      {e.sessionDurationSummary?.variants?.length ? (
                        <div className="d-flex flex-column gap-1 small text-muted">
                          {e.sessionDurationSummary.variants.map(
                            (variant, index) => (
                              <span
                                key={`${variant.variantKey ?? variant.variantName ?? "variant"}-${index}`}
                              >
                                {variant.variantKey ?? variant.variantName}:{" "}
                                {formatDurationMs(
                                  variant.averageVisibleMsPerSession,
                                )}
                              </span>
                            ),
                          )}
                        </div>
                      ) : null}
                    </div>
                  </td>
                  <td>{e.status}</td>
                  <td className="text-end">
                    <div className="d-flex flex-column gap-1">
                      <span className="fw-semibold">
                        Custo: {formatCurrencyPair(resolveExperimentCost(e))}
                      </span>
                      <span className="text-success fw-semibold">
                        Receita:{" "}
                        {formatCurrencyPair(resolveExperimentRevenue(e))}
                      </span>
                    </div>
                  </td>
                  <td>
                    <Link
                      className="btn btn-sm btn-outline-primary"
                      to={`/experiments/${e.id}`}
                    >
                      Visualizar
                    </Link>
                    <Link
                      className="btn btn-sm btn-outline-success ms-1"
                      to={`/experiments/${e.id}/cockpit`}
                    >
                      Cockpit
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
                    {canReactivate && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-success ms-1"
                        onClick={() => {
                          setReactivationExperimentId(String(e.id));
                          setReactivationReason(
                            e.id === "67"
                              ? "Retomar o Experimento 67 para medir a versão atual do PDE Musa em produção como novo ciclo dentro do mesmo aprendizado."
                              : "",
                          );
                        }}
                      >
                        Retornar à atividade
                      </button>
                    )}
                    {e.terminalReconciliationAvailable && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-danger ms-1"
                        disabled={reconcilingExperimentId === String(e.id)}
                        onClick={() =>
                          handleTerminalReconciliation(String(e.id))
                        }
                      >
                        {reconcilingExperimentId === String(e.id) && (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            role="status"
                            aria-hidden="true"
                          />
                        )}
                        Concluir pelo limite financeiro
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
      {experiments.length === 0 && (
        <div className="alert alert-light border" role="status">
          Nenhum experimento encontrado para os filtros selecionados.
        </div>
      )}
      {reactivationExperimentId && (
        <div className="modal d-block" tabIndex={-1} role="dialog">
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h2 className="modal-title h5">
                  Retornar experimento à atividade
                </h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Fechar"
                  onClick={() => {
                    setReactivationExperimentId(null);
                    setReactivationReason("");
                  }}
                />
              </div>
              <div className="modal-body">
                <label className="form-label" htmlFor="reactivation-reason">
                  Motivo do retorno
                </label>
                <textarea
                  id="reactivation-reason"
                  className="form-control"
                  rows={4}
                  maxLength={1024}
                  value={reactivationReason}
                  onChange={(event) =>
                    setReactivationReason(event.target.value)
                  }
                />
                <div className="form-text">
                  Esse motivo fica registrado no histórico do experimento.
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => {
                    setReactivationExperimentId(null);
                    setReactivationReason("");
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-success"
                  disabled={reactivateExperiment.isPending}
                  onClick={handleReactivate}
                >
                  {reactivateExperiment.isPending && (
                    <span
                      className="spinner-border spinner-border-sm me-1"
                      role="status"
                      aria-hidden="true"
                    />
                  )}
                  Reativar
                </button>
              </div>
            </div>
          </div>
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
