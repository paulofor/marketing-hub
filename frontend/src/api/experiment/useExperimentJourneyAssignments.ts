import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { JourneyAssignment } from "../journey/types";

export interface ExperimentJourneyAssignments {
  journeyId?: number | null;
  templateId?: number | null;
  assignments: JourneyAssignment[];
}

export function useExperimentJourneyAssignments(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-journey-assignments", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentJourneyAssignments>(
        `/api/experiments/${experimentId}/journey/assignments`,
      );
      return data;
    },
  });
}
