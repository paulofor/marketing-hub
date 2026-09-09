import { useEffect, useRef } from "react";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import {
  cycleError,
  useMeasurementReconciliation,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

export default function LearningCycleAutomaticMeasurement({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const reconciliation = useMeasurementReconciliation(
    cycle.productId,
    cycle.id,
  );
  const attempted = useRef<string | undefined>(undefined);
  const latest = cycle.events[cycle.events.length - 1];
  const blocked = latest?.action === "MEASUREMENT_BLOCKED" ? latest : undefined;
  async function reconcile() {
    try {
      onUpdated(
        await reconciliation.mutateAsync({
          requestKey: createCycleRequestKey(),
          expectedRevision: cycle.revision,
        }),
      );
    } catch {
      /* O erro contratual permanece visível no componente. */
    }
  }
  useEffect(() => {
    const attemptKey = `${cycle.id}:${cycle.revision}`;
    if (
      attempted.current === attemptKey ||
      blocked ||
      cycle.stage !== "MEASUREMENT" ||
      cycle.status !== "OPEN"
    )
      return;
    attempted.current = attemptKey;
    void reconcile();
  });
  return (
    <section
      className="card card-body mb-3"
      aria-label="Conciliação automática"
    >
      <h3 className="h5">Conciliação automática de resultados</h3>
      <p>
        O backend lê o funil atribuído, a mídia, as vendas, os reembolsos, os
        custos e o valor entregue do experimento #{cycle.experimentId}. Nenhum
        resultado é digitado nesta tela.
      </p>
      {reconciliation.isPending ? (
        <p role="status">
          <span
            className="spinner-border spinner-border-sm me-2"
            aria-label="Conciliando"
          />
          Conciliando fontes oficiais...
        </p>
      ) : null}
      {blocked ? (
        <div className="alert alert-warning" role="alert">
          <strong>A conciliação foi bloqueada.</strong>{" "}
          {String(blocked.evidence.blocker ?? blocked.summary)}
        </div>
      ) : null}
      {reconciliation.isError ? (
        <div className="alert alert-danger" role="alert">
          {cycleError(reconciliation.error)}
        </div>
      ) : null}
      {blocked || reconciliation.isError ? (
        <button
          className="btn btn-outline-primary align-self-start"
          type="button"
          disabled={reconciliation.isPending}
          onClick={() => void reconcile()}
        >
          {reconciliation.isPending ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              aria-label="Conciliando novamente"
            />
          ) : null}
          Tentar conciliação novamente
        </button>
      ) : null}
    </section>
  );
}
