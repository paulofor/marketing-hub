import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { CreateSalesVideoProfilePayload, SalesVideoProfile } from "./types";

export function useCreateSalesVideoProfile(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateSalesVideoProfilePayload) => {
      if (!productId) {
        throw new Error("Produto inválido para criação de perfil");
      }
      const { data } = await axios.post<SalesVideoProfile>(
        `/api/products/${productId}/sales-videos/profiles`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-profiles", productId] });
    },
  });
}
