import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type OprmJobStatus =
  | "PENDING"
  | "CLAIMED"
  | "RUNNING"
  | "SUCCEEDED"
  | "FAILED"
  | "RETRY_WAIT"
  | "CANCELLED";

export interface OprmWorkspaceOccupation {
  occupationSeedRef: string;
  lastJobStatus: OprmJobStatus;
  lastCorrelationId: string;
  lastUpdatedAt: string;
}

export function useOprmWorkspaceOccupations() {
  return useQuery({
    queryKey: ["oprm", "workspace", "occupations"],
    queryFn: async () => {
      const { data } = await axios.get<OprmWorkspaceOccupation[]>(
        "/api/oprm/jobs/workspace/occupations",
      );
      return data;
    },
  });
}
