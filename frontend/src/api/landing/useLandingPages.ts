import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LandingPage {
  id: number;
  url: string;
  type: string;
  status: string;
}

export function useLandingPages(expId: string) {
  return useQuery({
    queryKey: ["landings", expId],
    queryFn: async () => {
      const { data } = await axios.get<LandingPage[]>(`/api/experiments/${expId}/landing`);
      return data;
    },
  });
}
