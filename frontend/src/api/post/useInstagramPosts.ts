import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface InstagramPost {
  id: number;
  caption: string;
  mediaUrl: string;
}

export function useInstagramPosts(accountId: string) {
  return useQuery({
    queryKey: ["instagram-posts", accountId],
    queryFn: async () => {
      const { data } = await axios.get<InstagramPost[]>(
        `/api/accounts/instagram/${accountId}/posts`,
      );
      return data;
    },
  });
}
