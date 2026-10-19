import axios from "axios";
import { useState } from "react";
import type { BusinessProcessActivityExecution } from "../../api/businessProcess/types";

/** Reconhece somente a especificação auditada de Íris pronta para renderização no mesmo escopo. */
export function canReapplyMaterialization(
  execution: BusinessProcessActivityExecution,
) {
  if (
    execution.status !== "BLOCKED" ||
    execution.assignedAgentKey !== "communication-director" ||
    execution.executionMode !== "MODEL" ||
    !execution.sourceReference ||
    !execution.executionError ||
    !execution.comments ||
    !execution.evidenceJson
  )
    return false;
  try {
    const result = JSON.parse(execution.comments);
    const evidence = JSON.parse(execution.evidenceJson);
    return (
      result?.executionStatus === "READY_FOR_RENDER" &&
      result.activityId === "nonAudiovisual" &&
      result.outputType === "NON_AUDIOVISUAL_PACKAGE" &&
      result.sourceReference === execution.sourceReference &&
      evidence !== null &&
      typeof evidence === "object" &&
      !Array.isArray(evidence)
    );
  } catch {
    return false;
  }
}

/** Retoma a materialização pelo contrato existente, preservando a resposta e o custo do modelo. */
export default function PreservedMaterializationRetry({
  execution,
  onRefresh,
}: {
  execution: BusinessProcessActivityExecution;
  onRefresh?: () => void;
}) {
  const [pending, setPending] = useState(false);
  const [queued, setQueued] = useState(false);
  const [failed, setFailed] = useState(false);
  if (!canReapplyMaterialization(execution) && !queued) return null;
  const retry = async () => {
    setPending(true);
    setFailed(false);
    try {
      await axios.patch(
        `/api/agent-tasks/${execution.taskId}/status`,
        {
          status: "IN_PROGRESS",
        },
        { timeout: 45000 },
      );
      setQueued(true);
    } catch {
      setFailed(true);
    } finally {
      setPending(false);
      onRefresh?.();
    }
  };
  return (
    <section
      className="alert alert-info mt-3"
      aria-label="Retomada da materialização"
    >
      <p className="mb-2">
        A resposta de Íris está preservada. Depois de corrigir o impedimento
        técnico, o executor pode reaplicá-la sem uma nova chamada de IA. A peça
        continuará sujeita às revisões.
      </p>
      {queued ? (
        <p role="status" className="mb-0">
          Retomada registrada, preservando resposta e custos anteriores.
        </p>
      ) : (
        <button
          type="button"
          className="btn btn-outline-primary"
          disabled={pending}
          onClick={() => void retry()}
        >
          {pending ? "Solicitando..." : "Reaplicar resultado sem nova IA"}
        </button>
      )}
      {failed ? (
        <p role="alert" className="mt-2 mb-0">
          Não foi possível confirmar a retomada. O resultado foi preservado.
        </p>
      ) : null}
    </section>
  );
}
