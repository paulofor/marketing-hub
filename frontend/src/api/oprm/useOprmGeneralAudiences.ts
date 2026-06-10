import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export type OprmGeneralAudienceSeedType =
  | "CATEGORY"
  | "DESIRE"
  | "LIFE_CONTEXT"
  | "BEHAVIOR"
  | "CHANNEL"
  | "PAIN_CLUSTER";

export type OprmGeneralAudienceSeedStatus =
  | "DRAFT"
  | "READY_FOR_RESEARCH"
  | "RESEARCHING"
  | "MAPPED"
  | "PAUSED"
  | "ARCHIVED";

export type OprmGeneralAudienceSubnicheStatus =
  | "DISCOVERED"
  | "NEEDS_REVIEW"
  | "APPROVED_FOR_EXPERIMENT"
  | "REJECTED"
  | "CONVERTED_TO_NICHE";

export interface OprmGeneralAudienceSeedSummary {
  id: number;
  name: string;
  marketContext?: string | null;
  country: string;
  language: string;
  seedType: OprmGeneralAudienceSeedType;
  status: OprmGeneralAudienceSeedStatus;
  updatedAt: string;
}

export interface OprmGeneralAudienceSeed extends OprmGeneralAudienceSeedSummary {
  description?: string | null;
  businessGoal?: string | null;
  riskNotes?: string | null;
  createdAt: string;
}

export interface OprmGeneralAudienceSubnicheSummary {
  id: number;
  seedId: number;
  name: string;
  personaSummary?: string | null;
  painSummary?: string | null;
  channelsSummary?: string | null;
  qualificationQuestion?: string | null;
  status: OprmGeneralAudienceSubnicheStatus;
  opportunityScore?: number | null;
  riskScore?: number | null;
  marketNicheId?: number | null;
  updatedAt: string;
}

export interface OprmGeneralAudienceSubniche extends OprmGeneralAudienceSubnicheSummary {
  desiredOutcomeSummary?: string | null;
  languagePatterns?: string | null;
  createdAt: string;
}

export interface CreateGeneralAudienceSeedPayload {
  name: string;
  description?: string;
  marketContext?: string;
  country?: string;
  language?: string;
  seedType: OprmGeneralAudienceSeedType;
  status?: OprmGeneralAudienceSeedStatus;
  businessGoal?: string;
  riskNotes?: string;
}

export interface ConvertGeneralAudienceSubnicheResponse {
  subnicheId: number;
  seedId: number;
  marketNicheId: number;
  marketNicheName: string;
  subnicheStatus: OprmGeneralAudienceSubnicheStatus;
  reusedExistingMarketNiche: boolean;
  convertedAt: string;
}

export interface CreateGeneralAudienceSubnichePayload {
  name: string;
  personaSummary?: string;
  painSummary?: string;
  desiredOutcomeSummary?: string;
  languagePatterns?: string;
  channelsSummary?: string;
  qualificationQuestion?: string;
  status?: OprmGeneralAudienceSubnicheStatus;
  opportunityScore?: number;
  riskScore?: number;
  marketNicheId?: number;
}

export const seedTypeLabels: Record<OprmGeneralAudienceSeedType, string> = {
  CATEGORY: "Categoria ampla",
  DESIRE: "Desejo amplo",
  LIFE_CONTEXT: "Contexto de vida",
  BEHAVIOR: "Comportamento",
  CHANNEL: "Canal",
  PAIN_CLUSTER: "Cluster de dores",
};

export const seedStatusLabels: Record<OprmGeneralAudienceSeedStatus, string> = {
  DRAFT: "Rascunho",
  READY_FOR_RESEARCH: "Pronta para pesquisa",
  RESEARCHING: "Pesquisando",
  MAPPED: "Mapeada",
  PAUSED: "Pausada",
  ARCHIVED: "Arquivada",
};

export const subnicheStatusLabels: Record<
  OprmGeneralAudienceSubnicheStatus,
  string
> = {
  DISCOVERED: "Descoberto",
  NEEDS_REVIEW: "Precisa revisão",
  APPROVED_FOR_EXPERIMENT: "Aprovado para experimento",
  REJECTED: "Rejeitado",
  CONVERTED_TO_NICHE: "Convertido em nicho",
};

const generalAudienceKeys = {
  seeds: ["oprm", "general-audiences", "seeds"] as const,
  seed: (seedId: number) =>
    ["oprm", "general-audiences", "seeds", seedId] as const,
  subniches: (seedId: number) =>
    ["oprm", "general-audiences", "seeds", seedId, "subniches"] as const,
  subniche: (subnicheId: number) =>
    ["oprm", "general-audiences", "subniches", subnicheId] as const,
};

async function parseJsonResponse<T>(response: Response, errorMessage: string) {
  if (!response.ok) {
    throw new Error(`${errorMessage} (status ${response.status}).`);
  }
  return (await response.json()) as T;
}

async function sendJson<T>(
  path: string,
  method: "POST" | "PATCH",
  payload?: unknown,
  errorMessage = "Não foi possível concluir a operação",
) {
  const response = await fetch(buildApiUrl(path), {
    method,
    headers: { "Content-Type": "application/json" },
    body: payload == null ? undefined : JSON.stringify(payload),
  });
  return parseJsonResponse<T>(response, errorMessage);
}

export function useOprmGeneralAudienceSeeds() {
  return useQuery({
    queryKey: generalAudienceKeys.seeds,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl("/api/oprm/general-audiences/seeds"),
      );
      return parseJsonResponse<OprmGeneralAudienceSeedSummary[]>(
        response,
        "Não foi possível carregar as sementes de públicos gerais",
      );
    },
  });
}

export function useOprmGeneralAudienceSeed(seedId?: number) {
  return useQuery({
    queryKey: seedId
      ? generalAudienceKeys.seed(seedId)
      : ["oprm", "general-audience-seed", "missing"],
    enabled: seedId != null,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl(`/api/oprm/general-audiences/seeds/${seedId}`),
      );
      return parseJsonResponse<OprmGeneralAudienceSeed>(
        response,
        "Não foi possível carregar a semente de público geral",
      );
    },
  });
}

export function useOprmGeneralAudienceSubniches(seedId?: number) {
  return useQuery({
    queryKey: seedId
      ? generalAudienceKeys.subniches(seedId)
      : ["oprm", "general-audience-subniches", "missing"],
    enabled: seedId != null,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl(`/api/oprm/general-audiences/seeds/${seedId}/subniches`),
      );
      return parseJsonResponse<OprmGeneralAudienceSubnicheSummary[]>(
        response,
        "Não foi possível carregar os subnichos da semente",
      );
    },
  });
}

export function useOprmGeneralAudienceSubniche(subnicheId?: number) {
  return useQuery({
    queryKey: subnicheId
      ? generalAudienceKeys.subniche(subnicheId)
      : ["oprm", "general-audience-subniche", "missing"],
    enabled: subnicheId != null,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl(`/api/oprm/general-audiences/subniches/${subnicheId}`),
      );
      return parseJsonResponse<OprmGeneralAudienceSubniche>(
        response,
        "Não foi possível carregar o subnicho de público geral",
      );
    },
  });
}

export function useCreateOprmGeneralAudienceSeed() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateGeneralAudienceSeedPayload) =>
      sendJson<OprmGeneralAudienceSeed>(
        "/api/oprm/general-audiences/seeds",
        "POST",
        payload,
        "Não foi possível criar a semente de público geral",
      ),
    onSuccess: (seed) => {
      queryClient.invalidateQueries({ queryKey: generalAudienceKeys.seeds });
      queryClient.setQueryData(generalAudienceKeys.seed(seed.id), seed);
    },
  });
}

export function useUpdateOprmGeneralAudienceSeed(seedId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: Partial<CreateGeneralAudienceSeedPayload>) =>
      sendJson<OprmGeneralAudienceSeed>(
        `/api/oprm/general-audiences/seeds/${seedId}`,
        "PATCH",
        payload,
        "Não foi possível atualizar a semente de público geral",
      ),
    onSuccess: (seed) => {
      queryClient.invalidateQueries({ queryKey: generalAudienceKeys.seeds });
      queryClient.setQueryData(generalAudienceKeys.seed(seed.id), seed);
    },
  });
}

export function useArchiveOprmGeneralAudienceSeed(seedId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      sendJson<OprmGeneralAudienceSeed>(
        `/api/oprm/general-audiences/seeds/${seedId}/archive`,
        "POST",
        undefined,
        "Não foi possível arquivar a semente de público geral",
      ),
    onSuccess: (seed) => {
      queryClient.invalidateQueries({ queryKey: generalAudienceKeys.seeds });
      queryClient.setQueryData(generalAudienceKeys.seed(seed.id), seed);
    },
  });
}

export function useCreateOprmGeneralAudienceSubniche(seedId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateGeneralAudienceSubnichePayload) =>
      sendJson<OprmGeneralAudienceSubniche>(
        `/api/oprm/general-audiences/seeds/${seedId}/subniches`,
        "POST",
        payload,
        "Não foi possível criar o subnicho de público geral",
      ),
    onSuccess: (subniche) => {
      queryClient.invalidateQueries({
        queryKey: generalAudienceKeys.subniches(seedId),
      });
      queryClient.setQueryData(
        generalAudienceKeys.subniche(subniche.id),
        subniche,
      );
    },
  });
}

export function useUpdateOprmGeneralAudienceSubniche(subnicheId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: Partial<CreateGeneralAudienceSubnichePayload>) =>
      sendJson<OprmGeneralAudienceSubniche>(
        `/api/oprm/general-audiences/subniches/${subnicheId}`,
        "PATCH",
        payload,
        "Não foi possível atualizar o subnicho de público geral",
      ),
    onSuccess: (subniche) => {
      queryClient.invalidateQueries({
        queryKey: generalAudienceKeys.subniches(subniche.seedId),
      });
      queryClient.setQueryData(
        generalAudienceKeys.subniche(subniche.id),
        subniche,
      );
    },
  });
}

export function useApproveOprmGeneralAudienceSubniche(subnicheId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      sendJson<OprmGeneralAudienceSubniche>(
        `/api/oprm/general-audiences/subniches/${subnicheId}/approve`,
        "POST",
        undefined,
        "Não foi possível aprovar o subnicho de público geral",
      ),
    onSuccess: (subniche) => {
      queryClient.invalidateQueries({
        queryKey: generalAudienceKeys.subniches(subniche.seedId),
      });
      queryClient.setQueryData(
        generalAudienceKeys.subniche(subniche.id),
        subniche,
      );
    },
  });
}

export function useRejectOprmGeneralAudienceSubniche(subnicheId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      sendJson<OprmGeneralAudienceSubniche>(
        `/api/oprm/general-audiences/subniches/${subnicheId}/reject`,
        "POST",
        undefined,
        "Não foi possível rejeitar o subnicho de público geral",
      ),
    onSuccess: (subniche) => {
      queryClient.invalidateQueries({
        queryKey: generalAudienceKeys.subniches(subniche.seedId),
      });
      queryClient.setQueryData(
        generalAudienceKeys.subniche(subniche.id),
        subniche,
      );
    },
  });
}

export function useConvertOprmGeneralAudienceSubnicheToMarketNiche(
  subnicheId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      sendJson<ConvertGeneralAudienceSubnicheResponse>(
        `/api/oprm/general-audiences/subniches/${subnicheId}/convert-to-market-niche`,
        "POST",
        undefined,
        "Não foi possível converter o subnicho em nicho",
      ),
    onSuccess: (conversion) => {
      queryClient.invalidateQueries({
        queryKey: generalAudienceKeys.subniches(conversion.seedId),
      });
      queryClient.invalidateQueries({
        queryKey: generalAudienceKeys.subniche(conversion.subnicheId),
      });
    },
  });
}
