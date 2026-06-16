import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export function useRequestFacebookPixel(id?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      if (!id) {
        throw new Error("Nicho inválido para solicitação de pixel.");
      }
      const { data } = await axios.post<MarketNiche>(
        `/api/facebook-pixels/niches/${id}/request`,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["niche", id], data);
      queryClient.invalidateQueries({ queryKey: ["niches"] });
    },
  });
}
