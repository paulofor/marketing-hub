import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export interface WorksheetLine {
  code: string;
  planned: number | null;
  reportedCost: number | null;
  forecast: number | null;
  source: string | null;
}
export interface CostWorksheetRequest {
  requestKey: string;
  expectedRevision: number;
  chainDefinitionId: number;
  experimentId: number;
  productVersion: string;
  periodStart: string;
  periodEnd: string;
  expectedRevenueBrl: number | null;
  usdBrl: number | null;
  exchangeSource: string | null;
  exchangeCheckedOn: string | null;
  operatorName: string;
  justification: string;
  approveOperatingBudgets: boolean;
  lines: WorksheetLine[];
}
export interface CostWorksheet {
  contractVersion: string;
  eventId: number | null;
  revision: number;
  editable: boolean;
  saved: CostWorksheetRequest | null;
  rows: Array<
    WorksheetLine & {
      label: string;
      currency: string;
      basis: string;
      description: string;
      estimatedCost: number | null;
      approved: number | null;
      approvalStatus: string;
      variance: number | null;
      estimateSource: string;
      canApprove: boolean;
      approvalBlocker: string | null;
      approvalScope: "PLANNING" | "EXECUTION_LIMIT";
      executionBlocker: string | null;
    }
  >;
  totals: Array<{
    currency: string;
    planned: number | null;
    reportedCost: number | null;
    forecast: number | null;
    missingPlanned: number;
    missingReported: number;
    missingForecast: number;
  }>;
  projectedResultBrl: number | null;
  gaps: string[];
  ceilingHistory: Array<{
    eventId: number;
    code: string;
    approved: number;
    operatorName: string;
    recordedAt: string;
    justification: string;
    current: boolean;
    sourceCycleId: number;
    carriedForward: boolean;
  }>;
  history: Array<{
    eventId: number;
    revision: number;
    recordedAt: string;
    operatorName: string;
    productVersion: string;
    current: boolean;
    operatingBudgetsApproved: boolean;
    snapshot: CostWorksheetRequest;
  }>;
}
export interface ApproveCostCeilingRequest {
  requestKey: string;
  expectedRevision: number;
  chainDefinitionId: number;
  experimentId: number;
  productVersion: string;
  code: string;
  approved: number;
  operatorName: string;
  justification: string;
  confirmed: boolean;
}
export function useCostWorksheet(
  productId: number,
  chainId: number,
  cycleId: number,
) {
  const client = useQueryClient();
  const key = ["cost-worksheet", productId, chainId, cycleId];
  const url = `/api/business-process-chains/learning-cycles/v1/products/${productId}/${cycleId}/cost-worksheet`;
  const query = useQuery({
    queryKey: key,
    queryFn: async () =>
      (await axios.get<CostWorksheet>(url, { params: { chainId } })).data,
    refetchOnWindowFocus: false,
    retry: false,
  });
  const approve = useMutation({
    mutationFn: async (body: ApproveCostCeilingRequest) =>
      (
        await axios.post<CostWorksheet>(
          url.replace(/cost-worksheet$/, "cost-ceilings"),
          body,
        )
      ).data,
    onSuccess: async (data) => {
      client.setQueryData(key, data);
      await client.invalidateQueries({
        queryKey: ["execution-finance", productId, chainId, cycleId],
      });
      await Promise.all([
        client.invalidateQueries({ queryKey: ["learning-cycles", productId] }),
        client.invalidateQueries({
          queryKey: ["video-budget", productId, cycleId, chainId],
        }),
        client.invalidateQueries({
          queryKey: ["cycle-process-context", productId],
        }),
        client.invalidateQueries({
          queryKey: ["process-automation", productId],
        }),
      ]);
    },
  });
  return { query, approve };
}
