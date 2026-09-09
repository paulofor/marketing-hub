import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { cycleApi, type LearningCycle } from "./useLearningCycles";

const alternative = z.object({
  option: z.string(),
  benefit: z.string(),
  risk: z.string(),
  effort: z.string(),
  salesImpact: z.string(),
});
const proposalSchema = z.object({
  id: z.number().nullable(),
  cycleId: z.number(),
  cycleRevision: z.number(),
  status: z.enum([
    "WAITING",
    "QUEUED",
    "RUNNING",
    "READY",
    "FAILED",
    "EXPIRED",
    "STALE",
    "APPROVED",
  ]),
  agentKey: z.literal("experiment-strategist"),
  agentName: z.string(),
  agentId: z.number().nullable(),
  automaticExecutionEnabled: z.boolean(),
  activityDefinitionId: z.number().nullable(),
  operatorName: z.string(),
  error: z.string().nullable(),
  createdAt: z.string().nullable(),
  finishedAt: z.string().nullable(),
  approvedAt: z.string().nullable(),
  approvedEventId: z.number().nullable(),
  proposal: z
    .object({
      contractVersion: z.literal("LEARNING_CYCLE_DECISION_PROPOSAL_V1"),
      action: z.enum([
        "ADJUST",
        "CONTINUE",
        "FIX_MEASUREMENT",
        "SCALE",
        "STOP",
        "INCONCLUSIVE",
      ]),
      summary: z.string(),
      rootCause: z.string(),
      learning: z.string(),
      nextHypothesis: z.string(),
      evidenceLimits: z.string(),
      correctionPlan: z.string(),
      scaleHypothesis: z.string(),
      evidenceReference: z.string(),
      returnProcessId: z.number().nullable(),
      returnActivityId: z.string().nullable(),
      evidenceEventIds: z.array(z.number()),
      alternatives: z.array(alternative).length(3),
      selectedAlternative: z.number().int().min(0).max(2),
    })
    .nullable(),
});
export type DecisionProposal = z.infer<typeof proposalSchema>;
const url = (cycle: LearningCycle) =>
  `${cycleApi}/products/${cycle.productId}/${cycle.id}/decision-proposal`;
export function useDecisionProposal(cycle: LearningCycle) {
  return useQuery({
    queryKey: [
      "cycle-decision-proposal",
      cycle.productId,
      cycle.id,
      cycle.revision,
    ],
    queryFn: async () =>
      proposalSchema.parse((await axios.get(url(cycle))).data),
    refetchInterval: (query) =>
      ["WAITING", "QUEUED", "RUNNING"].includes(query.state.data?.status ?? "")
        ? 3000
        : false,
  });
}
export function useRetryDecisionProposal(cycle: LearningCycle) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (previousProposalId: number) =>
      axios.post(`${url(cycle)}/retry`, {
        expectedRevision: cycle.revision,
        previousProposalId,
      }),
    onSuccess: () =>
      client.invalidateQueries({
        queryKey: ["cycle-decision-proposal", cycle.productId, cycle.id],
      }),
  });
}
export function useDecisionProposalAudit(
  cycle: LearningCycle,
  enabled: boolean,
) {
  return useQuery({
    queryKey: [
      "cycle-decision-proposal-audit",
      cycle.productId,
      cycle.id,
      cycle.revision,
    ],
    enabled,
    queryFn: async () =>
      (await axios.get(`${url(cycle)}/audit`)).data as unknown,
  });
}
