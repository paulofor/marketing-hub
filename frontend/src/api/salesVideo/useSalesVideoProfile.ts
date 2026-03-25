import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoProfile } from "./types";

export function useSalesVideoProfile(profileId?: string | number) {
  return useQuery({
    queryKey: ["sales-video-profile", profileId],
    enabled: Boolean(profileId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoProfile>(`/api/sales-videos/profiles/${profileId}`);
      return data;
    },
  });
}
