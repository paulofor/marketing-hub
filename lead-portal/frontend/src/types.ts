export type LeadStatus =
  | "PROCESSING"
  | "WATERMARK_PENDING"
  | "WATERMARKING"
  | "COMPLETED";

export interface LeadDetails {
  id: string;
  name: string;
  email: string;
  notes?: string | null;
  status: LeadStatus;
  createdAt: string;
  completedAt?: string | null;
  result?: string | null;
  imageUrl: string;
}

export interface CreateLeadPayload {
  name: string;
  email: string;
  notes?: string;
  image: File;
}

export type FlowQuestionType =
  | "TEXT"
  | "TEXTAREA"
  | "NUMBER"
  | "EMAIL"
  | "PHONE"
  | "DATE"
  | "SINGLE_CHOICE"
  | "MULTIPLE_CHOICE"
  | "IMAGE_UPLOAD";

export interface FlowQuestion {
  title: string;
  dataKey: string;
  type: FlowQuestionType;
  required: boolean;
  description?: string | null;
  placeholder?: string | null;
  options: string[];
}

export interface LeadPortalFlow {
  slug: string;
  name: string;
  description?: string | null;
  questions: FlowQuestion[];
}

export interface FlowSubmissionPayload {
  name: string;
  email: string;
  answers: Record<string, string | string[]>;
  imageKey?: string;
}

export interface FlowSubmissionResponse {
  id: string;
  flowSlug: string;
  name: string;
  email: string;
  imageUrl?: string | null;
  createdAt: string;
}
