import { useMutation } from "@tanstack/react-query";
import axios from "axios";

export interface GraphCallResult {
  operation: string;
  status: "SUCCESS" | "FAILED" | "SKIPPED";
  httpStatus?: number | null;
  message?: string | null;
  code?: number | null;
  subcode?: number | null;
  fbtraceId?: string | null;
  tokenDebug?: TokenDebugDetails | null;
}

export interface TokenDebugDetails {
  valid?: boolean | null;
  type?: string | null;
  appId?: string | null;
  application?: string | null;
  userId?: string | null;
  expiresAt?: string | null;
  issuedAt?: string | null;
}

export interface PermissionDiagnostic {
  permission: string;
  status?: string | null;
  required: boolean;
  recommended: boolean;
}

export interface FacebookTokenDiagnosticResponse {
  accountId: number;
  accountName?: string | null;
  adAccountId?: string | null;
  tokenSource: string;
  checkedAt: string;
  hasToken: boolean;
  tokenDebug: GraphCallResult;
  permissions: PermissionDiagnostic[];
  adAccountAccess: GraphCallResult;
  videoLibraryReadiness: GraphCallResult;
  requiredPermissions: string[];
  recommendedPermissions: string[];
}

export interface FacebookVideoUploadTestResponse {
  accountId: number;
  accountName?: string | null;
  checkedAt: string;
  success: boolean;
  videoId?: string | null;
  upload: GraphCallResult;
}

export function useFacebookTokenDiagnostics() {
  return useMutation({
    mutationFn: async (accountId: number) => {
      const { data } = await axios.post<FacebookTokenDiagnosticResponse>(
        `/api/accounts/facebook/${accountId}/token/diagnostics`,
      );
      return data;
    },
  });
}

export function useFacebookVideoUploadTest() {
  return useMutation({
    mutationFn: async (accountId: number) => {
      const { data } = await axios.post<FacebookVideoUploadTestResponse>(
        `/api/accounts/facebook/${accountId}/token/video-upload-test`,
      );
      return data;
    },
  });
}
