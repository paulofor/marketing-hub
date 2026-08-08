import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Hypothesis } from "./useHypothesisBoard";

export interface HypothesisPage {
  items: Hypothesis[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export function useHypotheses(status: string = "ALL", page = 0, size = 25) {
  return useQuery({
    queryKey: ["hypotheses", status, page, size],
    queryFn: async () => {
      const { data } = await axios.get<HypothesisPage>("/api/hypotheses/page", {
        params: { status, page, size },
      });
      return data;
    },
  });
}
