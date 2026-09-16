import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export const catalogApi = "/api/catalogo-vivo/v1/opala";
export type PromptVersion = {
  id: number;
  bindingId: number;
  versionNumber: number;
  text: string;
  sha256: string;
  status: string;
  createdBy: string;
  createdAt: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewNote?: string;
};
export type PromptBinding = {
  id: number;
  activityId: string;
  activityName: string;
  agentId: number;
  agentKey: string;
  agentName: string;
  activeVersionId: number | null;
  processId: number;
  processVersion: number;
  productTypeCode: string;
  schemaId: string;
  schemaSha256: string;
  executorModule: string;
};
export type PromptCatalog = {
  project: string;
  processId: number;
  origin: string;
  ready: boolean;
  issues: string[];
  pinnedTasks: number;
  resolutionFailures: number;
  items: {
    binding: PromptBinding;
    versions: PromptVersion[];
    usages?: {
      taskId: number;
      versionId: number;
      versionNumber: number;
      sourceReference: string;
      status: string;
      fixedAt: string;
    }[];
    events: {
      id: number;
      versionId: number;
      action: string;
      operatorName: string;
      note: string;
      createdAt: string;
    }[];
  }[];
};
export type OpalaAdoption = {
  productId: number;
  cycleId: number;
  experimentId: number;
  adopted: boolean;
  canAdopt: boolean;
  reason: string;
  processDefinitionId?: number;
  preparationUrl?: string;
  catalogUrl: string;
  operatorName?: string;
  adoptedAt?: string;
};
export function usePromptCatalog() {
  return useQuery({
    queryKey: ["catalogo-vivo", "opala"],
    queryFn: async () => (await axios.get<PromptCatalog>(catalogApi)).data,
  });
}
export function useCatalogCommand() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({ path, body }: { path: string; body: unknown }) =>
      (await axios.post(catalogApi + path, body)).data,
    onSuccess: () => client.invalidateQueries({ queryKey: ["catalogo-vivo"] }),
  });
}
export function useOpalaAdoption(productId: number, cycleId: number) {
  return useQuery({
    queryKey: ["catalogo-vivo", "adoption", productId, cycleId],
    queryFn: async () =>
      (
        await axios.get<OpalaAdoption>(
          `${catalogApi}/products/${productId}/cycles/${cycleId}/adoption`,
        )
      ).data,
  });
}
export function catalogError(error: unknown) {
  if (axios.isAxiosError(error))
    return (
      error.response?.data?.detail ||
      error.response?.data?.message ||
      error.message
    );
  return error instanceof Error
    ? error.message
    : "Não foi possível concluir a operação. Atualize a leitura e tente novamente.";
}
