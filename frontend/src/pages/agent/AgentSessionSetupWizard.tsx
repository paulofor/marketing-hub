import { AgentWorkMonitor } from "../../api/agent/useAgentWorkMonitor";
import {
  useAgentExecutorOperation,
  useStartAgentExecutorOperation,
} from "../../api/agent/useAgentExecutorOperation";

interface Props {
  agents: AgentWorkMonitor[];
  onAuthenticate: (agent: AgentWorkMonitor) => void;
  onClose: () => void;
}

/** Conduz atualização, reinício e autenticação dos executores Codex em sequência. */
export default function AgentSessionSetupWizard({
  agents,
  onAuthenticate,
  onClose,
}: Props) {
  const current =
    agents.find(
      (item) =>
        !item.executorHealth.versionCurrent ||
        !item.executorHealth.backendAccessible ||
        !item.executorHealth.codexAuthenticated,
    ) ?? null;
  const operation = useAgentExecutorOperation(current?.agentId);
  const startOperation = useStartAgentExecutorOperation();
  const operationRunning = ["REQUESTED", "RUNNING"].includes(
    operation.data?.status ?? "",
  );

  return (
    <section
      className="card border-primary mb-4"
      aria-label="Assistente de sessões Codex"
    >
      <div className="card-body">
        <div className="d-flex justify-content-between gap-3">
          <div>
            <h2 className="h5">Preparar {agents.length} agentes Codex</h2>
            <p className="small text-body-secondary mb-0">
              O assistente usa a versão e a saúde confirmadas pelo backend e
              avança um agente por vez.
            </p>
          </div>
          <button className="btn-close" aria-label="Fechar" onClick={onClose} />
        </div>

        <ol className="mt-3 mb-3">
          {agents.map((agent) => {
            const ready =
              agent.executorHealth.versionCurrent &&
              agent.executorHealth.backendAccessible &&
              agent.executorHealth.codexAuthenticated;
            return (
              <li key={agent.agentId} className={ready ? "text-success" : ""}>
                {agent.nickname}:{" "}
                {ready ? "pronto" : agent.executorHealth.status}
              </li>
            );
          })}
        </ol>

        {!current ? (
          <div className="alert alert-success mb-0">
            Todos os agentes estão na versão esperada, acessam o backend e têm
            autenticação comprovada.
          </div>
        ) : (
          <div className="alert alert-light border mb-0">
            <div className="fw-semibold">Próximo: {current.nickname}</div>
            {!current.executorHealth.versionCurrent ? (
              <>
                <p className="small mb-2">
                  Atualize da versão{" "}
                  {current.executorHealth.deployedVersion ?? "?"} para{" "}
                  {current.executorHealth.expectedVersion}.
                </p>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={startOperation.isPending || operationRunning}
                  onClick={() =>
                    startOperation.mutate({
                      agentId: current.agentId,
                      operation: "UPDATE",
                    })
                  }
                >
                  {startOperation.isPending || operationRunning ? (
                    <span className="spinner-border spinner-border-sm me-2" />
                  ) : null}
                  Atualizar executor
                </button>
              </>
            ) : !current.executorHealth.backendAccessible ? (
              <button
                type="button"
                className="btn btn-outline-primary"
                disabled={startOperation.isPending || operationRunning}
                onClick={() =>
                  startOperation.mutate({
                    agentId: current.agentId,
                    operation: "RESTART",
                  })
                }
              >
                {startOperation.isPending || operationRunning ? (
                  <span className="spinner-border spinner-border-sm me-2" />
                ) : null}
                Reiniciar executor
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => onAuthenticate(current)}
              >
                Criar sessão de {current.nickname}
              </button>
            )}
            {operation.data ? (
              <p className="small mt-2 mb-0">
                Operação {operation.data.status}
                {operation.data.detail ? `: ${operation.data.detail}` : ""}
              </p>
            ) : null}
            {startOperation.isError ? (
              <p className="text-danger small mt-2 mb-0">
                Não foi possível solicitar a operação.
              </p>
            ) : null}
          </div>
        )}
      </div>
    </section>
  );
}
