import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface CoursePlan {
  id: number;
  targetAudience: string;
  transformation: string;
  macroTopics: string;
  modules: string;
  objectives: string;
  resources: string;
}

/** Query to list course plans */
export function useCoursePlans() {
  return useQuery({
    queryKey: ["coursePlans"],
    queryFn: async () => {
      const { data } = await axios.get<CoursePlan[]>("/api/courses");
      return data;
    },
  });
}
