import { useMutation } from "@tanstack/react-query";
import axios from "axios";

export function useCreateLanding(expId: string) {
  return useMutation({
    mutationFn: async (body: { type: string; html: string }) => {
      const { data } = await axios.post(`/api/experiments/${expId}/landing`, body);
      return data;
    },
  });
}
