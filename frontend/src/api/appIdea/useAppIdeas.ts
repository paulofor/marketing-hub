import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface AppIdea {
  id: number;
  name: string;
  niche: string;
  targetAudience?: string;
  problemToSolve?: string;
  valueProposition?: string;
  coreFeatures?: string;
  differentiator?: string;
  monetization?: string;
  goToMarket?: string;
  technologyStack?: string;
  model?: string;
  prompt?: string;
  createdAt: string;
  updatedAt: string;
}

export function useAppIdeas() {
  return useQuery({
    queryKey: ["app-ideas"],
    queryFn: async () => {
      const { data } = await axios.get<AppIdea[]>("/api/app-ideas");
      return data;
    },
  });
}
