import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoScript } from "./types";

export function useSalesVideoScripts(profileId?: string | number) {
  return useQuery({
    queryKey: ["sales-video-scripts", profileId],
    enabled: Boolean(profileId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoScript[]>(`/api/sales-videos/profiles/${profileId}/scripts`);
      return data;
    },
  });
}
