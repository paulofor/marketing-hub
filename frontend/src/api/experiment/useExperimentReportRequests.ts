import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentReportStatus =
  | "PENDING"
  | "PROCESSING"
  | "READY"
  | "FAILED";

export interface ExperimentReportRequest {
  id: number;
  experimentId: number;
  status: ExperimentReportStatus;
  requestedAt: string;
  completedAt?: string | null;
  requestedBy?: string | null;
  downloadUrl?: string | null;
  failureReason?: string | null;
}

export function useExperimentReportRequests(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-report-requests", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentReportRequest[]>(
        `/api/experiments/${experimentId}/report-requests`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
