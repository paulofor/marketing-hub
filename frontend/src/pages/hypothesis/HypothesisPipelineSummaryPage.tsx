import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import { useHypothesisPipelineSummary } from "../../api/hypothesis/useHypothesisPipelineSummary";

function formatDate(value?: string | null) {
  if (!value) return "—";
  return new Date(value).toLocaleString("pt-BR");
}

export default function HypothesisPipelineSummaryPage() {
  const { nicheId } = useParams();
  const summaryQuery = useHypothesisPipelineSummary(nicheId);
  const summaries = summaryQuery.data ?? [];

  return (
    <div className="hypothesis-pipeline-summary-page">
      <PageTitle icon={hypothesisIcon}>Resumo do framework</PageTitle>

      <div className="mb-3">
        <Link
          className="btn btn-outline-secondary"
          to={`/niches/${nicheId}/hypotheses/new`}
        >
          Voltar para nova hipótese
        </Link>
      </div>

      <section className="card mb-4">
        <div className="card-body">
          <p className="mb-2">
            <strong>Nicho:</strong> #{nicheId}
          </p>
          <p className="text-muted mb-0">
            Esta tela mostra somente o conteúdo final gravado no banco de dados
            para cada etapa concluída do framework. Esse conteúdo é o insumo
            reaproveitado pelos próximos pipelines.
          </p>
        </div>
      </section>

      {summaryQuery.isLoading ? (
        <p className="text-muted">Carregando resumo do framework...</p>
      ) : null}
      {summaryQuery.isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar o resumo do framework.
        </div>
      ) : null}

      {!summaryQuery.isLoading && !summaryQuery.isError ? (
        <div className="d-flex flex-column gap-3">
          {summaries.map((stage) => (
            <section className="card" key={stage.stageCode}>
              <div className="card-header d-flex flex-column flex-lg-row gap-2 justify-content-lg-between">
                <div>
                  <h2 className="h5 mb-1">
                    Etapa {stage.stageNumber} — {stage.stageTitle}
                  </h2>
                  <div className="text-muted small">
                    Observação: origem em tabela{" "}
                    <code>{stage.sourceTable}</code>, campo{" "}
                    <code>{stage.sourceField}</code>.
                  </div>
                </div>
                <div className="text-muted small text-lg-end">
                  Status: {stage.status ?? "sem conteúdo final"}
                  <br />
                  Concluído em: {formatDate(stage.completedAt)}
                </div>
              </div>
              <div className="card-body">
                {stage.jobid ? (
                  <p className="small text-muted mb-3">
                    Job final usado como origem:{" "}
                    <Link
                      to={`/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions/${stage.jobid}`}
                    >
                      {stage.jobid}
                    </Link>
                  </p>
                ) : null}
                {stage.finalContent ? (
                  <CollapsibleJsonViewer content={stage.finalContent} />
                ) : (
                  <div className="alert alert-warning mb-0">
                    Esta etapa ainda não possui conteúdo final concluído para
                    reaproveitamento em outros pipelines.
                  </div>
                )}
              </div>
            </section>
          ))}
        </div>
      ) : null}
    </div>
  );
}
