import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface TiktokAccount {
  id: number;
  name: string;
  advertiserId: string;
  hasAccessToken: boolean;
  maskedAccessToken?: string | null;
  appId?: string | null;
  clientKey?: string | null;
  hasAppSecret: boolean;
  metricsEnabled: boolean;
  publicationEnabled: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
  lastDiagnosticAt?: string | null;
  lastDiagnosticStatus?: string | null;
  lastDiagnosticMessage?: string | null;
}

export function useTiktokAccounts() {
  return useQuery({
    queryKey: ["tiktok-accounts"],
    queryFn: async () => {
      const { data } = await axios.get<TiktokAccount[]>("/api/tiktok/accounts");
      return data;
    },
    staleTime: 1000 * 60 * 5,
  });
}
