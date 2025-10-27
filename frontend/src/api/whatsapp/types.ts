export type WhatsAppMessageDirection = "INBOUND" | "OUTBOUND";

export type WhatsAppMessageType =
  | "TEXT"
  | "IMAGE"
  | "TEMPLATE"
  | "DOCUMENT"
  | "UNKNOWN";

export interface WhatsAppAccount {
  id: number;
  displayName: string;
  phoneNumber: string | null;
  phoneNumberId: string;
  businessAccountId: string | null;
  accessToken: string | null;
  verifyToken: string | null;
  baseUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WhatsAppMessage {
  id: number;
  accountId: number | null;
  direction: WhatsAppMessageDirection;
  messageType: WhatsAppMessageType | null;
  messageId: string | null;
  fromNumber: string | null;
  toNumber: string | null;
  status: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  textBody: string | null;
  imageUrl: string | null;
  imageId: string | null;
  mimeType: string | null;
  caption: string | null;
  conversationId: string | null;
  contextJson: string | null;
  payloadJson: string | null;
  statusPayloadJson: string | null;
  messageTimestamp: string | null;
  sentAt: string | null;
  receivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SaveWhatsAppAccountInput {
  id?: number;
  displayName: string;
  phoneNumber?: string;
  phoneNumberId: string;
  businessAccountId?: string;
  accessToken?: string;
  verifyToken?: string;
  baseUrl?: string;
  active: boolean;
}

export interface SendWhatsAppMessageInput {
  to: string;
  type: WhatsAppMessageType;
  textBody?: string;
  imageUrl?: string;
  caption?: string;
}
