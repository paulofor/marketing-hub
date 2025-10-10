import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

export function useDeleteJourneyStep() {
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/journey-steps/${id}`);
    },
    onError: () => {
      toast.error("Não foi possível remover uma das etapas do template.");
    },
  });
}
