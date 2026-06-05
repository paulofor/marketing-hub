import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface MarketNiche {
  id: number;
  name: string;
  description: string;
  interestCategory: string;
  roleCategory: string;
  facebookPixelId?: string | null;
  facebookPixelCode?: string | null;
  facebookPixelCreatedAt?: string | null;
  interestList?: string[];
  roleList?: string[];
  behaviorList?: string[];
  demandVolume: string;
  promises: string;
  offers: string;
  cost?: number | null;
  expense?: number | null;
  totalCost?: number | null;
  totalRevenue?: number | null;
  baseSegmentation: string;
  interests: string;
  demographicFilters: string;
  extraTips: string;
  hypothesesToGenerate?: number;
  hypothesisModel?: string;
  interestsToGenerate?: number;
  interestModel?: string;
  jobTitlesToGenerate?: number;
  jobTitleModel?: string;
  behaviorsToGenerate?: number;
  behaviorModel?: string;
  detailedDescriptionsToGenerate?: number;
  detailedDescriptionModel?: string;
  differentiatedTechnologyId?: number | null;
  hypothesisDetailedDescriptionId?: number | null;
  chatDialogId?: number;
  enrichedNicheProfileId?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export function useNiches() {
  return useQuery({
    queryKey: ["niches"],
    queryFn: async () => {
      const { data } = await axios.get<MarketNiche[]>("/api/niches");
      return data;
    },
  });
}
