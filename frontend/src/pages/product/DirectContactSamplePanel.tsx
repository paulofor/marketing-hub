import { FormEvent, useState } from "react";
import axios from "axios";
import { CheckCircle2, ShieldCheck, UserPlus } from "lucide-react";
import {
  useExperimentDirectContactSample,
  useRegisterExperimentDirectContact,
} from "../../api/experiment/useExperimentDirectContactSample";
import DirectRecruitmentPanel from "./DirectRecruitmentPanel";

type DirectContactSamplePanelProps = {
  experimentId: number;
  productId: number;
  processDefinitionId: number;
};

/** Gera o valor aceito pelo campo datetime-local no fuso do navegador. */
function localDateTimeValue(date = new Date()) {
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

/** Converte uma data local preenchida pelo operador para o instante auditável UTC. */
function utcInstant(value: string) {
  return new Date(value).toISOString();
}

/** Exibe e registra a coorte consentida sem enviar mensagens nem persistir identidade em claro. */
export default function DirectContactSamplePanel({
  experimentId,
  productId,
  processDefinitionId,
}: DirectContactSamplePanelProps) {
  const sample = useExperimentDirectContactSample(experimentId);
  const register = useRegisterExperimentDirectContact(
    experimentId,
    productId,
    processDefinitionId,
  );
  const now = localDateTimeValue();
  const [contactReference, setContactReference] = useState("");
  const [consentEvidenceReference, setConsentEvidenceReference] = useState("");
  const [consentRecordedAt, setConsentRecordedAt] = useState(now);
  const [contactedAt, setContactedAt] = useState(now);
  const [recordedBy, setRecordedBy] = useState("");
  const [audienceFitConfirmed, setAudienceFitConfirmed] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  /** Valida o formulário e registra somente uma abordagem que já aconteceu. */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLocalError(null);
    try {
      await register.mutateAsync({
        contactReference,
        consentEvidenceReference,
        consentRecordedAt: utcInstant(consentRecordedAt),
        contactedAt: utcInstant(contactedAt),
        audienceFitConfirmed,
        recordedBy,
      });
      setContactReference("");
      setConsentEvidenceReference("");
      setAudienceFitConfirmed(false);
      setConsentRecordedAt(localDateTimeValue());
      setContactedAt(localDateTimeValue());
    } catch (error) {
      if (axios.isAxiosError(error)) {
        setLocalError(
          error.response?.data?.message ??
            "Não foi possível registrar este contato.",
        );
      } else {
        setLocalError(
          error instanceof Error
            ? error.message
            : "Não foi possível registrar este contato.",
        );
      }
    }
  }

  if (sample.isLoading) {
    return (
      <div className="alert alert-light border" role="status">
        Carregando a amostra consentida...
      </div>
    );
  }

  if (sample.isError || !sample.data) {
    return (
      <div className="alert alert-danger" role="alert">
        Não foi possível consultar a amostra direta do experimento.
      </div>
    );
  }

  const progress = sample.data.targetContacts
    ? Math.min(
        100,
        Math.round(
          (sample.data.recordedContacts / sample.data.targetContacts) * 100,
        ),
      )
    : 0;

  return (
    <>
      <DirectRecruitmentPanel experimentId={experimentId} />
      <section
        className="card border-0 bg-light mb-4"
        aria-label="Amostra direta consentida"
      >
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h3 className="h5 mb-1 d-flex align-items-center gap-2">
                <ShieldCheck size={20} aria-hidden="true" />
                Amostra individual consentida
              </h3>
              <p className="text-body-secondary mb-0">
                Registre somente depois do contato real. Telefone ou e-mail é
                pseudonimizado no navegador e nunca chega ao backend em claro.
              </p>
            </div>
            <span
              className={`badge ${sample.data.readyForHermesReview ? "text-bg-success" : "text-bg-secondary"}`}
            >
              {sample.data.recordedContacts}/{sample.data.targetContacts}{" "}
              contatos
            </span>
          </div>

          <div
            className="progress mt-3"
            role="progressbar"
            aria-label="Avanço da amostra consentida"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div className="progress-bar" style={{ width: `${progress}%` }}>
              {progress}%
            </div>
          </div>

          {sample.data.readyForHermesReview ? (
            <div
              className="alert alert-success mt-3 mb-0 d-flex gap-2"
              role="status"
            >
              <CheckCircle2 size={20} aria-hidden="true" />A amostra atingiu a
              meta. Reavalie esta atividade para Hermes consolidar o resultado
              real.
            </div>
          ) : (
            <p className="small text-body-secondary mt-2 mb-0">
              Faltam {sample.data.remainingContacts} contatos. Repetir Hermes
              antes disso não acrescenta evidência e permanece desabilitado.
            </p>
          )}

          <form className="row g-3 mt-1" onSubmit={handleSubmit}>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="direct-contact-reference">
                Telefone ou e-mail do contato *
              </label>
              <input
                className="form-control"
                id="direct-contact-reference"
                value={contactReference}
                onChange={(event) => setContactReference(event.target.value)}
                required
                autoComplete="off"
              />
              <div className="form-text">
                Usado apenas localmente para evitar duplicidade; o servidor
                recebe somente SHA-256.
              </div>
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="direct-contact-evidence">
                Referência da evidência de consentimento *
              </label>
              <input
                className="form-control"
                id="direct-contact-evidence"
                placeholder="internal://consentimentos/... ou https://..."
                value={consentEvidenceReference}
                onChange={(event) =>
                  setConsentEvidenceReference(event.target.value)
                }
                required
              />
            </div>
            <div className="col-12 col-md-6 col-lg-4">
              <label className="form-label" htmlFor="direct-contact-consent-at">
                Consentimento registrado em *
              </label>
              <input
                className="form-control"
                id="direct-contact-consent-at"
                type="datetime-local"
                value={consentRecordedAt}
                onChange={(event) => setConsentRecordedAt(event.target.value)}
                required
              />
            </div>
            <div className="col-12 col-md-6 col-lg-4">
              <label
                className="form-label"
                htmlFor="direct-contact-contacted-at"
              >
                Contato realizado em *
              </label>
              <input
                className="form-control"
                id="direct-contact-contacted-at"
                type="datetime-local"
                value={contactedAt}
                onChange={(event) => setContactedAt(event.target.value)}
                required
              />
            </div>
            <div className="col-12 col-lg-4">
              <label
                className="form-label"
                htmlFor="direct-contact-recorded-by"
              >
                Registrado por *
              </label>
              <input
                className="form-control"
                id="direct-contact-recorded-by"
                value={recordedBy}
                onChange={(event) => setRecordedBy(event.target.value)}
                required
              />
            </div>
            <div className="col-12">
              <div className="form-check">
                <input
                  className="form-check-input"
                  id="direct-contact-audience-fit"
                  type="checkbox"
                  checked={audienceFitConfirmed}
                  onChange={(event) =>
                    setAudienceFitConfirmed(event.target.checked)
                  }
                  required
                />
                <label
                  className="form-check-label"
                  htmlFor="direct-contact-audience-fit"
                >
                  Confirmo que o contato consentiu e pertence ao público do
                  experimento.
                </label>
              </div>
            </div>

            {localError ? (
              <div className="col-12">
                <div className="alert alert-danger mb-0" role="alert">
                  {localError}
                </div>
              </div>
            ) : null}

            <div className="col-12">
              <button
                className="btn btn-primary d-inline-flex align-items-center gap-2"
                type="submit"
                disabled={
                  register.isPending || sample.data.readyForHermesReview
                }
              >
                <UserPlus size={17} aria-hidden="true" />
                {register.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                ) : null}
                {register.isPending
                  ? "Registrando..."
                  : "Registrar contato realizado"}
              </button>
            </div>
          </form>

          {sample.data.contacts.length > 0 ? (
            <div className="table-responsive mt-4">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Identificador pseudonimizado</th>
                    <th>Consentimento</th>
                    <th>Contato</th>
                    <th>Operador</th>
                  </tr>
                </thead>
                <tbody>
                  {sample.data.contacts.map((contact, index) => (
                    <tr key={contact.id}>
                      <td>{index + 1}</td>
                      <td>
                        <code>…{contact.contactFingerprintSuffix}</code>
                      </td>
                      <td>
                        {new Date(contact.consentRecordedAt).toLocaleString(
                          "pt-BR",
                        )}
                      </td>
                      <td>
                        {new Date(contact.contactedAt).toLocaleString("pt-BR")}
                      </td>
                      <td>{contact.recordedBy}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </section>
    </>
  );
}
