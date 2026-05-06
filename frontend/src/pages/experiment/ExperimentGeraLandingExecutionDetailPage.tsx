import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { useGeraLandingStageExecutionDetail } from "../../api/experiment/useGeraLandingStageExecutions";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import MarkdownContentViewer from "../../components/MarkdownContentViewer";

function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function ExperimentGeraLandingExecutionDetailPage() {
  const { id: experimentId, jobId } = useParams();
  const detailQuery = useGeraLandingStageExecutionDetail(experimentId, jobId);

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
          <PageTitle icon={experimentIcon}>Detalhe da execução Gera Landing</PageTitle>
          <p className="text-muted mb-0">Visualização completa do registro da tabela gera_landing_stage_execution.</p>
        </div>
        <Link to={`/experiments/${experimentId}`} className="btn btn-outline-secondary">
          Voltar
        </Link>
      </div>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          {detailQuery.isLoading ? (
            <p className="text-muted mb-0">Carregando detalhes da execução...</p>
          ) : detailQuery.isError || !detailQuery.data ? (
            <p className="text-danger mb-0">Não foi possível carregar os detalhes da execução.</p>
          ) : (
            <div className="d-flex flex-column gap-3">
              <div className="row g-3 small">
                <div className="col-md-6"><strong>Job ID:</strong> {detailQuery.data.idJob}</div>
                <div className="col-md-6"><strong>Status:</strong> {detailQuery.data.status}</div>
                <div className="col-md-6"><strong>Stage:</strong> {detailQuery.data.stageCode}</div>
                <div className="col-md-6"><strong>OpenAI Job ID:</strong> {detailQuery.data.openAiJobId ?? "—"}</div>
                <div className="col-md-6"><strong>Solicitado em:</strong> {formatDateTime(detailQuery.data.executionRequestedAt)}</div>
                <div className="col-md-6"><strong>Criado em:</strong> {formatDateTime(detailQuery.data.createdAt)}</div>
                <div className="col-md-6"><strong>Processamento iniciado:</strong> {formatDateTime(detailQuery.data.processingStartedAt)}</div>
                <div className="col-md-6"><strong>Concluído em:</strong> {formatDateTime(detailQuery.data.completedAt)}</div>
                <div className="col-md-6"><strong>Input tokens:</strong> {detailQuery.data.inputTokens ?? "—"}</div>
                <div className="col-md-6"><strong>Output tokens:</strong> {detailQuery.data.outputTokens ?? "—"}</div>
                <div className="col-md-6"><strong>Custo USD:</strong> {detailQuery.data.costUsd ?? "—"}</div>
                <div className="col-md-6"><strong>Prompt template ID:</strong> {detailQuery.data.promptTemplateId ?? "—"}</div>
              </div>

              <div>
                <h6>Prompt content</h6>
                <CollapsibleJsonViewer content={detailQuery.data.promptContent} />
              </div>
              <div>
                <h6>Prompt</h6>
                <CollapsibleJsonViewer content={detailQuery.data.prompt} />
              </div>
              <div>
                <h6>OpenAI request body (prompt cru enviado)</h6>
                <CollapsibleJsonViewer content={detailQuery.data.openAiRequestBody} />
              </div>
              <div>
                <h6>Schema JSON enviado para o modelo</h6>
                <CollapsibleJsonViewer content={detailQuery.data.schemaJson} />
              </div>
              <div>
                <h6>Conteúdo do arquivo .md usado no prompt</h6>
                <MarkdownContentViewer content={detailQuery.data.promptMarkdownContent} />
              </div>
              <div>
                <h6>Model response</h6>
                <CollapsibleJsonViewer content={detailQuery.data.modelResponse} />
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
