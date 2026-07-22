import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Product } from "./useProducts";

export function useApplyDefaultPdePersuasiveJourney() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (productId: number) => {
      const { data } = await axios.post<Product>(
        `/api/products/${productId}/pde-persuasive-journey/default`,
      );
      return data;
    },
    onSuccess: (product) => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({
        queryKey: ["product", String(product.id)],
      });
      queryClient.invalidateQueries({ queryKey: ["product", product.id] });
    },
  });
}
