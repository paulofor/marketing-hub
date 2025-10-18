import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { AppIdea } from "./useAppIdeas";

export interface CreateAppIdea {
  name: string;
  marketNicheId: number;
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
}

export function useCreateAppIdea() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateAppIdea) => {
      const { data } = await axios.post<AppIdea>("/api/app-ideas", payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["app-ideas"] });
    },
  });
}
