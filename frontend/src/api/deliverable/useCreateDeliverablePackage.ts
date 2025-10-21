import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { DeliverablePackage } from "./types";

export interface CreateDeliverablePackagePayload {
  name: string;
  description?: string;
  model?: string;
  prompt: string;
  deliverableIds: number[];
}

export function useCreateDeliverablePackage(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateDeliverablePackagePayload) => {
      const { data } = await axios.post<DeliverablePackage>(
        `/api/experiments/${experimentId}/deliverable-packages`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["deliverable-packages", experimentId],
      });
      queryClient.invalidateQueries({ queryKey: ["deliverable-packages"] });
    },
  });
}
