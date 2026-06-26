import { useMutation, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

async function confirmOprmNichoCnaeV3Finalization(
  cnaeCode: string,
): Promise<void> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/v3/cnaes/${encodeURIComponent(cnaeCode)}/progress/confirm-finalization`,
    ),
    { method: "POST" },
  );

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível confirmar a finalização v3 do NichoCNAE (status ${response.status}).`,
    );
  }
}

export function useConfirmOprmNichoCnaeV3Finalization(cnaeCode: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => confirmOprmNichoCnaeV3Finalization(cnaeCode),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["oprm-nichocnae-v3-progress", cnaeCode],
      });
    },
  });
}
