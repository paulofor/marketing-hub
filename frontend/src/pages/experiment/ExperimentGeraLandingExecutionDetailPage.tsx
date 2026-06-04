import { Link, useParams, useSearchParams } from "react-router-dom";
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

export interface QualityReviewSentScreenshot {
  src: string;
  label: string;
}

function isDisplayableImageReference(value: string) {
  const trimmed = value.trim();
  return (
    /^data:image\/[a-zA-Z0-9.+-]+;base64,/.test(trimmed) ||
    /^https?:\/\//i.test(trimmed)
  );
}

function imageUrlFromUnknown(value: unknown): string | null {
  if (typeof value === "string" && isDisplayableImageReference(value)) {
    return value.trim();
  }

  if (value && typeof value === "object" && !Array.isArray(value)) {
    const record = value as Record<string, unknown>;
    return imageUrlFromUnknown(record.url ?? record.href ?? record.src);
  }

  return null;
}

function labelFromImageNode(
  node: Record<string, unknown>,
  fallbackIndex: number,
) {
  const labelCandidates = [
    node.label,
    node.name,
    node.title,
    node.viewport,
    node.device,
    node.detail,
  ];
  const label = labelCandidates.find(
    (candidate): candidate is string =>
      typeof candidate === "string" && candidate.trim().length > 0,
  );
  return label ? label.trim() : `Screenshot ${fallbackIndex}`;
}

export function extractQualityReviewSentScreenshots(
  raw?: string | null,
): QualityReviewSentScreenshot[] {
  if (!raw?.trim()) return [];

  const screenshots: QualityReviewSentScreenshot[] = [];
  const seen = new Set<string>();

  const addScreenshot = (src: string, label?: string) => {
    if (seen.has(src)) return;
    seen.add(src);
    screenshots.push({
      src,
      label: label?.trim() || `Screenshot ${screenshots.length + 1}`,
    });
  };

  const visit = (value: unknown, keyHint?: string) => {
    if (typeof value === "string") {
      if (isDisplayableImageReference(value)) {
        addScreenshot(value.trim(), keyHint);
      }
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => visit(item, keyHint));
      return;
    }

    if (!value || typeof value !== "object") return;

    const record = value as Record<string, unknown>;
    const type =
      typeof record.type === "string" ? record.type.toLowerCase() : "";
    const nodeLooksLikeImage =
      type.includes("image") ||
      Object.keys(record).some((key) => /image|screenshot/i.test(key));

    const directImage =
      imageUrlFromUnknown(record.image_url) ??
      imageUrlFromUnknown(record.imageUrl) ??
      imageUrlFromUnknown(record.screenshotUrl) ??
      imageUrlFromUnknown(record.screenshot_url) ??
      (nodeLooksLikeImage
        ? imageUrlFromUnknown(record.url ?? record.src)
        : null);

    if (directImage) {
      addScreenshot(
        directImage,
        labelFromImageNode(record, screenshots.length + 1),
      );
    }

    Object.entries(record).forEach(([key, child]) => visit(child, key));
  };

  try {
    visit(JSON.parse(raw));
  } catch {
    const dataImageMatches = raw.match(
      /data:image\/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\r\n_-]+/g,
    );
    dataImageMatches?.forEach((match) =>
      addScreenshot(match.replace(/\s/g, "")),
    );
  }

  return screenshots;
}

function parseQualityReviewAudit(raw?: string | null) {
  if (!raw?.trim()) return null;
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function auditText(value: unknown) {
  return typeof value === "string" && value.trim() ? value : "—";
}

function auditNumber(value: unknown) {
  return typeof value === "number" ? value.toLocaleString("pt-BR") : "—";
}

function auditBoolean(value: unknown) {
  return value === true ? "Sim" : value === false ? "Não" : "—";
}

function auditScreenshots(value: unknown) {
  return Array.isArray(value)
    ? (value.filter(
        (item): item is Record<string, unknown> =>
          Boolean(item) && typeof item === "object" && !Array.isArray(item),
      ) as Record<string, unknown>[])
    : [];
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
  const [searchParams] = useSearchParams();
  const stageCode =
    searchParams.get("stageCode")?.trim() || "landing-page-wireframe";
  const detailQuery = useGeraLandingStageExecutionDetail(
    experimentId,
    jobId,
    stageCode,
  );
  const modelUsed = extractModelFromRequestBody(
    detailQuery.data?.openAiRequestBody,
  );
  const provisionalHtml = detailQuery.data?.provisionalHtml?.trim() ?? "";
  const errorFileContent = extractErrorFileContent(
    detailQuery.data?.errorDetail,
  );
  const isQualityReviewStage =
    (detailQuery.data?.stageCode ?? stageCode) ===
    "landing-page-quality-review";
  const qualityReviewSentScreenshots = isQualityReviewStage
    ? extractQualityReviewSentScreenshots(detailQuery.data?.openAiRequestBody)
    : [];
  const qualityReviewAudit = isQualityReviewStage
    ? parseQualityReviewAudit(detailQuery.data?.qualityReviewAudit)
    : null;
  const qualityReviewAuditScreenshots = auditScreenshots(
    qualityReviewAudit?.screenshots,
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
              <span className="badge text-bg-light border border-secondary-subtle text-dark ms-3 align-middle fw-semibold">
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

              {isQualityReviewStage && qualityReviewAudit ? (
                <div className="border rounded p-3 bg-light-subtle">
                  <div className="d-flex align-items-center justify-content-between gap-2 mb-2 flex-wrap">
                    <div>
                      <h6 className="mb-1">Auditoria da evidência visual</h6>
                      <p className="text-muted small mb-0">
                        Hashes e alertas usados para detectar reuso de HTML,
                        screenshots repetidos e decisões divergentes.
                      </p>
                    </div>
                    <span className="badge text-bg-light border border-secondary-subtle text-dark">
                      {auditText(qualityReviewAudit.auditSchemaVersion)}
                    </span>
                  </div>
                  {qualityReviewAudit.contradictoryDecisionDetected === true ? (
                    <div className="alert alert-warning py-2 mb-3" role="alert">
                      <strong>Decisão contraditória detectada:</strong>{" "}
                      {auditText(qualityReviewAudit.auditWarning)}
                    </div>
                  ) : qualityReviewAudit.evidenceReuseDetected === true ? (
                    <div className="alert alert-info py-2 mb-3" role="alert">
                      <strong>Evidência visual reutilizada:</strong> esta
                      execução avaliou os mesmos hashes de HTML/screenshots de
                      outro job.
                    </div>
                  ) : null}
                  <div className="row g-2 small">
                    <div className="col-md-6">
                      <strong>HTML SHA-256:</strong>{" "}
                      <code>
                        {auditText(qualityReviewAudit.landingHtmlSha256)}
                      </code>
                    </div>
                    <div className="col-md-6">
                      <strong>Tamanho HTML:</strong>{" "}
                      {auditNumber(qualityReviewAudit.landingHtmlLength)}{" "}
                      caracteres
                    </div>
                    <div className="col-md-6">
                      <strong>Prompt SHA-256:</strong>{" "}
                      <code>{auditText(qualityReviewAudit.promptSha256)}</code>
                    </div>
                    <div className="col-md-6">
                      <strong>Request SHA-256:</strong>{" "}
                      <code>
                        {auditText(qualityReviewAudit.openAiRequestBodySha256)}
                      </code>
                    </div>
                    <div className="col-md-6">
                      <strong>Modelo de visão:</strong>{" "}
                      {auditText(qualityReviewAudit.visionModel)}
                    </div>
                    <div className="col-md-6">
                      <strong>Detalhe da imagem:</strong>{" "}
                      {auditText(qualityReviewAudit.imageDetail)}
                    </div>
                    <div className="col-md-6">
                      <strong>Reuso detectado:</strong>{" "}
                      {auditBoolean(qualityReviewAudit.evidenceReuseDetected)}
                    </div>
                    <div className="col-md-6">
                      <strong>Decisão contraditória:</strong>{" "}
                      {auditBoolean(
                        qualityReviewAudit.contradictoryDecisionDetected,
                      )}
                    </div>
                    {qualityReviewAudit.reusedEvidenceFromJobId ? (
                      <div className="col-md-6">
                        <strong>Job com mesma evidência:</strong>{" "}
                        <code>
                          {auditText(
                            qualityReviewAudit.reusedEvidenceFromJobId,
                          )}
                        </code>
                      </div>
                    ) : null}
                    {qualityReviewAudit.reusedEvidencePreviousApprovalRecommendation ? (
                      <div className="col-md-6">
                        <strong>Decisão anterior:</strong>{" "}
                        {auditText(
                          qualityReviewAudit.reusedEvidencePreviousApprovalRecommendation,
                        )}
                      </div>
                    ) : null}
                  </div>
                  {qualityReviewAuditScreenshots.length > 0 ? (
                    <div className="table-responsive mt-3">
                      <table className="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th>Viewport</th>
                            <th>Bytes</th>
                            <th>SHA-256</th>
                            <th>URL</th>
                          </tr>
                        </thead>
                        <tbody>
                          {qualityReviewAuditScreenshots.map((item, index) => (
                            <tr key={`${auditText(item.viewport)}-${index}`}>
                              <td>{auditText(item.viewport)}</td>
                              <td>{auditNumber(item.bytes)}</td>
                              <td>
                                <code>{auditText(item.sha256)}</code>
                              </td>
                              <td>
                                {typeof item.publicUrl === "string" ? (
                                  <a
                                    href={item.publicUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                  >
                                    Abrir
                                  </a>
                                ) : (
                                  "—"
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : null}
                </div>
              ) : null}

              {isQualityReviewStage ? (
                <div>
                  <div className="d-flex align-items-center justify-content-between gap-2 mb-2 flex-wrap">
                    <div>
                      <h6 className="mb-1">Screenshots das imagens enviadas</h6>
                      <p className="text-muted small mb-0">
                        Imagens anexadas ao request visual enviado para o
                        Quality Gate.
                      </p>
                    </div>
                    <span className="badge text-bg-light border border-secondary-subtle text-dark">
                      {qualityReviewSentScreenshots.length} imagem
                      {qualityReviewSentScreenshots.length === 1 ? "" : "s"}
                    </span>
                  </div>
                  {qualityReviewSentScreenshots.length > 0 ? (
                    <div className="row g-3">
                      {qualityReviewSentScreenshots.map((screenshot, index) => (
                        <div className="col-12 col-xl-6" key={screenshot.src}>
                          <div className="border rounded bg-light p-2 h-100">
                            <div className="d-flex justify-content-between align-items-center gap-2 mb-2">
                              <strong className="small">
                                {screenshot.label || `Screenshot ${index + 1}`}
                              </strong>
                              <a
                                href={screenshot.src}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="btn btn-sm btn-outline-secondary"
                              >
                                Abrir imagem
                              </a>
                            </div>
                            <img
                              src={screenshot.src}
                              alt={`Screenshot enviado ao Quality Gate ${index + 1}`}
                              className="img-fluid rounded border bg-white d-block mx-auto"
                              style={{
                                maxHeight: "520px",
                                objectFit: "contain",
                              }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-muted mb-0">
                      Nenhuma imagem enviada foi encontrada no request visual
                      desta execução.
                    </p>
                  )}
                </div>
              ) : null}

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
