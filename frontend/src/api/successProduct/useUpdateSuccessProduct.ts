import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { SuccessProduct } from "./useSuccessProducts";

export function useUpdateSuccessProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (p: SuccessProduct) => {
      const { id, ...body } = p;
      const { data } = await axios.put<SuccessProduct>(
        `/api/success-products/${id}`,
        body,
      );
      return data;
    },
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["successProducts"] });
      qc.invalidateQueries({ queryKey: ["successProduct", data.id] });
    },
  });
}
