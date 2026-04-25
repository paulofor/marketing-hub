import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { MoisWorkspaceDashboard } from "./types";

export function useMoisDashboard(workspaceId: string) {
  return useQuery({
    queryKey: ["mois", "dashboard", workspaceId],
    queryFn: async () => {
      const { data } = await axios.get<MoisWorkspaceDashboard>(`/api/v1/mois/workspaces/${workspaceId}/dashboard`);
      return data;
    },
    enabled: workspaceId.trim().length > 0,
  });
}
