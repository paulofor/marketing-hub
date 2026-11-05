import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

const schema = z.object({
  productId: z.number(),
  chainDefinitionId: z.number(),
  cycleId: z.number(),
  experimentId: z.number(),
  productVersion: z.string(),
  revision: z.number(),
  scope: z.literal("SINGLE_VIDEO_INCREMENTAL_ALL_IN_V1"),
  currency: z.literal("USD"),
  canAuthorize: z.boolean(),
  blocker: z.string().nullable(),
  startsProduction: z.literal(false),
  guidance: z.string(),
  authorizations: z.array(
    z.object({
      eventId: z.number(),
      reference: z.string(),
      videoProjectId: z.number(),
      profileId: z.number(),
      totalLimitUsd: z.number(),
      operatorName: z.string(),
      authorizationSource: z.string(),
      authorizedAt: z.string(),
      current: z.boolean(),
    }),
  ),
});
export type SingleVideoBudget = z.infer<typeof schema>;
export type SingleVideoAuthorization = {
  requestKey: string;
  expectedRevision: number;
  chainDefinitionId: number;
  experimentId: number;
  productVersion: string;
  videoProjectId: number;
  profileId: number;
  totalLimitUsd: string;
  operatorName: string;
  authorizationSource: string;
  confirmed: boolean;
};

/** Lê e registra a decisão integral no ciclo, sem disparar produção ou interpretar notas livres. */
export function useSingleVideoBudget(
  productId: number,
  cycleId: number,
  chainId: number,
) {
  const client = useQueryClient();
  const key = ["single-video-budget", productId, cycleId, chainId];
  const url = `/api/business-process-chains/learning-cycles/v1/products/${productId}/${cycleId}/single-video-budget`;
  const parse = (input: unknown) => {
    const result = schema.parse(input);
    if (
      result.productId !== productId ||
      result.cycleId !== cycleId ||
      result.chainDefinitionId !== chainId
    )
      throw new Error("Contexto financeiro divergente.");
    return result;
  };
  const query = useQuery({
    queryKey: key,
    retry: false,
    enabled: [productId, cycleId, chainId].every(
      (n) => Number.isSafeInteger(n) && n > 0,
    ),
    queryFn: async () =>
      parse((await axios.get(url, { params: { chainId } })).data),
  });
  const authorize = useMutation({
    mutationFn: async (request: SingleVideoAuthorization) =>
      parse((await axios.post(url, request)).data),
    onSuccess: (data) => {
      client.setQueryData(key, data);
      void client.invalidateQueries({ queryKey: ["learning-cycles"] });
      void client.invalidateQueries({ queryKey: ["execution-finance"] });
    },
  });
  return { query, authorize };
}
