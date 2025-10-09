import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type {
  JourneyTemplate,
  JourneyTemplateRequestPayload,
} from "./types";

export function useCreateJourneyTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: JourneyTemplateRequestPayload) => {
      const { data } = await axios.post<JourneyTemplate>(
        "/api/journey-templates",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["journey-templates"] });
    },
    onError: () => {
      toast.error("Não foi possível criar o template de jornada.");
    },
  });
}
