import { useQuery } from "@tanstack/react-query";
import axios from "axios";

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
