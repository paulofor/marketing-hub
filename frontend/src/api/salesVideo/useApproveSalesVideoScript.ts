import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { ApproveSalesVideoScriptPayload, SalesVideoScript } from "./types";

export function useApproveSalesVideoScript(profileId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: ApproveSalesVideoScriptPayload) => {
      if (!profileId) {
        throw new Error("Perfil inválido para aprovação");
      }
      const { data } = await axios.post<SalesVideoScript>(
        `/api/sales-videos/profiles/${profileId}/approve-script`,
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
