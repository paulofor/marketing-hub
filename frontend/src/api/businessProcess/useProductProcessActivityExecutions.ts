import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  ProductProcessActivityExecutionHistory,
  ProductProcessActivityExecutionRequest,
} from "./types";

/** Consulta as atividades e tarefas auditáveis de um produto no processo selecionado. */
export function useProductProcessActivityExecutions(
  productId?: number,
  processDefinitionId?: number,
) {
  return useQuery({
    queryKey: [
      "products",
      productId,
      "business-processes",
      processDefinitionId,
      "activity-executions",
    ],
    enabled: Boolean(productId && processDefinitionId),
    queryFn: async () =>
      (
        await axios.get<ProductProcessActivityExecutionHistory>(
          `/api/business-processes/${processDefinitionId}/products/${productId}/activity-executions`,
        )
      ).data,
  });
}

/** Solicita ao backend todas as tarefas responsáveis pela atividade do produto. */
export function useRequestProductProcessActivityExecution(
  productId: number,
  processDefinitionId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (activityId: string) =>
      (
        await axios.post<ProductProcessActivityExecutionRequest>(
          `/api/business-processes/${processDefinitionId}/products/${productId}/activities/${activityId}/execution-requests`,
        )
      ).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: [
          "products",
          productId,
          "business-processes",
          processDefinitionId,
          "activity-executions",
        ],
      });
      await queryClient.invalidateQueries({
        queryKey: ["products", "value-chain-positions"],
      });
    },
  });
}
