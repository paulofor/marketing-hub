import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface DiscoveredMicroservice {
  serviceName: string;
  image?: string;
  hostPort?: number;
  containerPort?: number;
  baseUrl: string;
  healthCheckPath: string;
}

export function useDiscoveredMicroservices() {
  return useQuery({
    queryKey: ["microservices", "discover"],
    queryFn: async () => {
      const { data } = await axios.get<DiscoveredMicroservice[]>(
        "/api/microservices/discover",
      );
      return data;
    },
  });
}
