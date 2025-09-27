import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FacebookPage {
  id: number;
  accountId: number;
  pageId: string;
  name: string;
}

export function useFacebookPages(accountId?: string | number) {
  return useQuery({
    queryKey: ["facebook-pages", accountId],
    queryFn: async () => {
      if (!accountId) {
        return [] as FacebookPage[];
      }
      const { data } = await axios.get<FacebookPage[]>(
        `/api/accounts/facebook/${accountId}/pages`,
      );
      return data;
    },
    enabled: Boolean(accountId),
  });
}
