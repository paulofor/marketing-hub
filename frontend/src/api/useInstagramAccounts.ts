import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface InstagramAccount {
  id: number;
  name: string;
  handle: string;
  code: string;
}

export function useInstagramAccounts() {
  return useQuery({
    queryKey: ["instagram-accounts"],
    queryFn: async () => {
      const { data } = await axios.get<InstagramAccount[]>(
        "/api/accounts/instagram",
      );
      return data;
    },
    staleTime: 1000 * 60 * 5,
  });
}
