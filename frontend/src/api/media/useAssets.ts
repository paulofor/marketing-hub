import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

import { resolveAssetUrl } from '../../utils/resolveAssetUrl';

export interface Asset {
  id: number;
  type: string;
  provider: string;
  status: string;
  url?: string | null;
  payload?: string | null;
  campaignId?: number | null;
  publicUrl?: string | null;
}

export function normalizeAsset(asset: Asset): Asset {
  const resolvedUrl = asset.url ? resolveAssetUrl(asset.url) : '';
  return {
    ...asset,
    publicUrl: resolvedUrl || undefined,
  };
}

/** Fetches list of media assets */
export function useAssets(status?: string) {
  return useQuery({
    queryKey: ['assets', status],
    queryFn: async () => {
      const { data } = await axios.get<Asset[]>('/api/media', {
        params: { status },
      });
      return data.map(normalizeAsset);
    },
  });
}
