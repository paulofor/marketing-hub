import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { AiPromptSchemaTemplate } from "./types";

export function useAiPromptSchemaTemplate(templateKey: string) {
  return useQuery({
    queryKey: ["aiPromptSchemaTemplate", templateKey],
    enabled: Boolean(templateKey),
    queryFn: async () => {
      const { data } = await axios.get<AiPromptSchemaTemplate>(
        `/api/ai-prompt-schema-templates/${encodeURIComponent(templateKey)}`,
      );
      return data;
    },
  });
}
