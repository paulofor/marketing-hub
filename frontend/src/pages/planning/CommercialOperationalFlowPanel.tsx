import { useCommercialPlanOperationalFlow } from "../../api/planning/useCommercialPlans";

/** Apresenta ao usuário apenas o avanço, o bloqueio e a próxima ação do plano. */
export default function CommercialOperationalFlowPanel({ planId }: { planId: number }) {
  const query = useCommercialPlanOperationalFlow(planId);
  const flow = query.data;

  if (query.isLoading) return <div className="card"><div className="card-body">Carregando fluxo comercial...</div></div>;
  if (query.isError || !flow) return <div className="alert alert-danger">Não foi possível carregar o fluxo comercial.</div>;

  return (
    <section className="card border-primary" data-testid="commercial-operational-flow">
      <div className="card-body d-grid gap-3">
        <div>
          <h2 className="h5 mb-1">Próximo passo para vender</h2>
          <p className="text-body-secondary mb-0">Os especialistas atuam nos bastidores somente quando a etapa precisa deles.</p>
        </div>
        <ol className="d-flex flex-wrap gap-2 list-unstyled mb-0" aria-label="Fluxo comercial">
          {flow.stages.map((stage, index) => (
            <li className={`border rounded px-3 py-2 ${stage.status === "ATUAL" ? "border-primary bg-primary-subtle" : ""}`} key={stage.code}>
              <small className="d-block text-body-secondary">{index + 1}</small>
              <strong>{stage.label}</strong>
              <span className={`badge ms-2 ${stage.status === "CONCLUIDO" ? "text-bg-success" : stage.status === "ATUAL" ? "text-bg-primary" : "text-bg-light border"}`}>
                {stage.status === "CONCLUIDO" ? "Concluído" : stage.status === "ATUAL" ? "Agora" : "Depois"}
              </span>
            </li>
          ))}
        </ol>
        <div className={`alert mb-0 ${flow.status === "BLOQUEADO" ? "alert-warning" : "alert-primary"}`}>
          <strong>Próxima ação:</strong> {flow.nextAction}
          {flow.blocker ? <span className="d-block mt-1"><strong>Bloqueio:</strong> {flow.blocker}</span> : null}
        </div>
        <div className="row g-2 small">
          <div className="col-lg-6"><strong>Métrica:</strong> {flow.expectedMetric}</div>
          <div className="col-lg-6"><strong>Critério:</strong> {flow.decisionCriterion}</div>
        </div>
        <details>
          <summary>Decisões dos especialistas</summary>
          <div className="row g-2 mt-1">
            {flow.specialistDecisions.map((item) => (
              <div className="col-md-6" key={item.specialist}>
                <div className="border rounded p-2 h-100">
                  <strong>{item.specialist}</strong> · {item.decision}
                  <small className="d-block text-body-secondary">{item.responsibility}</small>
                  <span className="d-block mt-1">{item.nextAction}</span>
                </div>
              </div>
            ))}
          </div>
        </details>
      </div>
    </section>
  );
}
