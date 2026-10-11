import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { DeliveryExample } from "./types";

export function useDeliveryExamples(projectId?: number) {
  return useQuery({
    queryKey: ["video-delivery-examples", projectId],
    enabled: Boolean(projectId),
    queryFn: async () => {
      const { data } = await axios.get<DeliveryExample[]>(
        `/api/sales-videos/projects/${projectId}/delivery-examples`,
      );
      return data;
    },
  });
}
