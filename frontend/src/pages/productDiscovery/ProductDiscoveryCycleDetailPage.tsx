import { useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  productDiscoveryDecisionLabels,
  productDiscoveryStatusLabels,
  useProductDiscoveryCycle,
} from "../../api/productDiscovery/useProductDiscovery";

function parseCycleId(value: string | undefined) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export default function ProductDiscoveryCycleDetailPage() {
  const { cycleId } = useParams();
  const query = useProductDiscoveryCycle(parseCycleId(cycleId));
  const detail = query.data;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Ranking de Oportunidades PDE</PageTitle>
        {detail ? (
          <p className="text-secondary mb-0">
            {detail.cycle.theme} ·{" "}
            {productDiscoveryStatusLabels[detail.cycle.status]}
          </p>
        ) : null}
      </header>

      {query.isLoading ? (
        <div className="text-secondary">Carregando ranking...</div>
      ) : null}
      {query.isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar este ciclo.
        </div>
      ) : null}
      {detail?.cycle.errorMessage ? (
        <div className="alert alert-danger">{detail.cycle.errorMessage}</div>
      ) : null}
      {detail?.cycle.decisionSummary ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body">
            <h2 className="h5 mb-2">Decisão do ciclo</h2>
            <p className="mb-0">{detail.cycle.decisionSummary}</p>
          </div>
        </section>
      ) : null}

      <section className="d-flex flex-column gap-3">
        {(detail?.opportunities ?? []).map((opportunity) => (
          <article className="card border-0 shadow-sm" key={opportunity.id}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                <div>
                  <h2 className="h5 mb-1">{opportunity.name}</h2>
                  <p className="text-secondary mb-0">
                    {opportunity.primaryAudience}
                  </p>
                </div>
                <div className="text-end">
                  <strong className="h4 d-block mb-0">
                    {Number(opportunity.score).toFixed(0)}
                  </strong>
                  <span className="badge text-bg-light">
                    {productDiscoveryDecisionLabels[opportunity.decision]}
                  </span>
                </div>
              </div>
              <div className="row g-3">
                <div className="col-md-6">
                  <h3 className="h6">Dor raiz</h3>
                  <p>{opportunity.rootPain}</p>
                </div>
                <div className="col-md-6">
                  <h3 className="h6">Microexperiência PDE</h3>
                  <p>{opportunity.pdeExperience || "-"}</p>
                </div>
                <div className="col-md-6">
                  <h3 className="h6">Escala</h3>
                  <p>{opportunity.scaleEvidence || "-"}</p>
                </div>
                <div className="col-md-6">
                  <h3 className="h6">Lacuna</h3>
                  <p>{opportunity.unmetnessEvidence || "-"}</p>
                </div>
                <div className="col-md-6">
                  <h3 className="h6">Ângulo inicial</h3>
                  <p>{opportunity.firstCampaignAngle || "-"}</p>
                </div>
                <div className="col-md-6">
                  <h3 className="h6">Risco comercial</h3>
                  <p>{opportunity.commercialRisk || "-"}</p>
                </div>
              </div>
            </div>
          </article>
        ))}
        {detail && detail.opportunities.length === 0 ? (
          <div className="alert alert-secondary">
            O worker ainda não registrou oportunidades para este ciclo.
          </div>
        ) : null}
      </section>
    </div>
  );
}
