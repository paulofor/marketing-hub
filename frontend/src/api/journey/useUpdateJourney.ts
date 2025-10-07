import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { Journey, JourneyUpdatePayload } from "./types";

export function useUpdateJourney(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: JourneyUpdatePayload) => {
      const { data } = await axios.patch<Journey>(`/api/journeys/${id}`, payload);
      return data;
    },
    onSuccess: (journey) => {
      queryClient.invalidateQueries({ queryKey: ["journeys"] });
      queryClient.invalidateQueries({ queryKey: ["journeys", "metrics"] });
      queryClient.invalidateQueries({ queryKey: ["journeys", id] });
      toast.success(`Jornada "${journey.name}" atualizada.`);
    },
    onError: () => {
      toast.error("Não foi possível atualizar a jornada.");
    },
  });
}
