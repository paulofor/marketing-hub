import { keepPreviousData, useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { PageResponse } from "../journey/types";

export interface MicroserviceException {
  id: number;
  microserviceId: number;
  microserviceName: string;
  exceptionType?: string | null;
  message: string;
  stackTrace?: string | null;
  severity?: string | null;
  serviceVersion?: string | null;
  hostname?: string | null;
  context?: string | null;
  occurredAt?: string | null;
  createdAt?: string | null;
}

export interface MicroserviceExceptionQuery {
  page?: number;
  size?: number;
  microserviceId?: number;
  severity?: string;
}

export function useMicroserviceExceptions(params: MicroserviceExceptionQuery) {
  return useQuery<PageResponse<MicroserviceException>>({
    queryKey: ["microservice-exceptions", params],
    queryFn: async () => {
      const { data } = await axios.get<PageResponse<MicroserviceException>>("/api/microservice-exceptions", {
        params,
      });
      return data;
    },
    placeholderData: keepPreviousData,
  });
}
