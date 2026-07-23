import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoJob } from "./types";

export function useProductSalesVideoJobs(productId?: string | number) {
  return useQuery({
    queryKey: ["product-sales-video-jobs", productId],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoJob[]>(
        `/api/products/${productId}/sales-videos/jobs`,
      );
      return data;
    },
  });
}
