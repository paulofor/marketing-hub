import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ImageDeliverablePackage } from "./types";

export function useImageDeliverablePackages() {
  return useQuery<ImageDeliverablePackage[], Error>({
    queryKey: ["image-deliverable-packages"],
    queryFn: async () => {
      const { data } = await axios.get<ImageDeliverablePackage[]>(
        "/api/image-deliverable-packages",
      );
      return data;
    },
  });
}
