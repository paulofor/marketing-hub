import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface SalesPageType {
  code: string;
  name: string;
  description: string;
  commercialMechanism: string;
  leadCaptureStrategy: string;
  digitalBaitDelivery: string;
  defaultForAbTest: boolean;
  active: boolean;
}

export interface ExperimentSalesPageTypeSelection {
  id: number;
  typeCode: string;
  typeName: string;
  variantKey: string;
  trafficWeight: number;
  active: boolean;
  notes?: string | null;
  type: SalesPageType;
}

export interface UpdateExperimentSalesPageTypeSelection {
  typeCode: string;
  variantKey?: string;
  trafficWeight?: number;
  active?: boolean;
  notes?: string | null;
}

export function useSalesPageTypes() {
  return useQuery<SalesPageType[]>({
    queryKey: ["sales-page-types"],
    queryFn: async () => {
      const { data } = await axios.get<SalesPageType[]>("/api/sales-page-types");
      return data;
    },
  });
}

export function useExperimentSalesPageTypeSelections(experimentId?: string) {
  return useQuery<ExperimentSalesPageTypeSelection[]>({
    queryKey: ["experiment", experimentId, "sales-page-types"],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentSalesPageTypeSelection[]>(
        `/api/experiments/${experimentId}/sales-page-types`,
      );
      return data;
    },
  });
}

export function useUpdateExperimentSalesPageTypeSelections(experimentId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (selections: UpdateExperimentSalesPageTypeSelection[]) => {
      const { data } = await axios.put<ExperimentSalesPageTypeSelection[]>(
        `/api/experiments/${experimentId}/sales-page-types`,
        { selections },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentId, "sales-page-types"],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentId, "sales-page-ab-results"],
      });
    },
  });
}
