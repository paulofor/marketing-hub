import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { CreateSalesVideoCommercialPlaybookPayload, SalesVideoCommercialPlaybook } from "./types";

export function useCreateSalesVideoCommercialPlaybook(profileId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateSalesVideoCommercialPlaybookPayload) => {
      if (!profileId) throw new Error("Perfil inválido para criar playbook comercial");
      const { data } = await axios.post<SalesVideoCommercialPlaybook>(
        `/api/sales-videos/profiles/${profileId}/commercial-playbooks`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-commercial-playbooks", profileId] });
    },
  });
}
