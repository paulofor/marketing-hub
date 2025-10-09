import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { JourneyStep, JourneyStepRequestPayload } from "./types";

interface CreateJourneyStepInput {
  templateId: number;
  payload: JourneyStepRequestPayload;
}

export function useCreateJourneyStep() {
  return useMutation({
    mutationFn: async ({ templateId, payload }: CreateJourneyStepInput) => {
      const { data } = await axios.post<JourneyStep>(
        `/api/journey-templates/${templateId}/steps`,
        payload,
      );
      return data;
    },
    onError: () => {
      toast.error("Não foi possível salvar uma das etapas do template.");
    },
  });
}
