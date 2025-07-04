import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

export interface Asset {
  id: number;
  type: string;
  provider: string;
  status: string;
  url: string;
  payload: string;
  campaignId: number | null;
}

/** Fetches list of media assets */
export function useAssets(status?: string) {
  return useQuery({
    queryKey: ['assets', status],
    queryFn: async () => {
      const { data } = await axios.get<Asset[]>('/api/media', {
        params: { status },
      });
      return data;
    },
  });
}
