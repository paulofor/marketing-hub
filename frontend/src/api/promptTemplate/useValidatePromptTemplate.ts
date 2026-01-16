import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { PromptTemplateValidationRequest, PromptTemplateValidationResponse } from "./types";

export function useValidatePromptTemplate() {
  return useMutation({
    mutationFn: async (payload: PromptTemplateValidationRequest) => {
      const { data } = await axios.post<PromptTemplateValidationResponse>("/api/prompts/validate", payload);
      return data;
    },
  });
}
