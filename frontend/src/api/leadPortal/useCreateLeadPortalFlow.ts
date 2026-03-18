import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type LeadPortalQuestionType =
  | "TEXT"
  | "TEXTAREA"
  | "NUMBER"
  | "EMAIL"
  | "PHONE"
  | "DATE"
  | "SINGLE_CHOICE"
  | "MULTIPLE_CHOICE"
  | "IMAGE_UPLOAD";

export interface CreateLeadPortalFlowQuestionRequest {
  title: string;
  dataKey: string;
  type: LeadPortalQuestionType;
  required: boolean;
  description?: string;
  placeholder?: string;
  options?: string[];
}

export interface CreateLeadPortalFlowRequest {
  name: string;
  slug: string;
  description?: string;
  customFormHtml?: string | null;
  experimentId?: number | string | null;
  marketNicheId: number | string;
  model?: string;
  simpleFormStyleId?: number | string | null;
  questions: CreateLeadPortalFlowQuestionRequest[];
}

export function useCreateLeadPortalFlow() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: CreateLeadPortalFlowRequest) => {
      const { data } = await axios.post("/api/lead-portal-flows", payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lead-portal-flows"] });
    },
  });
}
