import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { VideoProject, VideoProjectPayload } from "./types";

export function useVideoProjects() {
  return useQuery({
    queryKey: ["sales-video-projects"],
    queryFn: async () => {
      const { data } = await axios.get<VideoProject[]>(
        "/api/sales-videos/projects",
      );
      return data;
    },
  });
}

export function useCreateVideoProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: VideoProjectPayload) => {
      const { data } = await axios.post<VideoProject>(
        "/api/sales-videos/projects",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-projects"] });
    },
  });
}

export function useUpdateVideoProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      projectId,
      payload,
    }: {
      projectId: number;
      payload: VideoProjectPayload;
    }) => {
      const { data } = await axios.patch<VideoProject>(
        `/api/sales-videos/projects/${projectId}`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-video-projects"] });
    },
  });
}
