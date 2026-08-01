import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { VideoReference, VideoReferencePayload } from "./types";

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
    mutationFn: async (payload: VideoReferencePayload) => {
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
