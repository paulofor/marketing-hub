import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FacebookAccountFinancialStrategy {
  dailyBudget?: string | null;
  billingEvent?: string | null;
  optimizationGoal?: string | null;
  destinationType?: string | null;
  bidStrategy?: string | null;
  bidAmount?: string | null;
  targetCountry?: string | null;
}

export interface FacebookAccount {
  id: number;
  name: string;
  currency: string;
  accessToken?: string | null;
  tokenExpiresAt?: string | null;
  tokenLastRefreshedAt?: string | null;
  authorizedUserId?: string | null;
  authorizedUserName?: string | null;
  authorizedUserEmail?: string | null;
  tokenExpired?: boolean;
  requiresTokenRenewal?: boolean;
  tokenExpiresInDays?: number | null;
  appId?: string | null;
  hasAppSecret?: boolean;
  tokenRenewalEnabled?: boolean;
  tokenRenewalStatus?: string | null;
  tokenRenewalLastAttemptAt?: string | null;
  tokenRenewedAt?: string | null;
  tokenRenewalLastError?: string | null;
  adAccountId?: string | null;
  defaultWebsiteUrl?: string | null;
  defaultCreativeMessageTemplate?: string | null;
  defaultCallToActionType?: string | null;
  financialStrategy?: FacebookAccountFinancialStrategy;
  adSetDailyBudget?: string | null;
  adSetBillingEvent?: string | null;
  adSetOptimizationGoal?: string | null;
  adSetDestinationType?: string | null;
  adSetBidStrategy?: string | null;
  adSetBidAmount?: string | null;
  adSetTargetCountry?: string | null;
  workerEnabled?: boolean;
}

export function useFacebookAccounts() {
  return useQuery({
    queryKey: ["facebook-accounts"],
    queryFn: async () => {
      const { data } = await axios.get<FacebookAccount[]>(
        "/api/accounts/facebook",
      );
      return data;
    },
    staleTime: 1000 * 60 * 5,
  });
}
