import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Hypothesis {
  id: number;
  experimentId: number;
  title: string;
  premiseAngleId?: number;
  offerType?: string;
  kpiTargetCpl?: number;
  status: string;
}

export function useHypothesisBoard(experimentId: string) {
  return useQuery({
    queryKey: ["hypothesis-board", experimentId],
    queryFn: async () => {
      const statuses = ["BACKLOG", "TESTING", "VALIDATED", "INVALIDATED"] as const;
      const entries = await Promise.all(
        statuses.map(async (s) => {
          const { data } = await axios.get<Hypothesis[]>(
            `/api/experiments/${experimentId}/hypotheses?status=${s}`,
          );
          return [s, data] as const;
        }),
      );
      return Object.fromEntries(entries) as Record<string, Hypothesis[]>;
    },
  });
}
