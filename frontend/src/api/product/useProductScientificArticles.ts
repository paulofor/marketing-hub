import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface ProductScientificArticle {
  id: number;
  productId: number;
  link: string;
  originalTitle: string;
  portugueseTitle: string;
  summary: string;
  mechanismApplication: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveProductScientificArticle {
  link: string;
  originalTitle: string;
  portugueseTitle: string;
  summary: string;
  mechanismApplication: string;
}

export function useProductScientificArticles(productId?: string | number) {
  return useQuery({
    queryKey: ["products", productId, "scientific-articles"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductScientificArticle[]>(
        `/api/products/${productId}/scientific-articles`,
      );
      return data;
    },
  });
}

export function useCreateProductScientificArticle(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: SaveProductScientificArticle) => {
      const { data } = await axios.post<ProductScientificArticle>(
        `/api/products/${productId}/scientific-articles`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "scientific-articles"],
      });
    },
  });
}

export function useUpdateProductScientificArticle(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      articleId,
      payload,
    }: {
      articleId: number;
      payload: SaveProductScientificArticle;
    }) => {
      const { data } = await axios.put<ProductScientificArticle>(
        `/api/products/${productId}/scientific-articles/${articleId}`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "scientific-articles"],
      });
    },
  });
}

export function useDeleteProductScientificArticle(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (articleId: number) => {
      await axios.delete(
        `/api/products/${productId}/scientific-articles/${articleId}`,
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "scientific-articles"],
      });
    },
  });
}
