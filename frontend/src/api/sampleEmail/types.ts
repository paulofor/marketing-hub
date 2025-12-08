export interface SampleEmail {
  id: number;
  experimentId: number;
  subject: string;
  previewText?: string | null;
  body?: string | null;
  callToAction?: string | null;
  model?: string | null;
  prompt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
