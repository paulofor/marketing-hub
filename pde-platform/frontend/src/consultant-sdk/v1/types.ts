export type ConsultantChannel = "PWA" | "WHATSAPP";

export type ConsultantBlocker = {
  blocked: boolean;
  reason?: string | null;
  userGuidance?: string | null;
  helpLinks: string[];
};

export type ConsultantTurnInput = {
  message: string;
  image?: File;
  imageConsent: boolean;
};

export type ConsultantTurnOutput = {
  message: string;
  recommendation: string;
  why: string;
  nextQuestion?: string | null;
  blocker?: ConsultantBlocker;
};

export type ConsultantTransport = (
  input: ConsultantTurnInput,
) => Promise<ConsultantTurnOutput>;

export type ConsultantConversationMessage = {
  id: string;
  role: "CUSTOMER" | "CONSULTANT";
  text: string;
  recommendation?: string;
  why?: string;
  nextQuestion?: string | null;
  imageUrl?: string;
  blocker?: ConsultantBlocker;
};
