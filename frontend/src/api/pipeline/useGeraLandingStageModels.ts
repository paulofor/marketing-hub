import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface GeraLandingStageModel {
  stageCode: string;
  pipelineId?: number | null;
  pipelineCode?: string | null;
  pipelineStageId?: number | null;
  pipelineStageCode?: string | null;
  openAiModelId?: number | null;
  openAiModelName?: string | null;
  openAiModelCode?: string | null;
}

export function useGeraLandingStageModels() {
  return useQuery({
    queryKey: ["pipelines", "geralanding", "stage-models"],
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageModel[]>(
        "/api/pipelines/geralanding/stage-models",
      );
      return data;
    },
  });
}
