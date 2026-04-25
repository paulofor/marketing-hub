import { useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useCreateMoisComparison } from "../../api/mois/useMoisSprintTwo";

const WORKSPACE_ID = "workspace-default";

export default function MoisComparisonPage() {
  const [referenceBaseId, setReferenceBaseId] = useState("");
  const [currentOfferId, setCurrentOfferId] = useState("");
  const comparisonMutation = useCreateMoisComparison();

  async function handleCompare(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await comparisonMutation.mutateAsync({ workspaceId: WORKSPACE_ID, referenceBaseId, currentOfferId });
    } catch {
      toast.error("Não foi possível gerar comparação.");
    }
  }

  const comparison = comparisonMutation.data;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Comparador MOIS</PageTitle>
          <p className="text-secondary mb-0">Sprint 2: comparação mercado vs oferta com scorecards.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <form className="row g-3" onSubmit={handleCompare}>
            <div className="col-12 col-md-6">
              <label className="form-label">Referência base *</label>
              <input className="form-control" value={referenceBaseId} onChange={(event) => setReferenceBaseId(event.target.value)} required />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Oferta atual *</label>
              <input className="form-control" value={currentOfferId} onChange={(event) => setCurrentOfferId(event.target.value)} required />
            </div>
            <div className="col-12 d-flex justify-content-end">
              <button type="submit" className="btn btn-primary d-inline-flex align-items-center gap-2" disabled={comparisonMutation.isPending}>
                {comparisonMutation.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
                Gerar comparação
              </button>
            </div>
          </form>
        </div>
      </section>

      {!comparison ? <div className="alert alert-info mb-0">Selecione uma referência e uma oferta para iniciar a comparação.</div> : null}

      {comparison ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5">Matriz de comparação</h2>
              <div className="table-responsive">
                <table className="table table-striped align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Dimensão</th>
                      <th>Mercado</th>
                      <th>Atual</th>
                      <th>Melhoria</th>
                    </tr>
                  </thead>
                  <tbody>
                    {comparison.dimensions.map((dimension) => (
                      <tr key={dimension.dimension}>
                        <td>{dimension.dimension}</td>
                        <td>{dimension.market}</td>
                        <td>{dimension.current}</td>
                        <td>{dimension.highlight}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          <section className="row g-3">
            {comparison.scorecards.map((score) => (
              <div className="col-12 col-md-6 col-xl-3" key={score.metric}>
                <article className="card border-0 shadow-sm h-100">
                  <div className="card-body">
                    <p className="text-uppercase text-secondary small mb-1">{score.metric}</p>
                    <strong className="h4 d-block">{score.value}</strong>
                    <p className="mb-0 text-secondary small">{score.explanation}</p>
                  </div>
                </article>
              </div>
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}
