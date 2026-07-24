import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  RequestSalesVideoPostProductionPayload,
  SalesVideoJob,
} from "./types";

export function useRequestSalesVideoPostProduction(
  jobId?: string | number,
  productId?: string | number,
  profileId?: string | number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RequestSalesVideoPostProductionPayload) => {
      if (!jobId) {
        throw new Error("Vídeo inválido para pós-produção");
      }
      const { data } = await axios.post<SalesVideoJob>(
        `/api/sales-videos/jobs/${jobId}/request-post-production`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["product-sales-video-jobs", productId],
      });
      queryClient.invalidateQueries({
        queryKey: ["sales-video-jobs", profileId],
      });
      queryClient.invalidateQueries({
        queryKey: ["sales-video-profile", profileId],
      });
    },
  });
}
