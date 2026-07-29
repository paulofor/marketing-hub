import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { OpsMonitorModule } from "./useOpsMonitorModules";
import type { OpsMonitorModulePayload } from "./useCreateOpsMonitorModule";

export function useUpdateOpsMonitorModule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: OpsMonitorModule) => {
      const body: OpsMonitorModulePayload = {
        code: payload.code,
        name: payload.name,
        type: payload.type,
        baseUrl: payload.baseUrl,
        healthPath: payload.healthPath,
        logPath: payload.logPath,
        publishedVersion: payload.publishedVersion,
        productUrl: payload.productUrl,
        monitoringUrl: payload.monitoringUrl,
        containerImageVersion: payload.containerImageVersion,
        enabled: payload.enabled,
        criticality: payload.criticality,
        offlineThresholdSeconds: payload.offlineThresholdSeconds,
      };
      const { data } = await axios.put<OpsMonitorModule>(
        `/api/ops-monitor/v1/modules/${encodeURIComponent(payload.code)}`,
        body,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["ops-monitor", "modules"] });
      queryClient.invalidateQueries({
        queryKey: ["ops-monitor", "modules", data.code],
      });
      queryClient.invalidateQueries({ queryKey: ["ops-monitor"] });
    },
  });
}
