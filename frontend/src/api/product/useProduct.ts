import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Product } from "./useProducts";

export function useProduct(id?: string | number) {
  return useQuery({
    queryKey: ["product", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<Product>(`/api/products/${id}`);
      return data;
    },
  });
}
