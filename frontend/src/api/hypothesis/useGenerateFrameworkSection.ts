import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { Hypothesis } from "./useHypothesisBoard";
import type { HypothesisFrameworkSection } from "./types";

interface GeneratePayload {
  section: HypothesisFrameworkSection;
  customInstructions?: string;
  model?: string;
}

export function useGenerateFrameworkSection(
  hypothesisId?: string,
  nicheId?: string,
) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ section, ...payload }: GeneratePayload) => {
      if (!hypothesisId) {
        throw new Error("Hipótese ainda não foi criada");
      }
      const { data } = await axios.post<Hypothesis>(
        `/api/hypotheses/${hypothesisId}/framework/${section}/generate`,
        {
          customInstructions: payload.customInstructions,
          model: payload.model,
        },
      );
      return data;
    },
    onSuccess: () => {
      if (nicheId) {
        qc.invalidateQueries({ queryKey: ["hypothesis-board", nicheId] });
      }
      qc.invalidateQueries({ queryKey: ["hypothesis", nicheId, hypothesisId] });
      toast.success("Seção atualizada com IA");
    },
    onError: () => {
      toast.error("Não foi possível gerar essa seção com IA");
    },
  });
}
