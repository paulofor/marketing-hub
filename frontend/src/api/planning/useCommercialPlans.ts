import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type CommercialPlanStatus =
  "DRAFT" | "IN_PROGRESS" | "BLOCKED" | "COMPLETED" | "CANCELLED";

export type CommercialPlanRecommendation =
  "CONTINUE" | "CORRECT" | "PAUSE" | "END";

export type CommercialPlanMilestoneStatus =
  "PENDING" | "IN_PROGRESS" | "DONE" | "BLOCKED";

export interface CommercialPlanMilestone {
  id: number;
  sequenceOrder: number;
  code: string;
  name: string;
  status: CommercialPlanMilestoneStatus;
  dueDate?: string | null;
  targetCost?: number | null;
  targetRevenue?: number | null;
  experimentsToCreate?: number | null;
  experimentsToPublish?: number | null;
  productsToValidate?: number | null;
  productTypesToExplore?: number | null;
  approachesToTest?: number | null;
  customerConversationsTarget?: number | null;
  actualCampaignCost?: number | null;
  actualAiCost?: number | null;
  actualTotalCost?: number | null;
  actualRevenue?: number | null;
  actualExperimentsCreated?: number | null;
  actualExperimentsPublished?: number | null;
  executionSyncedAt?: string | null;
  evidenceSource?: string | null;
  blocker?: string | null;
  recommendedNextAction?: string | null;
}

export interface CommercialPlanSimulation {
  id: number;
  recommendation: CommercialPlanRecommendation;
  mostLikelyScenario?: string | null;
  bestRealisticScenario?: string | null;
  worstLikelyScenario?: string | null;
  mainRisk?: string | null;
  bestNextAction?: string | null;
  actionToAvoid?: string | null;
  continueCondition?: string | null;
  stopCondition?: string | null;
  evidence7Days?: string | null;
  evidence14Days?: string | null;
  evidence30Days?: string | null;
  decisionNotes?: string | null;
  createdAt?: string | null;
}

export interface CommercialPlan {
  id: number;
  name: string;
  planType: "FIRST_SALE";
  status: CommercialPlanStatus;
  nicheId?: number | null;
  nicheName?: string | null;
  hypothesisId?: string | null;
  hypothesisTitle?: string | null;
  experimentId?: number | null;
  experimentName?: string | null;
  experiments: Array<{ id: number; name: string; status: string }>;
  commercialObjective?: string | null;
  targetAudience?: string | null;
  mainPain?: string | null;
  mainOffer?: string | null;
  mainLeadMagnet?: string | null;
  mainChannel?: string | null;
  mainMetric?: string | null;
  successCriteria?: string | null;
  stopCriteria?: string | null;
  deadline?: string | null;
  maxBudget?: number | null;
  targetRevenue?: number | null;
  operationalRevenueTarget?: number | null;
  offerPriceBrl?: number | null;
  variableCostPerSaleBrl?: number | null;
  expectedMonthlyTraffic?: number | null;
  expectedConversionRatePercent?: number | null;
  expectedCacBrl?: number | null;
  expectedRefundRatePercent?: number | null;
  fixedOperationalCostBrl?: number | null;
  experimentsToCreate?: number | null;
  experimentsToPublish?: number | null;
  productsToValidate?: number | null;
  productTypesToExplore?: number | null;
  approachesToTest?: number | null;
  customerConversationsTarget?: number | null;
  actualCampaignCost?: number | null;
  actualAiCost?: number | null;
  actualTotalCost?: number | null;
  actualRevenue?: number | null;
  actualExperimentsCreated?: number | null;
  actualExperimentsPublished?: number | null;
  executionSyncedAt?: string | null;
  daysRemaining: number;
  nextAction?: string | null;
  currentBlocker?: string | null;
  rootCause?: string | null;
  mostLikelyScenario?: string | null;
  mainFutureRisk?: string | null;
  actionToAvoid?: string | null;
  milestones: CommercialPlanMilestone[];
  simulations: CommercialPlanSimulation[];
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CommercialPlanVisualAsset {
  id: number;
  assetUrl: string;
  mediaType: "IMAGE" | "VIDEO";
  label: string;
  purpose: string;
  purposes: string[];
  origin: string;
  rightsStatement: string;
  versionNumber: number;
  status: "DRAFT" | "APPROVED" | "RETIRED";
  sourceAssetId?: number | null;
  agentReviewStatus?:
    "PENDING" | "PROCESSING" | "APPROVED" | "ADJUST" | "FAILED" | null;
  agentReviewSummary?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CommercialPlanImageStudioJob {
  id: number;
  commercialPlanId: number;
  sourceAssetId?: number | null;
  resultAssetId?: number | null;
  operation: "CREATE" | "EDIT";
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  label: string;
  prompt: string;
  purposes: string[];
  size: string;
  quality: string;
  model?: string | null;
  costUsd?: number | null;
  error?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt: string;
}

export interface CreateCommercialPlanImageStudioJobPayload {
  operation: "CREATE" | "EDIT";
  sourceAssetId?: number | null;
  referenceAssetIds: number[];
  prompt: string;
  label: string;
  purposes: string[];
  size: string;
  quality: string;
}

export interface CreateCommercialPlanVisualAssetPayload {
  assetUrl: string;
  mediaType: "IMAGE" | "VIDEO";
  label: string;
  purpose: string;
  origin: string;
  rightsStatement: string;
}

export interface CommercialPlanVersion {
  id: number;
  commercialPlanId: number;
  versionNumber: number;
  snapshotJson: string;
  changedBy: string;
  changeReason: string;
  createdAt: string;
}

export interface RevenueProjectionExecution {
  id: number;
  commercialPlanId: number;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
  authorityMode:
    "READ_ONLY_REVENUE_PROJECTION" | "COMMERCIAL_ASSUMPTIONS_VALIDATION";
  commercialPlanVersion: number;
  agentTaskId?: number | null;
  projectionRequest?: string | null;
  reconciliationJson?: string | null;
  dailyReport?: string | null;
  errorMessage?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt: string;
}

/** Consulta o trabalho conjunto de Atena e Plutus para completar premissas ausentes. */
export function useCommercialAssumptionDefinitions(planId?: number | null) {
  return useQuery({
    queryKey: ["commercial-assumption-definitions", planId],
    enabled: !!planId && planId > 0,
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<RevenueProjectionExecution[]>(
        `/api/financial-agent/v1/commercial-plans/${planId}/commercial-assumptions`,
      );
      return data;
    },
  });
}

/** Inicia Atena e encadeia automaticamente a validação financeira de Plutus. */
export function useRequestCommercialAssumptions(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      if (!planId) throw new Error("Plano comercial não informado.");
      await axios.post(
        `/api/experiment-strategist/v1/commercial-plans/${planId}/commercial-assumptions`,
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["commercial-assumption-definitions", planId],
      });
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-agent-activity", planId],
      });
    },
  });
}

export interface CommercialPlanAgentActivityEntry {
  recordType: string;
  agentKey: string;
  agentNickname: string;
  title: string;
  status: string;
  detail?: string | null;
  finalOpinion?: string | null;
  difficulty?: string | null;
  externalDecisionRequired: boolean;
  externalDecision?: string | null;
  sourceReference?: string | null;
  budgetLimitUsd?: number | null;
  knownCostUsd?: number | null;
  financialDecision?: string | null;
  occurredAt?: string | null;
}

export interface CommercialPlanAgentActivity {
  commercialPlanId: number;
  currentVersion: number;
  budgetLimitBrl?: number | null;
  campaignCostBrl?: number | null;
  aiCostBrl?: number | null;
  totalCostBrl?: number | null;
  revenueBrl?: number | null;
  videoBudgetLimitUsd: number;
  videoKnownCostUsd: number;
  openTasks: number;
  pendingDecisions: number;
  entries: CommercialPlanAgentActivityEntry[];
}

export interface CommercialPlanWeekExperiment {
  id: number;
  name: string;
  nicheId?: number | null;
  nicheName?: string | null;
  hypothesisId?: string | null;
  hypothesisTitle?: string | null;
  productType?: string | null;
  manual?: boolean | null;
  abTest?: boolean | null;
  status?: string | null;
  createdAt?: string | null;
  campaignCost?: number | null;
  aiCost?: number | null;
  videoCost?: number | null;
  totalCost?: number | null;
  revenue?: number | null;
  impressions?: number | null;
  clicks?: number | null;
  visitors?: number | null;
  leads?: number | null;
  checkoutClicks?: number | null;
  purchases?: number | null;
  averageProductViewTimeMs?: number | null;
  result?: string | null;
}

export interface CommercialPlanWeekObjective {
  id?: number | null;
  sequenceOrder: number;
  objectiveText: string;
  score?: number | null;
  planVersionNumber?: number | null;
  assignedAgentKey?: string | null;
  assignedAgentNickname?: string | null;
  expectedResult?: string | null;
  executionStatus?:
    "PLANNED" | "IN_PROGRESS" | "BLOCKED" | "COMPLETED" | "CANCELLED";
  dueDate?: string | null;
  plannedCost?: number | null;
  plannedRevenue?: number | null;
}

export interface CommercialPlanFunnelStage {
  code: string;
  name: string;
  plannedTotal?: number | null;
  actualTotal?: number | null;
  conversionFromPreviousStep?: number | null;
  costPerConversion?: number | null;
  uniqueCount?: number | null;
  lastEventAt?: string | null;
  applicable?: boolean | null;
  evidenceSource?: string | null;
}

export interface CommercialPlanWeek {
  weekNumber: number;
  startDate: string;
  endDate: string;
  experimentsCreated: number;
  totalCost?: number | null;
  totalRevenue?: number | null;
  objectivesEditable?: boolean | null;
  objectiveEditWindowMessage?: string | null;
  funnelStages?: CommercialPlanFunnelStage[] | null;
  objectives: CommercialPlanWeekObjective[];
  experiments: CommercialPlanWeekExperiment[];
}

export interface SaveCommercialPlanPayload {
  name: string;
  status?: CommercialPlanStatus;
  nicheId?: number | null;
  hypothesisId?: string | null;
  experimentId?: number | null;
  commercialObjective?: string;
  targetAudience?: string;
  mainPain?: string;
  mainOffer?: string;
  mainLeadMagnet?: string;
  mainChannel?: string;
  mainMetric?: string;
  successCriteria?: string;
  stopCriteria?: string;
  deadline?: string;
  maxBudget?: number | null;
  targetRevenue?: number | null;
  operationalRevenueTarget?: number | null;
  offerPriceBrl?: number | null;
  variableCostPerSaleBrl?: number | null;
  expectedMonthlyTraffic?: number | null;
  expectedConversionRatePercent?: number | null;
  expectedCacBrl?: number | null;
  expectedRefundRatePercent?: number | null;
  fixedOperationalCostBrl?: number | null;
  experimentsToCreate?: number | null;
  experimentsToPublish?: number | null;
  productsToValidate?: number | null;
  productTypesToExplore?: number | null;
  approachesToTest?: number | null;
  customerConversationsTarget?: number | null;
  nextAction?: string;
  currentBlocker?: string;
  rootCause?: string;
}

export interface CommercialPlanJourneyHomologation {
  planId: number;
  experimentId: number;
  status: string;
  requestedAt: string;
}

export interface CommercialPlanOperationalFlow {
  commercialPlanId: number;
  currentStage: string;
  status: "APROVADO" | "BLOQUEADO" | "AJUSTE_NECESSARIO" | "EM_ANDAMENTO";
  nextAction: string;
  blocker?: string | null;
  expectedMetric: string;
  decisionCriterion: string;
  stages: Array<{
    code: string;
    label: string;
    status: "CONCLUIDO" | "ATUAL" | "PENDENTE";
  }>;
  specialistDecisions: Array<{
    specialist: string;
    responsibility: string;
    decision: string;
    nextAction: string;
  }>;
}

/** Consulta a visão canônica e simplificada do avanço comercial. */
export function useCommercialPlanOperationalFlow(planId?: number | null) {
  return useQuery({
    queryKey: ["commercial-plan-operational-flow", planId],
    enabled: !!planId && planId > 0,
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<CommercialPlanOperationalFlow>(
        `/api/planning/commercial-plans/${planId}/operational-flow`,
      );
      return data;
    },
  });
}

export function useCommercialPlans() {
  return useQuery({
    queryKey: ["commercial-plans"],
    queryFn: async () => {
      const { data } = await axios.get<CommercialPlan[]>(
        "/api/planning/commercial-plans",
      );
      return data;
    },
  });
}

/** Consulta o Kit Visual canônico do plano comercial. */
export function useCommercialPlanVisualAssets(planId?: number | null) {
  return useQuery({
    queryKey: ["commercial-plan-visual-assets", planId],
    enabled: !!planId,
    queryFn: async () => {
      const { data } = await axios.get<CommercialPlanVisualAsset[]>(
        `/api/planning/commercial-plans/${planId}/visual-assets`,
      );
      return data;
    },
  });
}

/** Anexa uma referência como rascunho para revisão independente. */
export function useCreateCommercialPlanVisualAsset(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateCommercialPlanVisualAssetPayload) => {
      const { data } = await axios.post<CommercialPlanVisualAsset>(
        `/api/planning/commercial-plans/${planId}/visual-assets`,
        payload,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-visual-assets", planId],
      }),
  });
}

/** Aprova ou retira uma referência sem apagar o histórico. */
export function useUpdateCommercialPlanVisualAssetStatus(
  planId?: number | null,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      assetId,
      status,
    }: {
      assetId: number;
      status: "APPROVED" | "RETIRED";
    }) => {
      const { data } = await axios.patch<CommercialPlanVisualAsset>(
        `/api/planning/commercial-plans/${planId}/visual-assets/${assetId}/status`,
        { status },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-visual-assets", planId],
      }),
  });
}

/** Consulta a fila auditável de criação e edição executada por Têmis. */
export function useCommercialPlanImageStudioJobs(planId?: number | null) {
  return useQuery({
    queryKey: ["commercial-plan-image-studio-jobs", planId],
    enabled: !!planId,
    refetchInterval: 10_000,
    queryFn: async () => {
      const { data } = await axios.get<CommercialPlanImageStudioJob[]>(
        `/api/planning/commercial-plans/${planId}/image-studio/jobs`,
      );
      return data;
    },
  });
}

/** Solicita a Têmis uma nova versão sem publicar ou aprovar automaticamente. */
export function useCreateCommercialPlanImageStudioJob(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateCommercialPlanImageStudioJobPayload) => {
      const { data } = await axios.post<CommercialPlanImageStudioJob>(
        `/api/planning/commercial-plans/${planId}/image-studio/jobs`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-image-studio-jobs", planId],
      });
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-visual-assets", planId],
      });
    },
  });
}

/** Consulta o histórico oficial que orientou tarefas, gates e agentes. */
export function useCommercialPlanVersions(planId?: number | null) {
  return useQuery({
    queryKey: ["commercial-plan-versions", planId],
    enabled: !!planId && planId > 0,
    queryFn: async () => {
      const { data } = await axios.get<CommercialPlanVersion[]>(
        `/api/planning/commercial-plans/${planId}/versions`,
      );
      return data;
    },
  });
}

/** Consulta a prestação de contas persistida dos agentes no plano. */
export function useCommercialPlanAgentActivity(planId?: number | null) {
  return useQuery({
    queryKey: ["commercial-plan-agent-activity", planId],
    enabled: !!planId && planId > 0,
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<CommercialPlanAgentActivity>(
        `/api/planning/commercial-plans/${planId}/agent-activity`,
      );
      return data;
    },
  });
}

/** Solicita a homologação integral da jornada pelo agente Dédalo. */
export function useRequestCommercialPlanJourneyHomologation(
  planId?: number | null,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (experimentId: number) => {
      if (!planId) throw new Error("Plano comercial não informado.");
      const { data } = await axios.post<CommercialPlanJourneyHomologation>(
        `/api/planning/commercial-plans/${planId}/journey-homologations`,
        null,
        { params: { experimentId } },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-agent-activity", planId],
      });
    },
  });
}

/** Consulta as projeções de Plutus sem misturá-las às receitas realizadas. */
export function useRevenueProjections(planId?: number | null) {
  return useQuery({
    queryKey: ["revenue-projections", planId],
    enabled: !!planId && planId > 0,
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<RevenueProjectionExecution[]>(
        `/api/financial-agent/v1/commercial-plans/${planId}/revenue-projections`,
      );
      return data;
    },
  });
}

/** Enfileira uma projeção versionada e abre a tarefa correspondente na mesa de Plutus. */
export function useRequestRevenueProjection(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (decisionContext?: string) => {
      if (!planId) throw new Error("Plano comercial não informado.");
      const { data } = await axios.post<RevenueProjectionExecution>(
        `/api/financial-agent/v1/commercial-plans/${planId}/revenue-projections`,
        { decisionContext: decisionContext || null },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["revenue-projections", planId],
      });
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-agent-activity", planId],
      });
    },
  });
}

export function useCommercialPlanWeeks(
  planId?: number | null,
  referenceMonth?: string | null,
) {
  return useQuery({
    queryKey: ["commercial-plan-weeks", planId, referenceMonth],
    enabled: !!planId,
    queryFn: async () => {
      const params = referenceMonth != null ? { referenceMonth } : undefined;
      const { data } = await axios.get<CommercialPlanWeek[]>(
        `/api/planning/commercial-plans/${planId}/weeks`,
        { params },
      );
      return data;
    },
  });
}

export function useUpdateCommercialPlanWeekObjectives(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      weekNumber,
      objectives,
    }: {
      weekNumber: number;
      objectives: CommercialPlanWeekObjective[];
    }) => {
      if (!planId) {
        throw new Error("Plano comercial não informado.");
      }
      const { data } = await axios.put<CommercialPlanWeekObjective[]>(
        `/api/planning/commercial-plans/${planId}/weeks/${weekNumber}/objectives`,
        { objectives },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-weeks", planId],
      }),
  });
}

export function useUpdateCommercialPlanWeekCommitmentStatus(
  planId?: number | null,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      commitmentId,
      status,
      score,
    }: {
      commitmentId: number;
      status: NonNullable<CommercialPlanWeekObjective["executionStatus"]>;
      score?: number | null;
    }) => {
      if (!planId) throw new Error("Plano comercial não informado.");
      const { data } = await axios.patch<CommercialPlanWeekObjective>(
        `/api/planning/commercial-plans/${planId}/weeks/commitments/${commitmentId}/status`,
        { status, score },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-weeks", planId],
      }),
  });
}

export function useCreateCommercialPlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: SaveCommercialPlanPayload) => {
      const { data } = await axios.post<CommercialPlan>(
        "/api/planning/commercial-plans",
        payload,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["commercial-plans"] }),
  });
}

export function useUpdateCommercialPlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      payload,
    }: {
      id: number;
      payload: SaveCommercialPlanPayload;
    }) => {
      const { data } = await axios.put<CommercialPlan>(
        `/api/planning/commercial-plans/${id}`,
        payload,
      );
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["commercial-plans"] });
      queryClient.invalidateQueries({
        queryKey: ["commercial-plan-versions", variables.id],
      });
    },
  });
}

export function useSimulateCommercialPlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      decisionNotes,
    }: {
      id: number;
      decisionNotes?: string;
    }) => {
      const { data } = await axios.post<CommercialPlanSimulation>(
        `/api/planning/commercial-plans/${id}/simulations`,
        { decisionNotes },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["commercial-plans"] }),
  });
}
