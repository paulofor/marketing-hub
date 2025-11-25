export type ImageDeliverableStatus =
  | "RECEIVED"
  | "PROCESSED"
  | "GENERATION_WITH_WATERMARK"
  | "PURCHASED"
  | "GENERATION_NO_WATERMARK"
  | "FAILED";

type ImageDeliverableAccessType = "PREMIUM" | "FREE";

export interface ImageDeliverableItem {
  id: number;
  assetId: number;
  assetUrl: string;
  accessType: ImageDeliverableAccessType;
  position: number;
  createdAt: string;
}

export interface ImageDeliverablePackage {
  id: number;
  leadId: string;
  inputAssetId: number;
  inputAssetUrl: string;
  status: ImageDeliverableStatus;
  plannedOutputs: number | null;
  freeImages: number;
  model: string | null;
  prompt: string;
  items: ImageDeliverableItem[];
  createdAt: string;
  updatedAt: string;
}
