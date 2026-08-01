import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type StudioCharacterOption = {
  key: string;
  name: string;
  status: "Aprovado" | "Evitar" | "Reprovado";
  imageUrl: string;
  description: string;
  reason: string;
  bibleText: string;
};

export type StudioCaptionPreset = {
  key: string;
  label: string;
  style: string;
  description: string;
  planText: string;
};

export type VideoStudioCatalog = {
  characters: StudioCharacterOption[];
  captionPresets: StudioCaptionPreset[];
};

export function useVideoStudioCatalog() {
  return useQuery({
    queryKey: ["sales-video-studio-catalog"],
    queryFn: async () => {
      const { data } = await axios.get<VideoStudioCatalog>(
        "/api/sales-videos/studio/catalog",
      );
      return data;
    },
  });
}
