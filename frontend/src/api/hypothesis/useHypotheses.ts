import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Hypothesis } from "./useHypothesisBoard";

export function useHypotheses(status: string = "ALL") {
  return useQuery({
    queryKey: ["hypotheses", status],
    queryFn: async () => {
      const { data } = await axios.get<Hypothesis[]>(
        `/api/hypotheses?status=${status}`,
      );
      return data;
    },
  });
}
