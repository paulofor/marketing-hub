import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Microservice } from "./useMicroservices";

export function useMicroservice(id: number) {
  return useQuery({
    queryKey: ["microservices", id],
    enabled: !!id,
    queryFn: async () => {
      const { data } = await axios.get<Microservice>(`/api/microservices/${id}`);
      return data;
    },
  });
}
