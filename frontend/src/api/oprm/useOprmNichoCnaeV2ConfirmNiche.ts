import { useMutation, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV2ConfirmNicheResponse {
  jobId: string;
  cnaeCode: string;
  marketNicheId: number;
  nicheName: string;
  aiCostUsd: number;
  status: string;
  message: string;
  updatedAt: string;
}

async function confirmOprmNichoCnaeV2Niche({
  jobId,
  nicheName,
}: {
  jobId: string;
  nicheName: string;
}): Promise<OprmNichoCnaeV2ConfirmNicheResponse> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/jobs/${encodeURIComponent(jobId)}/confirm-niche`,
    ),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nicheName }),
    },
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível confirmar o nicho (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeV2ConfirmNicheResponse;
}

export function useOprmNichoCnaeV2ConfirmNiche(jobId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (nicheName: string) =>
      confirmOprmNichoCnaeV2Niche({ jobId: jobId ?? "", nicheName }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["oprm", "nichocnae", "v2", "job-detail", jobId],
      });
    },
  });
}
