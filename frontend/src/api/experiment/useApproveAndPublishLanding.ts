import { useMutation } from "@tanstack/react-query";
import axios from "axios";

export interface LandingVariantLinks {
  variant: string;
  flowId: number;
  iframeUrl: string | null;
  standaloneUrl: string | null;
}

export interface LandingPublicationResult {
  experimentId: number;
  flowId: number;
  approved: boolean;
  published: boolean;
  publicUrl: string | null;
  variantLinks: LandingVariantLinks[];
  facebookPixelId: string | null;
  pixelAppliedAutomatically: boolean;
  message: string;
}

export function useApproveAndPublishLanding(experimentId: number) {
  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<LandingPublicationResult>(
        `/api/experiments/${experimentId}/pipeline/landing-page-html/approve-and-publish`,
      );
      return data;
    },
  });
}
