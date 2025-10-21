import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Deliverable } from "./types";

export interface CreateDeliverablePayload {
  title: string;
  description?: string;
  content?: string;
  model?: string;
  prompt: string;
}

export function useCreateDeliverable(nicheId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateDeliverablePayload) => {
      const { data } = await axios.post<Deliverable>(
        `/api/niches/${nicheId}/deliverables`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["deliverables", "niche", nicheId] });
      queryClient.invalidateQueries({ queryKey: ["deliverables"] });
    },
  });
}
