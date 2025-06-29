import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

export interface FacebookAccount {
  id: string;
  name: string;
  currency: string;
}

export function useFacebookAccounts() {
  return useQuery({
    queryKey: ['facebook-accounts'],
    queryFn: async () => {
      const { data } = await axios.get<FacebookAccount[]>('/api/accounts/facebook');
      return data;
    },
    staleTime: 1000 * 60 * 5,
  });
}
