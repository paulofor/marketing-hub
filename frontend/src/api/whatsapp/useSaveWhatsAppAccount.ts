import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { SaveWhatsAppAccountInput, WhatsAppAccount } from "./types";

export function useSaveWhatsAppAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: SaveWhatsAppAccountInput) => {
      if (input.id) {
        const { data } = await axios.put<WhatsAppAccount>(
          `/api/whatsapp/accounts/${input.id}`,
          input,
        );
        return data;
      }
      const { data } = await axios.post<WhatsAppAccount>(
        "/api/whatsapp/accounts",
        input,
      );
      return data;
    },
    onSuccess: (account) => {
      queryClient.invalidateQueries({ queryKey: ["whatsappAccounts"] });
      toast.success(`Conta "${account.displayName}" salva.`);
    },
    onError: () => {
      toast.error("Não foi possível salvar a conta do WhatsApp.");
    },
  });
}
