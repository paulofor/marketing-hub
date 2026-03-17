import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalImagePromptPlaceholder {
  token: string;
  description: string;
  example?: string | null;
}

export interface LeadPortalImagePromptMetadata {
  defaultTemplate: string;
  defaultModel: string;
  defaultBatchSize: number;
  placeholders: LeadPortalImagePromptPlaceholder[];
}

export function useLeadPortalImagePromptMetadata() {
  return useQuery({
    queryKey: ["lead-portal-image-prompt-metadata"],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalImagePromptMetadata>(
        "/api/lead-portal/image-prompts/metadata",
      );
      return data;
    },
  });
}
