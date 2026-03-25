import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoProfile } from "./types";

export function useSalesVideoProfiles(productId?: string | number) {
  return useQuery({
    queryKey: ["sales-video-profiles", productId],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoProfile[]>(
        `/api/products/${productId}/sales-videos/profiles`,
      );
      return data;
    },
  });
}
