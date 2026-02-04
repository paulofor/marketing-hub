import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingAudienceType, TargetingRequest } from "./types";
import {
  TARGETING_REQUESTS_QUERY_KEY,
  buildTargetingRequestsQueryKey,
  type TargetingRequestQueryFilters,
} from "./useTargetingRequests";

export interface CreateTargetingRequestPayload {
  descricao: string;
  idioma?: string;
  pais?: string;
  publico_tipo?: TargetingAudienceType;
  niche_id?: number;
  hypothesis_id?: string;
}

export function useCreateTargetingRequest(filters?: TargetingRequestQueryFilters) {
  const queryClient = useQueryClient();
  const queryKey = buildTargetingRequestsQueryKey(filters);

  return useMutation({
    mutationFn: async (payload: CreateTargetingRequestPayload) => {
      const { data } = await axios.post<TargetingRequest>(
        "/api/targeting/requests",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: [TARGETING_REQUESTS_QUERY_KEY] });
    },
  });
}
