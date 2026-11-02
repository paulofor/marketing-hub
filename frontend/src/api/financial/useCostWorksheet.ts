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
  const save = useMutation({
    mutationFn: async (body: CostWorksheetRequest) =>
      (await axios.post<CostWorksheet>(url, body)).data,
    onSuccess: (data) => {
      client.setQueryData(key, data);
      void client.invalidateQueries({
        queryKey: ["execution-finance", productId, chainId, cycleId],
      });
      void client.invalidateQueries({
        queryKey: ["learning-cycles", productId],
      });
    },
  });
  return { query, save };
}
