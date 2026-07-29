import { useQuery } from "@tanstack/react-query";
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

export interface OperationalInventory {
  services: DiscoveredMicroservice[];
  deployments: DeploymentWorkflowInventory[];
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
