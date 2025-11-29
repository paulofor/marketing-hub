import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { FlowSubmissionImagePackageStatus, LeadPortalImagePackage } from "./useLeadPortalSubmissions";

export interface LeadPortalImageReference {
  type: "ORIGINAL" | "GENERATED" | string;
  url?: string | null;
  downloadUrl?: string | null;
  accessType?: string | null;
  assetId?: number | null;
  position?: number | null;
  prompt?: string | null;
  model?: string | null;
  createdAt?: string | null;
  itemId?: number | null;
  storedFileName?: string | null;
}

export interface LeadPortalImagePackageDetail extends LeadPortalImagePackage {
  status: FlowSubmissionImagePackageStatus;
  submission: {
    flowSlug?: string | null;
    name?: string | null;
    email?: string | null;
    phone?: string | null;
    imageQuestionKey?: string | null;
  };
  originalImage?: LeadPortalImageReference | null;
  generatedImages: LeadPortalImageReference[];
}

export function useLeadPortalImagePackageDetail(id?: number | null) {
  return useQuery<LeadPortalImagePackageDetail, Error>({
    queryKey: ["lead-portal-image-package", id],
    enabled: typeof id === "number" && id > 0,
    queryFn: async () => {
      if (!id) {
        throw new Error("Identificador do pacote é obrigatório");
      }
      const { data } = await axios.get<LeadPortalImagePackageDetail>(
        `/api/lead-portal/image-packages/${id}`,
      );
      return data;
    },
    staleTime: 30_000,
  });
}
