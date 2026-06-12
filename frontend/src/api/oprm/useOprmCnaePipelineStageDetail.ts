import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export type OprmCnaePipelineStageCode =
  | "cycle"
  | "seed"
  | "search"
  | "fetch"
  | "signals"
  | "synthesis"
  | "mei"
  | "quality"
  | "materialization";

const stageEndpointByCode: Record<OprmCnaePipelineStageCode, string> = {
  cycle: "routine-research-cycle",
  seed: "niche-research-seed-builder",
  search: "source-searcher",
  fetch: "source-fetcher",
  signals: "signal-extractor",
  synthesis: "routine-synthesizer",
  mei: "mei-audience-profiles/research-cycles",
  quality: "routine-quality-gate",
  materialization: "enriched-niche-materializer",
};

function buildStageDetailPath(
  stageCode: OprmCnaePipelineStageCode,
  researchCycleId: number,
) {
  if (stageCode === "mei") {
    return `/api/oprm/nichocnae/${stageEndpointByCode[stageCode]}/${researchCycleId}`;
  }
  return `/api/oprm/nichocnae/${stageEndpointByCode[stageCode]}/stage-executions/${researchCycleId}`;
}

async function fetchStageDetail(
  stageCode: OprmCnaePipelineStageCode,
  researchCycleId: number,
): Promise<unknown | null> {
  const response = await fetch(
    buildApiUrl(buildStageDetailPath(stageCode, researchCycleId)),
  );
  if (response.status === 404 || response.status === 409) {
    return null;
  }
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o detalhe da etapa (status ${response.status}).`,
    );
  }
  return (await response.json()) as unknown;
}

export function isOprmCnaePipelineStageCode(
  value: string | undefined,
): value is OprmCnaePipelineStageCode {
  return Boolean(
    value && Object.prototype.hasOwnProperty.call(stageEndpointByCode, value),
  );
}

export function useOprmCnaePipelineStageDetail(
  stageCode: OprmCnaePipelineStageCode | undefined,
  researchCycleId: number | undefined,
) {
  return useQuery({
    queryKey: [
      "oprm",
      "nichocnae",
      "pipeline-stage-detail",
      stageCode,
      researchCycleId,
    ],
    queryFn: () => fetchStageDetail(stageCode as OprmCnaePipelineStageCode, researchCycleId as number),
    enabled: Boolean(stageCode && researchCycleId),
    retry: false,
  });
}
