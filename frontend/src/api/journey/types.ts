export type JourneyStatus =
  | "DRAFT"
  | "ACTIVE"
  | "PAUSED"
  | "COMPLETED"
  | "ARCHIVED";

export type JourneyPhase =
  | "ATTENTION"
  | "INTEREST"
  | "DESIRE"
  | "ACTION";

export type JourneyStimulusType = "AD" | "EMAIL" | "WHATSAPP" | "LANDING_PAGE";

export interface JourneyStep {
  id: number;
  templateId: number;
  position: number;
  name?: string | null;
  description?: string | null;
  phase: JourneyPhase;
  stimulusType: JourneyStimulusType;
  creativeId?: number | null;
  angleId?: number | null;
  visualProofId?: number | null;
  emotionalTriggerId?: number | null;
  entryCondition?: string | null;
  exitCondition?: string | null;
  delayMinutes?: number | null;
  metadata: Record<string, string>;
}

export interface Journey {
  id: number;
  templateId: number;
  templateName: string;
  name: string;
  description?: string | null;
  status: JourneyStatus;
  marketNicheId?: number | null;
  experimentId?: number | null;
  segmentReference?: string | null;
  segmentFilter?: string | null;
  metadata: Record<string, string>;
  startAt?: string | null;
  endAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface JourneyRequestPayload {
  templateId: number;
  name: string;
  description?: string;
  status?: JourneyStatus;
  marketNicheId?: number;
  experimentId?: number;
  segmentReference?: string;
  segmentFilter?: string;
  startAt?: string | null;
  endAt?: string | null;
  metadata?: Record<string, string>;
}

export type JourneyUpdatePayload = Partial<JourneyRequestPayload>;

export interface JourneyTemplateSummary {
  id: number;
  name: string;
  objective?: string | null;
  phases: string[];
  preferredChannel?: string | null;
  tags: string[];
  metadata: Record<string, string>;
  steps: JourneyStep[];
  createdAt: string;
  updatedAt: string;
}

export interface JourneyTemplate {
  id: number;
  name: string;
  description?: string | null;
  objective?: string | null;
  phases: JourneyPhase[];
  preferredChannel?: string | null;
  tags: string[];
  metadata: Record<string, string>;
  steps: JourneyStep[];
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size?: number;
}

export interface JourneyMetrics {
  totalJourneys: number;
  statusBreakdown: Partial<Record<JourneyStatus, number>>;
}
