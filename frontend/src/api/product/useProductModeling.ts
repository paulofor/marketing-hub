import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export interface ProductModeling {
  id: number;
  productId: number | null;
  productName: string;
  desiredOutcome: string;
  referenceUrls: string[];
  bigIdeaSnapshot: { centralIdea: string } | null;
  discoveryCycleId: number;
  status: string;
  guidance: string;
  researchUrl: string;
  productUrl: string | null;
  createdAt: string;
  learningCycleId: number | null;
  chainUrl: string | null;
  researchScope: string;
  targetMarket: string;
  internationalAssessment: {
    references: Array<{
      referenceUrl: string;
      sourceUrl: string;
      observedEvidence: string;
      originMarket: string;
      signalType: string;
      evidenceLevel: string;
      limitation: string;
    }>;
    brazilAdaptation: Record<string, string>;
  } | null;
  availableCycles: Array<{
    cycleId: number;
    experimentId: number;
    productVersion: string;
    url: string;
  }>;
  stages: Array<{
    taskId: number;
    agent: string;
    activity: string;
    status: string;
    resultSummary: string | null;
    activityUrl: string | null;
  }>;
}
export interface PrepareModeling {
  requestKey: string;
  productId: number | null;
  newProductName: string | null;
  desiredOutcome: string;
  referenceUrls: string[];
}
export function useProductModelings() {
  return useQuery({
    queryKey: ["product-modeling"],
    queryFn: async () =>
      (await axios.get<ProductModeling[]>("/api/product-modeling/v1")).data,
    refetchInterval: 15000,
  });
}
export function useModelingAction() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (
      value:
        | PrepareModeling
        | { start: number }
        | { adopt: number; cycleId: number },
    ) =>
      ("start" in value
        ? await axios.post<ProductModeling>(
            `/api/product-modeling/v1/${value.start}/start`,
          )
        : "adopt" in value
          ? await axios.post<ProductModeling>(
              `/api/product-modeling/v1/${value.adopt}/adopt`,
              { cycleId: value.cycleId },
            )
          : await axios.post<ProductModeling>("/api/product-modeling/v1", value)
      ).data,
    onSuccess: () =>
      client.invalidateQueries({ queryKey: ["product-modeling"] }),
  });
}
