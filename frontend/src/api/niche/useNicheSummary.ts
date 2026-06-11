import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface NicheSummaryItem {
  id: number;
  name: string;
  enrichedNicheProfileId?: number | null;
  createdAt?: string | null;
  totalCost?: number | null;
  pipelineHypothesesCount: number;
  experimentsCount: number;
}

export interface NicheSummaryPage {
  items: NicheSummaryItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export function useNicheSummary(page: number, size = 30) {
  return useQuery({
    queryKey: ["niches-summary", page, size],
    queryFn: async () => {
      const { data } = await axios.get<NicheSummaryPage>(
        "/api/niches/summary",
        {
          params: { page, size },
        },
      );
      return data;
    },
  });
}
