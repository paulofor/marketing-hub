import { useMutation } from "@tanstack/react-query";
import axios from "axios";

interface CreateOprmJobRequest {
  jobType: "OCCUPATION_MAPPING";
  occupationSeedRef: string;
  correlationId?: string;
  inputRefs?: string[];
}

export function useCreateOprmJob() {
  return useMutation({
    mutationFn: async (request: CreateOprmJobRequest) => {
      const { data } = await axios.post("/api/oprm/jobs", request);
      return data;
    },
  });
}
