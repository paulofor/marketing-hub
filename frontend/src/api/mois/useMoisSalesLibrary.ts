import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { MoisSalesLibraryEntryPageResponse } from "./types";

export function useMoisSalesLibraryEntries(workspaceId: string, page: number, pageSize: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", workspaceId, page, pageSize],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryEntryPageResponse>("/api/mois/sales-library/entries", {
        params: { workspaceId, page, pageSize },
      });
      return data;
    },
  });
}
