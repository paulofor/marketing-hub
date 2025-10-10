import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { JourneyStep, JourneyStepUpdatePayload } from "./types";

interface UpdateJourneyStepInput {
  id: number;
  payload: JourneyStepUpdatePayload;
}

export function useUpdateJourneyStep() {
  return useMutation({
    mutationFn: async ({ id, payload }: UpdateJourneyStepInput) => {
      const { data } = await axios.patch<JourneyStep>(
        `/api/journey-steps/${id}`,
        payload,
      );
      return data;
    },
    onError: () => {
      toast.error("Não foi possível atualizar uma das etapas do template.");
    },
  });
}
