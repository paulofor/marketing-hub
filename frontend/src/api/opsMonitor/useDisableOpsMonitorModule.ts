import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useDisableOpsMonitorModule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (code: string) => {
      await axios.delete(
        `/api/ops-monitor/v1/modules/${encodeURIComponent(code)}`,
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ops-monitor", "modules"] });
      queryClient.invalidateQueries({ queryKey: ["ops-monitor"] });
    },
  });
}
