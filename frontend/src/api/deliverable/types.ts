export interface Deliverable {
  id: number;
  nicheId: number;
  nicheName: string;
  title: string;
  description?: string | null;
  content?: string | null;
  model?: string | null;
  prompt: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DeliverablePackage {
  id: number;
  experimentId?: number | null;
  experimentName?: string | null;
  hypothesisId?: string | null;
  hypothesisTitle?: string | null;
  nicheId?: number | null;
  nicheName?: string | null;
  name: string;
  description?: string | null;
  model?: string | null;
  prompt: string;
  deliverables: Deliverable[];
  createdAt?: string;
  updatedAt?: string;
}
