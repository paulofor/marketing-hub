import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMoisDashboard } from "../../api/mois/useMoisDashboard";
import "./mois.css";

const WORKSPACE_ID = "workspace-default";
const STAGES = ["Coleta", "Extração", "Síntese", "Aplicação", "Teste"];

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "-"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function MoisWorkspacePage() {
  const dashboardQuery = useMoisDashboard(WORKSPACE_ID);
  const dashboard = dashboardQuery.data;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap align-items-center justify-content-between gap-3">
        <div>
          <PageTitle>MOIS Workspace</PageTitle>
          <p className="text-secondary mb-0">Visão Sprint 1 com KPIs e análises recentes.</p>
        </div>
        <Link className="btn btn-primary" to="/mois/references/new">
          Nova análise
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h6 mb-0">Pipeline MOIS</h2>
          <ol className="mois-stepper mb-0">
            {STAGES.map((stage) => (
              <li key={stage} className="mois-stepper__item">
                <span>{stage}</span>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {dashboardQuery.isLoading ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body">
            <div className="placeholder-glow">
              <span className="placeholder col-12 mb-2" />
              <span className="placeholder col-8" />
            </div>
          </div>
        </section>
      ) : null}

      {dashboardQuery.isError ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível carregar o workspace MOIS. Tente novamente.
        </div>
      ) : null}

      {!dashboardQuery.isLoading && !dashboardQuery.isError && dashboard ? (
        <>
          <section className="row g-3">
            <div className="col-12 col-md-6 col-xl-3">
              <article className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <p className="text-secondary mb-1">Coletas</p>
                  <strong className="h4 mb-0">{dashboard.kpis.collections}</strong>
                </div>
              </article>
            </div>
            <div className="col-12 col-md-6 col-xl-3">
              <article className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <p className="text-secondary mb-1">Extrações</p>
                  <strong className="h4 mb-0">{dashboard.kpis.extractions}</strong>
                </div>
              </article>
            </div>
            <div className="col-12 col-md-6 col-xl-3">
              <article className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <p className="text-secondary mb-1">Aplicações</p>
                  <strong className="h4 mb-0">{dashboard.kpis.applications}</strong>
                </div>
              </article>
            </div>
            <div className="col-12 col-md-6 col-xl-3">
              <article className="card border-0 shadow-sm h-100">
                <div className="card-body">
                  <p className="text-secondary mb-1">Testes</p>
                  <strong className="h4 mb-0">{dashboard.kpis.tests}</strong>
                </div>
              </article>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <div className="d-flex align-items-center justify-content-between">
                <h2 className="h5 mb-0">Análises recentes</h2>
                <span className="badge text-bg-light">Etapa atual: {dashboard.currentStage}</span>
              </div>

              {dashboard.recentAnalyses.length === 0 ? (
                <div className="alert alert-secondary mb-0">Nenhuma análise disponível. Crie a primeira referência.</div>
              ) : (
                <div className="table-responsive">
                  <table className="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Nicho</th>
                        <th>Status</th>
                        <th>Atualizado em</th>
                        <th>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dashboard.recentAnalyses.map((analysis) => (
                        <tr key={analysis.analysisId}>
                          <td>{analysis.niche}</td>
                          <td>{analysis.status}</td>
                          <td>{formatDate(analysis.updatedAt)}</td>
                          <td>
                            <div className="d-flex gap-2">
                              <button type="button" className="btn btn-outline-secondary btn-sm" disabled>
                                Retomar
                              </button>
                              <button type="button" className="btn btn-outline-primary btn-sm" disabled>
                                Aplicar
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
