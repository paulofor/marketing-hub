import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { AiPromptSchemaTemplate } from "./types";

export function useAiPromptSchemaTemplates(
  pipelineCode?: string,
  stageCode?: string,
) {
  return useQuery({
    queryKey: ["aiPromptSchemaTemplates", pipelineCode ?? "", stageCode ?? ""],
    queryFn: async () => {
      const params = {
        ...(pipelineCode ? { pipelineCode } : {}),
        ...(stageCode ? { stageCode } : {}),
      };
      const { data } = await axios.get<AiPromptSchemaTemplate[]>(
        "/api/ai-prompt-schema-templates",
        { params },
      );
      return data;
    },
  });
}
