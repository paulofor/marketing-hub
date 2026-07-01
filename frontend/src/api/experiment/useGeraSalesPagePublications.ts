import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface GeraSalesPagePublicationStage {
  idJob: string;
  stageCode: string;
  status: string;
  completedAt?: string | null;
  promptTemplateKey?: string | null;
  prompt?: string | null;
  promptMarkdownContent?: string | null;
  schemaJson?: string | null;
  openAiModel?: string | null;
  openAiRequestBody?: string | null;
  modelResponse?: string | null;
  rawResponse?: string | null;
  inputTokens?: number | null;
  outputTokens?: number | null;
  costUsd?: number | null;
}

export interface GeraSalesPagePublication {
  id: number;
  experimentId: number;
  publicationJobId: string;
  publishedAt: string;
  salesPageUrl?: string | null;
  checkoutUrl?: string | null;
  html?: string | null;
  publicationPackageJson?: string | null;
  stages: GeraSalesPagePublicationStage[];
}

export function useGeraSalesPagePublications(experimentId?: string | number) {
  return useQuery({
    queryKey: ["experiments", experimentId, "gerasalespage-publications"],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<GeraSalesPagePublication[]>(
        `/api/experiments/${experimentId}/gerasalespage/v1/publications`,
      );
      return data;
    },
  });
}
