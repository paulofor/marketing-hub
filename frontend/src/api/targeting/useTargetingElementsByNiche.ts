import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  TargetingElement,
  TargetingElementStatus,
  TargetingElementType,
} from "./types";

interface UseTargetingElementsOptions {
  type?: TargetingElementType;
  status?: TargetingElementStatus;
}

export function useTargetingElementsByNiche(
  nicheId?: string,
  options?: UseTargetingElementsOptions,
) {
  const typeFilter = options?.type ?? "ALL";
  const statusFilter = options?.status ?? "ALL";
  return useQuery({
    queryKey: ["niche-targeting-elements", nicheId, typeFilter, statusFilter],
    enabled: Boolean(nicheId),
    queryFn: async () => {
      if (!nicheId) return [] as TargetingElement[];
      const params: Record<string, string> = {};
      if (options?.type) params.type = options.type;
      if (options?.status) params.status = options.status;
      const { data } = await axios.get<TargetingElement[]>(
        `/api/niches/${nicheId}/targeting-elements`,
        {
          params: Object.keys(params).length > 0 ? params : undefined,
        },
      );
      return data;
    },
  });
}
