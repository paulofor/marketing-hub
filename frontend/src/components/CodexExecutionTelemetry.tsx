import { useCodexExecutionTelemetry } from "../api/agent/useCodexExecutionTelemetry";

/** Exibe progresso observável e deixa explícito quando tokens não foram informados. */
export default function CodexExecutionTelemetry({
  agentType,
  executionId,
}: {
  agentType: string;
  executionId: number;
}) {
  const telemetry = useCodexExecutionTelemetry(agentType, executionId);
  if (!telemetry.data) return null;
  const value = telemetry.data;
  return (
    <div
      className={`alert py-2 mt-2 mb-2 ${value.stale ? "alert-warning" : "alert-info"}`}
      data-testid="codex-execution-telemetry"
    >
      <div>
        <strong>Execução Codex:</strong>{" "}
        {value.processAlive ? "processando" : value.status.toLowerCase()}
        {value.stale ? " · sem heartbeat recente" : ""}
      </div>
      <small>
        Última atividade:{" "}
        {new Date(value.lastActivityAt).toLocaleString("pt-BR")} · eventos:{" "}
        {value.eventCount} · saída: {value.outputBytes.toLocaleString("pt-BR")}{" "}
        bytes
        {value.inputTokens != null || value.outputTokens != null
          ? ` · tokens: ${(value.inputTokens ?? 0) + (value.outputTokens ?? 0)}`
          : " · tokens: não informados pelo Codex"}
      </small>
    </div>
  );
}
