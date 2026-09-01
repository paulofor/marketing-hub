import { FormEvent, useEffect, useMemo, useState } from "react";
import axios from "axios";
import { ArrowRight, CheckCircle2, Lock, MessageCircle } from "lucide-react";
import { useParams } from "react-router-dom";
import {
  recruitmentAttribution,
  registerDirectRecruitmentVisit,
  SubmitDirectRecruitmentResult,
  usePublicDirectRecruitment,
  useSubmitDirectRecruitment,
} from "../../api/experiment/useExperimentDirectRecruitment";
import "./DirectRecruitmentLandingPage.css";

/** Cria uma chave UUID compatível mesmo quando randomUUID não estiver disponível. */
function newUuid() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (value) => {
    const random = Math.floor(Math.random() * 16);
    const next = value === "x" ? random : (random & 0x3) | 0x8;
    return next.toString(16);
  });
}

/** Mantém um visitante pseudônimo estável somente no navegador atual. */
function visitorKey(token: string) {
  const key = `direct-recruitment-visitor:${token}`;
  try {
    const existing = window.localStorage.getItem(key);
    if (existing) return existing;
    const created = newUuid();
    window.localStorage.setItem(key, created);
    return created;
  } catch {
    return newUuid();
  }
}

/** Extrai a mensagem funcional do backend sem limpar as respostas do formulário. */
function requestError(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return error instanceof Error ? error.message : fallback;
}

/** Exibe o convite público, qualifica o perfil e libera a oferta após consentimento. */
export default function DirectRecruitmentLandingPage() {
  const { token = "" } = useParams();
  const campaign = usePublicDirectRecruitment(token);
  const attribution = useMemo(
    () => recruitmentAttribution(window.location.search),
    [],
  );
  const submit = useSubmitDirectRecruitment(
    token,
    campaign.data?.experimentId ?? 0,
  );
  const [visitError, setVisitError] = useState<string | null>(null);
  const [visitReady, setVisitReady] = useState(false);
  const [contactReference, setContactReference] = useState("");
  const [serviceSegment, setServiceSegment] = useState("");
  const [weeklyRange, setWeeklyRange] = useState("");
  const [usesWhatsapp, setUsesWhatsapp] = useState("");
  const [decisionMaker, setDecisionMaker] = useState("");
  const [wantsImplementation, setWantsImplementation] = useState("");
  const [consentAccepted, setConsentAccepted] = useState(false);
  const [submissionKey] = useState(newUuid);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SubmitDirectRecruitmentResult | null>(
    null,
  );

  useEffect(() => {
    if (!campaign.data?.acceptingSubmissions || !token) return;
    let active = true;
    setVisitError(null);
    registerDirectRecruitmentVisit(token, visitorKey(token), attribution)
      .then(() => {
        if (active) setVisitReady(true);
      })
      .catch(() => {
        if (active) {
          setVisitReady(false);
          setVisitError(
            "Não conseguimos iniciar sua participação. Recarregue a página para tentar novamente.",
          );
        }
      });
    return () => {
      active = false;
    };
  }, [attribution, campaign.data?.acceptingSubmissions, token]);

  /** Envia somente respostas categóricas e o fingerprint produzido no navegador. */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!campaign.data) return;
    setError(null);
    try {
      const response = await submit.mutateAsync({
        contactReference,
        submissionKey,
        serviceSegment,
        weeklyConversationsRange: weeklyRange,
        usesWhatsapp: usesWhatsapp === "true",
        decisionMaker: decisionMaker === "true",
        wantsPersonalizedImplementation: wantsImplementation === "true",
        consentAccepted,
        consentVersion: campaign.data.consentVersion,
        ...attribution,
      });
      setResult(response);
    } catch (cause) {
      setError(
        requestError(
          cause,
          "Não foi possível concluir sua participação. Suas respostas foram preservadas.",
        ),
      );
    }
  }

  if (campaign.isLoading) {
    return (
      <main className="direct-recruitment-page direct-recruitment-page--center">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Carregando convite...</span>
        </div>
      </main>
    );
  }

  if (campaign.isError || !campaign.data) {
    return (
      <main className="direct-recruitment-page direct-recruitment-page--center">
        <div className="direct-recruitment-card text-center">
          <h1 className="h3">Convite indisponível</h1>
          <p className="text-body-secondary mb-0">
            Este endereço não corresponde a uma validação ativa.
          </p>
        </div>
      </main>
    );
  }

  const data = campaign.data;

  return (
    <main className="direct-recruitment-page">
      <div className="direct-recruitment-shell">
        <section className="direct-recruitment-hero">
          <div className="direct-recruitment-eyebrow">
            <MessageCircle size={18} aria-hidden="true" />
            Convite para pequenos prestadores de serviços
          </div>
          <h1>{data.headline}</h1>
          <p className="direct-recruitment-lead">{data.bodyText}</p>
          <div className="direct-recruitment-benefit">
            <CheckCircle2 size={20} aria-hidden="true" />
            <span>
              Descubra em menos de dois minutos se esta implantação faz sentido
              para o seu atendimento.
            </span>
          </div>
          <p className="direct-recruitment-audience">
            <strong>Este piloto é para:</strong> {data.audienceSummary}
          </p>
        </section>

        <section
          className="direct-recruitment-card"
          aria-label="Formulário de participação"
        >
          {result ? (
            <div className="direct-recruitment-result" role="status">
              <CheckCircle2 size={42} aria-hidden="true" />
              <h2 className="h3 mt-3">
                {result.qualified
                  ? "Seu perfil combina com este piloto"
                  : "Participação registrada"}
              </h2>
              <p>{result.message}</p>
              {result.qualified && result.offerUrl ? (
                <a
                  className="btn btn-primary btn-lg d-inline-flex align-items-center gap-2"
                  href={result.offerUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Conhecer {data.productName}
                  <ArrowRight size={19} aria-hidden="true" />
                </a>
              ) : null}
            </div>
          ) : !data.acceptingSubmissions ? (
            <div className="text-center py-4" role="status">
              <h2 className="h4">Participação encerrada</h2>
              <p className="text-body-secondary mb-0">
                {data.availabilityMessage}
              </p>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="mb-4">
                <span className="direct-recruitment-step">1</span>
                <h2 className="h4 d-inline ms-2">Seu atendimento hoje</h2>
              </div>

              <div className="mb-3">
                <label className="form-label" htmlFor="recruitment-segment">
                  Em qual tipo de serviço você atua? *
                </label>
                <select
                  className="form-select"
                  id="recruitment-segment"
                  value={serviceSegment}
                  onChange={(event) => setServiceSegment(event.target.value)}
                  required
                >
                  <option value="">Selecione</option>
                  <option value="BEAUTY_WELLNESS">Beleza e bem-estar</option>
                  <option value="CONSULTING">
                    Consultoria e serviços profissionais
                  </option>
                  <option value="HOME_SERVICES">Serviços para casa</option>
                  <option value="HEALTH">Saúde e cuidados</option>
                  <option value="EDUCATION">Educação e aulas</option>
                  <option value="OTHER_SERVICE">Outro serviço</option>
                </select>
              </div>

              <div className="mb-3">
                <label className="form-label" htmlFor="recruitment-volume">
                  Quantas conversas com clientes você costuma ter por semana? *
                </label>
                <select
                  className="form-select"
                  id="recruitment-volume"
                  value={weeklyRange}
                  onChange={(event) => setWeeklyRange(event.target.value)}
                  required
                >
                  <option value="">Selecione</option>
                  <option value="ONE_TO_TEN">1 a 10</option>
                  <option value="ELEVEN_TO_THIRTY">11 a 30</option>
                  <option value="OVER_THIRTY">Mais de 30</option>
                </select>
              </div>

              <div className="row g-3">
                <div className="col-12 col-md-4">
                  <label className="form-label" htmlFor="recruitment-whatsapp">
                    Usa WhatsApp para atender? *
                  </label>
                  <select
                    className="form-select"
                    id="recruitment-whatsapp"
                    value={usesWhatsapp}
                    onChange={(event) => setUsesWhatsapp(event.target.value)}
                    required
                  >
                    <option value="">Selecione</option>
                    <option value="true">Sim</option>
                    <option value="false">Não</option>
                  </select>
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label" htmlFor="recruitment-decision">
                    Decide sobre esse atendimento? *
                  </label>
                  <select
                    className="form-select"
                    id="recruitment-decision"
                    value={decisionMaker}
                    onChange={(event) => setDecisionMaker(event.target.value)}
                    required
                  >
                    <option value="">Selecione</option>
                    <option value="true">Sim</option>
                    <option value="false">Não</option>
                  </select>
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label" htmlFor="recruitment-interest">
                    Quer uma implantação personalizada? *
                  </label>
                  <select
                    className="form-select"
                    id="recruitment-interest"
                    value={wantsImplementation}
                    onChange={(event) =>
                      setWantsImplementation(event.target.value)
                    }
                    required
                  >
                    <option value="">Selecione</option>
                    <option value="true">Sim, quero conhecer</option>
                    <option value="false">Não neste momento</option>
                  </select>
                </div>
              </div>

              <hr className="my-4" />

              <div className="mb-4">
                <span className="direct-recruitment-step">2</span>
                <h2 className="h4 d-inline ms-2">Sua participação</h2>
              </div>

              <div className="mb-3">
                <label className="form-label" htmlFor="recruitment-contact">
                  Seu WhatsApp ou e-mail *
                </label>
                <input
                  className="form-control"
                  id="recruitment-contact"
                  value={contactReference}
                  onChange={(event) => setContactReference(event.target.value)}
                  required
                  autoComplete="email"
                />
                <div className="form-text d-flex align-items-start gap-1">
                  <Lock
                    size={15}
                    className="mt-1 flex-shrink-0"
                    aria-hidden="true"
                  />
                  <span>
                    Usado somente neste navegador para evitar participação
                    duplicada. O servidor recebe apenas um fingerprint SHA-256
                    pseudonimizado.
                  </span>
                </div>
              </div>

              <div className="form-check mb-3">
                <input
                  className="form-check-input"
                  id="recruitment-consent"
                  type="checkbox"
                  checked={consentAccepted}
                  onChange={(event) => setConsentAccepted(event.target.checked)}
                  required
                />
                <label
                  className="form-check-label"
                  htmlFor="recruitment-consent"
                >
                  {data.consentText} *
                </label>
              </div>

              {visitError ? (
                <div className="alert alert-warning" role="alert">
                  {visitError}
                </div>
              ) : null}
              {error ? (
                <div className="alert alert-danger" role="alert">
                  {error}
                </div>
              ) : null}

              <button
                className="btn btn-primary btn-lg w-100 d-flex justify-content-center align-items-center gap-2"
                type="submit"
                disabled={submit.isPending || !visitReady}
              >
                {submit.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                ) : null}
                {submit.isPending
                  ? "Verificando seu perfil..."
                  : visitReady
                    ? "Quero participar e conhecer a solução"
                    : "Preparando participação..."}
                {!submit.isPending && visitReady ? (
                  <ArrowRight size={19} aria-hidden="true" />
                ) : null}
              </button>
              <p className="small text-center text-body-secondary mt-3 mb-0">
                Participar não obriga compra. Venda e pagamento só existem se
                você decidir avançar na página oficial.
              </p>
            </form>
          )}
          <p className="small text-body-secondary text-center mt-3 mb-0">
            Consulte a{" "}
            <a href={data.privacyPolicyUrl} target="_blank" rel="noreferrer">
              política de privacidade
            </a>{" "}
            desta validação.
          </p>
        </section>
      </div>
    </main>
  );
}
