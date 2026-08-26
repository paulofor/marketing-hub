import { useProcessInstances } from "../../api/agentTask/useAgentTasks";
import type { ProcessInstanceOperationalState } from "../../api/agentTask/types";
import AgentTaskModelUsage from "../../components/AgentTaskModelUsage";
import AgentTaskFailureAudit from "../../components/AgentTaskFailureAudit";

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

/** Formata marco temporal persistido sem fabricar uma data ausente. */
function formatDateTime(value?: string) {
  return value ? dateTimeFormatter.format(new Date(value)) : "Não registrada";
}

/** Apresenta somente o custo conhecido e explicita a cobertura da medição. */
function formatCost(value?: number) {
  return value === undefined
    ? "Não reportado"
    : new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 4,
        maximumFractionDigits: 8,
      }).format(value);
}

const coverageLabel = {
  COMPLETE: "cobertura completa",
  PARTIAL: "cobertura parcial",
  NOT_REPORTED: "custo não reportado",
};

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
              A atividade consolida o objetivo e cada tarefa abaixo representa
              uma tentativa auditável.
            </p>
            <div className="d-grid gap-2">
              {instance.activities.map((activity, index) => {
                const presentation =
                  statePresentation[activity.operationalState];
                return (
                  <article
                    className="border rounded p-3"
                    key={`${activity.activityId}:${activity.occurrenceNumber}`}
                  >
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <strong>
                        Atividade {index + 1}. {activity.activityName}
                      </strong>
                      <span className={`badge ${presentation.className}`}>
                        {presentation.label}
                      </span>
                    </div>
                    {activity.objective ? (
                      <p className="mb-2 mt-2">
                        Objetivo: {activity.objective}
                      </p>
                    ) : null}
                    <p className="small text-body-secondary mb-3">
                      {activity.stateReason}
                    </p>
                    <dl className="row small mb-3">
                      <dt className="col-sm-3">Instância</dt>
                      <dd className="col-sm-9 mb-1">
                        Ocorrência #{activity.occurrenceNumber}
                      </dd>
                      <dt className="col-sm-3">Entrada</dt>
                      <dd className="col-sm-9 mb-1">
                        {formatDateTime(activity.enteredAt)}
                      </dd>
                      <dt className="col-sm-3">Saída</dt>
                      <dd className="col-sm-9 mb-1">
                        {formatDateTime(activity.exitedAt)}
                      </dd>
                      <dt className="col-sm-3">Objetivo atingido</dt>
                      <dd className="col-sm-9 mb-1">
                        {activity.objectiveAchieved ? "Sim" : "Ainda não"}
                      </dd>
                      <dt className="col-sm-3">Custo conhecido</dt>
                      <dd className="col-sm-9 mb-1">
                        {formatCost(activity.knownCostUsd)} ·{" "}
                        {coverageLabel[activity.costCoverage]}
                      </dd>
                    </dl>
                    <details open={activity.tasks.length === 1}>
                      <summary>
                        {activity.tasks.length} tarefa(s)/tentativa(s)
                      </summary>
                      <div className="d-grid gap-2 mt-2">
                        {activity.tasks.map((task) => {
                          const taskPresentation =
                            statePresentation[task.operationalState];
                          return (
                            <div
                              className="bg-body-tertiary rounded p-3"
                              key={task.taskId}
                            >
                              <div className="d-flex flex-wrap justify-content-between gap-2">
                                <strong>
                                  Tentativa {task.attemptNumber} · tarefa #
                                  {task.taskId}
                                </strong>
                                <span
                                  className={`badge ${taskPresentation.className}`}
                                >
                                  {taskPresentation.label}
                                </span>
                              </div>
                              <div className="small mt-1">
                                {task.agentNickname} · {task.stateReason}
                              </div>
                              <div className="mt-2">
                                <AgentTaskModelUsage usage={task} />
                              </div>
                              <div className="mt-2">
                                <AgentTaskFailureAudit
                                  audit={task.failureAudit}
                                />
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </details>
                  </article>
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
