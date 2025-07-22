import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Angle } from "./useAngles";

export interface UpdateAngle {
  id: number;
  name: string;
  description?: string;
}

export function useUpdateAngle() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateAngle) => {
      const { data: angle } = await axios.put<Angle>(`/api/angles/${data.id}`, data);
      return angle;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["angles"] });
    },
  });
}
