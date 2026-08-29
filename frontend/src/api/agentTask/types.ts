export type AgentTaskStatus =
  "PENDING" | "IN_PROGRESS" | "COMPLETED" | "BLOCKED" | "CANCELLED";

export interface AgentTaskAuditLink {
  label: string;
  url: string;
  accessMethod?: string;
  accessedAt?: string;
}

export interface AgentTaskVisualEvidence {
  id: number;
  captureSessionId: string;
  evidenceKey: string;
  evidenceType: "FULL_PAGE" | "FOLD";
  label: string;
  deviceProfile: "IPHONE_15_PRO" | "PIXEL_7" | "DESKTOP_1440";
  pageNumber: number;
  foldNumber?: number;
  viewportWidth: number;
  viewportHeight: number;
  pageHeightPx: number;
  scrollY: number;
  sourceUrl: string;
  finalUrl: string;
  contentUrl: string;
  sizeBytes: number;
  sha256: string;
  capturedAt: string;
}

export interface PsiqueVisualFoldAnalysis {
  artifactId: number;
  deviceProfile: AgentTaskVisualEvidence["deviceProfile"];
  pageNumber: number;
  foldNumber: number;
  aestheticAssessment: string;
  visualHierarchy: string;
  legibility: string;
  emotionEvoked: string;
  ctaVisibility: string;
}

export interface PsiqueVisualAudit {
  captureSessionId: string;
  mobileFirst: true;
  fullPageEvidenceIds: number[];
  fullPageContinuity: string;
  overallAestheticAssessment: string;
  foldAnalyses: PsiqueVisualFoldAnalysis[];
}

export interface PsiquePurchaseEmotion {
  acquisitionExpectation: string;
  acquisitionAnxiety: string;
  expectedPostDeliveryFeeling: string;
  emotionalTension: string;
  evidenceBoundary: string;
}

export interface AgentTaskBlockerGuidance {
  category:
    | "FUNCTIONAL_ADJUSTMENT"
    | "MISSING_EVIDENCE"
    | "COMMERCIAL_RISK"
    | "AUTHORIZATION_REQUIRED"
    | "TECHNICAL_FAILURE";
  recommendedAction: string;
  helpLinks: AgentTaskAuditLink[];
}

export interface AgentTaskFailureAudit {
  readiness: "COMPLETE" | "PARTIAL";
  intendedWork: string;
  sourceReference?: string;
  processCode?: string;
  activityId?: string;
  activityName?: string;
  authorityPolicy?: string;
  accessedEvidenceJson?: string;
  producedOutputJson?: string;
  error?: string;
  blockerGuidance?: AgentTaskBlockerGuidance;
  accessedUrls?: AgentTaskAuditLink[];
  missingEvidence: string[];
}

export interface AgentTask {
  id: number;
  assignedAgentId: number;
  assignedAgentKey: string;
  assignedAgentNickname: string;
  requestedByType: "HUMAN" | "AGENT";
  requestedByAgentId?: number;
  requestedByAgentKey?: string;
  requestedByName: string;
  title: string;
  description: string;
  priority: "LOW" | "NORMAL" | "HIGH" | "URGENT";
  status: AgentTaskStatus;
  sourceReference?: string;
  processDefinitionId?: number;
  processCode?: string;
  processVersionNumber?: number;
  processActivityId?: string;
  processActivityName?: string;
  exceptional: boolean;
  exceptionReason?: string;
  taskKind: "WORK" | "GATE_DECISION";
  gateCode?: string;
  gateStatus?: "PENDING" | "APPROVED" | "REJECTED";
  gateDecisionReason?: string;
  gateDecidedAt?: string;
  resultJson?: string;
  evidenceJson?: string;
  executionError?: string;
  failureAudit?: AgentTaskFailureAudit;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus:
    | "NOT_REPORTED"
    | "NOT_APPLICABLE"
    | "ESTIMATED"
    | "PARTIALLY_ESTIMATED"
    | "PRICING_UNAVAILABLE";
  modelUsageUpdatedAt?: string;
  executionModelCode?: string;
  executionMode?: "MODEL" | "DETERMINISTIC" | "NOT_STARTED";
  executionReasoningEffort?: string;
  executionPrompt?: string;
  executionAgentPrompt?: string;
  executionActivityPrompt?: string;
  blockerGuidance?: AgentTaskBlockerGuidance;
  accessedUrls?: AgentTaskAuditLink[];
  visualEvidence?: AgentTaskVisualEvidence[];
  visualAudit?: PsiqueVisualAudit;
  purchaseEmotion?: PsiquePurchaseEmotion;
  receivedAt?: string;
  deliveredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAgentTaskPayload {
  assignedAgentKey: string;
  requestedByName: string;
  title: string;
  description: string;
  priority: AgentTask["priority"];
  sourceReference?: string;
  processDefinitionId?: number;
  processActivityId?: string;
  exceptional: boolean;
  exceptionReason?: string;
}

export type ProcessInstanceOperationalState =
  | "RELEASED"
  | "WAITING_PREDECESSOR"
  | "IN_PROGRESS"
  | "BLOCKED"
  | "COMPLETED"
  | "CANCELLED"
  | "SUPERSEDED_LEGACY";

export interface ProcessInstanceTask {
  taskId: number;
  activityInstanceId?: number;
  attemptNumber: number;
  activityId?: string;
  activityName: string;
  agentKey: string;
  agentNickname: string;
  taskStatus: AgentTaskStatus;
  operationalState: ProcessInstanceOperationalState;
  stateReason: string;
  failureAudit?: AgentTaskFailureAudit;
  executionMode?: "MODEL" | "DETERMINISTIC" | "NOT_STARTED";
  modelCode?: string;
  reasoningEffort?: string;
  promptSent?: string;
  agentPromptPart?: string;
  activityPromptPart?: string;
  blockerGuidance?: AgentTaskBlockerGuidance;
  accessedUrls?: AgentTaskAuditLink[];
  visualEvidence?: AgentTaskVisualEvidence[];
  visualAudit?: PsiqueVisualAudit;
  purchaseEmotion?: PsiquePurchaseEmotion;
  inputTokens?: number;
  cachedInputTokens?: number;
  outputTokens?: number;
  estimatedCostUsd?: number;
  costEstimationStatus: AgentTask["costEstimationStatus"];
  receivedAt?: string;
  deliveredAt?: string;
}

export interface ProcessInstanceActivity {
  activityInstanceId?: number;
  activityDefinitionId?: number;
  activityId: string;
  activityName: string;
  objective?: string;
  occurrenceNumber: number;
  status: AgentTaskStatus;
  operationalState: ProcessInstanceOperationalState;
  stateReason: string;
  enteredAt?: string;
  exitedAt?: string;
  objectiveAchieved: boolean;
  knownCostUsd?: number;
  costCoverage: "COMPLETE" | "PARTIAL" | "NOT_REPORTED";
  evidenceQuality:
    "DIRECT" | "MIXED" | "BACKFILLED_FROM_TASKS" | "LEGACY_DERIVED";
  tasks: ProcessInstanceTask[];
}

export interface ProcessInstance {
  processDefinitionId: number;
  processCode: string;
  processVersionNumber: number;
  sourceReference: string;
  activities: ProcessInstanceActivity[];
  tasks: ProcessInstanceTask[];
  supersededLegacyTasks: ProcessInstanceTask[];
}
