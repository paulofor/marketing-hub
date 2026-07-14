import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";

export type FashionChatAccountStatus =
  "AUTHENTICATED" | "NOT_AUTHENTICATED" | "UNAVAILABLE" | "UNKNOWN";

export interface FashionChatValidationStatus {
  serviceBaseUrl: string;
  checkedAt: string;
  ready: boolean;
  readyHttpStatus?: number | null;
  readyError?: string | null;
  accountStatus: FashionChatAccountStatus;
  authenticated?: boolean | null;
  connected?: boolean | null;
  executable?: boolean | null;
  blockReason?: string | null;
  accountHttpStatus?: number | null;
  accountError?: string | null;
  accountPayload?: unknown;
}

export interface StartFashionChatLoginResponse {
  serviceBaseUrl: string;
  httpStatus?: number | null;
  verificationUri?: string | null;
  userCode?: string | null;
  expiresIn?: number | null;
  interval?: number | null;
  payload?: unknown;
  errorMessage?: string | null;
}

export function useFashionChatValidationStatus() {
  return useQuery({
    queryKey: ["fashion-chat", "validation", "status"],
    queryFn: async () => {
      const { data } = await axios.get<FashionChatValidationStatus>(
        "/api/fashion-chat/validation/status",
      );
      return data;
    },
    refetchInterval: 15_000,
  });
}

export function useStartFashionChatLogin() {
  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<StartFashionChatLoginResponse>(
        "/api/fashion-chat/validation/login/start",
      );
      return data;
    },
  });
}
