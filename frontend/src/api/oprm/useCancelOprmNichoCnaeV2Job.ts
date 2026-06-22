import { useMutation, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV2JobCancelResult {
  jobId: string;
  cnaeCode: string;
  canceledExecutions: number;
  status: string;
  message: string;
  updatedAt: string;
}

async function cancelOprmNichoCnaeV2Job(
  cnaeCode: string,
  jobId: string,
): Promise<OprmNichoCnaeV2JobCancelResult> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/jobs/${encodeURIComponent(jobId)}/cancel`,
    ),
    { method: "POST" },
  );
  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível cancelar o job v2 do NichoCNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeV2JobCancelResult;
}

export function useCancelOprmNichoCnaeV2Job(cnaeCode: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (jobId: string) => cancelOprmNichoCnaeV2Job(cnaeCode, jobId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["oprm", "nichocnae", "v2", cnaeCode, "jobs"],
      });
    },
  });
}
