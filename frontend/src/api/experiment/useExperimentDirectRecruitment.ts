import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { sha256 } from "js-sha256";
import { fingerprintDirectContact } from "./useExperimentDirectContactSample";

export type DirectRecruitmentCampaign = {
  id: number | null;
  experimentId: number;
  productName: string;
  status: "NOT_CREATED" | "DRAFT" | "ACTIVE" | "PAUSED" | "COMPLETED";
  contractVersion: string;
  headline: string;
  bodyText: string;
  audienceSummary: string;
  consentText: string;
  consentVersion: string;
  offerUrl: string;
  offerCta: string;
  privacyPolicyUrl: string;
  publicPath: string | null;
  targetContacts: number;
  remainingContacts: number;
  uniqueVisits: number;
  submissions: number;
  qualifiedSubmissions: number;
  notQualifiedSubmissions: number;
  recordedContacts: number;
  connectedOrganicAccounts: number;
  acquisitionStatus:
    | "NOT_CREATED"
    | "DRAFT_REQUIRES_APPROVAL"
    | "ACTIVE_WITHOUT_DISTRIBUTION"
    | "READY_FOR_ORGANIC_DISTRIBUTION"
    | "PAUSED"
    | "SAMPLE_COMPLETE";
  distributionGuidance: string;
  createdBy: string | null;
  statusChangedBy: string | null;
  statusReason: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  activatedAt: string | null;
  pausedAt: string | null;
  completedAt: string | null;
};

export type PublicDirectRecruitment = {
  token: string;
  experimentId: number;
  status: "DRAFT" | "ACTIVE" | "PAUSED" | "COMPLETED";
  acceptingSubmissions: boolean;
  productName: string;
  headline: string;
  bodyText: string;
  audienceSummary: string;
  consentText: string;
  consentVersion: string;
  privacyPolicyUrl: string;
  targetContacts: number;
  remainingContacts: number;
  availabilityMessage: string;
};

export type RecruitmentAttribution = {
  utmSource?: string;
  utmMedium?: string;
  utmCampaign?: string;
  utmContent?: string;
};

export type SubmitDirectRecruitmentInput = RecruitmentAttribution & {
  contactReference: string;
  submissionKey: string;
  serviceSegment: string;
  weeklyConversationsRange: string;
  usesWhatsapp: boolean;
  decisionMaker: boolean;
  wantsPersonalizedImplementation: boolean;
  consentAccepted: boolean;
  consentVersion: string;
};

export type SubmitDirectRecruitmentResult = {
  submissionId: number;
  status: "QUALIFIED" | "NOT_QUALIFIED";
  qualified: boolean;
  message: string;
  offerUrl: string | null;
  remainingContacts: number;
  sampleComplete: boolean;
};

/** Produz um identificador estável do navegador sem expor IP ou user-agent. */
export function fingerprintRecruitmentVisitor(
  token: string,
  visitorKey: string,
) {
  return sha256(`direct-recruitment:${token}:${visitorKey}`);
}

/** Lê somente os parâmetros de atribuição aceitos pelo contrato do backend. */
export function recruitmentAttribution(search: string): RecruitmentAttribution {
  const params = new URLSearchParams(search);
  return {
    utmSource: params.get("utm_source")?.slice(0, 100) || undefined,
    utmMedium: params.get("utm_medium")?.slice(0, 100) || undefined,
    utmCampaign: params.get("utm_campaign")?.slice(0, 100) || undefined,
    utmContent: params.get("utm_content")?.slice(0, 100) || undefined,
  };
}

/** Consulta conteúdo, métricas e bloqueios da atividade administrativa. */
export function useExperimentDirectRecruitment(experimentId?: number) {
  return useQuery({
    queryKey: ["experiments", experimentId, "direct-recruitment"],
    enabled: Boolean(experimentId),
    refetchInterval: 15_000,
    queryFn: async () =>
      (
        await axios.get<DirectRecruitmentCampaign>(
          `/api/experiments/${experimentId}/direct-recruitment`,
        )
      ).data,
  });
}

/** Cria o rascunho do convite e atualiza a atividade na tela. */
export function useCreateDirectRecruitmentDraft(experimentId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (createdBy: string) =>
      (
        await axios.post<DirectRecruitmentCampaign>(
          `/api/experiments/${experimentId}/direct-recruitment/draft`,
          { createdBy },
        )
      ).data,
    onSuccess: (data) =>
      queryClient.setQueryData(
        ["experiments", experimentId, "direct-recruitment"],
        data,
      ),
  });
}

/** Registra a aprovação humana e torna o convite publicamente acessível. */
export function useActivateDirectRecruitment(experimentId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (approvedBy: string) =>
      (
        await axios.post<DirectRecruitmentCampaign>(
          `/api/experiments/${experimentId}/direct-recruitment/activate`,
          { approvedBy, approvalConfirmed: true },
        )
      ).data,
    onSuccess: (data) =>
      queryClient.setQueryData(
        ["experiments", experimentId, "direct-recruitment"],
        data,
      ),
  });
}

/** Pausa o convite e mantém os resultados coletados visíveis. */
export function usePauseDirectRecruitment(experimentId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      pausedBy,
      reason,
    }: {
      pausedBy: string;
      reason: string;
    }) =>
      (
        await axios.post<DirectRecruitmentCampaign>(
          `/api/experiments/${experimentId}/direct-recruitment/pause`,
          { pausedBy, reason },
        )
      ).data,
    onSuccess: (data) =>
      queryClient.setQueryData(
        ["experiments", experimentId, "direct-recruitment"],
        data,
      ),
  });
}

/** Consulta a versão pública do convite pelo token opaco. */
export function usePublicDirectRecruitment(token?: string) {
  return useQuery({
    queryKey: ["public", "direct-recruitment", token],
    enabled: Boolean(token),
    retry: false,
    queryFn: async () =>
      (
        await axios.get<PublicDirectRecruitment>(
          `/api/public/direct-recruitments/${token}`,
        )
      ).data,
  });
}

/** Registra uma visita pseudonimizada e idempotente. */
export async function registerDirectRecruitmentVisit(
  token: string,
  visitorKey: string,
  attribution: RecruitmentAttribution,
) {
  return (
    await axios.post<{ counted: boolean; uniqueVisits: number }>(
      `/api/public/direct-recruitments/${token}/visits`,
      {
        visitorFingerprint: fingerprintRecruitmentVisitor(token, visitorKey),
        ...attribution,
      },
    )
  ).data;
}

/** Envia a adesão pseudonimizada e mantém a oferta condicionada à qualificação. */
export function useSubmitDirectRecruitment(
  token: string,
  experimentId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: SubmitDirectRecruitmentInput) =>
      (
        await axios.post<SubmitDirectRecruitmentResult>(
          `/api/public/direct-recruitments/${token}/submissions`,
          {
            contactFingerprint: fingerprintDirectContact(
              input.contactReference,
              experimentId,
            ),
            submissionKey: input.submissionKey,
            serviceSegment: input.serviceSegment,
            weeklyConversationsRange: input.weeklyConversationsRange,
            usesWhatsapp: input.usesWhatsapp,
            decisionMaker: input.decisionMaker,
            wantsPersonalizedImplementation:
              input.wantsPersonalizedImplementation,
            consentAccepted: input.consentAccepted,
            consentVersion: input.consentVersion,
            utmSource: input.utmSource,
            utmMedium: input.utmMedium,
            utmCampaign: input.utmCampaign,
            utmContent: input.utmContent,
          },
        )
      ).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ["public", "direct-recruitment", token],
      });
    },
  });
}
