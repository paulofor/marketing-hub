import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Angle } from "./useAngles";

export interface CreateAngle {
  name: string;
  description?: string;
  frameType?: string;
}

export function useCreateAngle() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateAngle) => {
      const { data: angle } = await axios.post<Angle>("/api/angles", data);
      return angle;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["angles"] });
    },
  });
}
