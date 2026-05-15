import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface GeneralSetting {
  name: string;
  value: string | null;
  updatedAt: string | null;
}

export const CLICKBANK_ACCESS_TOKEN_SETTING_NAME = "clickbank_access_token_jwt";

export function useClickbankAccessTokenSetting() {
  return useQuery({
    queryKey: ["general-setting", CLICKBANK_ACCESS_TOKEN_SETTING_NAME],
    queryFn: async () => {
      try {
        const { data } = await axios.get<GeneralSetting>(
          `/api/settings/${CLICKBANK_ACCESS_TOKEN_SETTING_NAME}`,
        );
        return data;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          return {
            name: CLICKBANK_ACCESS_TOKEN_SETTING_NAME,
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

export function useUpdateClickbankAccessTokenSetting() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (value: string) => {
      const payload = { value };
      const { data } = await axios.put<GeneralSetting>(
        `/api/settings/${CLICKBANK_ACCESS_TOKEN_SETTING_NAME}`,
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(
        ["general-setting", CLICKBANK_ACCESS_TOKEN_SETTING_NAME],
        data,
      );
    },
  });
}
