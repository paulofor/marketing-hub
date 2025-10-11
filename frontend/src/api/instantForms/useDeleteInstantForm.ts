import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface DeleteInstantFormParams {
  id: number;
  hypothesisId?: string;
  experimentId?: string;
}

export function useDeleteInstantForm({ id, hypothesisId, experimentId }: DeleteInstantFormParams) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await axios.delete(`/api/instant-forms/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["instant-form", id] });
      if (hypothesisId) {
        queryClient.invalidateQueries({ queryKey: ["instant-forms", hypothesisId] });
      }
      if (experimentId) {
        queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] });
      }
    },
  });
}
