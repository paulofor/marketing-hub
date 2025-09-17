import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Audience } from "./useAudiencesByNiche";

interface UpdateAudienceInput {
  id: number;
  approved?: boolean;
}

export function useUpdateAudience(nicheId?: string) {
  const queryClient = useQueryClient();
  return useMutation<Audience, unknown, UpdateAudienceInput>({
    mutationFn: async ({ id, ...payload }: UpdateAudienceInput) => {
      const { data } = await axios.patch<Audience>(`/api/audiences/${id}`, payload);
      return data;
    },
    onSuccess: (_, variables) => {
      if (nicheId) {
        queryClient.invalidateQueries({ queryKey: ["niche-audiences", nicheId] });
      }
      queryClient.invalidateQueries({ queryKey: ["audiences"] });
      if (variables.id) {
        queryClient.invalidateQueries({ queryKey: ["audience", variables.id] });
      }
    },
  });
}
