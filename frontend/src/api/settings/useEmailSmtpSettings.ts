import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface EmailSmtpSettings {
  providerName: string | null;
  host: string | null;
  port: number | null;
  authEnabled: boolean;
  username: string | null;
  fromName: string | null;
  fromEmail: string | null;
  useStartTls: boolean;
  useSsl: boolean;
  connectionTimeoutMs: number | null;
  readTimeoutMs: number | null;
  writeTimeoutMs: number | null;
  dryRun: boolean;
  hasPassword: boolean;
  updatedAt: string | null;
}

export interface UpdateEmailSmtpSettingsPayload {
  providerName?: string | null;
  host: string;
  port: number;
  authEnabled: boolean;
  username?: string | null;
  password?: string | null;
  fromName?: string | null;
  fromEmail: string;
  useStartTls: boolean;
  useSsl: boolean;
  connectionTimeoutMs: number;
  readTimeoutMs: number;
  writeTimeoutMs: number;
  dryRun: boolean;
}

export interface TestEmailSettingsPayload {
  recipient: string;
  subject?: string;
  message?: string;
}

export interface TestEmailSettingsResponse {
  success: boolean;
  message: string;
  sentAt: string;
}

const QUERY_KEY = ["email-smtp-settings"];

export function useEmailSmtpSettings() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => {
      const { data } = await axios.get<EmailSmtpSettings>(
        "/api/settings/email-service/smtp",
      );
      return data;
    },
    staleTime: 1000 * 60,
  });
}

export function useUpdateEmailSmtpSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpdateEmailSmtpSettingsPayload) => {
      const { data } = await axios.put<EmailSmtpSettings>(
        "/api/settings/email-service/smtp",
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(QUERY_KEY, data);
    },
  });
}

export function useTestEmailSettings() {
  return useMutation({
    mutationFn: async (payload: TestEmailSettingsPayload) => {
      const { data } = await axios.post<TestEmailSettingsResponse>(
        "/api/settings/email-service/smtp/test",
        payload,
      );
      return data;
    },
  });
}
