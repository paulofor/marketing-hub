import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { WhatsAppAccount } from "./types";

export function useWhatsAppAccounts() {
  return useQuery({
    queryKey: ["whatsappAccounts"],
    queryFn: async () => {
      const { data } = await axios.get<WhatsAppAccount[]>(
        "/api/whatsapp/accounts",
      );
      return data;
    },
  });
}
