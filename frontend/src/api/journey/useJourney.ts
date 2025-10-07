import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { Journey } from "./types";

export function useJourney(id?: number) {
  return useQuery({
    queryKey: ["journeys", id],
    enabled: typeof id === "number" && id > 0,
    queryFn: async () => {
      const { data } = await axios.get<Journey>(`/api/journeys/${id}`);
      return data;
    },
  });
}
