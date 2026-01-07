import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { DifferentiatedTechnology } from "./useDifferentiatedTechnologies";

export function useDifferentiatedTechnology(id?: number) {
  return useQuery({
    queryKey: ["differentiatedTechnology", id],
    queryFn: async () => {
      const { data } = await axios.get<DifferentiatedTechnology>(
        `/api/differentiated-technologies/${id}`,
      );
      return data;
    },
    enabled: !!id,
  });
}
