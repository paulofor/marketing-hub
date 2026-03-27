import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { DeliverablePackage } from "./types";
import type { CreateDeliverablePackagePayload } from "./useCreateDeliverablePackage";

export function useCreateHypothesisDeliverablePackage(hypothesisId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateDeliverablePackagePayload) => {
      const { data } = await axios.post<DeliverablePackage>(
        `/api/hypotheses/${hypothesisId}/deliverable-packages`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["deliverable-packages", "hypothesis", hypothesisId],
      });
      queryClient.invalidateQueries({ queryKey: ["deliverable-packages"] });
    },
  });
}
