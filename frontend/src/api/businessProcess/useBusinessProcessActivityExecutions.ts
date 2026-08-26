import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { BusinessProcessActivityExecutionHistory } from "./types";

/** Consulta as dez tarefas mais recentes da atividade no processo canônico. */
export function useBusinessProcessActivityExecutions(
  processDefinitionId?: number,
  activityId?: string,
) {
  return useQuery({
    queryKey: [
      "business-processes",
      processDefinitionId,
      "activities",
      activityId,
      "executions",
    ],
    enabled: Boolean(processDefinitionId && activityId),
    queryFn: async () =>
      (
        await axios.get<BusinessProcessActivityExecutionHistory>(
          `/api/business-processes/${processDefinitionId}/activities/${encodeURIComponent(activityId!)}/executions`,
        )
      ).data,
  });
}
