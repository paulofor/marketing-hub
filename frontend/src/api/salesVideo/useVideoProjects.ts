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

export function useVideoProject(projectId?: number) {
  return useQuery({
    queryKey: ["sales-video-project", projectId],
    queryFn: async () => {
      const { data } = await axios.get<VideoProject>(
        `/api/sales-videos/projects/${projectId}`,
      );
      return data;
    },
    enabled: Boolean(projectId),
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
      queryClient.invalidateQueries({ queryKey: ["sales-video-project"] });
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
