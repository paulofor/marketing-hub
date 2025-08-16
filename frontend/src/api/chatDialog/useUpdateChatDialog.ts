import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { ChatDialog } from "./useChatDialogs";

export interface UpdateChatDialog {
  id: number;
  description: string;
}

export function useUpdateChatDialog() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...data }: UpdateChatDialog) => {
      const { data: dialog } = await axios.put<ChatDialog>(
        `/api/chat-dialogs/${id}`,
        data,
      );
      return dialog;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chatDialogs"] });
    },
  });
}
