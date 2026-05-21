import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { useGeraLandingStageExecutionDetail } from "../../api/experiment/useGeraLandingStageExecutions";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import MarkdownContentViewer from "../../components/MarkdownContentViewer";
import { useState } from "react";

function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function extractModelFromRequestBody(raw?: string) {
  if (!raw) return "—";
  try {
    const parsed = JSON.parse(raw);
    return typeof parsed?.model === "string" && parsed.model.trim()
      ? parsed.model
      : "—";
  } catch {
    return "—";
  }
}

function extractErrorFileContent(raw?: string) {
  if (!raw) return undefined;

  const marker = "error_file=";
  const markerIndex = raw.indexOf(marker);
  if (markerIndex < 0) return raw;

  const jsonSlice = raw.slice(markerIndex + marker.length).trim();
  const openingBraceIndex = jsonSlice.indexOf("{");
  if (openingBraceIndex < 0) return raw;

  let depth = 0;
  let inString = false;
  let isEscaped = false;

  for (let i = openingBraceIndex; i < jsonSlice.length; i += 1) {
    const char = jsonSlice[i];

    if (inString) {
      if (isEscaped) {
        isEscaped = false;
      } else if (char === "\\") {
        isEscaped = true;
      } else if (char === '"') {
        inString = false;
      }
      continue;
    }

    if (char === '"') {
      inString = true;
      continue;
    }

    if (char === "{") {
      depth += 1;
      continue;
    }

    if (char === "}") {
      depth -= 1;
      if (depth === 0) {
        return jsonSlice.slice(openingBraceIndex, i + 1);
      }
    }
  }

  return raw;
}

export default function ExperimentGeraLandingExecutionDetailPage() {
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const { id: experimentId, jobId } = useParams();
  const detailQuery = useGeraLandingStageExecutionDetail(experimentId, jobId);
  const modelUsed = extractModelFromRequestBody(
    detailQuery.data?.openAiRequestBody,
  );
  const provisionalHtml = detailQuery.data?.provisionalHtml?.trim() ?? "";
  const errorFileContent = extractErrorFileContent(
    detailQuery.data?.errorDetail,
  );

  const buildJsonDownloadProps = (fieldName: string, value?: string | null) => {
    if (!value) return null;

    const fileName = `${fieldName}-${detailQuery.data?.idJob ?? "geralanding"}.json`;
    return {
      href: `data:application/json;charset=utf-8,${encodeURIComponent(value)}`,
      fileName,
    };
  };

  const buildHtmlDownloadProps = (fieldName: string, value?: string | null) => {
    if (!value) return null;
    const fileName = `${fieldName}-${detailQuery.data?.idJob ?? "geralanding"}.html`;
    return {
      href: `data:text/html;charset=utf-8,${encodeURIComponent(value)}`,
      fileName,
    };
  };

  const handleCopyJson = async (label: string, value?: string | null) => {
    if (!value) return;

    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(value);
      } else {
        const textArea = document.createElement("textarea");
        textArea.value = value;
        textArea.style.position = "fixed";
        textArea.style.opacity = "0";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        document.execCommand("copy");
        document.body.removeChild(textArea);
      }
      setCopiedField(label);
      window.setTimeout(
        () => setCopiedField((current) => (current === label ? null : current)),
        2000,
      );
    } catch {
      setCopiedField(null);
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb mb-0">
          <li className="breadcrumb-item">
            <Link to={`/experiments/${experimentId}`}>Experimento</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Detalhe da execução
          </li>
        </ol>
      </nav>

      <div className="d-flex justify-content-between align-items-start">
        <div>
          <PageTitle icon={experimentIcon}>
            Detalhe da execução Gera Landing
            {detailQuery.data?.stageCode ? (
              <span className="badge bg-primary text-white ms-3 align-middle fw-semibold">
                {detailQuery.data.stageCode}
              </span>
            ) : null}
          </PageTitle>
          <p className="text-muted mb-0">
            Visualização completa do registro da tabela
            gera_landing_stage_execution.
          </p>
        </div>
        <Link
          to={`/experiments/${experimentId}`}
          className="btn btn-outline-secondary"
        >
          Voltar
        </Link>
      </div>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          {detailQuery.isLoading ? (
            <p className="text-muted mb-0">
              Carregando detalhes da execução...
            </p>
          ) : detailQuery.isError || !detailQuery.data ? (
            <p className="text-danger mb-0">
              Não foi possível carregar os detalhes da execução.
            </p>
          ) : (
            <div className="d-flex flex-column gap-3">
              {detailQuery.data.errorMessage ? (
                <div className="alert alert-danger mb-0" role="alert">
                  <strong>Motivo da falha:</strong>{" "}
                  {detailQuery.data.errorMessage}
                </div>
              ) : detailQuery.data.status === "FALHA" ? (
                <div className="alert alert-warning mb-0" role="alert">
                  <strong>Motivo da falha:</strong> não informado pelo Worker
                  AI.
                </div>
              ) : null}
              {errorFileContent ? (
                <div className="alert alert-secondary mb-0" role="alert">
                  <div className="d-flex align-items-center gap-2 flex-wrap">
                    <strong className="mb-0">Detalhe técnico do erro:</strong>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-secondary"
                      onClick={() =>
                        handleCopyJson("errorDetail", errorFileContent)
                      }
                    >
                      {copiedField === "errorDetail"
                        ? "Copiado!"
                        : "Copiar JSON"}
                    </button>
                  </div>
                  <div className="mt-2">
                    <CollapsibleJsonViewer
                      content={errorFileContent}
                      initiallyCollapsed
                    />
                  </div>
                </div>
              ) : null}

              <div className="row g-3 small">
                <div className="col-md-6 d-flex align-items-center gap-2 flex-wrap">
                  <strong className="mb-0">Job ID:</strong>
                  <span>{detailQuery.data.idJob}</span>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson("idJob", detailQuery.data.idJob)
                    }
                    aria-label="Copiar Job ID"
                    title="Copiar Job ID"
                  >
                    {copiedField === "idJob" ? (
                      "Copiado!"
                    ) : (
                      <i className="bi bi-copy" aria-hidden="true" />
                    )}
                  </button>
                </div>
                <div className="col-md-6">
                  <strong>Status:</strong> {detailQuery.data.status}
                </div>
                <div className="col-md-6">
                  <strong>Stage:</strong> {detailQuery.data.stageCode}
                </div>
                <div className="col-md-6">
                  <strong>OpenAI Job ID:</strong>{" "}
                  {detailQuery.data.openAiJobId ?? "—"}
                </div>
                <div className="col-md-6">
                  <strong>Modelo usado:</strong> {modelUsed}
                </div>
                <div className="col-md-6">
                  <strong>Criado em:</strong>{" "}
                  {formatDateTime(detailQuery.data.createdAt)}
                </div>
                <div className="col-md-6">
                  <strong>Concluído em:</strong>{" "}
                  {formatDateTime(detailQuery.data.completedAt)}
                </div>
                <div className="col-md-6">
                  <strong>Solicitado em:</strong>{" "}
                  {formatDateTime(detailQuery.data.executionRequestedAt)}
                </div>
                <div className="col-md-6">
                  <strong>Input tokens:</strong>{" "}
                  {detailQuery.data.inputTokens ?? "—"}
                </div>
                <div className="col-md-6">
                  <strong>Processamento iniciado:</strong>{" "}
                  {formatDateTime(detailQuery.data.processingStartedAt)}
                </div>
                <div className="col-md-6">
                  <strong>Output tokens:</strong>{" "}
                  {detailQuery.data.outputTokens ?? "—"}
                </div>
                <div className="col-md-6">
                  <strong>Prompt template ID:</strong>{" "}
                  {detailQuery.data.promptTemplateId ?? "—"}
                </div>
                <div className="col-md-6">
                  <strong>Custo USD:</strong> {detailQuery.data.costUsd ?? "—"}
                </div>
              </div>

              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <h6 className="mb-0">Prompt content</h6>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson(
                        "promptContent",
                        detailQuery.data.promptContent,
                      )
                    }
                  >
                    {copiedField === "promptContent"
                      ? "Copiado!"
                      : "Copiar JSON"}
                  </button>
                  {(() => {
                    const download = buildJsonDownloadProps(
                      "prompt-content",
                      detailQuery.data.promptContent,
                    );
                    if (!download) return null;
                    return (
                      <a
                        href={download.href}
                        download={download.fileName}
                        className="btn btn-sm btn-outline-secondary"
                      >
                        Baixar JSON
                      </a>
                    );
                  })()}
                </div>
                <CollapsibleJsonViewer
                  content={detailQuery.data.promptContent}
                  parseAsJson={false}
                />
              </div>
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <h6 className="mb-0">Prompt</h6>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson("prompt", detailQuery.data.prompt)
                    }
                  >
                    {copiedField === "prompt" ? "Copiado!" : "Copiar JSON"}
                  </button>
                  {(() => {
                    const download = buildJsonDownloadProps(
                      "prompt",
                      detailQuery.data.prompt,
                    );
                    if (!download) return null;
                    return (
                      <a
                        href={download.href}
                        download={download.fileName}
                        className="btn btn-sm btn-outline-secondary"
                      >
                        Baixar JSON
                      </a>
                    );
                  })()}
                </div>
                <CollapsibleJsonViewer
                  content={detailQuery.data.prompt}
                  parseAsJson={false}
                />
              </div>
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <h6 className="mb-0">
                    OpenAI request body (prompt cru enviado)
                  </h6>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson(
                        "openAiRequestBody",
                        detailQuery.data.openAiRequestBody,
                      )
                    }
                  >
                    {copiedField === "openAiRequestBody"
                      ? "Copiado!"
                      : "Copiar JSON"}
                  </button>
                  {(() => {
                    const download = buildJsonDownloadProps(
                      "openai-request-body",
                      detailQuery.data.openAiRequestBody,
                    );
                    if (!download) return null;
                    return (
                      <a
                        href={download.href}
                        download={download.fileName}
                        className="btn btn-sm btn-outline-secondary"
                      >
                        Baixar JSON
                      </a>
                    );
                  })()}
                </div>
                <CollapsibleJsonViewer
                  content={detailQuery.data.openAiRequestBody}
                />
              </div>
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <h6 className="mb-0">Schema JSON enviado para o modelo</h6>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson("schemaJson", detailQuery.data.schemaJson)
                    }
                  >
                    {copiedField === "schemaJson" ? "Copiado!" : "Copiar JSON"}
                  </button>
                  {(() => {
                    const download = buildJsonDownloadProps(
                      "schema-json",
                      detailQuery.data.schemaJson,
                    );
                    if (!download) return null;
                    return (
                      <a
                        href={download.href}
                        download={download.fileName}
                        className="btn btn-sm btn-outline-secondary"
                      >
                        Baixar JSON
                      </a>
                    );
                  })()}
                </div>
                <CollapsibleJsonViewer content={detailQuery.data.schemaJson} />
              </div>
              <div>
                <h6>Conteúdo do arquivo .md usado no prompt</h6>
                <MarkdownContentViewer
                  content={detailQuery.data.promptMarkdownContent}
                />
              </div>
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <h6 className="mb-0">Model response</h6>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson(
                        "modelResponse",
                        detailQuery.data.modelResponse,
                      )
                    }
                  >
                    {copiedField === "modelResponse"
                      ? "Copiado!"
                      : "Copiar JSON"}
                  </button>
                  {(() => {
                    const download = buildJsonDownloadProps(
                      "model-response",
                      detailQuery.data.modelResponse,
                    );
                    if (!download) return null;
                    return (
                      <a
                        href={download.href}
                        download={download.fileName}
                        className="btn btn-sm btn-outline-secondary"
                      >
                        Baixar JSON
                      </a>
                    );
                  })()}
                </div>
                <CollapsibleJsonViewer
                  content={detailQuery.data.modelResponse}
                />
              </div>
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <h6 className="mb-0">HTML provisório</h6>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() =>
                      handleCopyJson("provisionalHtml", provisionalHtml)
                    }
                  >
                    {copiedField === "provisionalHtml"
                      ? "Copiado!"
                      : "Copiar HTML"}
                  </button>
                  {(() => {
                    const download = buildHtmlDownloadProps(
                      "provisional-html",
                      provisionalHtml,
                    );
                    if (!download) return null;
                    return (
                      <a
                        href={download.href}
                        download={download.fileName}
                        className="btn btn-sm btn-outline-secondary"
                      >
                        Baixar HTML
                      </a>
                    );
                  })()}
                </div>
                {provisionalHtml ? (
                  <div className="d-flex flex-column gap-2">
                    <Link
                      to={`/experiments/${experimentId}/geralanding/stage-executions/${detailQuery.data.idJob}/provisional-html`}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Abrir HTML provisório em nova aba
                    </Link>
                    <pre
                      className="border rounded bg-light p-3 mb-0 small overflow-auto"
                      style={{ maxHeight: "320px" }}
                    >
                      <code>{provisionalHtml}</code>
                    </pre>
                  </div>
                ) : (
                  <p className="text-muted mb-0">
                    Nenhum HTML provisório disponível para este registro.
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
