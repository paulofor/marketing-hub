import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { Asset, normalizeAsset } from './useAssets';

export interface CreateVideo {
  provider: string;
  avatar: string;
  voice: string;
  script: string;
  campaignId?: number;
}

export interface CreateAudio {
  provider: string;
  voice: string;
  script: string;
  campaignId?: number;
}

/** Mutation to create video or audio media */
export function useCreateMedia() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateVideo | CreateAudio) => {
      const isVideo = (data as CreateVideo).avatar !== undefined;
      const url = isVideo ? '/api/media/video' : '/api/media/audio';
      const response = await axios.post<Asset>(url, data);
      return normalizeAsset(response.data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
    },
  });
}
