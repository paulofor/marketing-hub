import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { OpsMonitorModule } from "./useOpsMonitorModules";

export type OpsMonitorModulePayload = Omit<
  OpsMonitorModule,
  "id" | "createdAt" | "updatedAt"
>;

export function useCreateOpsMonitorModule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: OpsMonitorModulePayload) => {
      const { data } = await axios.post<OpsMonitorModule>(
        "/api/ops-monitor/v1/modules",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ops-monitor", "modules"] });
      queryClient.invalidateQueries({ queryKey: ["ops-monitor"] });
    },
  });
}
