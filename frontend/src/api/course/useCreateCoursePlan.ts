import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { CoursePlan } from "./useCoursePlans";

export interface CreateCoursePlan {
  targetAudience: string;
  transformation: string;
  macroTopics: string;
}

/** Mutation to create a course plan */
export function useCreateCoursePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateCoursePlan) => {
      const { data: plan } = await axios.post<CoursePlan>("/api/courses", data);
      return plan;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["coursePlans"] });
    },
  });
}
