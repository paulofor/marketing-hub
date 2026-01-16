import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { PromptDomain } from "./types";

export function usePromptDomains() {
  return useQuery({
    queryKey: ["prompt-domains"],
    queryFn: async () => {
      const { data } = await axios.get<PromptDomain[]>("/api/prompt-domains");
      return data;
    },
  });
}
