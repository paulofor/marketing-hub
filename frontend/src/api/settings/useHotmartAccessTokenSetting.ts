import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface GeneralSetting {
  name: string;
  value: string | null;
  updatedAt: string | null;
}

export const HOTMART_ACCESS_TOKEN_SETTING_NAME = "hotmart_access_token_jwt";

export function useHotmartAccessTokenSetting() {
  return useQuery({
    queryKey: ["general-setting", HOTMART_ACCESS_TOKEN_SETTING_NAME],
    queryFn: async () => {
      try {
        const { data } = await axios.get<GeneralSetting>(
          `/api/settings/${HOTMART_ACCESS_TOKEN_SETTING_NAME}`,
        );
        return data;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          return {
            name: HOTMART_ACCESS_TOKEN_SETTING_NAME,
            value: null,
            updatedAt: null,
          } satisfies GeneralSetting;
        }
        throw error;
      }
    },
    staleTime: 1000 * 60,
  });
}

export function useUpdateHotmartAccessTokenSetting() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (value: string) => {
      const payload = { value };
      const { data } = await axios.put<GeneralSetting>(
        `/api/settings/${HOTMART_ACCESS_TOKEN_SETTING_NAME}`,
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(
        ["general-setting", HOTMART_ACCESS_TOKEN_SETTING_NAME],
        data,
      );
    },
  });
}
