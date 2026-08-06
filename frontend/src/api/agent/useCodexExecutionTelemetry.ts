import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type CodexExecutionTelemetry = {
  status: "RUNNING" | "COMPLETED" | "FAILED";
  processId?: number | null;
  processAlive: boolean;
  eventCount: number;
  outputBytes: number;
  inputTokens?: number | null;
  outputTokens?: number | null;
  lastEventType?: string | null;
  lastActivityAt: string;
  startedAt: string;
  finishedAt?: string | null;
  stale: boolean;
};

/** Consulta sinais persistidos de progresso sem depender dos logs do worker. */
export function useCodexExecutionTelemetry(
  agentType: string,
  executionId: number,
) {
  return useQuery({
    queryKey: ["codex-execution-telemetry", agentType, executionId],
    retry: false,
    refetchInterval: 15_000,
    queryFn: async () =>
      (
        await axios.get<CodexExecutionTelemetry>(
          `/api/codex-agent-telemetry/v1/${agentType}/executions/${executionId}`,
        )
      ).data,
  });
}
