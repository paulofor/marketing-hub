import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface MarketNiche {
  id: number;
  name: string;
  description: string;
  demandVolume: string;
  promises: string;
  offers: string;
  baseSegmentation: string;
  interests: string;
  demographicFilters: string;
  extraTips: string;
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
