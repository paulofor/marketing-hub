import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export type ProfileContract = {
  productVersion: string;
  capability: string;
  purchasedOutcome: string;
  inputs: string[];
  deliverables: string[];
  qualityCriteria: string[];
  deliveryMode: string;
  revenueModel: string;
  audiovisualRequired: boolean;
  includedUnits: number;
  maximumAttempts: number;
  costModel: string;
  pricingRevision: string;
  usdBrl: number;
  maximumAttemptCostBrl: number;
  maximumDeliveryCostBrl: number;
  minimumContributionBrl: number;
  productionBudget: {
    costModel: string;
    maximumAttempts: number;
    maximumAttemptCostBrl: number;
    maximumTotalCostBrl: number;
  };
  scenarios: Array<{
    code: string;
    priceBrl: number;
    acquisitionBrl: number;
    feesBrl: number;
    supportBrl: number;
    storageDeliveryBrl: number;
    otherCostsBrl: number;
  }>;
};

export type ExecutionProfile = {
  id: number;
  productId: number;
  revision: number;
  chainId: number;
  commercialPlanId: number;
  commercialPlanVersion: number;
  productType?: string;
  productFormat?: string;
  contract: ProfileContract;
  routeName: string;
  work: Array<{
    processCode: string;
    activityId: string;
    name: string;
    owner: string;
    requirements: string[];
    applicable: boolean;
    applicabilityReason: string;
  }>;
  processes: Array<{
    id: number;
    code: string;
    version: number;
    name: string;
    parentCode?: string;
  }>;
  economics: Array<{
    code: string;
    revenueBrl: number;
    fullCostBrl: number;
    contributionBrl: number;
    viable: boolean;
  }>;
  blockers: string[];
  reviews: Array<{
    checkpoint: string;
    financialExecutionId: number;
    executionStatus: string;
    approved: boolean;
    decision: string;
    reviewedBy: string;
    rationale: string;
    createdAt: string;
  }>;
  bindings: Array<{
    id: number;
    sourceReference: string;
    learningCycleId?: number;
    createdBy: string;
  }>;
  consumption: Array<{
    id: number;
    operationKey: string;
    units: number;
    reservedBrl: number;
    actualBrl?: number;
    status: string;
    evidence?: string;
    testData: boolean;
  }>;
  createdAt: string;
  createdBy: string;
};

export type ProfileCatalog = {
  capabilities: Array<{ code: string; name: string }>;
  chains: Array<{ code: string; name: string }>;
  commercialPlans: Array<{ code: string; name: string }>;
  checkpoints: Array<{ code: string; name: string }>;
  imageModel: string;
};

const base = (id: string | number) =>
  `/api/products/${id}/execution-profiles/v1`;

export function useExecutionProfiles(productId: string) {
  return useQuery({
    queryKey: ["execution-profiles", productId],
    queryFn: async () =>
      (await axios.get<ExecutionProfile[]>(base(productId))).data,
  });
}

export function useProfileCatalog(productId: string) {
  return useQuery({
    queryKey: ["execution-profile-catalog", productId],
    queryFn: async () =>
      (await axios.get<ProfileCatalog>(`${base(productId)}/catalog`)).data,
  });
}

export function useChangeExecutionProfile(productId: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      action,
      body,
    }: {
      id?: number;
      action?:
        | "bindings"
        | "financial-analysis"
        | "financial-reviews"
        | "consumption-reconciliations";
      body?: unknown;
    }) =>
      (
        await axios.post<ExecutionProfile>(
          `${base(productId)}${id ? `/${id}/${action}` : ""}`,
          body,
        )
      ).data,
    onSuccess: async () => {
      await client.invalidateQueries({
        queryKey: ["execution-profiles", productId],
      });
    },
  });
}
