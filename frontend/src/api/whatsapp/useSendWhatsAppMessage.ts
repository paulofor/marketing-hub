import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios, { AxiosError } from "axios";
import { toast } from "react-toastify";
import type { SendWhatsAppMessageInput, WhatsAppMessage } from "./types";

export function useSendWhatsAppMessage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: SendWhatsAppMessageInput) => {
      const { data } = await axios.post<WhatsAppMessage>(
        "/api/whatsapp/messages/send",
        input,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["whatsappMessages"] });
      toast.success("Mensagem enviada com sucesso.");
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      const responseMessage = error.response?.data?.message;
      toast.error(responseMessage ?? "Não foi possível enviar a mensagem.");
    },
  });
}
