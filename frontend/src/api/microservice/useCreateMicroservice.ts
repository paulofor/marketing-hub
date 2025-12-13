import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Microservice } from "./useMicroservices";

export type MicroservicePayload = Omit<Microservice, "id" | "createdAt" | "updatedAt">;

export function useCreateMicroservice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: MicroservicePayload) => {
      const { data } = await axios.post<Microservice>('/api/microservices', payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["microservices"] });
    },
  });
}
