import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface ChangeAutomaticExecution {
  agentId: number;
  automaticExecutionEnabled: boolean;
}

/** Persiste PLAY/STOP no backend e recarrega a verdade exibida no monitor. */
export function useAgentAutomaticExecution() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      agentId,
      automaticExecutionEnabled,
    }: ChangeAutomaticExecution) =>
      (
        await axios.put(
          `/api/agents/work-monitor/${agentId}/automatic-execution`,
          { automaticExecutionEnabled },
        )
      ).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ["agents", "work-monitor"],
      });
    },
  });
}
