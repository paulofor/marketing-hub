import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { PageResponse } from "../journey/types";
import type { WhatsAppConversation } from "./types";

interface UseWhatsAppConversationsParams {
  page?: number;
  size?: number;
}

export function useWhatsAppConversations({
  page = 0,
  size = 25,
}: UseWhatsAppConversationsParams = {}) {
  return useQuery<PageResponse<WhatsAppConversation>>({
    queryKey: ["whatsappConversations", page, size],
    queryFn: async () => {
      const { data } = await axios.get<PageResponse<WhatsAppConversation>>(
        "/api/whatsapp/conversations",
        {
          params: {
            page,
            size,
          },
        },
      );
      return data;
    },
    placeholderData: (previousData) => previousData,
  });
}
