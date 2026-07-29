import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OpsMonitorModule {
  id: number;
  code: string;
  name: string;
  type: string;
  baseUrl: string;
  healthPath: string;
  logPath?: string | null;
  publishedVersion?: string | null;
  productUrl?: string | null;
  monitoringUrl?: string | null;
  containerImageVersion?: string | null;
  enabled: boolean;
  criticality: string;
  offlineThresholdSeconds: number;
  createdAt?: string;
  updatedAt?: string;
}

export function useOpsMonitorModules() {
  return useQuery({
    queryKey: ["ops-monitor", "modules"],
    queryFn: async () => {
      const { data } = await axios.get<OpsMonitorModule[]>(
        "/api/ops-monitor/v1/modules",
      );
      return data;
    },
  });
}
