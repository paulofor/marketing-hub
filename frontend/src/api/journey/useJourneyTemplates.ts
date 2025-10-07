import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  JourneyTemplateSummary,
  PageResponse,
} from "./types";

export interface JourneyTemplateFilters {
  page?: number;
  size?: number;
}

export function useJourneyTemplates(filters: JourneyTemplateFilters = {}) {
  const { page = 0, size = 100 } = filters;

  return useQuery<PageResponse<JourneyTemplateSummary>>({
    queryKey: ["journey-templates", page, size],
    queryFn: async () => {
      const { data } = await axios.get<PageResponse<JourneyTemplateSummary>>(
        "/api/journey-templates",
        {
          params: { page, size },
        },
      );
      return data;
    },
  });
}
