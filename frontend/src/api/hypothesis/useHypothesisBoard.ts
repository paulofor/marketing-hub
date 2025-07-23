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
      const { data } = await axios.get<Record<string, Hypothesis[]>>(
        `/api/experiments/${experimentId}/hypotheses/board`,
      );
      return data;
    },
  });
}
