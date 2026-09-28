import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export interface UpdateExperimentPublishingIdentity {
  facebookPageId: number | null;
  instagramAccountId: number;
}

export function useUpdateExperimentPublishingIdentity(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (identity: UpdateExperimentPublishingIdentity) => {
      const { data } = await axios.patch<Experiment>(
        `/api/experiments/${id}/publishing-identity`,
        identity,
      );
      return data;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["experiment", id] }),
        queryClient.invalidateQueries({ queryKey: ["experiments"] }),
        queryClient.invalidateQueries({
          queryKey: ["approved-visual-assets", id],
        }),
      ]);
    },
  });
}
