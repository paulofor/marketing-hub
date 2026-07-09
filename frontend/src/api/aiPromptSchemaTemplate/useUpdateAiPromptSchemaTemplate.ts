import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  AiPromptSchemaTemplate,
  UpdateAiPromptSchemaTemplatePayload,
} from "./types";

export function useUpdateAiPromptSchemaTemplate(templateKey: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpdateAiPromptSchemaTemplatePayload) => {
      const { data } = await axios.put<AiPromptSchemaTemplate>(
        `/api/ai-prompt-schema-templates/${encodeURIComponent(templateKey)}`,
        payload,
      );
      return data;
    },
    onSuccess: (template) => {
      queryClient.invalidateQueries({ queryKey: ["aiPromptSchemaTemplates"] });
      queryClient.setQueryData(
        ["aiPromptSchemaTemplate", template.templateKey],
        template,
      );
    },
  });
}
