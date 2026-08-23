import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Product } from "./useProducts";

export interface CreateProduct {
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
  marketNicheId?: number;
  avatar: string;
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

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateProduct) => {
      const { data: product } = await axios.post<Product>(
        "/api/products",
        data,
      );
      return product;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });
}
