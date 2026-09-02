import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface ChangeProductAutomaticExecution {
  productId: number;
  automaticExecutionEnabled: boolean;
}

export interface ProductAutomaticExecutionControl {
  productId: number;
  automaticExecutionEnabled: boolean;
  automaticExecutionStatus: "PLAY" | "STOP";
  changedAt?: string | null;
  changedBy?: string | null;
}

/** Consulta PLAY/STOP somente quando a tela precisa oferecer um comando operacional. */
export function useProductAutomaticExecutionControl(
  productId?: string | number,
  enabled = true,
) {
  return useQuery({
    queryKey: ["product", productId, "automatic-execution"],
    enabled: Boolean(productId) && enabled,
    queryFn: async () =>
      (
        await axios.get<ProductAutomaticExecutionControl>(
          `/api/products/${productId}/automatic-execution`,
        )
      ).data,
  });
}

/** Persiste PLAY/STOP no produto e recarrega a verdade exibida no catálogo. */
export function useProductAutomaticExecution() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      productId,
      automaticExecutionEnabled,
    }: ChangeProductAutomaticExecution) => {
      const { data } = await axios.put<ProductAutomaticExecutionControl>(
        `/api/products/${productId}/automatic-execution`,
        { automaticExecutionEnabled },
      );
      return data;
    },
    onSuccess: async (control) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["products"] }),
        queryClient.invalidateQueries({
          queryKey: ["product", String(control.productId)],
        }),
        queryClient.invalidateQueries({
          queryKey: ["product", control.productId],
        }),
        queryClient.invalidateQueries({
          queryKey: [
            "product",
            String(control.productId),
            "automatic-execution",
          ],
        }),
      ]);
    },
  });
}
