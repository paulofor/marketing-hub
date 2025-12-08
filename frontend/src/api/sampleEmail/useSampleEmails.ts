import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { SampleEmail } from "./types";

export function useSampleEmails(experimentId: string) {
  return useQuery({
    queryKey: ["sample-emails", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<SampleEmail[]>(
        `/api/experiments/${experimentId}/sample-emails`,
      );
      return data;
    },
  });
}
