import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FacebookConfigurationStatus {
  hasConfiguredPages: boolean;
}

export function useFacebookConfigurationStatus() {
  return useQuery({
    queryKey: ["facebook-configuration-status"],
    queryFn: async () => {
      const { data } = await axios.get<FacebookConfigurationStatus>(
        "/api/facebook/configuration-status",
      );
      return data;
    },
    staleTime: 1000 * 30,
  });
}
