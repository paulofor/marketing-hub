import { FormEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAgents } from "../../api/agent/useAgents";
import {
  useAgentTasks,
  useCreateAgentTask,
  useUpdateAgentTaskStatus,
} from "../../api/agentTask/useAgentTasks";
import { AgentTask, AgentTaskStatus } from "../../api/agentTask/types";
import PageTitle from "../../components/PageTitle";
import {
  useCommercialPlans,
  useCommercialPlanVersions,
} from "../../api/planning/useCommercialPlans";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { useExperiments } from "../../api/experiment/useExperiments";

const statusLabel: Record<AgentTaskStatus, string> = {
  PENDING: "Pendente",
  IN_PROGRESS: "Em andamento",
  COMPLETED: "Concluída",
  BLOCKED: "Bloqueada",
  CANCELLED: "Cancelada",
};

const nextStatuses: Record<AgentTaskStatus, AgentTaskStatus[]> = {
  PENDING: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED", "BLOCKED", "CANCELLED"],
  BLOCKED: ["IN_PROGRESS", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

const metaFieldLabel: Record<string, string> = {
  primaryText: "Texto principal",
  headline: "Título",
  description: "Descrição",
  cta: "CTA",
};

function experimentIdFromTask(task: AgentTask) {
  const match = task.sourceReference?.match(/^experiment:(\d+)$/);
  return match?.[1];
}

export default function AgentWorkspacePage() {
  const { id } = useParams();
  const agents = useAgents();
  const agent = (agents.data ?? []).find((item) => item.id === Number(id));
  const inbox = useAgentTasks(agent?.agentKey);
  const create = useCreateAgentTask(agent?.agentKey);
  const updateStatus = useUpdateAgentTaskStatus(agent?.agentKey);
  const experiments = useExperiments();
  const plans = useCommercialPlans();
  const [commercialPlanId, setCommercialPlanId] = useState<number | null>(null);
  const selectedPlanId = commercialPlanId ?? plans.data?.[0]?.id ?? null;
  const planVersions = useCommercialPlanVersions(selectedPlanId);
  const currentPlanVersion = planVersions.data?.[0]?.versionNumber;
  const [form, setForm] = useState({
    requestedByName: "Usuário do Marketing Hub",
    title: "",
    description: "",
    priority: "NORMAL" as AgentTask["priority"],
    sourceReference: "",
  });
  const openCount = useMemo(
    () =>
      (inbox.data ?? []).filter((task) =>
        ["PENDING", "IN_PROGRESS", "BLOCKED"].includes(task.status),
      ).length,
    [inbox.data],
  );

  if (agents.isLoading) return <p>Carregando agente...</p>;
  if (!agent) return <p>Agente não encontrado.</p>;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedPlanId || !currentPlanVersion) return;
    create.mutate(
      {
        ...form,
        sourceReference: `commercial-plan:${selectedPlanId}@v${currentPlanVersion}`,
        assignedAgentKey: agent.agentKey!,
      },
      {
        onSuccess: () =>
          setForm({
            ...form,
            title: "",
            description: "",
            sourceReference: "",
          }),
      },
    );
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div>
          <PageTitle>Mesa de {agent.nickname}</PageTitle>
          <p className="text-body-secondary mb-0">
            Caixa de entrada e acompanhamento das solicitações destinadas a este
            agente.
          </p>
        </div>
        <Link className="btn btn-outline-secondary btn-sm" to="/agents">
          Voltar aos agentes
        </Link>
      </div>

      <section className="card mb-4">
        <div className="card-body d-flex align-items-center gap-3">
          {agent.portraitUrl ? (
            <img
              src={resolveAssetUrl(agent.portraitUrl)}
              alt={`Figura mitológica de ${agent.nickname}`}
              className="rounded-circle border object-fit-cover"
              width={72}
              height={72}
            />
          ) : null}
          <div>
            <h2 className="h4 mb-1">{agent.nickname}</h2>
            <div>{agent.name}</div>
            <div className="small text-body-secondary">
              {openCount} tarefas abertas · status {agent.status}
            </div>
          </div>
        </div>
      </section>

      <div className="row g-4">
        <div className="col-lg-4">
          <section className="card">
            <div className="card-body">
              <h2 className="h5">Nova solicitação</h2>
              <form onSubmit={submit} className="d-grid gap-3">
                <div>
                  <label className="form-label" htmlFor="task-commercial-plan">
                    Plano comercial *
                  </label>
                  <select
                    id="task-commercial-plan"
                    className="form-select"
                    required
                    value={selectedPlanId ?? ""}
                    onChange={(event) =>
                      setCommercialPlanId(Number(event.target.value))
                    }
                  >
                    <option value="" disabled>
                      Selecione o objetivo comum
                    </option>
                    {(plans.data ?? []).map((plan) => (
                      <option key={plan.id} value={plan.id}>
                        {plan.name}
                      </option>
                    ))}
                  </select>
                  {currentPlanVersion ? (
                    <small className="text-body-secondary">
                      A tarefa será registrada no contexto v{currentPlanVersion}
                      .
                    </small>
                  ) : null}
                </div>
                <div>
                  <label className="form-label" htmlFor="requester-name">
                    Solicitante
                  </label>
                  <input
                    id="requester-name"
                    className="form-control"
                    maxLength={100}
                    required
                    value={form.requestedByName}
                    onChange={(event) =>
                      setForm({ ...form, requestedByName: event.target.value })
                    }
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="task-title">
                    Tarefa
                  </label>
                  <input
                    id="task-title"
                    className="form-control"
                    maxLength={160}
                    required
                    value={form.title}
                    onChange={(event) =>
                      setForm({ ...form, title: event.target.value })
                    }
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="task-description">
                    Resultado esperado
                  </label>
                  <textarea
                    id="task-description"
                    className="form-control"
                    rows={5}
                    required
                    value={form.description}
                    onChange={(event) =>
                      setForm({ ...form, description: event.target.value })
                    }
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="task-priority">
                    Prioridade
                  </label>
                  <select
                    id="task-priority"
                    className="form-select"
                    value={form.priority}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        priority: event.target.value as AgentTask["priority"],
                      })
                    }
                  >
                    <option value="LOW">Baixa</option>
                    <option value="NORMAL">Normal</option>
                    <option value="HIGH">Alta</option>
                    <option value="URGENT">Urgente</option>
                  </select>
                </div>
                <button
                  className="btn btn-primary"
                  disabled={
                    create.isPending || !selectedPlanId || !currentPlanVersion
                  }
                >
                  Enviar para {agent.nickname}
                </button>
              </form>
            </div>
          </section>
        </div>

        <div className="col-lg-8">
          <section className="card">
            <div className="card-body">
              <h2 className="h5">Caixa de entrada</h2>
              {inbox.isLoading ? <p>Carregando tarefas...</p> : null}
              <div className="d-grid gap-3">
                {(inbox.data ?? []).map((task) => (
                  <article key={task.id} className="border rounded p-3">
                    {(() => {
                      const experimentId = experimentIdFromTask(task);
                      const experiment = (experiments.data ?? []).find(
                        (item) => item.id === experimentId,
                      );
                      const failureReason = experiment?.creativeGenerationError;
                      const violations =
                        experiment?.creativeMetaCopyViolations ?? [];
                      if (
                        task.assignedAgentKey !== "meta-ad-approver" ||
                        task.status !== "BLOCKED" ||
                        !failureReason
                      ) {
                        return null;
                      }
                      return (
                        <div className="alert alert-danger mb-3" role="alert">
                          <strong>Corrija antes de reenfileirar Têmis</strong>
                          <div className="small mt-1">{failureReason}</div>
                          {violations.length > 0 ? (
                            <ul className="small mb-0 mt-2">
                              {violations.map((violation) => (
                                <li key={violation.field}>
                                  {metaFieldLabel[violation.field]}:{" "}
                                  {violation.actualLength}/{violation.maxLength}{" "}
                                  caracteres
                                </li>
                              ))}
                            </ul>
                          ) : null}
                        </div>
                      );
                    })()}
                    <div className="d-flex justify-content-between gap-3">
                      <div>
                        <h3 className="h6 mb-1">{task.title}</h3>
                        {task.taskKind === "GATE_DECISION" ? (
                          <span className="badge text-bg-warning mb-1">
                            Gate · {task.gateCode}
                          </span>
                        ) : null}
                        <div className="small text-body-secondary">
                          Solicitado por {task.requestedByName} ·{" "}
                          {task.requestedByType === "AGENT"
                            ? "agente"
                            : "usuário"}
                        </div>
                      </div>
                      <div className="text-end">
                        <span className="badge text-bg-light">
                          {statusLabel[task.status]}
                        </span>
                        <div className="small mt-1">{task.priority}</div>
                      </div>
                    </div>
                    <p className="mt-3 mb-2 text-break">{task.description}</p>
                    {task.gateStatus ? (
                      <div className="small mb-2">
                        Decisão do gate: <strong>{task.gateStatus}</strong>
                        {task.gateDecisionReason
                          ? ` · ${task.gateDecisionReason}`
                          : ""}
                      </div>
                    ) : null}
                    <div className="small text-body-secondary mb-3">
                      {new Date(task.createdAt).toLocaleString("pt-BR")}
                    </div>
                    <div className="d-flex flex-wrap gap-2">
                      {(task.taskKind === "GATE_DECISION" &&
                      task.gateStatus === "PENDING"
                        ? nextStatuses[task.status].filter(
                            (status) => status === "IN_PROGRESS",
                          )
                        : task.taskKind === "GATE_DECISION"
                          ? []
                          : nextStatuses[task.status]
                      ).map((status) => (
                        <button
                          key={status}
                          className="btn btn-sm btn-outline-primary"
                          disabled={updateStatus.isPending}
                          onClick={() =>
                            updateStatus.mutate({ id: task.id, status })
                          }
                        >
                          {statusLabel[status]}
                        </button>
                      ))}
                    </div>
                  </article>
                ))}
                {!inbox.isLoading && (inbox.data ?? []).length === 0 ? (
                  <p className="text-body-secondary mb-0">
                    Nenhuma solicitação recebida.
                  </p>
                ) : null}
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
