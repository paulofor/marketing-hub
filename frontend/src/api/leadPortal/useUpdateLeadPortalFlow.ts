import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

import type { CreateLeadPortalFlowQuestionRequest } from "./useCreateLeadPortalFlow";

export interface UpdateLeadPortalFlowRequest {
  name?: string;
  slug?: string;
  description?: string | null;
  customFormHtml?: string | null;
  marketNicheId?: number | string | null;
  model?: string | null;
  simpleFormStyleId?: number | string | null;
  questions?: CreateLeadPortalFlowQuestionRequest[];
}

interface UpdateLeadPortalFlowParams {
  id: number;
  payload: UpdateLeadPortalFlowRequest;
}

export function useUpdateLeadPortalFlow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, payload }: UpdateLeadPortalFlowParams) => {
      const { data } = await axios.put(`/api/lead-portal-flows/${id}`, payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lead-portal-flows"] });
    },
  });
}
