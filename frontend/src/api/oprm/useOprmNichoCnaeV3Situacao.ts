import { useQueries } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV3Situacao {
  idExterno: string;
  codigoEtapa: string;
  status: string;
  dataHora: string;
  jobId: string | null;
  request: string | null;
  requestInput: string | null;
  response: string | null;
  respostaFinal: string | null;
  quantidadeTokenEntrada: number | null;
  quantidadeTokenSaida: number | null;
  modelo: string | null;
  custo: number | string | null;
  descricaoErro: string | null;
  jobIdExterno: string | null;
  plataforma: string | null;
  prompt: string | null;
  schema: string | null;
  versaoPipeline: string | null;
}

const situacaoStatuses = [
  "AGUARDANDO_MODULO",
  "CONCLUIDO",
  "FALHA",
  "INICIADO",
];

async function fetchOprmNichoCnaeV3Situacao(
  cnaeCode: string,
  stageCode: string,
): Promise<OprmNichoCnaeV3Situacao[]> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprmcoletormei/nichocnae/v1/${encodeURIComponent(
        stageCode,
      )}/stage-executions/${encodeURIComponent(cnaeCode)}/situacao`,
    ),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: situacaoStatuses }),
    },
  );

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível carregar a situação da etapa ${stageCode} (status ${response.status}).`,
    );
  }

  return (await response.json()) as OprmNichoCnaeV3Situacao[];
}

export function useOprmNichoCnaeV3Situacoes(
  cnaeCode: string,
  stageCodes: string[],
) {
  return useQueries({
    queries: stageCodes.map((stageCode) => ({
      queryKey: ["oprm-nichocnae-v3-situacao", cnaeCode, stageCode],
      queryFn: () => fetchOprmNichoCnaeV3Situacao(cnaeCode, stageCode),
      enabled: Boolean(cnaeCode && stageCode),
      refetchInterval: 5000,
    })),
  });
}
