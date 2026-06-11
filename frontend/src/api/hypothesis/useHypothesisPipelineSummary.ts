import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface HypothesisStageFinalSummary {
  slug: string;
  stageNumber: number;
  stageTitle: string;
  stageCode: string;
  jobid?: string | null;
  status?: string | null;
  completedAt?: string | null;
  finalContent?: string | null;
  sourceTable: string;
  sourceField: string;
}

export function useHypothesisPipelineSummary(nicheId?: string) {
  return useQuery({
    queryKey: ["hypothesis-pipeline-summary", nicheId],
    enabled: Boolean(nicheId),
    queryFn: async () => {
      const { data } = await axios.get<HypothesisStageFinalSummary[]>(
        `/api/niches/${nicheId}/hypothesis-pipeline/summary`,
      );
      return data;
    },
  });
}
