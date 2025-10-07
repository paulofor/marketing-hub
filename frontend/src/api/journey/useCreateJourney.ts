import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { Journey, JourneyRequestPayload } from "./types";

export function useCreateJourney() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: JourneyRequestPayload) => {
      const { data } = await axios.post<Journey>("/api/journeys", payload);
      return data;
    },
    onSuccess: (journey) => {
      queryClient.invalidateQueries({ queryKey: ["journeys"] });
      queryClient.invalidateQueries({ queryKey: ["journeys", "metrics"] });
      toast.success(`Jornada "${journey.name}" criada com sucesso.`);
    },
    onError: () => {
      toast.error("Não foi possível criar a jornada.");
    },
  });
}
