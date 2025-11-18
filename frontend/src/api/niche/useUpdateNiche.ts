import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export function useUpdateNiche() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: MarketNiche) => {
      const { data: niche } = await axios.put<MarketNiche>(
        `/api/niches/${data.id}`,
        data,
      );
      return niche;
    },
    onSuccess: (niche) => {
      queryClient.setQueryData(["niche", niche.id], niche);
      queryClient.invalidateQueries({ queryKey: ["niches"] });
    },
  });
}
