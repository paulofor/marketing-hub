import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export function usePreviewCreative(id: number, enabled: boolean) {
  return useQuery({
    queryKey: ["creative-preview", id],
    enabled,
    queryFn: async () => {
      const { data } = await axios.get<string>(`/api/creatives/${id}/preview`);
      return data;
    },
  });
}
