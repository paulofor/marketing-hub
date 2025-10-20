import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface GeneralSetting {
  name: string;
  value: string | null;
  updatedAt: string | null;
}

export const PRIVACY_POLICY_SETTING_NAME = "privacy_policy_url";

export function usePrivacyPolicySetting() {
  return useQuery({
    queryKey: ["general-setting", PRIVACY_POLICY_SETTING_NAME],
    queryFn: async () => {
      try {
        const { data } = await axios.get<GeneralSetting>(
          `/api/settings/${PRIVACY_POLICY_SETTING_NAME}`,
        );
        return data;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          return {
            name: PRIVACY_POLICY_SETTING_NAME,
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

export function useUpdatePrivacyPolicySetting() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (value: string | null) => {
      const payload = { value };
      const { data } = await axios.put<GeneralSetting>(
        `/api/settings/${PRIVACY_POLICY_SETTING_NAME}`,
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(
        ["general-setting", PRIVACY_POLICY_SETTING_NAME],
        data,
      );
    },
  });
}
