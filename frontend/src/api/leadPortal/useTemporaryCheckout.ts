import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface TemporaryCheckout {
  productKey: string;
  productName: string;
  redirectUrl: string;
  temporaryCheckoutUrl: string;
  commercialCheckoutUrl: string;
  testAmount: number;
  status: "ACTIVE" | "RESTORED";
  activatedAt: string;
  expiresAt: string;
}

export interface ActivateTemporaryCheckout {
  productKey: string;
  productName: string;
  testAmount: number;
  commercialCheckoutUrl: string;
  durationMinutes: number;
}

export function useTemporaryCheckout(productKey: string) {
  return useQuery({
    queryKey: ["temporary-checkout", productKey],
    queryFn: async () =>
      (
        await axios.get<TemporaryCheckout>(
          `/api/lead-portal/payments/temporary-checkout/${productKey}`,
        )
      ).data,
    enabled: Boolean(productKey),
    retry: false,
    refetchInterval: 30_000,
  });
}

export function useActivateTemporaryCheckout() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (payload: ActivateTemporaryCheckout) =>
      (
        await axios.post<TemporaryCheckout>(
          "/api/lead-portal/payments/temporary-checkout",
          payload,
        )
      ).data,
    onSuccess: (data) =>
      client.setQueryData(["temporary-checkout", data.productKey], data),
  });
}

export function useRestoreTemporaryCheckout() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (productKey: string) =>
      (
        await axios.post<TemporaryCheckout>(
          `/api/lead-portal/payments/temporary-checkout/${productKey}/restore`,
        )
      ).data,
    onSuccess: (data) =>
      client.setQueryData(["temporary-checkout", data.productKey], data),
  });
}
