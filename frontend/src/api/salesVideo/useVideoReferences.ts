import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  VideoReference,
  VideoReferencePayload,
  VideoReferenceUploadPayload,
} from "./types";

export function useVideoReferences() {
  return useQuery({
    queryKey: ["sales-video-references"],
    queryFn: async () => {
      const { data } = await axios.get<VideoReference[]>(
        "/api/sales-videos/reference-videos",
      );
      return data;
    },
  });
}

export function useCreateVideoReference() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (
      payload: VideoReferencePayload | VideoReferenceUploadPayload,
    ) => {
      if ("file" in payload) {
        const formData = new FormData();
        formData.append("file", payload.file);
        formData.append("title", payload.title);
        formData.append("primaryLearningGoal", payload.primaryLearningGoal);
        if (payload.sourcePlatform) {
          formData.append("sourcePlatform", payload.sourcePlatform);
        }
        if (payload.niche) {
          formData.append("niche", payload.niche);
        }
        if (payload.funnelStage) {
          formData.append("funnelStage", payload.funnelStage);
        }
        if (payload.successEvidence) {
          formData.append("successEvidence", payload.successEvidence);
        }
        if (payload.createdBy) {
          formData.append("createdBy", payload.createdBy);
        }

        const { data } = await axios.post<VideoReference>(
          "/api/sales-videos/reference-videos",
          formData,
        );
        return data;
      }

      const { data } = await axios.post<VideoReference>(
        "/api/sales-videos/reference-videos",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-references"] });
    },
  });
}
