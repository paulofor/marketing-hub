import { Link } from "react-router-dom";
import type { SalesFlow } from "../api/learningCycle/salesFlow";
import { salesActivityStateLabels } from "../api/learningCycle/salesFlow";

/** Apresenta a passagem e os retornos enviados pelo backend, sem inferir a próxima atividade. */
export default function ProductSalesFlow({ flow }: { flow: SalesFlow }) {
  const currentActivityUrl = flow.currentActivityId
    ? `/products/${flow.productId}/value-chain-history/processes/${flow.modelProcessDefinitionId}/activities?learningCycleId=${flow.cycleId}#activity-${flow.currentActivityId}`
    : flow.navigationUrl;
  return (
    <section className="card mb-4" aria-label="Fluxo do Processo 6">
      <div className="card-body">
        <h2 className="h5">Processo 6 · Fluxo do produto</h2>
        <p className="text-muted">
          Experimento #{flow.experimentId} · ciclo #{flow.cycleId}
        </p>
        <p className="fw-semibold">
          {flow.currentActivityId
            ? `Atividade atual: 6.${flow.currentActivitySequenceNumber} — ${flow.currentActivityName}`
            : "Passagem encerrada"}
        </p>
        <p>{flow.reason}</p>
        <div className="d-flex flex-wrap gap-2 mb-3">
          <Link className="btn btn-primary" to={currentActivityUrl}>
            {flow.currentActivityId
              ? "Continuar fluxo registrado"
              : "Consultar decisão"}
          </Link>
          <Link className="btn btn-outline-primary" to={flow.modelUrl}>
            Ver caminhos e retornos no BPM
          </Link>
        </div>
        <ol className="list-unstyled mb-3">
          {flow.activities.map((activity) => (
            <li key={activity.activityId} className="border rounded p-3 mb-2">
              <strong>
                6.{activity.sequenceNumber} — {activity.name}
              </strong>
              <span className="badge text-bg-light border ms-2">
                {salesActivityStateLabels[activity.state] ?? activity.state}
              </span>
              <p className="mb-0 mt-1">{activity.reason}</p>
            </li>
          ))}
        </ol>
        <SalesFlowTransitions flow={flow} />
      </div>
    </section>
  );
}

/** Exibe somente o registro de passagens junto à atividade chamadora. */
export function SalesFlowTransitions({ flow }: { flow: SalesFlow }) {
  return (
    <details>
      <summary>
        Passagens e retornos registrados ({flow.transitions.length})
      </summary>
      <ol className="mt-3">
        {flow.transitions.map((transition) => (
          <li key={transition.eventId} className="mb-3">
            <strong>
              {transition.returnFlow ? "Retorno: " : "Passagem: "}
              {transition.from} → {transition.to}
            </strong>
            <p className="mb-1">{transition.reason}</p>
            <small>
              {transition.responsible} ·{" "}
              {new Date(transition.occurredAt).toLocaleString("pt-BR", {
                timeZone: "UTC",
              })}{" "}
              UTC
            </small>
            <div className="small text-muted text-break">
              Evidência: {transition.evidenceReference}
            </div>
          </li>
        ))}
      </ol>
    </details>
  );
}
