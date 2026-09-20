import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export type PlanEnvironment = "LIVE" | "TEST";
export type PlanScope = "products" | "product-types";
export type ScenarioCode = "CONSERVATIVE" | "BASE" | "OPTIMISTIC";
export const costLabels = {
  feePercent: "Taxa de pagamento (%)",
  taxPercent: "Tributos (%)",
  commissionPercent: "Comissões (%)",
  refundPercent: "Provisão de reembolsos (%)",
  fixedFeeBrl: "Taxa fixa por pedido (R$)",
  supportBrl: "Suporte por cliente (R$)",
  storageBrl: "Armazenamento por cliente (R$)",
  deliveryBrl: "Entrega por cliente (R$)",
  otherVariableBrl: "Outros custos variáveis por cliente (R$)",
  initialAiBrl: "Investimento inicial em IA (R$)",
  initialOtherBrl: "Demais investimentos iniciais (R$)",
  fixedPerPeriodBrl: "Custos fixos por período (R$)",
} as const;
export type CostKey = keyof typeof costLabels;
export interface PlanAssumptions {
  preparation?: { supportDays: number; personalizedAi: boolean } | null;
  productVersion: string | null;
  periodDays: number | null;
  validUntil: string;
  evidence: string;
  priceBrl: number | null;
  minimumMarginPercent: number | null;
  maximumCacBrl: number | null;
  ai: {
    currency: "BRL" | "USD";
    providerModel: string | null;
    perAttempt: number | null;
    pricingSource: string | null;
    pricingCheckedOn: string | null;
    usdBrl: number | null;
    exchangeSource: string | null;
    includedUnits: number | null;
    maximumAttempts: number | null;
    maximumCostPerCustomerBrl: number | null;
  };
  costs: Record<CostKey, number | null>;
  scenarios: Array<{
    code: ScenarioCode;
    customers: number | null;
    attemptsPerCustomer: number | null;
    cacBrl: number | null;
  }>;
}
export interface ScenarioResult {
  code: string;
  label: string;
  customers: number;
  attemptsPerCustomer: number;
  grossRevenueBrl: number;
  netRevenueBrl: number;
  aiCostPerCustomerBrl: number;
  aiCostPerUsefulUnitBrl: number;
  variableDeliveryPerCustomerBrl: number;
  contributionBeforeCacBrl: number;
  contributionAfterCacBrl: number;
  marginPercent: number | null;
  maximumAffordableCacBrl: number | null;
  operatingResultBrl: number;
  resultAfterInvestmentBrl: number;
  breakEvenCustomers: number | null;
  aiToNetRevenuePercent: number | null;
  viable: boolean;
  blockers: string[];
}
export interface FinancialPlan {
  id: number;
  scope: "PRODUCT" | "TYPE";
  scopeId: number;
  environment: PlanEnvironment;
  name: string;
  revision: number;
  templateId: number | null;
  commercialPlanId: number | null;
  commercialPlanVersion: number | null;
  createdBy: string;
  createdAt: string;
  assumptions: PlanAssumptions;
  evaluation: {
    status: string;
    label: string;
    blockers: string[];
    scenarios: ScenarioResult[];
  };
  stale: boolean;
  pendingActions: string[];
  canRequestAnalysis: boolean;
  analysis: null | {
    executionId: number;
    status: string;
    report: string | null;
    result: FinancialProjection | null;
    error: string | null;
    model: string | null;
    costUsd: number | null;
    costCoverage: string;
    finishedAt: string | null;
  };
}
export interface FinancialProjection {
  executiveSummary?: string;
  limitations?: string[];
  breakEven?: string;
  recommendedInitialInvestmentBrl?: number | null;
  recommendedCycleLimitBrl?: number | null;
  decisionCriteria?: Record<"continue" | "adjust" | "stop", string[]>;
  scenarios?: Array<{
    name: ScenarioCode;
    assumptions: string[];
    traffic: number | null;
    conversionRatePercent: number | null;
    averagePriceBrl: number | null;
    revenueBrl: number | null;
    contributionMarginPercent: number | null;
    profitBrl: number | null;
    cacBrl: number | null;
    roas: number | null;
  }>;
}
export interface SaveFinancialPlan {
  name: string;
  createdBy: string;
  expectedRevision: number;
  commercialPlanId: number | null;
  templateId: number | null;
  assumptions: PlanAssumptions;
}
export interface PlanCatalog {
  products: Array<{ id: number; name: string; productTypeId: number | null }>;
  productTypes: Array<{ id: number; name: string }>;
  commercialPlans: Array<{ id: number; name: string }>;
}
const base = "/api/financial-plans/v1";
export interface PlanPreparation {
  expectedRevision: number;
  sourceRevisionId: number | null;
  commercialPlanId: number | null;
  commercialPlanVersion: number | null;
  productVersion: string | null;
  supportDays: number;
  personalizedAi: boolean;
  suggestion: string;
  priceBrl: number | null;
  canPrepare: boolean;
  blocker: string | null;
}
export type PrepareFinancialPlan = Pick<
  PlanPreparation,
  | "expectedRevision"
  | "commercialPlanId"
  | "commercialPlanVersion"
  | "productVersion"
  | "supportDays"
  | "personalizedAi"
>;
export function useFinancialPlanPreparation(
  ownerId: number,
  environment: PlanEnvironment,
) {
  return useQuery({
    queryKey: ["financial-plan-preparation", ownerId, environment],
    queryFn: async () =>
      (
        await axios.get<PlanPreparation>(
          `${base}/products/${ownerId}/preparation`,
          { params: { environment } },
        )
      ).data,
    refetchOnWindowFocus: false,
    retry: false,
  });
}
export function usePrepareFinancialPlan(
  ownerId: number,
  environment: PlanEnvironment,
) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: async (body: PrepareFinancialPlan) =>
      (
        await axios.post<FinancialPlan>(
          `${base}/products/${ownerId}/preparation`,
          body,
          { params: { environment } },
        )
      ).data,
    onSuccess: async () => {
      await cache.invalidateQueries({
        queryKey: ["financial-plans", "products", ownerId, environment],
      });
      await cache.invalidateQueries({
        queryKey: ["financial-plan-preparation", ownerId, environment],
      });
    },
  });
}
export function useFinancialPlanCatalog(productId?: number) {
  return useQuery({
    queryKey: ["financial-plan-catalog", productId],
    queryFn: async () =>
      (
        await axios.get<PlanCatalog>(`${base}/catalog`, {
          params: { productId },
        })
      ).data,
  });
}
export function useFinancialPlans(
  scope: PlanScope,
  ownerId: number | undefined,
  environment: PlanEnvironment,
) {
  return useQuery({
    queryKey: ["financial-plans", scope, ownerId, environment],
    enabled: Boolean(ownerId),
    queryFn: async () =>
      (
        await axios.get<FinancialPlan[]>(`${base}/${scope}/${ownerId}`, {
          params: { environment },
        })
      ).data,
    refetchInterval: 5000,
  });
}
export function useSaveFinancialPlan(
  scope: PlanScope,
  ownerId: number,
  environment: PlanEnvironment,
) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: async (body: SaveFinancialPlan) =>
      (
        await axios.post<FinancialPlan>(`${base}/${scope}/${ownerId}`, body, {
          params: { environment },
        })
      ).data,
    onSuccess: () =>
      cache.invalidateQueries({
        queryKey: ["financial-plans", scope, ownerId, environment],
      }),
  });
}
export function useAnalyzeFinancialPlan(
  ownerId: number,
  environment: PlanEnvironment,
) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      (
        await axios.post<FinancialPlan>(
          `${base}/products/${ownerId}/revisions/${id}/analysis`,
          undefined,
          { params: { environment } },
        )
      ).data,
    onSuccess: () =>
      cache.invalidateQueries({
        queryKey: ["financial-plans", "products", ownerId, environment],
      }),
  });
}
