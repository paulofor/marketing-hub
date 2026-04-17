import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoProfile, UpdateSalesVideoCompliancePayload } from "./types";

export function useUpdateSalesVideoCompliance(profileId?: string | number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: UpdateSalesVideoCompliancePayload) => {
      if (!profileId) {
        throw new Error("Perfil inválido para atualização de compliance");
      }
      const { data } = await axios.patch<SalesVideoProfile>(
        `/api/sales-videos/profiles/${profileId}/compliance`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-profile", profileId] });
      queryClient.invalidateQueries({ queryKey: ["sales-video-profiles"] });
    },
  });
}
