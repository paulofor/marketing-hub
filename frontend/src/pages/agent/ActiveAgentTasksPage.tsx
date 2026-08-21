import { Link } from "react-router-dom";
import { useActiveAgentTasks } from "../../api/agentTask/useAgentTasks";
import { AgentTaskStatus } from "../../api/agentTask/types";
import AgentTaskModelUsage from "../../components/AgentTaskModelUsage";
import AgentTaskFailureAudit from "../../components/AgentTaskFailureAudit";
import PageTitle from "../../components/PageTitle";

const statusLabel: Record<AgentTaskStatus, string> = {
  PENDING: "Aguardando",
  IN_PROGRESS: "Em execução",
  BLOCKED: "Bloqueada",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

const statusClass: Record<AgentTaskStatus, string> = {
  PENDING: "text-bg-warning",
  IN_PROGRESS: "text-bg-success",
  BLOCKED: "text-bg-danger",
  COMPLETED: "text-bg-primary",
  CANCELLED: "text-bg-secondary",
};

/** Exibe a fila operacional central com a responsabilidade explícita por tarefa. */
export default function ActiveAgentTasksPage() {
  const tasks = useActiveAgentTasks();
  const items = tasks.data ?? [];

  return (
    <div>
      <PageTitle>Tarefas dos agentes</PageTitle>
      <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
        <p className="text-body-secondary mb-0">
          Trabalho aguardando, em execução ou bloqueado, atualizado a cada 15
          segundos.
        </p>
        <span className="badge text-bg-light">
          {items.length} tarefas ativas
        </span>
      </div>

      {tasks.isLoading ? <p>Carregando tarefas...</p> : null}
      {tasks.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível consultar a fila de tarefas.
        </div>
      ) : null}
      {!tasks.isLoading && !tasks.isError && items.length === 0 ? (
        <div className="alert alert-success">Nenhuma tarefa ativa.</div>
      ) : null}

      {items.length > 0 ? (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Tarefa</th>
                <th>Executor</th>
                <th>Status</th>
                <th>Prioridade</th>
                <th>Situação / bloqueio</th>
                <th>Origem</th>
                <th>Processo / atividade</th>
                <th>IA / custo</th>
                <th>Recebimento / entrega</th>
              </tr>
            </thead>
            <tbody>
              {items.map((task) => (
                <tr key={task.id}>
                  <td>
                    <div className="fw-semibold">
                      #{task.id} · {task.title}
                    </div>
                    <div className="small text-body-secondary">
                      {task.sourceReference || "Sem referência"}
                    </div>
                  </td>
                  <td>
                    <Link
                      to={`/agents/${task.assignedAgentId}`}
                      className="fw-semibold"
                    >
                      {task.assignedAgentNickname}
                    </Link>
                    <div className="small text-body-secondary">
                      {task.assignedAgentKey}
                    </div>
                  </td>
                  <td>
                    <span className={`badge ${statusClass[task.status]}`}>
                      {statusLabel[task.status]}
                    </span>
                  </td>
                  <td>{task.priority}</td>
                  <td>
                    {task.status === "BLOCKED" && task.gateDecisionReason
                      ? task.gateDecisionReason
                      : task.description}
                  </td>
                  <td>{task.requestedByName}</td>
                  <td>
                    {task.exceptional
                      ? `Exceção: ${task.exceptionReason}`
                      : task.processActivityName
                        ? `${task.processCode} v${task.processVersionNumber} · ${task.processActivityName}`
                        : "Legada"}
                  </td>
                  <td className="text-nowrap">
                    <AgentTaskModelUsage usage={task} />
                    <AgentTaskFailureAudit audit={task.failureAudit} />
                  </td>
                  <td className="text-nowrap small">
                    <div>
                      {task.receivedAt
                        ? `Recebida: ${new Date(task.receivedAt).toLocaleString("pt-BR")}`
                        : "Aguardando recebimento pelo executor"}
                    </div>
                    <div>
                      Entregue:{" "}
                      {task.deliveredAt
                        ? new Date(task.deliveredAt).toLocaleString("pt-BR")
                        : "—"}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
