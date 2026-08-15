export type ProcessNodeType = "START" | "TASK" | "GATEWAY" | "END";

export type ProcessNode = {
  id: string;
  type: ProcessNodeType;
  label: string;
  owner?: string;
  description?: string;
};

export type ProcessFlow = {
  from: string;
  to: string;
  label?: string;
};

export type ProcessDiagram = { nodes: ProcessNode[]; flows: ProcessFlow[] };

export type BusinessProcess = {
  id: number;
  processCode: string;
  name: string;
  purpose: string;
  ownerName: string;
  triggerDescription: string;
  outcomeDescription: string;
  versionNumber: number;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
  technicalReference?: string;
  diagram: ProcessDiagram;
  createdAt: string;
  publishedAt?: string;
};

export type CreateBusinessProcess = Omit<
  BusinessProcess,
  "id" | "status" | "createdAt" | "publishedAt"
>;
