import { Link, useParams } from "react-router-dom";
import { useOprmNichoCnaeV2JobDetail } from "../../api/oprm/useOprmNichoCnaeV2JobDetail";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { formatStage } from "./OprmNichoCnaeV2PipelinePage";

function formatDateTime(value: string | null | undefined) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function statusBadgeClass(status: string) {
  if (status === "COMPLETED") return "badge text-bg-success";
  if (status === "FAILED") return "badge text-bg-danger";
  if (status === "TECHNICAL_RETRY_SCHEDULED") return "badge text-bg-warning";
  if (status === "RUNNING") return "badge text-bg-primary";
  return "badge text-bg-secondary";
}

function parsePayload(payload: string | null | undefined) {
  if (!payload) return null;
  try {
    return JSON.parse(payload) as unknown;
  } catch {
    return null;
  }
}

function summarizePayload(payload: string | null | undefined) {
  if (!payload) return "Sem payload registrado.";
  const parsed = parsePayload(payload);
  if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
    const keys = Object.keys(parsed).slice(0, 6);
    if (keys.length === 0) return "Payload JSON vazio.";
    return `JSON registrado com campos: ${keys.join(", ")}${Object.keys(parsed).length > keys.length ? "..." : ""}.`;
  }
  if (Array.isArray(parsed))
    return `JSON registrado com ${parsed.length} item(ns).`;
  return "Conteúdo registrado em texto.";
}

function JsonValueViewer({
  name,
  value,
  level = 0,
}: {
  name?: string;
  value: unknown;
  level?: number;
}) {
  const isArray = Array.isArray(value);
  const isObject = value !== null && typeof value === "object" && !isArray;

  if (!isArray && !isObject) {
    return (
      <div className="font-monospace small">
        {name ? <span className="text-primary">{name}: </span> : null}
        <span>{JSON.stringify(value)}</span>
      </div>
    );
  }

  const entries = isArray
    ? value.map((item, index) => [String(index), item] as const)
    : Object.entries(value as Record<string, unknown>);
  const summary = isArray
    ? `${name ? `${name}: ` : ""}array com ${entries.length} item(ns)`
    : `${name ? `${name}: ` : ""}objeto com ${entries.length} campo(s)`;

  return (
    <details
      className="font-monospace small"
      style={{ marginLeft: level ? "1rem" : 0 }}
    >
      <summary className="json-tree-summary">{summary}</summary>
      <div className="border-start ps-2 ms-1 mt-1 d-flex flex-column gap-1">
        {entries.length === 0 ? (
          <span className="text-secondary">Sem campos internos.</span>
        ) : (
          entries.map(([entryName, entryValue]) => (
            <JsonValueViewer
              key={entryName}
              name={entryName}
              value={entryValue}
              level={level + 1}
            />
          ))
        )}
      </div>
    </details>
  );
}

function PayloadViewer({
  label,
  payload,
}: {
  label: string;
  payload: string | null | undefined;
}) {
  if (!payload) return <span>Sem payload registrado.</span>;

  const parsed = parsePayload(payload);
  const contentType = parsed === null ? "texto" : "JSON";

  return (
    <details className="border rounded bg-light-subtle p-2">
      <summary className="fw-semibold">
        {summarizePayload(payload)} Clique para ver o conteúdo {contentType} de{" "}
        {label}.
      </summary>
      <div
        className="mt-2 mb-0 bg-white border rounded p-3 overflow-auto"
        style={{ maxHeight: "26rem", whiteSpace: "pre-wrap" }}
      >
        {parsed === null ? (
          <pre className="mb-0 small">{payload}</pre>
        ) : (
          <JsonValueViewer value={parsed} />
        )}
      </div>
    </details>
  );
}

export default function OprmNichoCnaeV2JobDetailPage() {
  const { cnaeCode, jobId } = useParams();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : undefined;
  const decodedJobId = jobId ? decodeURIComponent(jobId) : undefined;
  const jobQuery = useOprmNichoCnaeV2JobDetail(decodedJobId);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <PageTitle>Relatório do job NichoCNAE v2</PageTitle>
            <p className="text-secondary mb-0">
              Mostra cada etapa persistida pelo backend e o que aconteceu até o
              ponto de fracasso ou conclusão.
            </p>
          </div>
          <Link
            className="btn btn-outline-secondary"
            to={
              decodedCnaeCode
                ? `/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}/pipeline-v2`
                : "/oprm"
            }
          >
            Voltar para jobs
          </Link>
        </div>
      </header>

      <OprmModuleNavigation />

      {jobQuery.isLoading ? (
        <div className="card border-0 shadow-sm">
          <div className="card-body text-secondary">
            Carregando relatório...
          </div>
        </div>
      ) : jobQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          {jobQuery.error.message}
        </div>
      ) : jobQuery.data ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex flex-wrap justify-content-between gap-3">
                <div>
                  <span className="text-secondary small text-uppercase fw-semibold">
                    CNAE {jobQuery.data.cnaeCode}
                  </span>
                  <h2 className="h5 mb-1">{jobQuery.data.jobId}</h2>
                  <p className="mb-0">
                    {jobQuery.data.outcomeMessage ??
                      jobQuery.data.finalDecisionReason ??
                      "Job sem mensagem final registrada."}
                  </p>
                </div>
                <span
                  className={
                    jobQuery.data.outcomeStatus === "FAILURE"
                      ? "badge text-bg-danger align-self-start"
                      : jobQuery.data.outcomeStatus === "SUCCESS"
                        ? "badge text-bg-success align-self-start"
                        : "badge text-bg-warning align-self-start"
                  }
                >
                  {jobQuery.data.finalDecisionLabel ??
                    jobQuery.data.outcomeStatus ??
                    jobQuery.data.status}
                </span>
              </div>
            </div>
          </section>

          <section
            className="d-flex flex-column gap-3"
            aria-label="Etapas executadas do job"
          >
            {jobQuery.data.stages.map((stage, index) => (
              <article
                className="card border-0 shadow-sm"
                key={stage.stageExecutionId}
              >
                <div className="card-body">
                  <div className="d-flex flex-wrap justify-content-between gap-3 mb-3">
                    <div>
                      <span className="badge text-bg-light border mb-2">
                        Etapa {index + 1}
                      </span>
                      <h3 className="h5 mb-1">
                        {formatStage(stage.stageCode)}
                      </h3>
                      <p className="text-secondary small mb-0">
                        Execução {stage.stageExecutionId} · tentativa{" "}
                        {stage.attemptNumber ?? "—"}
                        {stage.technicalRetryNumber
                          ? ` · retry ${stage.technicalRetryNumber}`
                          : ""}
                      </p>
                    </div>
                    <span className={statusBadgeClass(stage.status)}>
                      {stage.status}
                    </span>
                  </div>

                  <dl className="row small mb-0 g-2">
                    <dt className="col-md-3 text-secondary fw-normal">
                      O que entrou
                    </dt>
                    <dd className="col-md-9 mb-0">
                      <PayloadViewer
                        label="entrada"
                        payload={stage.inputPayload}
                      />
                    </dd>
                    <dt className="col-md-3 text-secondary fw-normal">
                      O que foi feito
                    </dt>
                    <dd className="col-md-9 mb-0">
                      <PayloadViewer
                        label="saída"
                        payload={stage.outputPayload}
                      />
                    </dd>
                    <dt className="col-md-3 text-secondary fw-normal">
                      Falha registrada
                    </dt>
                    <dd className="col-md-9 mb-0">
                      {stage.errorMessage ??
                        stage.failureType ??
                        "Sem falha registrada nesta etapa."}
                    </dd>
                    <dt className="col-md-3 text-secondary fw-normal">
                      Próxima etapa
                    </dt>
                    <dd className="col-md-9 mb-0">
                      {formatStage(stage.nextStageCode)}
                    </dd>
                    <dt className="col-md-3 text-secondary fw-normal">
                      Atualizado
                    </dt>
                    <dd className="col-md-9 mb-0">
                      {formatDateTime(stage.updatedAt)}
                    </dd>
                  </dl>
                </div>
              </article>
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}
