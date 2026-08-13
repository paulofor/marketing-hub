import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ProviderCreditPurchase = {
  id: number;
  provider: string;
  purchasedAt: string;
  amount: number;
  currency: string;
  creditsPurchased: number;
  evidenceReference?: string | null;
  createdAt: string;
};

export type RegisterProviderCreditPurchase = {
  purchasedAt: string;
  amount: number;
  currency: string;
  creditsPurchased: number;
  evidenceReference?: string | null;
};

export function useProviderCreditPurchases(provider?: string) {
  return useQuery({
    queryKey: ["provider-credit-purchases", provider],
    enabled: Boolean(provider),
    queryFn: async () => {
      const { data } = await axios.get<ProviderCreditPurchase[]>(
        `/api/financial-agent/v1/providers/${provider}/credit-purchases`,
      );
      return data;
    },
  });
}

export function useRegisterProviderCreditPurchase(provider?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RegisterProviderCreditPurchase) => {
      if (!provider) throw new Error("Provedor obrigatório");
      const { data } = await axios.post<ProviderCreditPurchase>(
        `/api/financial-agent/v1/providers/${provider}/credit-purchases`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["provider-credit-purchases", provider],
      });
      return queryClient.invalidateQueries({
        queryKey: ["financial-video-provider-credit-balances"],
      });
    },
  });
}
