import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type {
  JourneyTemplate,
  JourneyTemplateUpdatePayload,
} from "./types";

interface UpdateJourneyTemplateInput {
  id: number;
  payload: JourneyTemplateUpdatePayload;
}

export function useUpdateJourneyTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, payload }: UpdateJourneyTemplateInput) => {
      const { data } = await axios.patch<JourneyTemplate>(
        `/api/journey-templates/${id}`,
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["journey-templates"] });
      if (data?.id) {
        queryClient.invalidateQueries({
          queryKey: ["journey-template", data.id],
        });
      }
    },
    onError: () => {
      toast.error("Não foi possível atualizar o template de jornada.");
    },
  });
}
