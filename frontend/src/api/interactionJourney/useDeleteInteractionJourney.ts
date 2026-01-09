import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

export function useDeleteInteractionJourney() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/interaction-journeys/${id}`);
    },
    onSuccess: () => {
      toast.success("Jornada de interação removida.");
      queryClient.invalidateQueries({ queryKey: ["interaction-journeys"] });
    },
    onError: () => {
      toast.error("Não foi possível remover a jornada de interação.");
    },
  });
}
