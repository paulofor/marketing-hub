import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export type ProductDiscoveryCycleStatus =
  | "DRAFT"
  | "READY_FOR_RESEARCH"
  | "RESEARCHING"
  | "COMPLETED"
  | "FAILED"
  | "ARCHIVED";

export type ProductDiscoveryOpportunityDecision =
  | "APPROVE"
  | "RESEARCH_MORE"
  | "REJECT"
  | "HUMAN_REVIEW";

export interface ProductDiscoveryCycle {
  id: number;
  theme: string;
  targetAudience?: string | null;
  country: string;
  language: string;
  acquisitionChannel?: string | null;
  status: ProductDiscoveryCycleStatus;
  stageCode: string;
  decisionSummary?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductDiscoveryOpportunity {
  id: number;
  cycleId: number;
  name: string;
  primaryAudience: string;
  rootPain: string;
  practicalPain?: string | null;
  emotionalPain?: string | null;
  scaleEvidence?: string | null;
  unmetnessEvidence?: string | null;
  pdeExperience?: string | null;
  firstCampaignAngle?: string | null;
  commercialRisk?: string | null;
  evidenceJson?: string | null;
  score: number;
  decision: ProductDiscoveryOpportunityDecision;
  createdAt: string;
  updatedAt: string;
}

export interface ProductDiscoveryCycleDetail {
  cycle: ProductDiscoveryCycle;
  opportunities: ProductDiscoveryOpportunity[];
}

export interface ProductDiscoveryResearchTrack {
  name: string;
  focus: string;
  reason: string;
  theme: string;
  targetAudience: string;
  acquisitionChannel: string;
  objective: string;
  commercialConstraints: string;
  forbiddenCategories: string;
}

export interface ProductDiscoveryMaturityItem {
  position: number;
  niche: string;
  maturity: string;
  summary: string;
  commercialReason: string;
  recommendedAction: string;
  evidence: string[];
  guardrails: string[];
}

export interface ProductDiscoveryMaturityRanking {
  strategyName: string;
  decisionCriterion: string;
  recommendedPriority: string;
  items: ProductDiscoveryMaturityItem[];
  recommendedTracks: ProductDiscoveryResearchTrack[];
}

export interface CreateProductDiscoveryCyclePayload {
  theme: string;
  targetAudience?: string;
  country?: string;
  language?: string;
  acquisitionChannel?: string;
  commercialConstraints?: string;
  forbiddenCategories?: string;
  objective?: string;
}

export interface ProductDiscoveryLegacyCleanupResult {
  archivedCycles: number;
  archivedOpportunities: number;
  cycleIds: number[];
  reason: string;
}

export interface ProductDiscoveryPrivateValidationHandoffResult {
  cycleId: number;
  sourceReference: string;
  dossierReadyCount: number;
  status: "QUEUED_FOR_PRIVATE_VALIDATION";
  nextActivity: "ATENA_PRIVATE_PROTOTYPE_SELECTION";
  message: string;
}

export const productDiscoveryStatusLabels: Record<
  ProductDiscoveryCycleStatus,
  string
> = {
  DRAFT: "Rascunho",
  READY_FOR_RESEARCH: "Pronto para pesquisa",
  RESEARCHING: "Pesquisando",
  COMPLETED: "Concluído",
  FAILED: "Falhou",
  ARCHIVED: "Arquivado",
};

export const productDiscoveryDecisionLabels: Record<
  ProductDiscoveryOpportunityDecision,
  string
> = {
  APPROVE: "Aprovar",
  RESEARCH_MORE: "Pesquisar mais",
  REJECT: "Rejeitar",
  HUMAN_REVIEW: "Revisão humana",
};

const productDiscoveryKeys = {
  cycles: ["product-discovery", "cycles"] as const,
  cycle: (cycleId: number) => ["product-discovery", "cycles", cycleId] as const,
  maturityRanking: ["product-discovery", "maturity-ranking"] as const,
};

async function parseJsonResponse<T>(response: Response, errorMessage: string) {
  if (!response.ok) {
    throw new Error(`${errorMessage} (status ${response.status}).`);
  }
  return (await response.json()) as T;
}

export function useProductDiscoveryCycles() {
  return useQuery({
    queryKey: productDiscoveryKeys.cycles,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl("/api/product-discovery/v1/cycles"),
      );
      return parseJsonResponse<ProductDiscoveryCycle[]>(
        response,
        "Não foi possível carregar os ciclos de descoberta PDE",
      );
    },
  });
}

export function useProductDiscoveryCycle(cycleId?: number) {
  return useQuery({
    queryKey: cycleId
      ? productDiscoveryKeys.cycle(cycleId)
      : ["product-discovery", "cycle", "missing"],
    enabled: cycleId != null,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl(`/api/product-discovery/v1/cycles/${cycleId}`),
      );
      return parseJsonResponse<ProductDiscoveryCycleDetail>(
        response,
        "Não foi possível carregar o ciclo de descoberta PDE",
      );
    },
  });
}

export function useProductDiscoveryMaturityRanking() {
  return useQuery({
    queryKey: productDiscoveryKeys.maturityRanking,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl("/api/product-discovery/v1/maturity-ranking"),
      );
      return parseJsonResponse<ProductDiscoveryMaturityRanking>(
        response,
        "Não foi possível carregar o ranking de maturidade comercial",
      );
    },
  });
}

export function useCreateProductDiscoveryCycle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateProductDiscoveryCyclePayload) => {
      const response = await fetch(
        buildApiUrl("/api/product-discovery/v1/cycles"),
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        },
      );
      return parseJsonResponse<ProductDiscoveryCycle>(
        response,
        "Não foi possível criar o ciclo de descoberta PDE",
      );
    },
    onSuccess: async (cycle) => {
      await queryClient.invalidateQueries({
        queryKey: productDiscoveryKeys.cycles,
      });
      await queryClient.invalidateQueries({
        queryKey: productDiscoveryKeys.cycle(cycle.id),
      });
    },
  });
}

export function useArchiveArtificialLegacyEvidence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const response = await fetch(
        buildApiUrl(
          "/api/product-discovery/v1/legacy-artificial-evidence/archive",
        ),
        { method: "POST" },
      );
      return parseJsonResponse<ProductDiscoveryLegacyCleanupResult>(
        response,
        "Não foi possível invalidar as evidências artificiais legadas",
      );
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: productDiscoveryKeys.cycles,
        }),
        queryClient.invalidateQueries({
          queryKey: productDiscoveryKeys.maturityRanking,
        }),
      ]);
    },
  });
}

export function useResumeProductDiscoveryPrivateValidationHandoff() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (cycleId: number) => {
      const response = await fetch(
        buildApiUrl(
          `/api/product-discovery/v1/cycles/${cycleId}/private-validation-handoff`,
        ),
        { method: "POST" },
      );
      return parseJsonResponse<ProductDiscoveryPrivateValidationHandoffResult>(
        response,
        "Não foi possível encaminhar os dossiês atuais para Atena",
      );
    },
    onSuccess: async (result) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: productDiscoveryKeys.cycles,
        }),
        queryClient.invalidateQueries({
          queryKey: productDiscoveryKeys.cycle(result.cycleId),
        }),
        queryClient.invalidateQueries({
          queryKey: ["independent-business-process-executions"],
        }),
      ]);
    },
  });
}
