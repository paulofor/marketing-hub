import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { RequestSalesVideoMontagePayload, SalesVideoJob } from "./types";

export function useRequestSalesVideoMontage(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RequestSalesVideoMontagePayload) => {
      const { data } = await axios.post<SalesVideoJob>(
        "/api/sales-videos/jobs/request-montage",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["product-sales-video-jobs", productId],
      });
    },
  });
}
