import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { JourneyTemplate } from "./types";

export function useJourneyTemplate(id?: number) {
  return useQuery({
    queryKey: ["journey-template", id],
    enabled: typeof id === "number" && id > 0,
    queryFn: async () => {
      const { data } = await axios.get<JourneyTemplate>(`/api/journey-templates/${id}`);
      return data;
    },
  });
}
