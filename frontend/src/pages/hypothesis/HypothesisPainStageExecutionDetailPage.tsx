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

function AuditBlock({ title, value }: { title: string; value?: string }) {
  if (!value) return null;
  return (
    <section className="card mb-3">
      <div className="card-header fw-semibold">{title}</div>
      <div className="card-body">
        <CollapsibleJsonViewer content={value} />
      </div>
    </section>
  );
}

export default function HypothesisPainStageExecutionDetailPage() {
  const { nicheId, jobId } = useParams();
  const detailQuery = useHypothesisPainStageExecutionDetail(nicheId, jobId);
  const detail = detailQuery.data;

  return (
    <div className="hypothesis-pain-stage-execution-detail-page">
      <PageTitle icon={hypothesisIcon}>Detalhe do job da dor</PageTitle>

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
                <h2 className="h5 mb-1">Job {detail.jobid}</h2>
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
          />
          <AuditBlock title="Schema enviado" value={detail.schemaJson} />
          <AuditBlock title="Request OpenAI" value={detail.openAiRequestBody} />
          <AuditBlock title="Resposta do modelo" value={detail.modelResponse} />
          <AuditBlock
            title="Detalhe técnico do erro"
            value={detail.errorDetail}
          />
        </>
      ) : null}
    </div>
  );
}
