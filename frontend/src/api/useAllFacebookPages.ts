import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { FacebookAccount } from "./useFacebookAccounts";
import type { FacebookPage } from "./useFacebookPages";

export function useAllFacebookPages() {
  return useQuery({
    queryKey: ["facebook-pages", "all"],
    queryFn: async () => {
      const { data: accounts } = await axios.get<FacebookAccount[]>(
        "/api/accounts/facebook",
      );

      if (!Array.isArray(accounts) || accounts.length === 0) {
        return [] as FacebookPage[];
      }

      const pagesByAccount = await Promise.all(
        accounts.map(async (account) => {
          const { data } = await axios.get<FacebookPage[]>(
            `/api/accounts/facebook/${account.id}/pages`,
          );
          return data;
        }),
      );

      return pagesByAccount.flat();
    },
    staleTime: 1000 * 60 * 5,
  });
}
