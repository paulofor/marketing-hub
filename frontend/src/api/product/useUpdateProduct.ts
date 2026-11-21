import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Product } from "./useProducts";
import { CreateProduct } from "./useCreateProduct";

export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: CreateProduct }) => {
      const { data: product } = await axios.put<Product>(
        `/api/products/${id}`,
        data,
      );
      return product;
    },
    onSuccess: (product) => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["product", product.id] });
      queryClient.invalidateQueries({ queryKey: ["financial-plan-catalog"] });
      queryClient.invalidateQueries({
        queryKey: ["financial-plans", "products", product.id],
      });
      queryClient.invalidateQueries({
        queryKey: ["financial-plan-preparation", product.id],
      });
      queryClient.invalidateQueries({
        queryKey: ["execution-finance", product.id],
      });
    },
  });
}
