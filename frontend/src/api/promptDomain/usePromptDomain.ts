import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { PromptDomain } from "./types";

export function usePromptDomain(id?: string) {
  return useQuery({
    queryKey: ["prompt-domain", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<PromptDomain>(`/api/prompt-domains/${id}`);
      return data;
    },
  });
}
