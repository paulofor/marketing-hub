import { FormEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAgents } from "../../api/agent/useAgents";
import {
  useAgentTasks,
  useCreateAgentTask,
  useUpdateAgentTaskStatus,
} from "../../api/agentTask/useAgentTasks";
import { AgentTask, AgentTaskStatus } from "../../api/agentTask/types";
import AgentTaskModelUsage from "../../components/AgentTaskModelUsage";
import PageTitle from "../../components/PageTitle";
import {
  useCommercialPlans,
  useCommercialPlanVersions,
} from "../../api/planning/useCommercialPlans";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { useExperiments } from "../../api/experiment/useExperiments";
import { useBusinessProcesses } from "../../api/businessProcess/useBusinessProcesses";
import { useAgentWorkMonitor } from "../../api/agent/useAgentWorkMonitor";
import "./AgentWorkspacePage.css";

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

const RECENT_TASK_LIMIT = 5;

function experimentIdFromTask(task: AgentTask) {
  const match = task.sourceReference?.match(/^experiment:(\d+)$/);
  return match?.[1];
}

export default function AgentWorkspacePage() {
  const { id } = useParams();
  const agents = useAgents();
  const agent = (agents.data ?? []).find((item) => item.id === Number(id));
  const inbox = useAgentTasks(agent?.agentKey);
  const workMonitor = useAgentWorkMonitor();
  const create = useCreateAgentTask(agent?.agentKey);
  const updateStatus = useUpdateAgentTaskStatus(agent?.agentKey);
  const experiments = useExperiments();
  const plans = useCommercialPlans();
  const processes = useBusinessProcesses();
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
    processDefinitionId: "",
    processActivityId: "",
    exceptional: false,
    exceptionReason: "",
  });
  const publishedProcesses = (processes.data ?? []).filter(
    (process) => process.status === "PUBLISHED",
  );
  const selectedProcess = publishedProcesses.find(
    (process) => process.id === Number(form.processDefinitionId),
  );
  const availableActivities = (selectedProcess?.diagram.nodes ?? []).filter(
    (node) =>
      node.type === "TASK" &&
      (!node.owner ||
        node.owner
          .toLowerCase()
          .includes(agent?.nickname.toLowerCase() ?? "") ||
        node.owner
          .toLowerCase()
          .includes(agent?.agentKey?.toLowerCase() ?? "")),
  );
  const openCount = useMemo(
    () =>
      (inbox.data ?? []).filter((task) =>
        ["PENDING", "IN_PROGRESS", "BLOCKED"].includes(task.status),
      ).length,
    [inbox.data],
  );
  const recentTasks = (inbox.data ?? []).slice(0, RECENT_TASK_LIMIT);
  const activity = (workMonitor.data ?? []).find(
    (item) => item.agentId === Number(id),
  );
  const execution = activity?.executionActivity;
  const executionTokens =
    execution?.inputTokens != null || execution?.outputTokens != null
      ? (execution.inputTokens ?? 0) + (execution.outputTokens ?? 0)
      : null;

  if (agents.isLoading) return <p>Carregando agente...</p>;
  if (!agent) return <p>Agente não encontrado.</p>;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedPlanId || !currentPlanVersion) return;
    create.mutate(
      {
        ...form,
        processDefinitionId: form.exceptional
          ? undefined
          : Number(form.processDefinitionId),
        processActivityId: form.exceptional
          ? undefined
          : form.processActivityId,
        exceptionReason: form.exceptional ? form.exceptionReason : undefined,
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
            processDefinitionId: "",
            processActivityId: "",
            exceptional: false,
            exceptionReason: "",
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

      <section className="card mb-4" aria-labelledby="agent-activity-title">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3">
            <div>
              <h2 id="agent-activity-title" className="h5 mb-1">
                Atuação observável
              </h2>
              <p className="small text-body-secondary mb-0">
                Sinais técnicos persistidos pelo executor, atualizados a cada 15
                segundos.
              </p>
            </div>
            {activity ? (
              <span className="badge text-bg-light">
                {activity.combinedStatus}
              </span>
            ) : null}
          </div>
          {workMonitor.isLoading ? (
            <p className="mt-3 mb-0">Carregando atividade...</p>
          ) : null}
          {!workMonitor.isLoading && !activity ? (
            <p className="text-body-secondary mt-3 mb-0">
              O executor ainda não publicou sinais de atividade para este
              agente.
            </p>
          ) : null}
          {activity ? (
            <div className="row g-3 mt-1">
              <div className="col-md-4">
                <div className="small text-body-secondary">Trabalho atual</div>
                <div className="fw-semibold">{activity.currentWork}</div>
                {activity.progressDetail ? (
                  <div className="small">{activity.progressDetail}</div>
                ) : null}
              </div>
              <div className="col-md-4">
                <div className="small text-body-secondary">Último sinal</div>
                <div className="fw-semibold">
                  {activity.lastActivityAt
                    ? new Date(activity.lastActivityAt).toLocaleString("pt-BR")
                    : "Ainda não informado"}
                </div>
                <div className="small">
                  Executor {activity.executorHealth.status.toLowerCase()}
                </div>
              </div>
              <div className="col-md-4">
                <div className="small text-body-secondary">Tokens hoje</div>
                <div className="fw-semibold">
                  {activity.dailyTokens.toLocaleString("pt-BR")}
                </div>
                <div className="small text-body-secondary">
                  {executionTokens != null
                    ? `${executionTokens.toLocaleString("pt-BR")} tokens na execução atual`
                    : "O Codex ainda não informou o consumo desta execução"}
                </div>
              </div>
              {execution ? (
                <div className="col-12">
                  <div className="border rounded p-3 bg-body-tertiary">
                    <h3 className="h6 mb-3">Execução atual</h3>
                    <div className="row g-3">
                      <div className="col-sm-6 col-lg-3">
                        <div className="small text-body-secondary">
                          Processo
                        </div>
                        <div className="fw-semibold">
                          {execution.processAlive
                            ? "Ativo agora"
                            : "Sem processo ativo"}
                        </div>
                        <div className="small">Estado {execution.status}</div>
                      </div>
                      <div className="col-sm-6 col-lg-3">
                        <div className="small text-body-secondary">
                          Progresso técnico
                        </div>
                        <div className="fw-semibold">
                          {execution.eventCount.toLocaleString("pt-BR")} eventos
                        </div>
                        <div className="small">
                          {execution.outputBytes.toLocaleString("pt-BR")} bytes
                          produzidos
                        </div>
                      </div>
                      <div className="col-sm-6 col-lg-3">
                        <div className="small text-body-secondary">Início</div>
                        <div className="fw-semibold">
                          {execution.startedAt
                            ? new Date(execution.startedAt).toLocaleString(
                                "pt-BR",
                              )
                            : "Não informado"}
                        </div>
                        <div className="small">
                          Último evento:{" "}
                          {execution.lastEventType ?? "heartbeat"}
                        </div>
                      </div>
                      <div className="col-sm-6 col-lg-3">
                        <div className="small text-body-secondary">
                          Último heartbeat
                        </div>
                        <div className="fw-semibold">
                          {execution.lastHeartbeatAt
                            ? new Date(
                                execution.lastHeartbeatAt,
                              ).toLocaleString("pt-BR")
                            : "Não informado"}
                        </div>
                        <div
                          className={
                            execution.stale
                              ? "small text-danger"
                              : "small text-success"
                          }
                        >
                          {execution.stale
                            ? "Sem sinal há mais de 2 minutos"
                            : "Sinal dentro da janela esperada"}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="col-12 small text-body-secondary">
                  Ainda não há telemetria detalhada vinculada a esta execução.
                </div>
              )}
              {activity.difficulty ? (
                <div className="col-12">
                  <div className="alert alert-warning py-2 mb-0">
                    <strong>Bloqueio:</strong> {activity.difficulty}
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}
          <p className="small text-body-secondary mt-3 mb-0">
            Atividade e tokens confirmam execução técnica; a entrega e os gates
            do processo continuam sendo os critérios de qualidade.
          </p>
        </div>
      </section>

      <section className="card mb-4">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">Últimas tarefas</h2>
              <p className="small text-body-secondary mb-0">
                As {RECENT_TASK_LIMIT} solicitações mais recentes, incluindo
                trabalho concluído, bloqueado ou cancelado.
              </p>
            </div>
            <span className="badge text-bg-light">
              {recentTasks.length} exibidas
            </span>
          </div>
          {inbox.isLoading ? <p>Carregando histórico...</p> : null}
          {!inbox.isLoading && recentTasks.length === 0 ? (
            <p className="text-body-secondary mb-0">
              Nenhuma tarefa registrada para este agente.
            </p>
          ) : null}
          <div className="list-group list-group-flush">
            {recentTasks.map((task) => (
              <div
                key={task.id}
                className="list-group-item px-0 d-flex justify-content-between align-items-start gap-3"
              >
                <div>
                  <div className="fw-semibold">{task.title}</div>
                  <div className="small text-body-secondary">
                    Tarefa #{task.id} · atualizada em{" "}
                    {new Date(task.updatedAt).toLocaleString("pt-BR")}
                  </div>
                </div>
                <span className="badge text-bg-light text-nowrap">
                  {statusLabel[task.status]}
                </span>
              </div>
            ))}
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
                  <label className="form-label" htmlFor="task-process">
                    Processo publicado *
                  </label>
                  <select
                    id="task-process"
                    className="form-select"
                    required={!form.exceptional}
                    disabled={form.exceptional}
                    value={form.processDefinitionId}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        processDefinitionId: event.target.value,
                        processActivityId: "",
                      })
                    }
                  >
                    <option value="">Selecione o processo</option>
                    {publishedProcesses.map((process) => (
                      <option key={process.id} value={process.id}>
                        {process.name} · v{process.versionNumber}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label" htmlFor="task-activity">
                    Atividade do processo *
                  </label>
                  <select
                    id="task-activity"
                    className="form-select"
                    required={!form.exceptional}
                    disabled={form.exceptional || !selectedProcess}
                    value={form.processActivityId}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        processActivityId: event.target.value,
                      })
                    }
                  >
                    <option value="">Selecione a atividade</option>
                    {availableActivities.map((activity) => (
                      <option key={activity.id} value={activity.id}>
                        {activity.label}
                      </option>
                    ))}
                  </select>
                  {selectedProcess && availableActivities.length === 0 ? (
                    <small className="text-danger">
                      Este agente não é responsável por atividades desse
                      processo.
                    </small>
                  ) : null}
                </div>
                <div className="form-check">
                  <input
                    id="task-exceptional"
                    className="form-check-input"
                    type="checkbox"
                    checked={form.exceptional}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        exceptional: event.target.checked,
                        processDefinitionId: "",
                        processActivityId: "",
                      })
                    }
                  />
                  <label
                    className="form-check-label"
                    htmlFor="task-exceptional"
                  >
                    Atividade excepcional fora de processo
                  </label>
                </div>
                {form.exceptional ? (
                  <div>
                    <label
                      className="form-label"
                      htmlFor="task-exception-reason"
                    >
                      Justificativa da exceção *
                    </label>
                    <textarea
                      id="task-exception-reason"
                      className="form-control"
                      maxLength={500}
                      required
                      value={form.exceptionReason}
                      onChange={(event) =>
                        setForm({
                          ...form,
                          exceptionReason: event.target.value,
                        })
                      }
                    />
                  </div>
                ) : null}
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
                  <article
                    key={task.id}
                    className="agent-task-card border rounded p-3"
                  >
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
                    <div className="agent-task-card__header d-flex justify-content-between gap-3">
                      <div className="agent-task-card__identity">
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
                        <div className="small mt-1">
                          {task.exceptional ? (
                            <span className="agent-task-card__exception badge text-bg-warning">
                              Exceção · {task.exceptionReason}
                            </span>
                          ) : task.processActivityName ? (
                            <span>
                              {task.processCode} v{task.processVersionNumber} ·{" "}
                              {task.processActivityName}
                            </span>
                          ) : (
                            <span className="text-body-secondary">
                              Tarefa legada sem processo
                            </span>
                          )}
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
                    <div className="border rounded bg-body-tertiary p-2 mb-2">
                      <AgentTaskModelUsage usage={task} />
                    </div>
                    {task.gateStatus ? (
                      <div className="small mb-2">
                        Decisão do gate: <strong>{task.gateStatus}</strong>
                        {task.gateDecisionReason
                          ? ` · ${task.gateDecisionReason}`
                          : ""}
                      </div>
                    ) : null}
                    <div className="small text-body-secondary mb-3">
                      <div>
                        {task.receivedAt
                          ? `Recebida em: ${new Date(task.receivedAt).toLocaleString("pt-BR")}`
                          : "Aguardando recebimento pelo executor"}
                      </div>
                      <div>
                        Resultado entregue em:{" "}
                        {task.deliveredAt
                          ? new Date(task.deliveredAt).toLocaleString("pt-BR")
                          : "Ainda não entregue"}
                      </div>
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
