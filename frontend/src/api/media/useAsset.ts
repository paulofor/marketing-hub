import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Asset } from './useAssets';

/** Fetch single asset by id */
export function useAsset(id: number) {
  return useQuery({
    queryKey: ['asset', id],
    queryFn: async () => {
      const { data } = await axios.get<Asset>(`/api/media/${id}`);
      return data;
    },
  });
}
