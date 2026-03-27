import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { DeliverablePackage } from "./types";

export function useDeliverablePackagesByHypothesis(hypothesisId?: string) {
  return useQuery({
    queryKey: ["deliverable-packages", "hypothesis", hypothesisId],
    queryFn: async () => {
      const { data } = await axios.get<DeliverablePackage[]>(
        `/api/hypotheses/${hypothesisId}/deliverable-packages`,
      );
      return data;
    },
    enabled: Boolean(hypothesisId),
  });
}
