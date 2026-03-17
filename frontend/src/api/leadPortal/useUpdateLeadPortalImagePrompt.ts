import { useMutation } from "@tanstack/react-query";
import axios from "axios";

import type { LeadPortalFlow } from "./useLeadPortalFlows";

export interface UpdateLeadPortalImagePromptPayload {
  imagePromptModel?: string | null;
  imagePromptTemplate?: string | null;
  imagePromptBatchSize?: number | null;
}

interface UpdateLeadPortalImagePromptParams {
  id: number;
  payload: UpdateLeadPortalImagePromptPayload;
}

export function useUpdateLeadPortalImagePrompt() {
  return useMutation({
    mutationFn: async ({ id, payload }: UpdateLeadPortalImagePromptParams) => {
      const { data } = await axios.put<LeadPortalFlow>(
        `/api/lead-portal/image-prompts/${id}`,
        payload,
      );
      return data;
    },
  });
}
