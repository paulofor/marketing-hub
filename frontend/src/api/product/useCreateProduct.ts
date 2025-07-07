import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Product } from "./useProducts";

export interface CreateProduct {
  niche: string;
  avatar: string;
  explicitPain: string;
  promise: string;
  uniqueMechanism: string;
  tripwire: string;
  riskReversal: string;
  socialProof: string;
  checkoutMonetization: string;
  funnel: string;
  creativeVolume: string;
  storytelling: string;
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateProduct) => {
      const { data: product } = await axios.post<Product>("/api/products", data);
      return product;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });
}
