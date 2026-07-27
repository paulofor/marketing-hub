import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoJob, SalesVideoProviderFamily } from "../salesVideo/types";

export type RequestOrganicVideoRenderPayload = {
  title: string;
  scriptText: string;
  hookText: string;
  ctaText: string;
  captionText: string;
  providerFamily: SalesVideoProviderFamily;
  providerName: string;
  targetDurationSeconds: number;
  metadataJson: string;
  requestedBy: string;
};

type CreatedProfile = {
  id: number;
};

export function useRequestOrganicVideoRender(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RequestOrganicVideoRenderPayload) => {
      if (!productId) {
        throw new Error("Produto inválido para render orgânico");
      }
      const { data: profile } = await axios.post<CreatedProfile>(
        `/api/products/${productId}/sales-videos/profiles`,
        {
          videoKind: "HERO",
          avatarStrategy: "PLATFORM_TEST_AVATAR",
          title: payload.title,
          personaName: "Criadora MUSA orgânica",
          personaStyle:
            "mulher brasileira adulta, elegante, acessível e natural para Reels/TikTok",
          voiceStyle: "direta, íntima, leve e nativa de rede social",
          language: "pt-BR",
          targetDurationSeconds: payload.targetDurationSeconds,
        },
      );
      await axios.post(
        `/api/sales-videos/profiles/${profile.id}/approve-script`,
        {
          scriptText: payload.scriptText,
          hookText: payload.hookText,
          ctaText: payload.ctaText,
          captionText: payload.captionText,
          approvedBy: payload.requestedBy,
        },
      );
      const { data: job } = await axios.post<SalesVideoJob>(
        `/api/sales-videos/profiles/${profile.id}/request-render`,
        {
          requestedBy: payload.requestedBy,
          providerFamily: payload.providerFamily,
          providerName: payload.providerName,
          executionMode: "TEST",
          metadataJson: payload.metadataJson,
        },
      );
      return job;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "organic-video-plan"],
      });
      queryClient.invalidateQueries({
        queryKey: ["sales-video-profiles", productId],
      });
      queryClient.invalidateQueries({
        queryKey: ["sales-video-provider-scores"],
      });
    },
  });
}
