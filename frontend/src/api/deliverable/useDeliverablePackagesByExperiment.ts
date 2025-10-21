import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { DeliverablePackage } from "./types";

export function useDeliverablePackagesByExperiment(experimentId?: string | number) {
  return useQuery({
    queryKey: ["deliverable-packages", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<DeliverablePackage[]>(
        `/api/experiments/${experimentId}/deliverable-packages`,
      );
      return data;
    },
    enabled:
      experimentId !== undefined && experimentId !== null && experimentId !== "",
  });
}
