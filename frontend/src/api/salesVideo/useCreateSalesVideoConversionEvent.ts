import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { CreateSalesVideoConversionEventPayload, SalesVideoConversionEvent } from "./types";

export function useCreateSalesVideoConversionEvent(profileId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateSalesVideoConversionEventPayload) => {
      if (!profileId) throw new Error("Perfil inválido para registrar conversão");
      const { data } = await axios.post<SalesVideoConversionEvent>(
        `/api/sales-videos/profiles/${profileId}/conversion-events`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-performance-summary", profileId] });
    },
  });
}
