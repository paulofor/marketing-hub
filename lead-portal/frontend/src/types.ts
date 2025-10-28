export type LeadStatus = "PROCESSING" | "COMPLETED";

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
