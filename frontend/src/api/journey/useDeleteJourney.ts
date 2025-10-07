import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

export function useDeleteJourney(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      await axios.delete(`/api/journeys/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["journeys"] });
      queryClient.invalidateQueries({ queryKey: ["journeys", "metrics"] });
      toast.success("Jornada removida.");
    },
    onError: () => {
      toast.error("Não foi possível remover a jornada.");
    },
  });
}
