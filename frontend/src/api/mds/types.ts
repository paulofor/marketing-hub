export type MdsRequestStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "FAILED";

export interface MdsAdminRequestListItem {
  requestId: number;
  market: string;
  problem: string;
  desiredOutcome: string;
  status: MdsRequestStatus;
  currentStage: string;
  attempt: number;
  lastHeartbeatAt: string | null;
  updatedAt: string;
  retryEligible: boolean;
  retryReason: string;
}

export interface MdsAdminRequestListResponse {
  items: MdsAdminRequestListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MdsAdminProcessingEvent {
  eventId: number;
  stageName: string;
  eventType: "INFO" | "HEARTBEAT" | "WARNING" | "ERROR";
  message: string;
  payload: Record<string, unknown>;
  createdAt: string;
}

export interface MdsAdminRequestDetail {
  requestId: number;
  status: MdsRequestStatus;
  market: string;
  problem: string;
  desiredOutcome: string;
  deliveryConstraint: string | null;
  evidencePreference: string | null;
  correlationId: string;
  failureReason: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  context: Record<string, unknown>;
  timeline: MdsAdminProcessingEvent[];
  failureClassification: string;
  artifactsUrl: string;
  reportUrl: string;
  retryEligible: boolean;
  retryReason: string;
}

export interface MdsArtifactItem {
  artifactId: number;
  artifactType: string;
  schemaVersion: string;
  version: string;
  status: "DRAFT" | "VALIDATED" | "APPROVED";
  parentArtifactIds: number[];
  childArtifactIds: number[];
  content: Record<string, unknown>;
}

export interface MdsArtifactLineageEdge {
  id: number;
  parentArtifactId: number;
  childArtifactId: number;
  relationType: string;
}

export interface MdsAdminArtifactsResponse {
  requestId: number;
  artifacts: MdsArtifactItem[];
  lineage: MdsArtifactLineageEdge[];
}

export interface MdsReportResponse {
  requestId: number;
  artifactId: number;
  artifactType: string;
  schemaVersion: string;
  version: string;
  status: "DRAFT" | "VALIDATED" | "APPROVED";
  content: Record<string, unknown>;
}

export interface MdsRetryResponse {
  requestId: number;
  previousStatus: MdsRequestStatus;
  currentStatus: MdsRequestStatus;
  message: string;
}


export interface MdsHealthResponse {
  status: string;
  module: string;
}
