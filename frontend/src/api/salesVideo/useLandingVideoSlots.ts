import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { LandingVideoSlot } from "./types";

export function useLandingVideoSlots(landingId?: string | number) {
  return useQuery({
    queryKey: ["landing-video-slots", landingId],
    enabled: Boolean(landingId),
    queryFn: async () => {
      const { data } = await axios.get<LandingVideoSlot[]>(
        `/api/landing-pages/${landingId}/video-slots`,
      );
      return data;
    },
  });
}
