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
  const normalizedPayload = payload?.trim();
  const commandParts = [
    `curl --request ${normalizedMethod}`,
    `  '${escapeShellSingleQuotes(normalizedEndpoint)}'`,
    "  --header 'Content-Type: application/json'",
  ];
  if (
    normalizedPayload &&
    normalizedPayload !== "null" &&
    normalizedPayload !== "undefined"
  ) {
    commandParts.push(
      `  --data-raw '${escapeShellSingleQuotes(normalizedPayload)}'`,
    );
  }
  return commandParts
    .map((part, index) =>
      index === commandParts.length - 1 ? part : `${part} \\`,
    )
    .join("\n");
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
  const { job, apiLogs, payload, resultPayload } = data;
  const backLink = `/experiments/${experimentId}/adset-workflow`;
  const isFacebookJob = (job.worker ?? "").toUpperCase() === "FACEBOOK";
  const requestSectionTitle = isFacebookJob
    ? "Chamadas ao Facebook"
    : "Chamadas do AI Worker (ChatGPT batch)";
  const emptyLogMessage = isFacebookJob
    ? "Nenhuma chamada registrada para este job."
    : "Chamadas ao ChatGPT em modo batch serão exibidas aqui assim que o worker registrar os payloads.";
  const payloadAccordionId = `job-${job.id}-payloads`;
  const requestsAccordionId = `job-${job.id}-requests`;
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
      <div className="card mb-4">
        <div className="card-header">Payloads persistidos</div>
        <div className="card-body">
          {payload || resultPayload ? (
            <div className="accordion" id={payloadAccordionId}>
              {payload ? (
                <div className="accordion-item">
                  <h2 className="accordion-header" id="payload-heading">
                    <button
                      className="accordion-button collapsed"
                      type="button"
                      data-bs-toggle="collapse"
                      data-bs-target="#payload-collapse"
                      aria-expanded="false"
                      aria-controls="payload-collapse"
                    >
                      Payload enviado ao worker
                    </button>
                  </h2>
                  <div
                    id="payload-collapse"
                    className="accordion-collapse collapse"
                    aria-labelledby="payload-heading"
                    data-bs-parent={`#${payloadAccordionId}`}
                  >
                    <div className="accordion-body">
                      <pre
                        className="bg-light p-2 mb-0 rounded"
                        style={{ whiteSpace: "pre-wrap" }}
                      >
                        {formatJson(payload)}
                      </pre>
                    </div>
                  </div>
                </div>
              ) : null}
              {resultPayload ? (
                <div className="accordion-item">
                  <h2 className="accordion-header" id="result-heading">
                    <button
                      className="accordion-button collapsed"
                      type="button"
                      data-bs-toggle="collapse"
                      data-bs-target="#result-collapse"
                      aria-expanded="false"
                      aria-controls="result-collapse"
                    >
                      Resultado registrado
                    </button>
                  </h2>
                  <div
                    id="result-collapse"
                    className="accordion-collapse collapse"
                    aria-labelledby="result-heading"
                    data-bs-parent={`#${payloadAccordionId}`}
                  >
                    <div className="accordion-body">
                      <pre
                        className="bg-light p-2 mb-0 rounded"
                        style={{ whiteSpace: "pre-wrap" }}
                      >
                        {formatJson(resultPayload)}
                      </pre>
                    </div>
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}
          {!payload && !resultPayload ? (
            <p className="text-muted mb-0">
              Nenhum payload disponível para este job.
            </p>
          ) : null}
        </div>
      </div>
      <div className="card">
        <div className="card-header">{requestSectionTitle}</div>
        <div className="card-body">
          {!apiLogs?.length ? (
            <p className="text-muted mb-0">{emptyLogMessage}</p>
          ) : (
            <div className="accordion" id={requestsAccordionId}>
              {apiLogs.map((log) => {
                const durationMs =
                  log.requestedAt && log.respondedAt
                    ? Math.max(
                        0,
                        new Date(log.respondedAt).getTime() -
                          new Date(log.requestedAt).getTime(),
                      )
                    : null;
                const requestHeaderId = `request-heading-${log.id}`;
                const requestCollapseId = `request-collapse-${log.id}`;
                return (
                  <div key={log.id} className="accordion-item">
                    <h2 className="accordion-header" id={requestHeaderId}>
                      <button
                        className="accordion-button collapsed"
                        type="button"
                        data-bs-toggle="collapse"
                        data-bs-target={`#${requestCollapseId}`}
                        aria-expanded="false"
                        aria-controls={requestCollapseId}
                      >
                        <span>
                          <strong>{log.provider}</strong> ·{" "}
                          {log.httpMethod ?? "—"} · {log.endpoint ?? "—"}
                        </span>
                      </button>
                    </h2>
                    <div
                      id={requestCollapseId}
                      className="accordion-collapse collapse"
                      aria-labelledby={requestHeaderId}
                      data-bs-parent={`#${requestsAccordionId}`}
                    >
                      <div className="accordion-body">
                        <div className="text-muted small mb-3">
                          Início: {formatDateTime(log.requestedAt)}
                          {log.respondedAt
                            ? ` · Fim: ${formatDateTime(log.respondedAt)}`
                            : null}
                          {durationMs != null ? ` · ${durationMs} ms` : null}
                        </div>
                        <dl className="row mb-3 small">
                          <dt className="col-sm-3">Status HTTP</dt>
                          <dd className="col-sm-9">{log.statusCode ?? "—"}</dd>
                          <dt className="col-sm-3">Mensagem de erro</dt>
                          <dd className="col-sm-9">
                            {log.errorMessage ?? "—"}
                          </dd>
                        </dl>
                        <div className="row g-3">
                          <div className="col-12 col-lg-6">
                            <details>
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
                            <details>
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
                                    URL completa da Graph API do Facebook
                                    (ajuste o domínio/versão se necessário).
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
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
