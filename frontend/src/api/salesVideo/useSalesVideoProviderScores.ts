import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoProviderScore } from "./types";

export function useSalesVideoProviderScores() {
  return useQuery({
    queryKey: ["sales-video-provider-scores"],
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoProviderScore[]>(
        "/api/sales-videos/provider-scores",
      );
      return data;
    },
  });
}
