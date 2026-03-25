import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { LandingVideoSlot, UpdateLandingVideoSlotPayload } from "./types";

interface UpdateArgs {
  slotId: number;
  payload: UpdateLandingVideoSlotPayload;
}

export function useUpdateLandingVideoSlot(landingId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ slotId, payload }: UpdateArgs) => {
      if (!landingId) {
        throw new Error("Landing inválida para atualização");
      }
      const { data } = await axios.patch<LandingVideoSlot>(
        `/api/landing-pages/${landingId}/video-slots/${slotId}`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["landing-video-slots", landingId] });
    },
  });
}
