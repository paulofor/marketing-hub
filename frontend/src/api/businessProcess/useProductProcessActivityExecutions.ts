import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  ProductProcessActivityExecutionHistory,
  ProductProcessActivityHumanDecision,
  ProductProcessActivityExecutionRequest,
} from "./types";

export type ProductProcessActivityExecutionCommand = {
  activityId: string;
  decision?: ProductProcessActivityHumanDecision;
};

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
    mutationFn: async ({
      activityId,
      decision,
    }: ProductProcessActivityExecutionCommand) => {
      const url = `/api/business-processes/${processDefinitionId}/products/${productId}/activities/${activityId}/execution-requests`;
      return decision
        ? (
            await axios.post<ProductProcessActivityExecutionRequest>(
              url,
              decision,
            )
          ).data
        : (await axios.post<ProductProcessActivityExecutionRequest>(url)).data;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: [
            "products",
            productId,
            "business-processes",
            processDefinitionId,
            "activity-executions",
          ],
        }),
        queryClient.invalidateQueries({
          queryKey: ["products", "value-chain-positions"],
        }),
        queryClient.invalidateQueries({ queryKey: ["experiment-runs"] }),
        queryClient.invalidateQueries({
          queryKey: ["experiment-run-preflight"],
        }),
      ]);
    },
  });
}
