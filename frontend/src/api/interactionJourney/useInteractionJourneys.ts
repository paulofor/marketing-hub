import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { InteractionJourney } from "./types";

export function useInteractionJourneys() {
  return useQuery<InteractionJourney[]>({
    queryKey: ["interaction-journeys"],
    queryFn: async () => {
      const { data } = await axios.get<InteractionJourney[]>(
        "/api/interaction-journeys",
      );
      return data;
    },
  });
}
