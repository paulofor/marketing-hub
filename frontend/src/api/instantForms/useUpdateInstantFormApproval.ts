import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateInstantFormApprovalParams {
  id: number;
  hypothesisId?: string;
}

export function useUpdateInstantFormApproval({ id, hypothesisId }: UpdateInstantFormApprovalParams) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (approved: boolean) => {
      const { data } = await axios.patch(`/api/instant-forms/${id}/approval`, {
        approved,
      });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["instant-form", id] });
      if (hypothesisId) {
        queryClient.invalidateQueries({ queryKey: ["instant-forms", hypothesisId] });
      }
    },
  });
}
