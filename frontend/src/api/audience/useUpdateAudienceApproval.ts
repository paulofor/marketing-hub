import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateAudienceApprovalPayload {
  id: number;
  approved: boolean;
}

export function useUpdateAudienceApproval(nicheId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, approved }: UpdateAudienceApprovalPayload) => {
      await axios.patch(`/api/audiences/${id}/approval`, { approved });
    },
    onSuccess: () => {
      if (nicheId) {
        queryClient.invalidateQueries({
          queryKey: ["niche-audiences", nicheId],
        });
      }
    },
  });
}
