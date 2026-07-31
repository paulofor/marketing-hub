import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { PageResponse } from "../journey/types";
import type { WhatsAppMessage, WhatsAppMessageDirection } from "./types";

type DirectionFilter = WhatsAppMessageDirection | "ALL" | undefined;

interface UseWhatsAppMessagesParams {
  page?: number;
  size?: number;
  direction?: DirectionFilter;
  contactNumber?: string;
}

export function useWhatsAppMessages({
  page = 0,
  size = 25,
  direction,
  contactNumber,
}: UseWhatsAppMessagesParams = {}) {
  return useQuery<PageResponse<WhatsAppMessage>>({
    queryKey: [
      "whatsappMessages",
      page,
      size,
      direction ?? "ALL",
      contactNumber ?? "",
    ],
    queryFn: async () => {
      const params: Record<string, unknown> = {
        page,
        size,
      };
      if (direction && direction !== "ALL") {
        params.direction = direction;
      }
      if (contactNumber) {
        params.contactNumber = contactNumber;
      }
      const { data } = await axios.get<PageResponse<WhatsAppMessage>>(
        "/api/whatsapp/messages",
        {
          params,
        },
      );
      return data;
    },
    placeholderData: (previousData) => previousData,
  });
}
