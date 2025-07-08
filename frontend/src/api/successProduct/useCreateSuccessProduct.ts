import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { SuccessProduct } from "./useSuccessProducts";

export interface CreateSuccessProduct {
  description: string;
}

export function useCreateSuccessProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateSuccessProduct) => {
      const { data: product } = await axios.post<SuccessProduct>(
        "/api/success-products",
        data,
      );
      return product;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["successProducts"] });
    },
  });
}
