import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export interface CreateNiche {
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

export function useCreateNiche() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateNiche) => {
      const { data: niche } = await axios.post<MarketNiche>(
        "/api/niches",
        data,
      );
      return niche;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["niches"] });
    },
  });
}
