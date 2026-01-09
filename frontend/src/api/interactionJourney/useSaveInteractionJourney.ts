import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { InteractionJourney } from "./types";

export function useSaveInteractionJourney() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: InteractionJourney) => {
      if (payload.id) {
        const { data } = await axios.put<InteractionJourney>(
          `/api/interaction-journeys/${payload.id}`,
          payload,
        );
        return data;
      }
      const { data } = await axios.post<InteractionJourney>(
        "/api/interaction-journeys",
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      toast.success("Jornada de interação salva com sucesso.");
      queryClient.invalidateQueries({ queryKey: ["interaction-journeys"] });
      if (data?.id) {
        queryClient.invalidateQueries({
          queryKey: ["interaction-journey", data.id.toString()],
        });
      }
    },
    onError: () => {
      toast.error("Não foi possível salvar a jornada de interação.");
    },
  });
}
