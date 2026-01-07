import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { DifferentiatedTechnology } from "./useDifferentiatedTechnologies";

export type DifferentiatedTechnologyUpdatePayload = Omit<
  DifferentiatedTechnology,
  "id" | "createdAt" | "updatedAt"
>;

export function useUpdateDifferentiatedTechnology(id?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: DifferentiatedTechnologyUpdatePayload) => {
      const { data } = await axios.put<DifferentiatedTechnology>(
        `/api/differentiated-technologies/${id}`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["differentiatedTechnologies"] });
      queryClient.invalidateQueries({ queryKey: ["differentiatedTechnology", id] });
    },
  });
}
