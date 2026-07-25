import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ProductFinancialAmount {
  brl: number;
  usd: number;
}

export interface ProductFinancialLine {
  type: string;
  label: string;
  monthly: ProductFinancialAmount;
  annual: ProductFinancialAmount;
  source: string;
}

export interface ProductFinancialSummary {
  productId: number;
  productName?: string;
  productSlug?: string;
  exchangeRateBrlPerUsd: number;
  monthStart: string;
  yearStart: string;
  costs: ProductFinancialLine[];
  revenue: ProductFinancialLine;
  profit: ProductFinancialLine;
}

export function useProductFinancialSummary(productId?: string | number) {
  return useQuery({
    queryKey: ["products", productId, "financial-summary"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductFinancialSummary>(
        `/api/products/${productId}/financial-summary`,
      );
      return data;
    },
  });
}
