import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface MarketNiche {
  id: number;
  name: string;
  description: string;
  interestCategory: string;
  roleCategory: string;
  interestList?: string[];
  roleList?: string[];
  demandVolume: string;
  promises: string;
  offers: string;
  cost?: number | null;
  expense?: number | null;
  baseSegmentation: string;
  interests: string;
  demographicFilters: string;
  extraTips: string;
  hypothesesToGenerate?: number;
  hypothesisModel?: string;
  audiencesToGenerate?: number;
  differentiatedTechnologyId?: number | null;
  chatDialogId?: number;
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
