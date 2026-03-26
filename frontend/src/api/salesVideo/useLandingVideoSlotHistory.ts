import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { LandingVideoSlotHistory } from "./types";

interface Params {
  landingId?: number | string;
  slotId?: number;
}

export function useLandingVideoSlotHistory({ landingId, slotId }: Params) {
  return useQuery({
    queryKey: ["landing-video-slot-history", landingId, slotId],
    enabled: Boolean(landingId) && Boolean(slotId),
    queryFn: async () => {
      const { data } = await axios.get<LandingVideoSlotHistory[]>(
        `/api/landing-pages/${landingId}/video-slots/${slotId}/history`,
      );
      return data;
    },
  });
}
