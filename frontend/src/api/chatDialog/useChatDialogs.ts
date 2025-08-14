import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ChatDialog {
  id: number;
  url: string;
  description: string;
  theme: string;
}

export function useChatDialogs() {
  return useQuery({
    queryKey: ["chatDialogs"],
    queryFn: async () => {
      const { data } = await axios.get<ChatDialog[]>("/api/chat-dialogs");
      return data;
    },
  });
}

