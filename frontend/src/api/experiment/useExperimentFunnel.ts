import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentFunnelStage =
  | "VISUALIZACAO_ANUNCIO"
  | "ACESSO_FORM_LEAD"
  | "VISUALIZACAO_FORM"
  | "ENVIO_FORM"
  | "ABERTURA_EMAIL_AMOSTRA"
  | "ACESSO_CHECKOUT"
  | "COMPRA"
  | "ABERTURA_EMAIL_COMPRA"
  | "DOWNLOAD_MATERIAL_PAGO";

export interface ExperimentFunnelStageSummary {
  stage: ExperimentFunnelStage;
  label: string;
  order: number;
  autoCount: number;
  manualCount: number;
  totalCount: number;
  uniqueCount?: number | null;
  lastEventAt?: string | null;
  source?: string | null;
}

export function useExperimentFunnel(experimentId?: string) {
  return useQuery<ExperimentFunnelStageSummary[]>({
    queryKey: ["experiment", experimentId, "funnel"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentFunnelStageSummary[]>(
        `/api/experiments/${experimentId}/funnel`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
