import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  Journey,
  JourneyStatus,
  PageResponse,
} from "./types";

export interface JourneyFilters {
  page?: number;
  size?: number;
  templateId?: number;
  status?: JourneyStatus;
}

export function useJourneys(filters: JourneyFilters = {}) {
  const { page = 0, size = 12, templateId, status } = filters;

  return useQuery<PageResponse<Journey>>({
    queryKey: ["journeys", page, size, templateId ?? null, status ?? null],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size };
      if (templateId) {
        params.templateId = templateId;
      }
      if (status) {
        params.status = status;
      }
      const { data } = await axios.get<PageResponse<Journey>>("/api/journeys", {
        params,
      });
      return data;
    },
  });
}
