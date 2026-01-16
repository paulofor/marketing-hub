import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { PromptDomainObject } from "./types";

export function usePromptDomainObjects() {
  return useQuery({
    queryKey: ["prompt-domain-objects"],
    queryFn: async () => {
      const { data } = await axios.get<PromptDomainObject[]>("/api/prompt-domains/available-objects");
      return data;
    },
  });
}
