import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface CommercialCheckoutResponse {
  productKey: string;
  productId: number;
  experimentId: number;
  preferenceId?: string | null;
  checkoutUrl: string;
  amount: number;
  currency: string;
  deliveryPageUrl: string;
}

export function useCommercialCheckout(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<CommercialCheckoutResponse>(
        `/api/experiments/${experimentId}/commercial-checkout`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentId],
      });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
