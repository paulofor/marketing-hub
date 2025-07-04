import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { Asset } from './useAssets';

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
      return axios.post<Asset>(url, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
    },
  });
}
