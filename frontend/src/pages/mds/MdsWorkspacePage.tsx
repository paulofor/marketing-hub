import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, CheckCircle2, RefreshCw, RadioTower } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import PageTitle from "../../components/PageTitle";
import { useMdsHealth, useMdsRequests, useRetryMdsRequest } from "../../api/mds/useMdsAdmin";
import type { MdsRequestStatus } from "../../api/mds/types";

const STATUS_BADGE: Record<MdsRequestStatus, string> = {
  PENDING: "text-bg-secondary",
  IN_PROGRESS: "text-bg-primary",
  COMPLETED: "text-bg-success",
  FAILED: "text-bg-danger",
};

type Feedback = {
  type: "success" | "error";
  message: string;
} | null;

export default function MdsWorkspacePage() {
  const [status, setStatus] = useState<MdsRequestStatus | "">("");
  const [tenantOrProduct, setTenantOrProduct] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(true);

  const queryClient = useQueryClient();
  const requestsQuery = useMdsRequests(
    {
      status,
      tenantOrProduct,
      from: fromDate ? `${fromDate}T00:00:00Z` : undefined,
      to: toDate ? `${toDate}T23:59:59Z` : undefined,
      page: 0,
      size: 20,
    },
    autoRefreshEnabled,
  );
  const healthQuery = useMdsHealth();
  const retryMutation = useRetryMdsRequest();

  const statusCounts = useMemo(() => {
    const counts: Record<MdsRequestStatus, number> = {
      PENDING: 0,
      IN_PROGRESS: 0,
      COMPLETED: 0,
      FAILED: 0,
    };
    for (const item of requestsQuery.data?.items ?? []) {
      counts[item.status] += 1;
    }
    return counts;
  }, [requestsQuery.data?.items]);

  async function handleRetry(requestId: number) {
    try {
      const result = await retryMutation.mutateAsync(requestId);
      setFeedback({
        type: "success",
        message: `Retry aceito para request #${result.requestId}. Status anterior: ${result.previousStatus}.`,
      });
      await queryClient.invalidateQueries({ queryKey: ["mds", "requests"] });
    } catch {
      setFeedback({
        type: "error",
        message: `Não foi possível reenfileirar a request #${requestId}. Valide elegibilidade e tente novamente.`,
      });
    }
  }

  return (
    <div className="d-flex flex-column gap-4" aria-live="polite">
      <header className="d-flex flex-column gap-2">
        <PageTitle>MDS · Requests</PageTitle>
        <p className="text-secondary mb-0">Visão operacional da fila MDS com filtros e ações administrativas.</p>
      </header>

      <section className="card border-0 shadow-sm" aria-labelledby="mds-observability-title">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex align-items-center justify-content-between flex-wrap gap-2">
            <h2 id="mds-observability-title" className="h6 mb-0 d-flex align-items-center gap-2">
              <RadioTower size={16} /> Observabilidade da UI MDS
            </h2>
            <button
              type="button"
              className={`btn btn-sm ${autoRefreshEnabled ? "btn-outline-success" : "btn-outline-secondary"}`}
              onClick={() => setAutoRefreshEnabled((value) => !value)}
              aria-pressed={autoRefreshEnabled}
            >
              Auto-refresh: {autoRefreshEnabled ? "ligado" : "desligado"}
            </button>
          </div>

          <div className="row g-2">
            <div className="col-12 col-md-4">
              <div className="border rounded-3 p-2 h-100">
                <small className="text-secondary d-block">Saúde do backend MDS</small>
                {healthQuery.isLoading ? <span className="badge text-bg-light">Verificando...</span> : null}
                {healthQuery.isError ? <span className="badge text-bg-danger">Indisponível</span> : null}
                {healthQuery.data ? <span className="badge text-bg-success">{healthQuery.data.status} · {healthQuery.data.module}</span> : null}
              </div>
            </div>
            <div className="col-12 col-md-8">
              <div className="border rounded-3 p-2 h-100 d-flex flex-wrap gap-2">
                <span className="badge text-bg-secondary">PENDING: {statusCounts.PENDING}</span>
                <span className="badge text-bg-primary">IN_PROGRESS: {statusCounts.IN_PROGRESS}</span>
                <span className="badge text-bg-success">COMPLETED: {statusCounts.COMPLETED}</span>
                <span className="badge text-bg-danger">FAILED: {statusCounts.FAILED}</span>
                <span className="badge text-bg-light">Total: {requestsQuery.data?.totalElements ?? 0}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {feedback ? (
        <div className={`alert mb-0 d-flex align-items-start gap-2 ${feedback.type === "success" ? "alert-success" : "alert-danger"}`} role="alert">
          {feedback.type === "success" ? <CheckCircle2 size={18} className="mt-1" /> : <AlertTriangle size={18} className="mt-1" />}
          <div className="d-flex flex-column gap-2">
            <span>{feedback.message}</span>
            <button type="button" className="btn btn-sm btn-outline-secondary align-self-start" onClick={() => setFeedback(null)}>
              Fechar
            </button>
          </div>
        </div>
      ) : null}

      <section className="card border-0 shadow-sm" aria-labelledby="mds-filters-title">
        <div className="card-body">
          <h2 id="mds-filters-title" className="h6 mb-3">Filtros operacionais</h2>
          <div className="row g-3 align-items-end">
            <div className="col-12 col-md-4">
              <label className="form-label" htmlFor="mds-status">Status</label>
              <select
                id="mds-status"
                className="form-select"
                value={status}
                onChange={(event) => setStatus(event.target.value as MdsRequestStatus | "")}
              >
                <option value="">Todos</option>
                <option value="PENDING">Pendente</option>
                <option value="IN_PROGRESS">Em andamento</option>
                <option value="COMPLETED">Concluído</option>
                <option value="FAILED">Falhou</option>
              </select>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label" htmlFor="mds-period-from">Período inicial</label>
              <input
                id="mds-period-from"
                type="date"
                className="form-control"
                value={fromDate}
                onChange={(event) => setFromDate(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label" htmlFor="mds-period-to">Período final</label>
              <input
                id="mds-period-to"
                type="date"
                className="form-control"
                value={toDate}
                onChange={(event) => setToDate(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label" htmlFor="mds-tenant">Tenant/produto</label>
              <input
                id="mds-tenant"
                className="form-control"
                value={tenantOrProduct}
                onChange={(event) => setTenantOrProduct(event.target.value)}
                placeholder="Ex.: tenant-a"
              />
            </div>
            <div className="col-12 col-md-4 d-grid">
              <button
                type="button"
                className="btn btn-outline-primary"
                onClick={() => queryClient.invalidateQueries({ queryKey: ["mds", "requests"] })}
                disabled={requestsQuery.isFetching}
                aria-label="Atualizar fila MDS"
              >
                {requestsQuery.isFetching ? (
                  <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                ) : (
                  <RefreshCw size={16} aria-hidden="true" />
                )}
                <span className="ms-2">Atualizar</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      {requestsQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <span className="spinner-border text-primary" aria-hidden="true" />
        </div>
      ) : null}

      {requestsQuery.isError ? (
        <div className="alert alert-danger mb-0">Não foi possível carregar a fila MDS.</div>
      ) : null}

      {!requestsQuery.isLoading && !requestsQuery.isError ? (
        <section className="d-grid gap-3" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))" }}>
          {requestsQuery.data?.items.length ? requestsQuery.data.items.map((item) => (
            <article className="card border-0 shadow-sm" key={item.requestId}>
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex justify-content-between align-items-start gap-2">
                  <div>
                    <h2 className="h6 mb-1">Request #{item.requestId}</h2>
                    <p className="mb-0 text-secondary">{item.market}</p>
                  </div>
                  <span className={`badge ${STATUS_BADGE[item.status]}`}>{item.status}</span>
                </div>
                <p className="mb-0"><strong>Dor:</strong> {item.problem}</p>
                <p className="mb-0"><strong>Resultado:</strong> {item.desiredOutcome}</p>
                <div className="d-flex flex-column gap-1 text-secondary small">
                  <span>Estágio: {item.currentStage}</span>
                  <span>Tentativa: {item.attempt}</span>
                  <span>Heartbeat: {item.lastHeartbeatAt ?? "-"}</span>
                </div>
                {!item.retryEligible ? (
                  <div className="alert alert-warning py-2 px-3 mb-0 small">
                    Retry indisponível: {item.retryReason}
                  </div>
                ) : null}
                <div className="d-flex gap-2 flex-wrap">
                  <Link className="btn btn-outline-primary btn-sm" to={`/mds/requests/${item.requestId}`}>Detalhe</Link>
                  <Link className="btn btn-outline-secondary btn-sm" to={`/mds/requests/${item.requestId}/artifacts`}>Artefatos</Link>
                  <Link className="btn btn-outline-dark btn-sm" to={`/mds/reports/${item.requestId}`}>Relatório</Link>
                  <button
                    type="button"
                    className="btn btn-outline-warning btn-sm"
                    disabled={retryMutation.isPending || !item.retryEligible}
                    onClick={() => handleRetry(item.requestId)}
                  >
                    {retryMutation.isPending ? (
                      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                    ) : (
                      "Retry"
                    )}
                  </button>
                </div>
              </div>
            </article>
          )) : <div className="alert alert-secondary mb-0">Nenhuma request MDS encontrada para os filtros.</div>}
        </section>
      ) : null}
    </div>
  );
}
