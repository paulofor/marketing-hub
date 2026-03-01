import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalSimpleFormStyleDefinition {
  backgroundColor?: string | null;
  backgroundGradient?: string | null;
  backgroundPatternUrl?: string | null;
  cardBackground?: string | null;
  cardBorderColor?: string | null;
  cardShadow?: string | null;
  headingColor?: string | null;
  textColor?: string | null;
  mutedTextColor?: string | null;
  primaryColor?: string | null;
  accentColor?: string | null;
  buttonBackground?: string | null;
  buttonTextColor?: string | null;
  buttonShadow?: string | null;
  buttonBorderRadius?: string | null;
  highlightBackground?: string | null;
  inputBackground?: string | null;
  inputBorderColor?: string | null;
  heroLayout?: "image-left" | "image-right" | "stacked" | null;
  heroImageUrl?: string | null;
  heroImageBlendColor?: string | null;
}

export interface LeadPortalSimpleFormStyle {
  id: number;
  name: string;
  slug: string;
  description?: string | null;
  textModel?: string | null;
  textPrompt?: string | null;
  textParameters?: string | null;
  previewImageUrl?: string | null;
  definition?: LeadPortalSimpleFormStyleDefinition | null;
  generationCostUsd?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertLeadPortalSimpleFormStylePayload {
  name: string;
  slug: string;
  description?: string | null;
  textModel: string;
  textPrompt: string;
  previewImageUrl?: string | null;
  regenerate?: boolean;
}

export function useLeadPortalSimpleFormStyles() {
  return useQuery({
    queryKey: ["lead-portal-simple-form-styles"],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalSimpleFormStyle[]>(
        "/api/lead-portal/simple-form-styles",
      );
      return data;
    },
  });
}

export function useCreateLeadPortalSimpleFormStyle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpsertLeadPortalSimpleFormStylePayload) => {
      const { data } = await axios.post<LeadPortalSimpleFormStyle>(
        "/api/lead-portal/simple-form-styles",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lead-portal-simple-form-styles"] });
    },
  });
}

export function useUpdateLeadPortalSimpleFormStyle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (params: { id: number; payload: Partial<UpsertLeadPortalSimpleFormStylePayload> }) => {
      const { data } = await axios.put<LeadPortalSimpleFormStyle>(
        `/api/lead-portal/simple-form-styles/${params.id}`,
        params.payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lead-portal-simple-form-styles"] });
    },
  });
}
