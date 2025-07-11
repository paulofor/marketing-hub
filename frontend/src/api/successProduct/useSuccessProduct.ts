import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SuccessProduct } from "./useSuccessProducts";

export function useSuccessProduct(id: number) {
  return useQuery({
    queryKey: ["successProduct", id],
    queryFn: async () => {
      const { data } = await axios.get<SuccessProduct>(
        `/api/success-products/${id}`,
      );
      return data;
    },
  });
}
