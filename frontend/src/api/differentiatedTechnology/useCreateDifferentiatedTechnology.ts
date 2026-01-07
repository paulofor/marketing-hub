import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { DifferentiatedTechnology } from "./useDifferentiatedTechnologies";

export type DifferentiatedTechnologyPayload = Omit<
  DifferentiatedTechnology,
  "id" | "createdAt" | "updatedAt"
>;

export function useCreateDifferentiatedTechnology() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: DifferentiatedTechnologyPayload) => {
      const { data } = await axios.post<DifferentiatedTechnology>(
        "/api/differentiated-technologies",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["differentiatedTechnologies"] });
    },
  });
}
