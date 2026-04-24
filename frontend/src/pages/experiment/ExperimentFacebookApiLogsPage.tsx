import { Fragment, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  ExperimentFacebookApiLog,
  useExperimentFacebookApiLogs,
} from "../../api/experiment/useExperimentFacebookApiLogs";
import "./ExperimentFacebookApiLogsPage.css";

const LIMIT_OPTIONS = [25, 50, 100, 200, 400];

export default function ExperimentFacebookApiLogsPage() {
  const { id } = useParams();
  const experimentId = id ?? "";
  const [limit, setLimit] = useState(25);
  const { data, isLoading, isFetching, refetch } =
    useExperimentFacebookApiLogs(experimentId, limit);
  const [expanded, setExpanded] = useState<Record<number, boolean>>({});
  const [copyingLogId, setCopyingLogId] = useState<number | null>(null);
  const [copiedLogId, setCopiedLogId] = useState<number | null>(null);

  const stats = useMemo(() => {
    const logs = Array.isArray(data) ? data : [];
    const errors = logs.filter((log) => isErrorStatus(log)).length;
    const lastCall = logs.length ? logs[0].requestedAt ?? logs[0].createdAt : null;
    return {
      total: logs.length,
      errors,
      lastCall,
    };
  }, [data]);

  const toggleRow = (logId: number) => {
    setExpanded((prev) => ({ ...prev, [logId]: !prev[logId] }));
  };

  const handleCopyPayloads = async (log: ExperimentFacebookApiLog) => {
    const content = buildCopyPayload(log);
    setCopyingLogId(log.id);
    try {
      await navigator.clipboard.writeText(content);
      setCopiedLogId(log.id);
      window.setTimeout(() => {
        setCopiedLogId((current) => (current === log.id ? null : current));
      }, 2000);
    } catch (error) {
      console.error("Não foi possível copiar request/response.", error);
    } finally {
      setCopyingLogId((current) => (current === log.id ? null : current));
    }
  };

  return (
    <div className="container-fluid">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
        <PageTitle
          title="Chamadas Meta Ads"
          subtitle={
            <span>
              Experimento{" "}
              <Link to={`/experiments/${experimentId}`}>#{experimentId}</Link>
            </span>
          }
        />
        <div className="d-flex flex-wrap gap-2">
          <div className="form-floating">
            <select
              id="limit"
              className="form-select"
              value={limit}
              onChange={(event) => setLimit(Number(event.target.value))}
            >
              {LIMIT_OPTIONS.map((value) => (
                <option key={value} value={value}>
                  Últimas {value}
                </option>
              ))}
            </select>
            <label htmlFor="limit">Quantidade</label>
          </div>
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            {isFetching ? "Atualizando..." : "Atualizar"}
          </button>
          <Link
            to={`/experiments/${experimentId}`}
            className="btn btn-outline-secondary"
          >
            Voltar
          </Link>
        </div>
      </div>

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body d-flex flex-wrap gap-4">
          <StatTile
            label="Chamadas registradas"
            value={stats.total.toLocaleString("pt-BR")}
          />
          <StatTile
            label="Com erro"
            value={stats.errors.toLocaleString("pt-BR")}
            variant={stats.errors > 0 ? "danger" : "success"}
          />
          <StatTile
            label="Última chamada"
            value={formatDateTime(stats.lastCall) ?? "—"}
          />
        </div>
      </div>

      <div className="alert alert-info" role="alert">
        <p className="mb-1">
          As chamadas abaixo são capturadas diretamente do Facebook Graph API
          sempre que o worker tenta descobrir públicos ou validar segmentos.
        </p>
        <ul className="mb-0 small ps-3">
          <li>Cada linha representa uma requisição HTTP enviada pelo worker.</li>
          <li>
            Use o botão de expandir para ver o payload enviado (request) e o
            retorno recebido (response), sempre com tokens ofuscados.
          </li>
          <li>
            Clique em "Detalhar job" para abrir o histórico completo daquela
            etapa do playbook.
          </li>
        </ul>
      </div>

      {isLoading ? (
        <p>Carregando histórico…</p>
      ) : !data?.length ? (
        <div className="text-center py-5 text-muted">
          <p className="mb-0">Nenhuma chamada registrada para este experimento.</p>
        </div>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th style={{ width: "4rem" }}>Log</th>
                  <th>Horário</th>
                  <th>Contexto</th>
                  <th>Job</th>
                  <th>Endpoint</th>
                  <th>Status</th>
                  <th>Erro</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {data?.map((log) => {
                  const isExpanded = expanded[log.id];
                  return (
                    <Fragment key={log.id}>
                      <tr>
                        <td>
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => toggleRow(log.id)}
                          >
                            {isExpanded ? "Ocultar" : "Detalhar"}
                          </button>
                        </td>
                        <td>
                          <div className="fw-semibold">
                            {formatDateTime(log.requestedAt) ?? formatDateTime(log.createdAt) ?? "—"}
                          </div>
                          <small className="text-muted">
                            {formatDuration(log.durationMs)}
                          </small>
                        </td>
                        <td>
                          <div className="fw-semibold">{formatContextLabel(log)}</div>
                          <div className="small text-muted">
                            {contextOrigin(log)}
                          </div>
                        </td>
                        <td>
                          <div className="fw-semibold">
                            {log.jobType ?? "—"}
                          </div>
                          <div className="small text-muted">
                            Worker: {log.jobWorker ?? "?"}
                          </div>
                        </td>
                        <td>
                          <code className="d-block text-truncate" style={{ maxWidth: 360 }}>
                            {log.endpoint ?? "—"}
                          </code>
                        </td>
                        <td>
                          <StatusBadge log={log} />
                          {log.statusCode ? (
                            <div className="small text-muted">HTTP {log.statusCode}</div>
                          ) : null}
                        </td>
                        <td className="text-wrap" style={{ minWidth: 180 }}>
                          <small className="text-danger">
                            {log.errorMessage ?? "—"}
                          </small>
                        </td>
                        <td>
                          {log.jobId ? (
                            <Link
                              to={`/experiments/${experimentId}/adset-workflow/jobs/${log.jobId}`}
                              className="btn btn-link btn-sm p-0"
                            >
                              Detalhar job
                            </Link>
                          ) : (
                            <span className="text-muted small">—</span>
                          )}
                        </td>
                      </tr>
                      {isExpanded ? (
                        <tr>
                          <td colSpan={8}>
                            <div className="bg-light rounded border p-3">
                              <div className="d-flex justify-content-end mb-2">
                                <button
                                  type="button"
                                  className="btn btn-sm btn-outline-primary"
                                  onClick={() => handleCopyPayloads(log)}
                                  disabled={copyingLogId === log.id}
                                >
                                  {copyingLogId === log.id ? (
                                    <>
                                      <span
                                        className="spinner-border spinner-border-sm me-2"
                                        aria-hidden="true"
                                      />
                                      Copiando...
                                    </>
                                  ) : copiedLogId === log.id ? (
                                    "Copiado!"
                                  ) : (
                                    "Copiar request/response"
                                  )}
                                </button>
                              </div>
                              <div className="facebook-api-log-expanded-grid">
                                <div className="facebook-api-log-expanded-card">
                                  <h6 className="h6">Request</h6>
                                  <PayloadViewer payload={log.requestPayload} />
                                </div>
                                <div className="facebook-api-log-expanded-card">
                                  <h6 className="h6">Response</h6>
                                  <PayloadViewer payload={log.responsePayload} />
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function StatTile({
  label,
  value,
  variant = "secondary",
}: {
  label: string;
  value: string;
  variant?: string;
}) {
  return (
    <div>
      <div className="text-uppercase small text-muted">{label}</div>
      <div className={`display-6 fw-bold text-${variant}`}>{value}</div>
    </div>
  );
}

function StatusBadge({ log }: { log: ExperimentFacebookApiLog }) {
  const variant = isErrorStatus(log)
    ? "danger"
    : log.statusCode
      ? "success"
      : "secondary";
  const label = log.statusCode ? (log.statusCode >= 400 ? "Erro" : "OK") : "Sem status";
  return <span className={`badge text-bg-${variant}`}>{label}</span>;
}

function isErrorStatus(log?: ExperimentFacebookApiLog | null) {
  if (!log) return false;
  if (log.statusCode && log.statusCode >= 400) return true;
  return Boolean(log.errorMessage);
}

const CONTEXT_LABELS: Record<string, string> = {
  CAMPAIGN_CREATION: "Campanha",
  CAMPAIGN_AD_SET: "Conjunto de anúncios",
  CAMPAIGN_AD_CREATIVE: "Criativo",
  CAMPAIGN_AD: "Anúncio",
  TARGETING_SIMPLE_FLOW: "Fluxo simples de público",
};

function formatContextLabel(log: ExperimentFacebookApiLog) {
  if (log.context) {
    return CONTEXT_LABELS[log.context] ?? log.context;
  }
  if (log.jobType) {
    return `Playbook · ${log.jobType}`;
  }
  return "—";
}

function contextOrigin(log: ExperimentFacebookApiLog) {
  if (log.context) {
    return log.context === "TARGETING_SIMPLE_FLOW" ? "Fluxo simples" : "Campanha";
  }
  if (log.jobType) {
    return "Playbook";
  }
  return "—";
}


function formatDateTime(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "medium",
  });
}

function formatDuration(durationMs?: number | null) {
  if (durationMs == null) return "—";
  if (durationMs < 1000) return `${durationMs} ms`;
  return `${(durationMs / 1000).toFixed(2)} s`;
}

function PayloadViewer({ payload }: { payload?: string | null }) {
  if (!payload) {
    return <p className="text-muted small mb-0">Sem conteúdo</p>;
  }
  let formatted = payload;
  try {
    const parsed = JSON.parse(payload);
    formatted = JSON.stringify(parsed, null, 2);
  } catch (error) {
    formatted = payload;
  }
  return (
    <pre className="bg-white border rounded p-3 small overflow-auto" style={{ maxHeight: 280 }}>
      {formatted}
    </pre>
  );
}

function buildCopyPayload(log: ExperimentFacebookApiLog) {
  const request = formatPayloadForCopy(log.requestPayload);
  const response = formatPayloadForCopy(log.responsePayload);
  return `Request:\n${request}\n\nResponse:\n${response}`;
}

function formatPayloadForCopy(payload?: string | null) {
  if (!payload) return "Sem conteúdo";
  try {
    const parsed = JSON.parse(payload);
    return JSON.stringify(parsed, null, 2);
  } catch (error) {
    return payload;
  }
}
