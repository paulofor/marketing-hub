import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export function useNiche(id: number) {
  return useQuery({
    queryKey: ["niche", id],
    queryFn: async () => {
      const { data } = await axios.get<MarketNiche>(`/api/niches/${id}`);
      return data;
    },
    enabled: !!id,
  });
}
