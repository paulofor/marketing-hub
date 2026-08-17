import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Creative } from "./useCreatives";

interface SubmitProductProofPayload {
  experimentId: string;
  source: Creative;
  file: File;
}

/** Cria uma versão com prova visual real e a devolve ao gate obrigatório de Têmis. */
export function useSubmitProductProof(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ source, file }: SubmitProductProofPayload) => {
      const form = new FormData();
      form.append("file", file, file.name);
      form.append("category", "EXPERIMENT_CREATIVE");
      form.append("experimentId", experimentId);
      form.append(
        "prompt",
        "Prova visual real do produto fornecida para corrigir a demonstração do anúncio.",
      );

      const { data: asset } = await axios.post<{ url: string }>(
        "/api/assets",
        form,
      );
      const { data: version } = await axios.post<Creative>(
        `/api/creatives/${source.id}/versions`,
        {
          format: source.format || "IMAGE",
          headline: source.headline,
          primaryText: source.primaryText,
          imageUrl: asset.url,
          description: source.description || "",
          cta: source.cta || "LEARN_MORE",
          destinationUrl: source.destinationUrl || "",
          leadGenFormId: source.leadGenFormId || "",
          instagramUserId: source.instagramUserId || "",
          status: "DRAFT",
        },
      );
      const { data: submitted } = await axios.post<Creative>(
        `/api/creatives/${version.id}/agent-review/request`,
      );
      return submitted;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["creatives", experimentId] }),
  });
}
