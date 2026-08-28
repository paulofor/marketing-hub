import { Link, useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useAgents } from "../../api/agent/useAgents";
import { useAgentMaturity } from "../../api/agent/useAgentMaturity";
import { useAgentWorkMonitor } from "../../api/agent/useAgentWorkMonitor";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { useState } from "react";
import CodexAuthReconnectPanel from "./CodexAuthReconnectPanel";
import AgentSessionSetupWizard from "./AgentSessionSetupWizard";
import { useAgentAutomaticExecution } from "../../api/agent/useAgentAutomaticExecution";

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

function formatContractChangeDate(value?: string) {
  if (!value) return "Não informada";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "Não informada";
  return parsed.toLocaleDateString("pt-BR", {
    timeZone: "America/Sao_Paulo",
  });
}

function daysWithoutContractChange(value?: string, now = Date.now()) {
  if (!value) return null;
  const changedAt = new Date(value).getTime();
  if (Number.isNaN(changedAt)) return null;
  return Math.max(0, Math.floor((now - changedAt) / MILLISECONDS_PER_DAY));
}

function formatDaysWithoutContractChange(value?: string) {
  const days = daysWithoutContractChange(value);
  if (days === null) return "Tempo não informado";
  return `${days} ${days === 1 ? "dia" : "dias"} sem alteração do contrato`;
}

function formatWorkflowRunDate(value?: string) {
  if (!value) return "Não comprovado";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "Não comprovado";
  return parsed.toLocaleDateString("pt-BR", {
    timeZone: "America/Sao_Paulo",
  });
}

function daysWithoutWorkflowRun(value?: string, now = Date.now()) {
  if (!value) return null;
  const completedAt = new Date(value).getTime();
  if (Number.isNaN(completedAt)) return null;
  return Math.max(0, Math.floor((now - completedAt) / MILLISECONDS_PER_DAY));
}

function formatDaysWithoutWorkflowRun(value?: string) {
  const days = daysWithoutWorkflowRun(value);
  if (days === null) return "Tempo não comprovado";
  return `${days} ${days === 1 ? "dia" : "dias"} desde o workflow`;
}

function formatWorkflowConclusion(value?: string) {
  if (value === "success") return "sucesso";
  if (value === "failure") return "falha";
  if (value === "cancelled") return "cancelado";
  return value;
}

const CODEX_EXECUTORS = new Set([
  "customer-agent",
  "financial-agent",
  "growth-operator",
  "experiment-strategist",
  "meta-ad-approver",
  "landing-generator",
  "communication-director",
  "videomaker",
  "market-radar",
]);

export default function AgentListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useAgents();
  const maturity = useAgentMaturity();
  const workMonitor = useAgentWorkMonitor();
  const automaticExecution = useAgentAutomaticExecution();
  const agents = Array.isArray(data) ? data : [];
  const agentsById = new Map(agents.map((agent) => [agent.id, agent]));
  const [reconnectAgent, setReconnectAgent] = useState<{
    id: number;
    nickname: string;
  } | null>(null);
  const [showSetupWizard, setShowSetupWizard] = useState(false);
  const codexAgents = (workMonitor.data ?? []).filter((item) =>
    CODEX_EXECUTORS.has(item.agentKey),
  );

  return (
    <div>
      <PageTitle>Gestão de agentes</PageTitle>
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
        <p className="mb-0 text-body-secondary">
          Defina responsabilidades, contexto, análises, entregáveis e regras de
          coordenação do Orquestrador.
        </p>
        <div className="d-flex flex-wrap gap-2">
          <Link className="btn btn-outline-secondary btn-sm" to="/agent-themes">
            Temas
          </Link>
          <div className="d-flex flex-wrap gap-2">
            <Link className="btn btn-outline-primary" to="/agents/personas">
              Biblioteca de Personas
            </Link>
            <Link className="btn btn-primary" to="/agents/new">
              Novo agente
            </Link>
          </div>
        </div>
      </div>

      <section className="card mb-4">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h2 className="h5">Monitor de trabalho dos agentes</h2>
              <p className="small text-body-secondary mb-0">
                Atualização automática a cada 15 segundos com tarefas,
                pipelines, dificuldades e decisões externas pendentes. STOP
                impede novos trabalhos automáticos sem interromper à força o
                trabalho já iniciado.
              </p>
            </div>
            <span className="badge text-bg-light">
              {
                (workMonitor.data ?? []).filter((item) =>
                  ["WORKING", "DECISION_REQUIRED"].includes(item.workStatus),
                ).length
              }{" "}
              ativos
            </span>
            <button
              type="button"
              className="btn btn-sm btn-primary"
              onClick={() => setShowSetupWizard(true)}
            >
              Preparar sessões Codex
            </button>
          </div>
          <div className="table-responsive mt-3">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Agente</th>
                  <th>Estado</th>
                  <th>Automação</th>
                  <th>Trabalho atual</th>
                  <th>Dificuldade / decisão</th>
                  <th>Executor</th>
                  <th className="text-end">Tokens hoje</th>
                  <th>Última atividade</th>
                </tr>
              </thead>
              <tbody>
                {(workMonitor.data ?? []).map((item) => (
                  <tr key={item.agentId}>
                    <td>
                      <Link
                        to={`/agents/${item.agentId}`}
                        className="fw-semibold"
                      >
                        {item.nickname}
                      </Link>
                      <div className="small text-body-secondary">
                        {item.agentName}
                      </div>
                    </td>
                    <td>
                      <span
                        className={`badge ${item.workStatus === "BLOCKED" ? "text-bg-danger" : item.workStatus === "DECISION_REQUIRED" ? "text-bg-warning" : item.workStatus === "WORKING" ? "text-bg-success" : "text-bg-light"}`}
                      >
                        {item.workStatus}
                      </span>
                    </td>
                    <td>
                      <div className="d-flex flex-column align-items-start gap-1">
                        <span
                          className={`badge ${item.automaticExecutionEnabled ? "text-bg-success" : "text-bg-secondary"}`}
                        >
                          {item.automaticExecutionStatus}
                        </span>
                        <button
                          type="button"
                          className={`btn btn-sm ${item.automaticExecutionEnabled ? "btn-outline-danger" : "btn-outline-success"}`}
                          disabled={
                            automaticExecution.isPending &&
                            automaticExecution.variables?.agentId ===
                              item.agentId
                          }
                          aria-label={
                            item.automaticExecutionEnabled
                              ? `Parar execução automática de ${item.nickname}`
                              : `Ativar execução automática de ${item.nickname}`
                          }
                          onClick={() =>
                            automaticExecution.mutate({
                              agentId: item.agentId,
                              automaticExecutionEnabled:
                                !item.automaticExecutionEnabled,
                            })
                          }
                        >
                          {automaticExecution.isPending &&
                          automaticExecution.variables?.agentId ===
                            item.agentId ? (
                            <span
                              className="spinner-border spinner-border-sm me-1"
                              aria-hidden="true"
                            />
                          ) : null}
                          {item.automaticExecutionEnabled ? "■ Stop" : "▶ Play"}
                        </button>
                      </div>
                    </td>
                    <td>
                      <div className="fw-semibold">{item.currentWork}</div>
                      <div className="small text-body-secondary">
                        {item.progressDetail}
                      </div>
                      {item.taskId || item.executionId ? (
                        <div className="small text-body-secondary">
                          {item.taskId ? `Tarefa #${item.taskId}` : null}
                          {item.taskId && item.executionId ? " · " : null}
                          {item.executionId
                            ? `Execução #${item.executionId}`
                            : null}
                        </div>
                      ) : null}
                    </td>
                    <td>
                      {item.externalDecision ||
                        item.difficulty ||
                        "Sem dificuldade registrada"}
                    </td>
                    <td>
                      <span
                        className={`badge ${item.executorHealth.status === "READY" ? "text-bg-success" : item.executorHealth.status === "BLOCKED" ? "text-bg-danger" : "text-bg-warning"}`}
                      >
                        {item.combinedStatus ?? item.executorHealth.status}
                      </span>
                      <div className="small text-body-secondary mt-1">
                        versão {item.executorHealth.deployedVersion ?? "?"}/
                        {item.executorHealth.expectedVersion} · backend{" "}
                        {item.executorHealth.backendAccessible ? "ok" : "falha"}{" "}
                        · Codex{" "}
                        {item.executorHealth.codexAuthenticated
                          ? "autenticado"
                          : "não comprovado"}
                      </div>
                      {item.executorHealth.detail ? (
                        <div className="small">
                          {item.executorHealth.detail}
                        </div>
                      ) : null}
                      {CODEX_EXECUTORS.has(item.agentKey) ? (
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary mt-2"
                          onClick={() =>
                            setReconnectAgent({
                              id: item.agentId,
                              nickname: item.nickname,
                            })
                          }
                        >
                          Reconectar Codex
                        </button>
                      ) : null}
                    </td>
                    <td className="text-end text-nowrap">
                      <span className="fw-semibold">
                        {item.dailyTokens.toLocaleString("pt-BR")}
                      </span>
                      <div className="small text-body-secondary">
                        entrada + saída
                      </div>
                    </td>
                    <td className="small">
                      {item.lastActivityAt
                        ? new Date(item.lastActivityAt).toLocaleString("pt-BR")
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {automaticExecution.isError ? (
            <div className="alert alert-danger py-2 mt-3 mb-0" role="alert">
              Não foi possível alterar a automação. O estado anterior foi
              preservado.
            </div>
          ) : null}
        </div>
      </section>

      {showSetupWizard ? (
        <AgentSessionSetupWizard
          agents={codexAgents}
          onAuthenticate={(item) =>
            setReconnectAgent({ id: item.agentId, nickname: item.nickname })
          }
          onClose={() => setShowSetupWizard(false)}
        />
      ) : null}

      {reconnectAgent ? (
        <CodexAuthReconnectPanel
          agentId={reconnectAgent.id}
          nickname={reconnectAgent.nickname}
          onClose={() => setReconnectAgent(null)}
        />
      ) : null}

      <section className="card mb-4">
        <div className="card-body">
          <h2 className="h5">Maturidade e fechamento de ciclos</h2>
          <p className="small text-body-secondary">
            Resultados confirmados e pendências compartilhadas. Hipóteses e
            simulações não contam como resultado.
          </p>
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Agente</th>
                  <th>Nível</th>
                  <th>Execuções</th>
                  <th>Conclusão</th>
                  <th>Pendências</th>
                  <th>Resultados confirmados</th>
                  <th>Próximo avanço</th>
                </tr>
              </thead>
              <tbody>
                {(maturity.data ?? []).map((item) => {
                  const agent = agentsById.get(item.agentId);
                  return (
                    <tr key={item.agentId}>
                      <td>
                        <div className="fw-semibold">
                          {agent?.nickname || item.agentName}
                        </div>
                        {agent?.nickname ? (
                          <div className="small text-body-secondary">
                            {item.agentName}
                          </div>
                        ) : null}
                      </td>
                      <td>
                        <span className="badge text-bg-light">
                          {item.maturityLevel}
                        </span>
                      </td>
                      <td>{item.executions}</td>
                      <td>{item.completionRate}%</td>
                      <td>
                        {item.openTasks} abertas / {item.resolvedTasks}{" "}
                        resolvidas
                      </td>
                      <td>{item.confirmedResults}</td>
                      <td className="small">{item.nextMaturityAction}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      {isLoading ? (
        <p>Carregando...</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Agente</th>
                <th>Tema</th>
                <th>Modo</th>
                <th>Status / versão</th>
                <th>Entradas</th>
                <th>Saídas</th>
                <th>Funções internas</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {agents.map((agent) => (
                <tr key={agent.id}>
                  <td>
                    <div className="d-flex align-items-center gap-3">
                      {agent.portraitUrl ? (
                        <img
                          src={resolveAssetUrl(agent.portraitUrl)}
                          alt={`Figura mitológica de ${agent.nickname}`}
                          className="rounded-circle border object-fit-cover"
                          width={56}
                          height={56}
                        />
                      ) : (
                        <div
                          className="rounded-circle border d-flex align-items-center justify-content-center text-body-secondary"
                          style={{ width: 56, height: 56, flexShrink: 0 }}
                          aria-label={`${agent.nickname} sem imagem`}
                        >
                          ◇
                        </div>
                      )}
                      <div>
                        <div className="fw-semibold">{agent.nickname}</div>
                        <div className="text-body-secondary small">
                          {agent.name}
                        </div>
                        <div className="text-body-secondary small">
                          {agent.description || "Sem descrição"}
                        </div>
                        <div
                          className="small mt-1"
                          data-agent-contract-recency={agent.id}
                        >
                          <div className="fw-semibold">
                            Última alteração do contrato:{" "}
                            {formatContractChangeDate(
                              agent.lastContractChangeAt,
                            )}
                          </div>
                          <div
                            className="text-body-secondary"
                            title="Fonte: criação da versão atual do contrato auditável do agente"
                          >
                            {formatDaysWithoutContractChange(
                              agent.lastContractChangeAt,
                            )}
                          </div>
                          <div className="fw-semibold mt-1">
                            Último workflow do executor:{" "}
                            {formatWorkflowRunDate(agent.lastWorkflowRunAt)}
                            {agent.workflowConclusion
                              ? ` (${formatWorkflowConclusion(agent.workflowConclusion)})`
                              : ""}
                          </div>
                          {agent.workflowName || agent.workflowFile ? (
                            <div className="small text-body-secondary">
                              Workflow:{" "}
                              {agent.workflowName || agent.workflowFile}
                            </div>
                          ) : null}
                          <div
                            className="text-body-secondary"
                            title="Fonte: GitHub Actions, workflow concluído na branch main"
                          >
                            {formatDaysWithoutWorkflowRun(
                              agent.lastWorkflowRunAt,
                            )}
                          </div>
                          {agent.workflowUrl ? (
                            <a
                              className="small"
                              href={agent.workflowUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              Ver evidência no GitHub
                            </a>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>{agent.themeName || "-"}</td>
                  <td>
                    <span className="badge text-bg-light">
                      {agent.executionMode}
                    </span>
                  </td>
                  <td>
                    <span className="badge text-bg-primary">
                      {agent.status}
                    </span>
                    <div className="small text-body-secondary mt-1">
                      v{agent.currentVersion}
                    </div>
                  </td>
                  <td>{agent.inputs?.length ?? 0}</td>
                  <td>{agent.outputs?.length ?? 0}</td>
                  <td>{agent.internalFunctions?.length ?? 0}</td>
                  <td className="text-end">
                    <div className="d-flex justify-content-end gap-2">
                      <Link
                        className="btn btn-sm btn-outline-secondary"
                        to={`/agents/${agent.id}/details`}
                      >
                        Detalhe do agente
                      </Link>
                      <button
                        className="btn btn-sm btn-primary"
                        onClick={() => navigate(`/agents/${agent.id}`)}
                      >
                        Abrir mesa
                      </button>
                      <button
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => navigate(`/agents/${agent.id}/edit`)}
                      >
                        Editar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {agents.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center text-body-secondary">
                    Nenhum agente cadastrado ainda.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
