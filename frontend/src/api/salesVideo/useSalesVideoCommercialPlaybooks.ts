import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoCommercialPlaybook } from "./types";

export function useSalesVideoCommercialPlaybooks(profileId?: string | number) {
  return useQuery({
    queryKey: ["sales-video-commercial-playbooks", profileId],
    enabled: Boolean(profileId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoCommercialPlaybook[]>(
        `/api/sales-videos/profiles/${profileId}/commercial-playbooks`,
      );
      return data;
    },
  });
}
