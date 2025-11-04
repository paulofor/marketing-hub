import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateApprovalPayload {
  id: number;
  approved: boolean;
}

export function useUpdateLeadPortalFlowApproval() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, approved }: UpdateApprovalPayload) => {
      const { data } = await axios.patch(`/api/lead-portal-flows/${id}/approval`, {
        approved,
      });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lead-portal-flows"] });
    },
  });
}
