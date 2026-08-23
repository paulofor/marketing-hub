import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Product } from "./useProducts";

export type UpdateProductInternalName = {
  internalName: string;
};

export function useUpdateProductInternalName() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      data,
    }: {
      id: number;
      data: UpdateProductInternalName;
    }) => {
      const response = await axios.patch<Product>(
        `/api/products/${id}/internal-name`,
        data,
      );
      return response.data;
    },
    onSuccess: (product) => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["product", product.id] });
    },
  });
}
