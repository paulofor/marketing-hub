import { useProcessInstances } from "../../api/agentTask/useAgentTasks";
import type { ProcessInstanceOperationalState } from "../../api/agentTask/types";

const statePresentation: Record<
  ProcessInstanceOperationalState,
  { label: string; className: string }
> = {
  RELEASED: { label: "Liberada", className: "text-bg-success" },
  WAITING_PREDECESSOR: {
    label: "Aguardando predecessora",
    className: "text-bg-warning",
  },
  IN_PROGRESS: { label: "Em execução", className: "text-bg-primary" },
  BLOCKED: { label: "Bloqueada", className: "text-bg-danger" },
  COMPLETED: { label: "Concluída", className: "text-bg-success" },
  CANCELLED: { label: "Cancelada", className: "text-bg-secondary" },
  SUPERSEDED_LEGACY: {
    label: "Legada substituída",
    className: "text-bg-secondary",
  },
};

/** Exibe a instância BPM do experimento sem recalcular a elegibilidade no navegador. */
export default function ExperimentProcessInstanceTab({
  experimentId,
}: {
  experimentId: string;
}) {
  const query = useProcessInstances(`experiment:${experimentId}`);
  if (query.isLoading) return <p>Carregando instância do processo...</p>;
  if (query.isError)
    return (
      <div className="alert alert-danger">
        Não foi possível consultar o processo.
      </div>
    );
  if (!query.data?.length)
    return (
      <div className="alert alert-info">
        Este experimento ainda não possui instância BPM.
      </div>
    );

  return (
    <div className="d-grid gap-4 py-3">
      {query.data.map((instance) => (
        <section className="card" key={instance.processDefinitionId}>
          <div className="card-body">
            <h3 className="h5 mb-1">
              {instance.processCode} · v{instance.processVersionNumber}
            </h3>
            <p className="text-body-secondary">
              A liberação é calculada pelo backend conforme o diagrama
              publicado.
            </p>
            <div className="d-grid gap-2">
              {instance.tasks.map((task, index) => {
                const presentation = statePresentation[task.operationalState];
                return (
                  <div className="border rounded p-3" key={task.taskId}>
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <strong>
                        {index + 1}. {task.activityName} · #{task.taskId}
                      </strong>
                      <span className={`badge ${presentation.className}`}>
                        {presentation.label}
                      </span>
                    </div>
                    <div className="small mt-1">
                      {task.agentNickname} · {task.stateReason}
                    </div>
                  </div>
                );
              })}
            </div>
            {instance.supersededLegacyTasks.length > 0 ? (
              <details className="mt-3">
                <summary>
                  {instance.supersededLegacyTasks.length} tarefa(s) legada(s)
                  substituída(s)
                </summary>
                <ul className="mt-2 mb-0">
                  {instance.supersededLegacyTasks.map((task) => (
                    <li key={task.taskId}>
                      #{task.taskId} · {task.activityName} ·{" "}
                      {task.agentNickname}
                    </li>
                  ))}
                </ul>
              </details>
            ) : null}
          </div>
        </section>
      ))}
    </div>
  );
}
