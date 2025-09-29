import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
export interface FacebookAccountPayload {
  id?: number;
  name: string;
  currency: string;
  accessToken?: string | null;
  tokenExpiresAt?: string | null;
  tokenLastRefreshedAt?: string | null;
  authorizedUserId?: string | null;
  authorizedUserName?: string | null;
  authorizedUserEmail?: string | null;
  appId?: string | null;
  businessManagerAppId?: string | null;
  appSecret?: string | null;
  tokenRenewalEnabled?: boolean;
  adAccountId?: string | null;
  defaultPageId?: string | null;
  defaultWebsiteUrl?: string | null;
  defaultInstagramActorId?: string | null;
  defaultCreativeMessageTemplate?: string | null;
  defaultCallToActionType?: string | null;
  adSetDailyBudget?: string | null;
  adSetBillingEvent?: string | null;
  adSetOptimizationGoal?: string | null;
  adSetDestinationType?: string | null;
  adSetBidStrategy?: string | null;
  adSetBidAmount?: string | null;
  adSetTargetCountry?: string | null;
  workerEnabled?: boolean;
}

export function useCreateFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: FacebookAccountPayload) =>
      axios.post("/api/accounts/facebook", account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}

export function useUpdateFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: FacebookAccountPayload) =>
      axios.put(`/api/accounts/facebook/${account.id}`, account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}

export function useDeleteFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      axios.delete(`/api/accounts/facebook/${id}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}
