import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalEmailTemplatePlaceholder {
  key: string;
  token: string;
  label: string;
  description: string;
}

export interface LeadPortalEmailTemplate {
  subject: string | null;
  html: string | null;
  updatedAt: string | null;
  placeholders: LeadPortalEmailTemplatePlaceholder[];
}

export interface UpdateLeadPortalEmailTemplatePayload {
  subject: string | null;
  html: string | null;
}

const QUERY_KEY = ["lead-portal-email-template"] as const;

const EMPTY_TEMPLATE: LeadPortalEmailTemplate = {
  subject: null,
  html: null,
  updatedAt: null,
  placeholders: [],
};

export function useLeadPortalEmailTemplate() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => {
      try {
        const { data } = await axios.get<LeadPortalEmailTemplate>(
          "/api/lead-portal/email-template",
        );
        return data;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          return EMPTY_TEMPLATE;
        }
        throw error;
      }
    },
    staleTime: 60 * 1000,
  });
}

export function useUpdateLeadPortalEmailTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpdateLeadPortalEmailTemplatePayload) => {
      const { data } = await axios.put<LeadPortalEmailTemplate>(
        "/api/lead-portal/email-template",
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(QUERY_KEY, data);
    },
  });
}
