import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Creative {
  id: number;
  experimentId: number;
  headline: string;
  primaryText: string;
  imageUrl: string;
  status: string;
}

export function useCreatives(expId: string) {
  return useQuery({
    queryKey: ["creatives", expId],
    queryFn: async () => {
      const { data } = await axios.get<Creative[]>(
        `/api/experiments/${expId}/creatives`,
      );
      return data;
    },
  });
}
