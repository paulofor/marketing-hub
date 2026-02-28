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
  campaignCode?: string | null;
}

export interface FlowSubmissionResponse {
  id: string;
  flowSlug: string;
  name: string;
  email: string;
  imageUrl?: string | null;
  createdAt: string;
}

export interface ImageMaterialCostTotal {
  currency: string;
  amount: number;
}

export interface ImagePackageSummary {
  packageId: number;
  submissionId: string;
  status: string;
  professionalName: string;
  contactSummary: string;
  studioName?: string | null;
  location?: string | null;
  services: string[];
  plannedOutputs?: number | null;
  totalPrice?: number | null;
  currency?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  failureReason?: string | null;
}

export interface ImageMaterialDashboard {
  flowSlug: string;
  totalSubmissions: number;
  packagesQueued: number;
  packagesInProgress: number;
  packagesCompleted: number;
  packagesFailed: number;
  plannedImages: number;
  imagesGenerated: number;
  estimatedCostUsd: number;
  payments: ImageMaterialCostTotal[];
  recentPackages: ImagePackageSummary[];
}

export interface ImageMaterialCase {
  submissionId: string;
  flowSlug: string;
  activityType: string;
  professionalName: string;
  email: string;
  contactSummary: string;
  studioName?: string | null;
  location?: string | null;
  services: string[];
  answers: Record<string, unknown>;
  packages: ImageMaterialCasePackage[];
}

export interface ImageMaterialCasePackage {
  packageId: number;
  status: string;
  plannedOutputs?: number | null;
  freeImages?: number | null;
  model?: string | null;
  prompt: string;
  totalPrice?: number | null;
  currency?: string | null;
  failureReason?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  history: ImageMaterialCasePackageStatus[];
}

export interface ImageMaterialCasePackageStatus {
  status: string;
  createdAt?: string | null;
  reason?: string | null;
}
