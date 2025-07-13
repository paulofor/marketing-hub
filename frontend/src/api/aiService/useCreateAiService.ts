import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { AiService } from "./useAiServices";

export interface CreateAiService {
  name: string;
  objective: string;
  url: string;
  price: number;
  cost: number;
}

export function useCreateAiService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateAiService) => {
      const { data: service } = await axios.post<AiService>(
        "/api/ai-services",
        data,
      );
      return service;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["aiServices"] });
    },
  });
}
