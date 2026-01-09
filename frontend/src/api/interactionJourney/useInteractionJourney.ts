import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { InteractionJourney } from "./types";

export function useInteractionJourney(id?: string) {
  return useQuery<InteractionJourney>({
    queryKey: ["interaction-journey", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<InteractionJourney>(
        `/api/interaction-journeys/${id}`,
      );
      return data;
    },
  });
}
