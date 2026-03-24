import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { ExperimentReportRequest } from "./useExperimentReportRequests";

interface CreateExperimentReportPayload {
  requestedBy?: string;
}

export function useCreateExperimentReportRequest(experimentId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload?: CreateExperimentReportPayload) => {
      if (!experimentId) {
        throw new Error("Experiment id is required");
      }
      const body = payload ?? {};
      const { data } = await axios.post<ExperimentReportRequest>(
        `/api/experiments/${experimentId}/report-requests`,
        body,
      );
      return data;
    },
    onSuccess: () => {
      if (experimentId) {
        queryClient.invalidateQueries({
          queryKey: ["experiment-report-requests", experimentId],
        });
        queryClient.invalidateQueries({
          queryKey: ["experiment-report-material", experimentId],
        });
      }
      toast.success("Solicitação de relatório registrada");
    },
    onError: (error: unknown) => {
      const message =
        axios.isAxiosError(error)
          ? error.response?.data?.message ?? error.message
          : (error as Error)?.message;
      toast.error(message ?? "Não foi possível solicitar o relatório agora");
    },
  });
}
