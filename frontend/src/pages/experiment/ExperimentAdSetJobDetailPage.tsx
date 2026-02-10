import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useExperimentAdSetJobDetail } from "../../api/experiment/useExperimentAdSetJobDetail";
function escapeShellSingleQuotes(value: string) {
  return value.replace(/'/g, "'\\''");
}
const FACEBOOK_GRAPH_BASE_URL = "https://graph.facebook.com";

function normalizeEndpoint(endpoint?: string | null, provider?: string | null) {
  if (!endpoint) return "$API_BASE_URL";
  if (/^https?:\/\//i.test(endpoint)) return endpoint;
  const normalizedPath = endpoint.startsWith("./")
    ? endpoint.slice(1)
    : endpoint;

  if (provider?.toUpperCase() === "FACEBOOK") {
    return normalizedPath.startsWith("/")
      ? `${FACEBOOK_GRAPH_BASE_URL}${normalizedPath}`
      : `${FACEBOOK_GRAPH_BASE_URL}/${normalizedPath}`;
  }
  return normalizedPath.startsWith("/")
    ? `$API_BASE_URL${normalizedPath}`
    : `$API_BASE_URL/${normalizedPath}`;
}
function buildCurlCommand(
  method?: string | null,
  endpoint?: string | null,
  provider?: string | null,
  payload?: string | null,
) {
  const normalizedMethod = (method ?? "GET").toUpperCase();
  const normalizedEndpoint = normalizeEndpoint(endpoint, provider);
  const commandParts = [
    "curl --request",
    normalizedMethod,
    `'${escapeShellSingleQuotes(normalizedEndpoint)}'`,
    "--header 'Content-Type: application/json'",
  ];
  if (payload) {
    commandParts.push(
      `--data-raw '${escapeShellSingleQuotes(formatJson(payload))}'`,
    );
  }
  return commandParts.join(" \\n");
}
function formatDateTime(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "medium",
  });
}
function formatJson(value?: string | null) {
  if (!value) return "Não disponível";
  try {
    const parsed = JSON.parse(value);
    return JSON.stringify(parsed, null, 2);
  } catch (err) {
    return value;
  }
}
export default function ExperimentAdSetJobDetailPage() {
  const { id: experimentId, jobId } = useParams();
  const { data, isLoading } = useExperimentAdSetJobDetail(experimentId, jobId);
  if (isLoading) {
    return <p>Carregando...</p>;
  }
  if (!data || !data.job) {
    return (
      <div className="container-fluid">
        <p>Job não encontrado.</p>
        <Link
          to={`/experiments/${experimentId}/adset-workflow`}
          className="btn btn-link"
        >
          Voltar para o playbook
        </Link>
      </div>
    );
  }
  const { job, apiLogs } = data;
  const backLink = `/experiments/${experimentId}/adset-workflow`;
  return (
    <div className="container-fluid">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <PageTitle
          title={`Job #${job.id}`}
          subtitle={
            <span>
              Experimento{" "}
              <Link to={`/experiments/${experimentId}`}>#{experimentId}</Link>
            </span>
          }
        />
        <Link to={backLink} className="btn btn-outline-secondary">
          Voltar
        </Link>
      </div>
      <div className="card mb-4">
        <div className="card-header">Resumo do job</div>
        <div className="card-body">
          <div className="row gy-2">
            <div className="col-12 col-md-4">
              <strong>Tipo:</strong> {job.type}
            </div>
            <div className="col-12 col-md-4">
              <strong>Worker:</strong> {job.worker}
            </div>
            <div className="col-12 col-md-4">
              <strong>Status:</strong> {job.status}
            </div>
            <div className="col-12 col-md-4">
              <strong>Início:</strong> {formatDateTime(job.startedAt)}
            </div>
            <div className="col-12 col-md-4">
              <strong>Término:</strong> {formatDateTime(job.finishedAt)}
            </div>
            <div className="col-12 col-md-4">
              <strong>Tentativas:</strong> {job.attemptCount ?? 0}
            </div>
            {job.errorMessage ? (
              <div className="col-12">
                <strong className="text-danger">Último erro:</strong>{" "}
                {job.errorMessage}
              </div>
            ) : null}
          </div>
        </div>
      </div>
      <div className="card">
        <div className="card-header">Chamadas ao Facebook</div>
        <div className="card-body">
          {!apiLogs?.length ? (
            <p className="text-muted mb-0">
              Nenhuma chamada registrada para este job.
            </p>
          ) : (
            apiLogs.map((log) => {
              const durationMs =
                log.requestedAt && log.respondedAt
                  ? Math.max(
                      0,
                      new Date(log.respondedAt).getTime() -
                        new Date(log.requestedAt).getTime(),
                    )
                  : null;
              return (
                <div key={log.id} className="mb-4 border rounded">
                  <div className="p-3 border-bottom bg-light d-flex flex-column flex-md-row justify-content-between gap-2">
                    <div>
                      <strong>{log.provider}</strong> · {log.httpMethod ?? "—"}{" "}
                      · {log.endpoint ?? "—"}
                    </div>
                    <div className="text-muted small">
                      Início: {formatDateTime(log.requestedAt)}
                      {log.respondedAt
                        ? ` · Fim: ${formatDateTime(log.respondedAt)}`
                        : null}
                      {durationMs != null ? ` · ${durationMs} ms` : null}
                    </div>
                  </div>
                  <div className="p-3">
                    <dl className="row mb-3 small">
                      <dt className="col-sm-3">Status HTTP</dt>
                      <dd className="col-sm-9">{log.statusCode ?? "—"}</dd>
                      <dt className="col-sm-3">Mensagem de erro</dt>
                      <dd className="col-sm-9">{log.errorMessage ?? "—"}</dd>
                    </dl>
                    <div className="row g-3">
                      <div className="col-12 col-lg-6">
                        <details open>
                          <summary>Payload enviado</summary>
                          <pre
                            className="bg-dark text-white p-2 mt-2 rounded"
                            style={{ whiteSpace: "pre-wrap" }}
                          >
                            {formatJson(log.requestPayload)}
                          </pre>
                        </details>
                      </div>
                      <div className="col-12 col-lg-6">
                        <details open>
                          <summary>Resposta recebida</summary>
                          <pre
                            className="bg-dark text-white p-2 mt-2 rounded"
                            style={{ whiteSpace: "pre-wrap" }}
                          >
                            {formatJson(log.responsePayload)}
                          </pre>
                        </details>
                      </div>
                      <div className="col-12">
                        <details>
                          <summary>Versão cURL (teste local)</summary>
                          <p className="small text-muted mt-2 mb-2">
                            {log.provider?.toUpperCase() === "FACEBOOK" ? (
                              <>
                                URL completa da Graph API do Facebook (ajuste o
                                domínio/versão se necessário).
                              </>
                            ) : (
                              <>
                                Defina <code>API_BASE_URL</code> antes de
                                executar, por exemplo:
                                <code className="ms-1">
                                  export API_BASE_URL=http://localhost:8000
                                </code>
                              </>
                            )}
                          </p>
                          <pre
                            className="bg-dark text-white p-2 rounded"
                            style={{ whiteSpace: "pre-wrap" }}
                          >
                            {buildCurlCommand(
                              log.httpMethod,
                              log.endpoint,
                              log.provider,
                              log.requestPayload,
                            )}
                          </pre>
                        </details>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
