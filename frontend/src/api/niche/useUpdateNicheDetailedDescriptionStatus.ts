import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { NicheDetailedDescription } from "./useNicheDetailedDescriptions";

interface UpdateNicheDetailedDescriptionStatusPayload {
  nicheId: number | string;
  descriptionId: number;
  active: boolean;
}

export function useUpdateNicheDetailedDescriptionStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ nicheId, descriptionId, active }: UpdateNicheDetailedDescriptionStatusPayload) => {
      const { data } = await axios.patch<NicheDetailedDescription>(
        `/api/niches/${nicheId}/descriptions/${descriptionId}/active`,
        { active },
      );
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["niche-descriptions", variables.nicheId],
      });
    },
  });
}
