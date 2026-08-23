import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface TerminalReconciliationResponse {
  experimentId: number;
  status: string;
  invalidated: boolean;
}

export function useReconcileExperimentTerminalState() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (experimentId: string) => {
      const { data } = await axios.post<TerminalReconciliationResponse>(
        `/api/experiments/${experimentId}/terminal-reconciliation`,
      );
      return data;
    },
    onSuccess: (_data, experimentId) => {
      queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({ queryKey: ["experiments-summary"] });
    },
  });
}
