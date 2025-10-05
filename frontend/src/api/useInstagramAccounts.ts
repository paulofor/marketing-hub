import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface InstagramAccount {
  id: number;
  name: string;
  avatarUrl?: string | null;
  currency: string;
  instagramUserId: string;
  facebookPageId: string;
  adAccountId: string;
  accessToken?: string | null;
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
