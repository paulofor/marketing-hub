import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingAudienceType, TargetingRequest } from "./types";

export interface CreateTargetingRequestPayload {
  descricao: string;
  idioma?: string;
  pais?: string;
  publico_tipo?: TargetingAudienceType;
}

export function useCreateTargetingRequest() {
  return useMutation({
    mutationFn: async (payload: CreateTargetingRequestPayload) => {
      const { data } = await axios.post<TargetingRequest>(
        "/api/targeting/requests",
        payload,
      );
      return data;
    },
  });
}
