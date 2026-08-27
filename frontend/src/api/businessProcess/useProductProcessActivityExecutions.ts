import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ProductProcessActivityExecutionHistory } from "./types";

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
