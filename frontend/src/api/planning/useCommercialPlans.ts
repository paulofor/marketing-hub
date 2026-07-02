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
  experimentsToCreate?: number | null;
  experimentsToPublish?: number | null;
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
  experimentsToCreate?: number | null;
  experimentsToPublish?: number | null;
  nextAction?: string;
  currentBlocker?: string;
  rootCause?: string;
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
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["commercial-plans"] }),
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
