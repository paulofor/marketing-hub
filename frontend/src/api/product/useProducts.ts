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
