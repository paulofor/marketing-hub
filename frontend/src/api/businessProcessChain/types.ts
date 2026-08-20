export type BusinessProcessChainSummary = {
  id: number;
  chainCode: string;
  name: string;
  purpose: string;
  outcomeDescription: string;
  primaryMetric: string;
  versionNumber: number;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
  processCount: number;
  publishedAt?: string;
};

export type BusinessProcessChainProcess = {
  sequenceNumber: number;
  valueContribution: string;
  processDefinitionId: number;
  processCode: string;
  name: string;
  purpose: string;
  ownerName: string;
  triggerDescription: string;
  outcomeDescription: string;
  versionNumber: number;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
};

export type BusinessProcessChainDetail = BusinessProcessChainSummary & {
  createdAt: string;
  processes: BusinessProcessChainProcess[];
};
