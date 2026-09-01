import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { sha256 } from "js-sha256";

export type ExperimentDirectContact = {
  id: number;
  contactFingerprintSuffix: string;
  consentEvidenceReference: string;
  consentRecordedAt: string;
  contactedAt: string;
  audienceFitConfirmed: boolean;
  recordedBy: string;
  createdAt: string;
};

export type ExperimentDirectContactSample = {
  experimentId: number;
  platform: "DIRECT_ONE_TO_ONE";
  experimentStatus: string;
  targetContacts: number;
  recordedContacts: number;
  remainingContacts: number;
  readyForHermesReview: boolean;
  operationalStatus:
    "ACCUMULATING_CONSENTED_SAMPLE" | "READY_FOR_HERMES_REVIEW";
  contacts: ExperimentDirectContact[];
};

export type RegisterExperimentDirectContactInput = {
  contactReference: string;
  consentEvidenceReference: string;
  consentRecordedAt: string;
  contactedAt: string;
  audienceFitConfirmed: boolean;
  recordedBy: string;
};

/** Normaliza a identidade no navegador e gera um identificador segregado por experimento. */
export function fingerprintDirectContact(
  reference: string,
  experimentId: number,
) {
  const trimmed = reference.trim().toLowerCase();
  const email = trimmed.replace(/\s+/g, "");
  const phone = trimmed.replace(/\D/g, "");
  const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const isPhone = phone.length >= 8 && phone.length <= 15;
  if (!isEmail && !isPhone) {
    throw new Error(
      "Informe um telefone ou e-mail válido para identificar o contato.",
    );
  }
  const normalized = isEmail ? email : phone;
  return sha256(`experiment:${experimentId}:${normalized}`);
}

/** Consulta o placar oficial da amostra direta do experimento. */
export function useExperimentDirectContactSample(experimentId?: number) {
  return useQuery({
    queryKey: ["experiments", experimentId, "direct-contact-sample"],
    enabled: Boolean(experimentId),
    refetchInterval: 15_000,
    queryFn: async () =>
      (
        await axios.get<ExperimentDirectContactSample>(
          `/api/experiments/${experimentId}/direct-contact-sample`,
        )
      ).data,
  });
}

/** Registra um contato real sem transmitir telefone ou e-mail em claro ao backend. */
export function useRegisterExperimentDirectContact(
  experimentId: number,
  productId: number,
  processDefinitionId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: RegisterExperimentDirectContactInput) =>
      (
        await axios.post<ExperimentDirectContactSample>(
          `/api/experiments/${experimentId}/direct-contact-sample/contacts`,
          {
            contactFingerprint: fingerprintDirectContact(
              input.contactReference,
              experimentId,
            ),
            consentEvidenceReference: input.consentEvidenceReference,
            consentRecordedAt: input.consentRecordedAt,
            contactedAt: input.contactedAt,
            audienceFitConfirmed: input.audienceFitConfirmed,
            recordedBy: input.recordedBy,
          },
        )
      ).data,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["experiments", experimentId, "direct-contact-sample"],
        }),
        queryClient.invalidateQueries({
          queryKey: ["experiments", experimentId, "direct-recruitment"],
        }),
        queryClient.invalidateQueries({
          queryKey: [
            "products",
            productId,
            "business-processes",
            processDefinitionId,
            "activity-executions",
          ],
        }),
        queryClient.invalidateQueries({
          queryKey: ["experiment", String(experimentId), "cockpit"],
        }),
      ]);
    },
  });
}
