import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Microservice {
  id: number;
  name: string;
  description?: string;
  baseUrl?: string;
  category?: string;
  status?: string;
  owner?: string;
  documentationUrl?: string;
  healthCheckPath?: string;
  createdAt?: string;
  updatedAt?: string;
  lastExceptionAt?: string | null;
  lastExceptionMessage?: string | null;
  lastExceptionSeverity?: string | null;
  exceptionCount?: number;
}

export function useMicroservices() {
  return useQuery({
    queryKey: ["microservices"],
    queryFn: async () => {
      const { data } = await axios.get<Microservice[]>("/api/microservices");
      return data;
    },
  });
}
