import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Hypothesis {
  id: string;
  marketNicheId: number;
  title: string;
  promise?: string;
  problem?: string;
  persona?: string;
  mechanism?: string;
  uniqueMechanism?: string;
  entrega?: string;
  successRule?: string;
  prompt?: string;
  model?: string;
  premiseAngleId?: number;
  offerType?: string;
  price?: number;
  kpiTargetCpl?: number;
  costUsd?: number;
  cost?: number | null;
  expense?: number | null;
  status: string;
  generatedAt?: string;
  createdAt?: string;
}

export function useHypothesisBoard(nicheId: string) {
  return useQuery({
    queryKey: ["hypothesis-board", nicheId],
    queryFn: async () => {
      const statuses = [
        "BACKLOG",
        "TESTING",
        "VALIDATED",
        "INVALIDATED",
      ] as const;
      const entries = await Promise.all(
        statuses.map(async (s) => {
          const { data } = await axios.get<Hypothesis[]>(
            `/api/niches/${nicheId}/hypotheses?status=${s}`,
          );
          return [s, data] as const;
        }),
      );
      return Object.fromEntries(entries) as Record<string, Hypothesis[]>;
    },
  });
}
