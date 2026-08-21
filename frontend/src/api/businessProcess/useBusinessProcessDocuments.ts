import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { BusinessProcessActivityDocument } from "./types";

/** Consulta quais atividades já possuem documentos para habilitar os links no BPM. */
export function useBusinessProcessDocumentActivities(processId?: number) {
  return useQuery({
    queryKey: ["business-processes", processId, "document-activities"],
    enabled: Boolean(processId),
    queryFn: async () =>
      (
        await axios.get<string[]>(
          `/api/business-processes/${processId}/document-activities`,
        )
      ).data,
  });
}

/** Consulta os dez documentos mais recentes da atividade selecionada. */
export function useBusinessProcessActivityDocuments(
  processId?: number,
  activityId?: string,
) {
  return useQuery({
    queryKey: [
      "business-processes",
      processId,
      "activities",
      activityId,
      "documents",
    ],
    enabled: Boolean(processId && activityId),
    queryFn: async () =>
      (
        await axios.get<BusinessProcessActivityDocument[]>(
          `/api/business-processes/${processId}/activities/${encodeURIComponent(activityId!)}/documents`,
        )
      ).data,
  });
}

/** Consulta os dez documentos mais recentes vinculados ao processo inteiro. */
export function useBusinessProcessDocuments(processId?: number) {
  return useQuery({
    queryKey: ["business-processes", processId, "documents"],
    enabled: Boolean(processId),
    queryFn: async () =>
      (
        await axios.get<BusinessProcessActivityDocument[]>(
          `/api/business-processes/${processId}/documents`,
        )
      ).data,
  });
}
