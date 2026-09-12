import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { z } from "zod";

const api = "/api/business-process-chains/learning-cycles/v1/products";
const authorizationSchema = z.object({
  eventId: z.number().int().positive(),
  reference: z.string().startsWith("internal://learning-cycles/"),
  budgetLimitUsd: z.number().positive(),
  operatorName: z.string(),
  justification: z.string(),
  createdAt: z.string(),
  productVersion: z.string(),
  current: z.boolean(),
});
export type VideoBudgetAuthorization = z.infer<typeof authorizationSchema>;
const budgetSchema = z.object({
  productId: z.number().int().positive(),
  productName: z.string().nullable(),
  internalName: z.string().nullable(),
  chainDefinitionId: z.number().int().positive(),
  processDefinitionId: z.number().int().positive(),
  cycleId: z.number().int().positive(),
  experimentId: z.number().int().positive(),
  productVersion: z.string(),
  revision: z.number().int().nonnegative(),
  stageLabel: z.string(),
  status: z.enum(["AWAITING_LIMIT", "LIMIT_RECORDED"]),
  instruction: z.string(),
  scope: z.literal("TWO_VIDEOS_PRODUCTION_AND_REVIEW"),
  currency: z.literal("USD"),
  canAuthorize: z.boolean(),
  blocker: z.string().nullable(),
  currentAuthorization: authorizationSchema.nullable(),
  history: z.array(authorizationSchema),
  cycleUrl: z.string().startsWith("/business-process-chains/learning-cycles?"),
  financeUrl: z.string().startsWith("/financial/videos?"),
});
export type VideoBudget = z.infer<typeof budgetSchema>;
export type AuthorizeVideoBudget = {
  requestKey: string;
  expectedRevision: number;
  chainDefinitionId: number;
  experimentId: number;
  productVersion: string;
  budgetLimitUsd: string;
  operatorName: string;
  justification: string;
  confirmed: boolean;
};

/** Consulta o financeiro da ocorrência selecionada sem escolher outro ciclo. */
export function useVideoBudget(
  productId?: number,
  cycleId?: number,
  chainId?: number,
) {
  return useQuery({
    queryKey: ["video-budget", productId, cycleId, chainId],
    enabled: Boolean(productId && cycleId && chainId),
    retry: false,
    refetchOnWindowFocus: false,
    queryFn: async () => {
      const { data } = await axios.get(
        `${api}/${productId}/${cycleId}/video-budget`,
        { params: { chainId } },
      );
      const budget = budgetSchema.parse(data);
      if (
        budget.productId !== productId ||
        budget.cycleId !== cycleId ||
        budget.chainDefinitionId !== chainId
      )
        throw new Error("Contexto financeiro divergente.");
      return budget;
    },
  });
}

/** Registra apenas o teto humano e atualiza a leitura oficial do mesmo ciclo. */
export function useAuthorizeVideoBudget(
  productId: number,
  cycleId: number,
  chainId: number,
) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (body: AuthorizeVideoBudget) => {
      const { data } = await axios.post(
        `${api}/${productId}/${cycleId}/video-budget`,
        body,
      );
      return budgetSchema.parse(data);
    },
    onSuccess: async (data) => {
      client.setQueryData(["video-budget", productId, cycleId, chainId], data);
      await client.invalidateQueries({
        queryKey: ["learning-cycles", productId],
      });
    },
  });
}
