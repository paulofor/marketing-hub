import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { DiscoveredMicroservice } from "./useDiscoveredMicroservices";

export interface DeploymentWorkflowInventory {
  workflowFile: string;
  workflowName: string;
  jobName: string;
  deployHost?: string | null;
  deployUser?: string | null;
  remotePath?: string | null;
  secretReferences: string[];
  triggerMode: "manual" | "automatico" | string;
}

export interface VpsHostInventory {
  host: string;
  providerName?: string | null;
  providerEvidence?: string | null;
  cpu?: string | null;
  memoryGb?: number | null;
  diskGb?: number | null;
  operatingSystem?: string | null;
  monthlyCostBrl?: number | null;
  billingCycle?: string | null;
  costEvidence?: string | null;
  physicalSpecsEvidence?: string | null;
  notes?: string | null;
}

export interface OperationalInventory {
  services: DiscoveredMicroservice[];
  deployments: DeploymentWorkflowInventory[];
  hosts: VpsHostInventory[];
}

export function useOperationalInventory() {
  return useQuery({
    queryKey: ["microservices", "operational-inventory"],
    queryFn: async () => {
      const { data } = await axios.get<OperationalInventory>(
        "/api/microservices/operational-inventory",
      );
      return data;
    },
  });
}

export type VpsHostInventoryPayload = Omit<VpsHostInventory, "host">;

export function useVpsHostInventory(host: string) {
  return useQuery({
    queryKey: ["microservices", "operational-inventory", "hosts", host],
    enabled: Boolean(host),
    queryFn: async () => {
      const { data } = await axios.get<VpsHostInventory>(
        `/api/microservices/operational-inventory/hosts/${encodeURIComponent(host)}`,
      );
      return data;
    },
  });
}

export function useUpdateVpsHostInventory(host: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: VpsHostInventoryPayload) => {
      const { data } = await axios.put<VpsHostInventory>(
        `/api/microservices/operational-inventory/hosts/${encodeURIComponent(host)}`,
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({
        queryKey: ["microservices", "operational-inventory"],
      });
      queryClient.invalidateQueries({
        queryKey: [
          "microservices",
          "operational-inventory",
          "hosts",
          data.host,
        ],
      });
    },
  });
}
