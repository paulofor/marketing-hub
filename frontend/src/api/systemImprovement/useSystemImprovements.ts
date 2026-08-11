import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { SystemImprovement, SystemImprovementPayload } from "./types";

/** Consulta o backlog central de melhorias sugeridas pelos agentes. */
export function useSystemImprovements() {
  return useQuery({
    queryKey: ["system-improvements"],
    queryFn: async () =>
      (await axios.get<SystemImprovement[]>("/api/system-improvements")).data,
  });
}

/** Registra uma melhoria e atualiza imediatamente o backlog da tela. */
export function useCreateSystemImprovement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: SystemImprovementPayload) =>
      (await axios.post<SystemImprovement>("/api/system-improvements", payload))
        .data,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["system-improvements"] }),
  });
}
