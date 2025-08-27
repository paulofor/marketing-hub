import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ChatDialog } from "./useChatDialogs";

export function useChatDialog(id?: number) {
  return useQuery({
    queryKey: ["chatDialog", id],
    queryFn: async () => {
      const { data } = await axios.get<ChatDialog>(`/api/chat-dialogs/${id}`);
      return data;
    },
    enabled: !!id,
  });
}
