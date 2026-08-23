import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ProductTypeStatus = "PROPOSED" | "ACTIVE" | "RETIRED";

export interface ProductTypeDefinition {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  aliases: string[];
  status: ProductTypeStatus;
  productCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveProductType {
  code?: string;
  name: string;
  description?: string;
  aliases: string[];
  status: ProductTypeStatus;
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
