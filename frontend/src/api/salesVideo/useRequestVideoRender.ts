import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { RequestVideoRenderPayload, SalesVideoJob } from "./types";

export function useRequestVideoRender(profileId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RequestVideoRenderPayload) => {
      if (!profileId) {
        throw new Error("Perfil inválido para render");
      }
      const { data } = await axios.post<SalesVideoJob>(
        `/api/sales-videos/profiles/${profileId}/request-render`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-profile", profileId] });
      queryClient.invalidateQueries({ queryKey: ["sales-video-profiles"] });
      queryClient.invalidateQueries({ queryKey: ["sales-video-jobs", profileId] });
    },
  });
}
