import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  TargetingElementSource,
  TargetingElementStatus,
  TargetingElementType,
} from "./types";

export interface UpdateTargetingElementPayload {
  id: number;
  term?: string;
  description?: string | null;
  prompt?: string | null;
  model?: string | null;
  source?: TargetingElementSource | null;
  status?: TargetingElementStatus;
  notes?: string | null;
  lastReviewedBy?: string | null;
  metaId?: string | null;
  metaKey?: string | null;
  confidence?: number | null;
  type?: TargetingElementType;
}

export function useUpdateTargetingElement(nicheId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: UpdateTargetingElementPayload) => {
      const { data } = await axios.patch(`/api/targeting-elements/${id}`, payload);
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["niche-targeting-elements"] });
      if (nicheId) {
        queryClient.invalidateQueries({ queryKey: ["niche", Number(nicheId)] });
      }
      queryClient.invalidateQueries({
        queryKey: ["targeting-element", variables.id],
      });
    },
  });
}
