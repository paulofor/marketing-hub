import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface DiagnosticMessage {
  code: string;
  message: string;
}

export interface WorkerDiagnostics {
  hasAccount: boolean;
  ready: boolean;
  accountId: number | null;
  accountName: string | null;
  messages: DiagnosticMessage[];
}

export interface TokenRenewalAccountStatus {
  accountId: number;
  accountName: string;
  eligible: boolean;
  messages: DiagnosticMessage[];
}

export interface TokenRenewalDiagnostics {
  enabledAccounts: number;
  eligibleAccounts: number;
  accounts: TokenRenewalAccountStatus[];
}

export interface FacebookConfigurationStatus {
  hasConfiguredPages: boolean;
  worker: WorkerDiagnostics;
  tokenRenewal: TokenRenewalDiagnostics;
}

function normalizeFacebookConfigurationStatus(
  payload: Partial<FacebookConfigurationStatus> | undefined,
): FacebookConfigurationStatus {
  return {
    hasConfiguredPages: payload?.hasConfiguredPages ?? false,
    worker: {
      hasAccount: payload?.worker?.hasAccount ?? false,
      ready: payload?.worker?.ready ?? false,
      accountId: payload?.worker?.accountId ?? null,
      accountName: payload?.worker?.accountName ?? null,
      messages: payload?.worker?.messages ?? [],
    },
    tokenRenewal: {
      enabledAccounts: payload?.tokenRenewal?.enabledAccounts ?? 0,
      eligibleAccounts: payload?.tokenRenewal?.eligibleAccounts ?? 0,
      accounts: payload?.tokenRenewal?.accounts ?? [],
    },
  };
}

export function useFacebookConfigurationStatus() {
  return useQuery({
    queryKey: ["facebook-configuration-status"],
    queryFn: async () => {
      const { data } = await axios.get<Partial<FacebookConfigurationStatus>>(
        "/api/facebook/configuration-status",
      );
      return normalizeFacebookConfigurationStatus(data);
    },
    staleTime: 1000 * 30,
  });
}
