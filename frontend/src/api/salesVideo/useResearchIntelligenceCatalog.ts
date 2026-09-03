import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ResearchIntelligenceCatalog } from "./types";

export function useResearchIntelligenceCatalog() {
  return useQuery({
    queryKey: ["research-intelligence-catalog", "v1"],
    queryFn: async () => {
      const { data } = await axios.get<ResearchIntelligenceCatalog>(
        "/api/research-intelligence/v1/catalog",
      );
      return data;
    },
    staleTime: 5 * 60 * 1000,
  });
}
