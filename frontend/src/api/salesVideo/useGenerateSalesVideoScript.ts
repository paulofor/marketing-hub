import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { GenerateSalesVideoScriptPayload, SalesVideoJob } from "./types";

export function useGenerateSalesVideoScript(profileId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: GenerateSalesVideoScriptPayload) => {
      if (!profileId) {
        throw new Error("Perfil inválido para geração de script");
      }
      const { data } = await axios.post<SalesVideoJob>(
        `/api/sales-videos/profiles/${profileId}/generate-script`,
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
