import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Product {
  id: number;
  slug?: string;
  name?: string;
  internalName?: string;
  aliases?: string[];
  publicUrl?: string;
  logoUrl?: string;
  colorPalette?: string;
  targetAudience?: string;
  languageStyle?: string;
  codeModules?: string;
  productType?: string;
  productTypeId?: number;
  productTypeCode?: string;
  productTypeInternalName?: string;
  productTypeStatus?: "PROPOSED" | "ACTIVE" | "RETIRED";
  productFormat?: string;
  deliveryMode?: string;
  revenueModel?: string;
  valueUnit?: string;
  valueEvidenceMetric?: string;
  validationDefinitionVersion?: string;
  validationDefinitionJson?: string;
  desireAssociationMapVersion?: string;
  desireAssociationMapJson?: string;
  commercialStatus?: string;
  automaticExecutionEnabled?: boolean;
  automaticExecutionStatus?: "PLAY" | "STOP";
  automaticExecutionChangedAt?: string | null;
  automaticExecutionChangedBy?: string | null;
  currentPriceBrl?: number;
  primaryHypothesisId?: string;
  primaryHypothesis?: string;
  associatedExperiments?: string;
  commercialNotes?: string;
  sevenDayJourney?: string;
  supportMaterialPositioning?: string;
  primaryCta?: string;
  niche: string;
  avatar: string;
  videoSeedImageAssetId?: number | null;
  videoSeedCharacterName?: string | null;
  videoSeedReviewStatus?: "PENDING" | "APPROVED" | "REJECTED" | null;
  videoSeedReviewNotes?: string | null;
  videoSeedReviewedBy?: string | null;
  videoSeedReviewedAt?: string | null;
  marketNicheId?: number;
  instagramAccountId?: number;
  explicitPain: string;
  promise: string;
  uniqueMechanism: string;
  tripwire: string;
  riskReversal: string;
  socialProof: string;
  scientificEvidencePack?: string;
  pdeExperienceJson?: string;
  checkoutMonetization: string;
  funnel: string;
  creativeVolume: string;
  storytelling: string;
  aiCost: number;
  createdAt?: string;
  updatedAt?: string;
}

export function useProducts(query?: string) {
  return useQuery({
    queryKey: ["products", query ?? ""],
    queryFn: async () => {
      const { data } = query?.trim()
        ? await axios.get<Product[]>("/api/products", {
            params: { query: query.trim() },
          })
        : await axios.get<Product[]>("/api/products");
      return data;
    },
  });
}
