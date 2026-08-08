import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Product {
  id: number;
  slug?: string;
  name?: string;
  publicUrl?: string;
  logoUrl?: string;
  colorPalette?: string;
  targetAudience?: string;
  languageStyle?: string;
  codeModules?: string;
  productType?: string;
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

export function useProducts() {
  return useQuery({
    queryKey: ["products"],
    queryFn: async () => {
      const { data } = await axios.get<Product[]>("/api/products");
      return data;
    },
  });
}
