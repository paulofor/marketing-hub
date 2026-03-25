import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { CreateLandingVideoSlotPayload, LandingVideoSlot } from "./types";

export function useCreateLandingVideoSlot(landingId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateLandingVideoSlotPayload) => {
      if (!landingId) {
        throw new Error("Landing inválida para criação de slot");
      }
      const { data } = await axios.post<LandingVideoSlot>(
        `/api/landing-pages/${landingId}/video-slots`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["landing-video-slots", landingId] });
    },
  });
}
