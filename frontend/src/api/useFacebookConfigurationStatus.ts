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

export function useFacebookConfigurationStatus() {
  return useQuery({
    queryKey: ["facebook-configuration-status"],
    queryFn: async () => {
      const { data } = await axios.get<FacebookConfigurationStatus>(
        "/api/facebook/configuration-status",
      );
      return data;
    },
    staleTime: 1000 * 30,
  });
}
