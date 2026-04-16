import { AlertCircle } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useOprmInsightsWorkspaceData } from "../../api/oprm/useOprmInsightsWorkspaceData";
import OprmModuleNavigation from "./OprmModuleNavigation";

function toPercent(value: unknown): string {
  if (typeof value !== "number") {
    return "—";
  }
  return `${(value * 100).toFixed(1)}%`;
}

function scoreLabel(snapshot: Record<string, unknown>) {
  return toPercent(snapshot.recalibratedConfidenceScore ?? snapshot.confidenceScore);
}

export default function OprmFeedbackPage() {
  const { occupationSeedRef } = useParams();
  const insightsQuery = useOprmInsightsWorkspaceData(occupationSeedRef);

  const latest = insightsQuery.data?.feedbackComparison?.latestConfidence;
  const previous = insightsQuery.data?.feedbackComparison?.previousConfidence;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Feedback</PageTitle>
        <p className="text-secondary mb-0">
          Acompanhe snapshots de recalibração e compare a evolução de confiança por ocupação.
        </p>
      </header>

      <OprmModuleNavigation occupationSeedRef={occupationSeedRef} />

      {insightsQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando feedback do OPRM...</span>
          </div>
        </div>
      ) : null}

      {insightsQuery.isError ? (
        <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
          <AlertCircle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <strong>Não foi possível carregar o histórico de feedback.</strong>
            <p className="mb-0">Valide o endpoint de workspace de insights no backend.</p>
          </div>
        </div>
      ) : null}

      {!insightsQuery.isLoading && !insightsQuery.isError ? (
        <>
          <section className="row g-3">
            <div className="col-12 col-lg-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <h2 className="h6">Comparativo antes/depois</h2>
                  <dl className="row mb-0">
                    <dt className="col-6">Confiança anterior</dt>
                    <dd className="col-6 text-end">{toPercent(previous)}</dd>
                    <dt className="col-6">Confiança atual</dt>
                    <dd className="col-6 text-end">{toPercent(latest)}</dd>
                  </dl>
                </div>
              </div>
            </div>
            <div className="col-12 col-lg-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body d-flex flex-column gap-2">
                  <h2 className="h6">Próxima ação recomendada</h2>
                  <p className="text-secondary mb-0">
                    Use o comparativo para validar se a recalibração está melhorando a aderência comercial da oferta.
                  </p>
                  <div>
                    <Link className="btn btn-outline-primary btn-sm" to={`/oprm/offer/${encodeURIComponent(occupationSeedRef ?? "")}`}>
                      Revisar oferta atual
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <h2 className="h5 mb-0">Histórico por ocupação</h2>
              {insightsQuery.data?.feedbackSnapshots?.length ? (
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Snapshot</th>
                        <th>Confiança</th>
                        <th>Aderência de hipóteses</th>
                      </tr>
                    </thead>
                    <tbody>
                      {insightsQuery.data.feedbackSnapshots.map((snapshot, index) => (
                        <tr key={`feedback-${index}`}>
                          <td>{String(snapshot.generatedAt ?? `Execução ${index + 1}`)}</td>
                          <td>{scoreLabel(snapshot)}</td>
                          <td>{Array.isArray(snapshot.hypothesisRoutineFit) ? snapshot.hypothesisRoutineFit.length : 0}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="alert alert-secondary mb-0" role="status">
                  Nenhum `occupationFeedbackLoopSnapshot` encontrado para esta ocupação.
                </div>
              )}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
