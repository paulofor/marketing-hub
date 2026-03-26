import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoJob, SalesVideoRetryReason } from "./types";

interface RetryPayload {
  jobId: number;
  profileId: number | string;
  requestedBy: string;
  reason: SalesVideoRetryReason;
  notes?: string;
}

export function useRetrySalesVideoJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ jobId, requestedBy, reason, notes }: RetryPayload) => {
      const { data } = await axios.post<SalesVideoJob>(`/api/sales-videos/jobs/${jobId}/retry`, {
        requestedBy,
        reason,
        notes,
      });
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-jobs", variables.profileId] });
    },
  });
}
