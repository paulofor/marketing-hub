import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { AiPromptSchemaTemplate } from "./types";

export function useActivateAiPromptSchemaTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (templateKey: string) => {
      const { data } = await axios.post<AiPromptSchemaTemplate>(
        `/api/ai-prompt-schema-templates/${encodeURIComponent(templateKey)}/activate`,
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
