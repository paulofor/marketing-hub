import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { ChatDialog } from "./useChatDialogs";

export interface CreateChatDialog {
  url: string;
  description: string;
  theme: string;
}

export function useCreateChatDialog() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateChatDialog) => {
      const { data: dialog } = await axios.post<ChatDialog>(
        "/api/chat-dialogs",
        data
      );
      return dialog;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chatDialogs"] });
    },
  });
}

