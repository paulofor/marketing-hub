import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface EmailProviderPreset {
  id: string;
  name: string;
  headline: string;
  summary: string;
  docsUrl: string;
  pricingUrl: string;
  pricingSummary: string;
  bestFor: string;
  freeTier: string;
  host: string;
  port: number | null;
  alternativePorts: number[];
  authEnabled: boolean;
  useStartTls: boolean;
  useSsl: boolean;
  usernameHint: string;
  highlights: string[];
  notes: string | null;
}

export function useEmailProviderPresets() {
  return useQuery({
    queryKey: ["email-provider-presets"],
    queryFn: async () => {
      const { data } = await axios.get<EmailProviderPreset[]>(
        "/api/settings/email-service/providers",
      );
      return data;
    },
    staleTime: 1000 * 60 * 30,
  });
}
