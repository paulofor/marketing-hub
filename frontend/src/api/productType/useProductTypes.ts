import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ProductTypeStatus = "PROPOSED" | "ACTIVE" | "RETIRED";

export interface ProductTypeBlueprint {
  version?: string | null;
  primaryChannel?: string | null;
  customerJob?: string | null;
  valueMechanism?: string | null;
  experienceFlow?: string | null;
  requiredInputs?: string | null;
  expectedOutputs?: string | null;
  memoryStrategy?: string | null;
  integrationRequirements?: string | null;
  safetyGuardrails?: string | null;
  successMetrics?: string | null;
  backendSdkModule?: string | null;
  frontendSdkModule?: string | null;
}

export interface SaveProductTypeBlueprint {
  version: string;
  primaryChannel: string;
  customerJob: string;
  valueMechanism: string;
  experienceFlow: string;
  requiredInputs: string;
  expectedOutputs: string;
  memoryStrategy: string;
  integrationRequirements: string;
  safetyGuardrails: string;
  successMetrics: string;
  backendSdkModule: string;
  frontendSdkModule: string;
}

export interface ProductTypeDefinition {
  id: number;
  code: string;
  name: string;
  internalName?: string | null;
  description?: string | null;
  aliases: string[];
  status: ProductTypeStatus;
  blueprint?: ProductTypeBlueprint | null;
  constructionReady: boolean;
  missingBlueprintFields: string[];
  productCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveProductType {
  code?: string;
  name: string;
  internalName: string;
  description?: string;
  aliases: string[];
  status: ProductTypeStatus;
  blueprint?: SaveProductTypeBlueprint | null;
}

export function useProductTypes(includeRetired = false, query = "") {
  return useQuery({
    queryKey: ["product-types", includeRetired, query.trim()],
    queryFn: async () => {
      const { data } = await axios.get<ProductTypeDefinition[]>(
        "/api/product-types",
        {
          params: {
            includeRetired,
            ...(query.trim() ? { query: query.trim() } : {}),
          },
        },
      );
      return data;
    },
  });
}

export function useSaveProductType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      data,
    }: {
      id?: number;
      data: SaveProductType;
    }) => {
      const response = id
        ? await axios.put<ProductTypeDefinition>(
            `/api/product-types/${id}`,
            data,
          )
        : await axios.post<ProductTypeDefinition>("/api/product-types", data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["product-types"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });
}
