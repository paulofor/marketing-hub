import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { OpsMonitorModule } from "./useOpsMonitorModules";

export function useOpsMonitorModule(code: string) {
  return useQuery({
    queryKey: ["ops-monitor", "modules", code],
    enabled: Boolean(code),
    queryFn: async () => {
      const { data } = await axios.get<OpsMonitorModule>(
        `/api/ops-monitor/v1/modules/${encodeURIComponent(code)}`,
      );
      return data;
    },
  });
}
