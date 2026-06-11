import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import { useHypothesisPainStageExecutionDetail } from "../../api/hypothesis/useHypothesisPainStageExecutionDetail";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";

function formatDate(value?: string) {
  if (!value) return "—";
  return new Date(value).toLocaleString("pt-BR");
}

function formatCost(value?: number) {
  return value != null ? `$${value}` : "—";
}

function formatFileName(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function isJsonContent(value: string) {
  try {
    JSON.parse(value);
    return true;
  } catch {
    return false;
  }
}

function buildDownloadHref(value: string) {
  const mimeType = isJsonContent(value) ? "application/json" : "text/plain";
  return `data:${mimeType};charset=utf-8,${encodeURIComponent(value)}`;
}

async function copyToClipboard(value: string) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(value);
    return;
  }

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

function AuditBlock({
  title,
  value,
  fileName,
  copiedField,
  onCopy,
  showWhenEmpty = false,
}: {
  title: string;
  value?: string;
  fileName: string;
  copiedField: string | null;
  onCopy: (field: string, value?: string) => void;
  showWhenEmpty?: boolean;
}) {
  if (!value && !showWhenEmpty) return null;
  const downloadName = `${fileName}.${value && isJsonContent(value) ? "json" : "txt"}`;

  return (
    <section className="card mb-3">
      <div className="card-header d-flex flex-column flex-md-row gap-2 justify-content-md-between align-items-md-center">
        <span className="fw-semibold">{title}</span>
        <div className="d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            disabled={!value}
            onClick={() => onCopy(fileName, value)}
          >
            {copiedField === fileName ? "Copiado!" : "Copiar"}
          </button>
          {value ? (
            <a
              className="btn btn-sm btn-outline-secondary"
              href={buildDownloadHref(value)}
              download={downloadName}
            >
              Baixar arquivo
            </a>
          ) : (
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              disabled
            >
              Baixar arquivo
            </button>
          )}
        </div>
      </div>
      <div className="card-body">
        <CollapsibleJsonViewer content={value} />
      </div>
    </section>
  );
}

export default function HypothesisPainStageExecutionDetailPage() {
  const { nicheId, jobId, stageSlug = "pain" } = useParams();
  const stageLabel = stageSlug === "result" ? "resultado" : "dor";
  const detailQuery = useHypothesisPainStageExecutionDetail(
    nicheId,
    jobId,
    stageSlug,
  );
  const detail = detailQuery.data;
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const handleCopy = async (field: string, value?: string) => {
    if (!value) return;

    try {
      await copyToClipboard(value);
      setCopiedField(field);
      window.setTimeout(
        () => setCopiedField((current) => (current === field ? null : current)),
        2000,
      );
    } catch {
      setCopiedField(null);
    }
  };

  const jobFileSuffix = formatFileName(detail?.jobid ?? jobId ?? "job");

  return (
    <div className="hypothesis-pain-stage-execution-detail-page">
      <PageTitle icon={hypothesisIcon}>
        Detalhe do job da {stageLabel}
      </PageTitle>

      <div className="mb-3">
        <Link
          className="btn btn-outline-secondary"
          to={`/niches/${nicheId}/hypotheses/new`}
        >
          Voltar para nova hipótese
        </Link>
      </div>

      {detailQuery.isLoading ? (
        <p className="text-muted">Carregando detalhe do job...</p>
      ) : null}
      {detailQuery.isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar o detalhe do job.
        </div>
      ) : null}

      {detail ? (
        <>
          <section className="card mb-3">
            <div className="card-header d-flex flex-column flex-lg-row gap-2 justify-content-lg-between">
              <div>
                <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                  <h2 className="h5 mb-0">Job {detail.jobid}</h2>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => handleCopy("jobid", detail.jobid)}
                  >
                    {copiedField === "jobid" ? "Job copiado!" : "Copiar jobid"}
                  </button>
                </div>
                <span className="badge text-bg-light">{detail.status}</span>
              </div>
              <div className="text-muted small text-lg-end">
                Nicho #{detail.marketNicheId}
                <br />
                Etapa {detail.stageCode}
              </div>
            </div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-4">
                  <strong>Solicitado</strong>
                  <div>{formatDate(detail.executionRequestedAt)}</div>
                </div>
                <div className="col-md-4">
                  <strong>Processamento iniciado</strong>
                  <div>{formatDate(detail.processingStartedAt)}</div>
                </div>
                <div className="col-md-4">
                  <strong>Concluído</strong>
                  <div>{formatDate(detail.completedAt)}</div>
                </div>
                <div className="col-md-4">
                  <strong>Modelo</strong>
                  <div>{detail.openAiModel ?? "—"}</div>
                </div>
                <div className="col-md-4">
                  <strong>Job OpenAI</strong>
                  <div className="text-break">{detail.openAiJobId ?? "—"}</div>
                </div>
                <div className="col-md-4">
                  <strong>Custo</strong>
                  <div>{formatCost(detail.costUsd)}</div>
                </div>
                <div className="col-md-4">
                  <strong>Tokens de entrada</strong>
                  <div>{detail.inputTokens ?? "—"}</div>
                </div>
                <div className="col-md-4">
                  <strong>Tokens de saída</strong>
                  <div>{detail.outputTokens ?? "—"}</div>
                </div>
                <div className="col-md-4">
                  <strong>Template do prompt</strong>
                  <div>{detail.promptTemplateId ?? "—"}</div>
                </div>
              </div>
              {detail.errorMessage ? (
                <div className="alert alert-danger mt-3 mb-0">
                  {detail.errorMessage}
                </div>
              ) : null}
            </div>
          </section>

          <AuditBlock
            title="Prompt usado"
            value={
              detail.promptMarkdownContent ??
              detail.prompt ??
              detail.promptContent
            }
            fileName={`prompt-usado-${jobFileSuffix}`}
            copiedField={copiedField}
            onCopy={handleCopy}
          />
          <AuditBlock
            title="Schema enviado"
            value={detail.schemaJson}
            fileName={`schema-enviado-${jobFileSuffix}`}
            copiedField={copiedField}
            onCopy={handleCopy}
          />
          <AuditBlock
            title="Request OpenAI"
            value={detail.openAiRequestBody}
            fileName={`request-openai-${jobFileSuffix}`}
            copiedField={copiedField}
            onCopy={handleCopy}
          />
          <AuditBlock
            title="Resposta do modelo"
            value={detail.modelResponse}
            fileName={`resposta-modelo-${jobFileSuffix}`}
            copiedField={copiedField}
            onCopy={handleCopy}
          />
          <AuditBlock
            title="Detalhe técnico do erro"
            value={detail.errorDetail}
            fileName={`detalhe-tecnico-erro-${jobFileSuffix}`}
            copiedField={copiedField}
            onCopy={handleCopy}
          />
          <AuditBlock
            title="JSON final gravado no banco de dados"
            value={detail.modelResponse}
            fileName={`json-final-banco-${jobFileSuffix}`}
            copiedField={copiedField}
            onCopy={handleCopy}
            showWhenEmpty
          />
        </>
      ) : null}
    </div>
  );
}
