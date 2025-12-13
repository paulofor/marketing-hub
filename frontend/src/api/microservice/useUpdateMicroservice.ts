import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Microservice } from "./useMicroservices";
import { MicroservicePayload } from "./useCreateMicroservice";

export function useUpdateMicroservice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Microservice) => {
      const body: MicroservicePayload = {
        name: payload.name,
        description: payload.description,
        baseUrl: payload.baseUrl,
        category: payload.category,
        status: payload.status,
        owner: payload.owner,
        documentationUrl: payload.documentationUrl,
        healthCheckPath: payload.healthCheckPath,
      };
      const { data } = await axios.put<Microservice>(
        `/api/microservices/${payload.id}`,
        body,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["microservices"] });
      if (data?.id) {
        queryClient.invalidateQueries({ queryKey: ["microservices", data.id] });
      }
    },
  });
}
