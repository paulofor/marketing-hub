import { Clock, Inbox, Loader2, Mail, Phone, RefreshCw, User } from "lucide-react";
import {
  useLeadPortalFormResponses,
  type LeadPortalFormResponse,
} from "../../api/leadPortal/useLeadPortalFormResponses";
import "./LeadPortalFormResponsesPage.css";

const LIMIT = 50;

function formatDateTime(value: string) {
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(value));
  } catch {
    return new Date(value).toLocaleString("pt-BR");
  }
}

function formatKey(key?: string | null) {
  if (!key) return "Resposta";
  return key.replace(/[_-]+/g, " ").replace(/\s+/g, " ").trim();
}

function renderAnswers(submission: LeadPortalFormResponse) {
  if (!submission.answers || submission.answers.length === 0) {
    return (
      <p className="lead-portal-form-responses__empty-answers">
        Sem respostas estruturadas — verifique o portal para obter detalhes.
      </p>
    );
  }

  return submission.answers.map((answer, index) => (
    <div className="lead-portal-form-responses__answer" key={`${submission.id}-${answer.key ?? "ans"}-${index}`}>
      <span className="lead-portal-form-responses__answer-key">{formatKey(answer.key)}</span>
      <span className="lead-portal-form-responses__answer-value">
        {answer.value?.trim() ? answer.value : "—"}
      </span>
    </div>
  ));
}

function buildFlowLabel(submission: LeadPortalFormResponse) {
  if (submission.flowName) return submission.flowName;
  if (submission.flowSlug) return submission.flowSlug;
  return "Fluxo desconhecido";
}

function buildExperimentLabel(submission: LeadPortalFormResponse) {
  if (submission.experimentName) return submission.experimentName;
  if (submission.experimentId) return `Experimento #${submission.experimentId}`;
  return null;
}

export default function LeadPortalFormResponsesPage() {
  const { data, isLoading, isError, refetch, isFetching } = useLeadPortalFormResponses(LIMIT);
  const submissions = data ?? [];

  const refreshButton = (
    <button
      type="button"
      className="lead-portal-form-responses__refresh"
      onClick={() => refetch()}
      disabled={isFetching}
    >
      {isFetching ? <Loader2 size={16} className="spin" /> : <RefreshCw size={16} />}
      <span>{isFetching ? "Atualizando..." : "Atualizar"}</span>
    </button>
  );

  return (
    <section className="lead-portal-form-responses">
      <header className="lead-portal-form-responses__header">
        <div>
          <p className="lead-portal-form-responses__eyebrow">Lead Portal</p>
          <h1>Respostas de formulários</h1>
          <p className="lead-portal-form-responses__subtitle">
            Monitorando os {LIMIT} envios mais recentes com data e hora de submissão.
          </p>
        </div>
        {refreshButton}
      </header>

      {isLoading && (
        <div className="lead-portal-form-responses__state">
          <Loader2 size={18} className="spin" />
          <span>Carregando respostas recentes...</span>
        </div>
      )}

      {isError && (
        <div className="lead-portal-form-responses__state lead-portal-form-responses__state--error">
          <Inbox size={18} />
          <div>
            <p>Não foi possível carregar as respostas agora.</p>
            <button type="button" onClick={() => refetch()}>
              Tentar novamente
            </button>
          </div>
        </div>
      )}

      {!isLoading && !isError && submissions.length === 0 && (
        <div className="lead-portal-form-responses__state lead-portal-form-responses__state--empty">
          <Inbox size={20} />
          <div>
            <p>Nenhum formulário enviado ainda.</p>
            <span>Assim que um lead completar o fluxo, os detalhes aparecerão aqui.</span>
          </div>
        </div>
      )}

      {!isLoading && !isError && submissions.length > 0 && (
        <ul className="lead-portal-form-responses__list">
          {submissions.map((submission) => (
            <li key={submission.id} className="lead-portal-form-responses__card">
              <div className="lead-portal-form-responses__card-header">
                <div>
                  <p className="lead-portal-form-responses__flow">{buildFlowLabel(submission)}</p>
                  <p className="lead-portal-form-responses__meta">
                    <Clock size={14} />
                    <span>{formatDateTime(submission.submittedAt)}</span>
                    {buildExperimentLabel(submission) && (
                      <span className="lead-portal-form-responses__experiment">
                        {buildExperimentLabel(submission)}
                      </span>
                    )}
                  </p>
                </div>
                <div className="lead-portal-form-responses__contacts">
                  {submission.name && (
                    <span title="Nome do lead">
                      <User size={14} />
                      {submission.name}
                    </span>
                  )}
                  {submission.email && (
                    <a href={`mailto:${submission.email}`} title="Enviar e-mail">
                      <Mail size={14} />
                      {submission.email}
                    </a>
                  )}
                  {submission.phone && (
                    <a href={`tel:${submission.phone}`} title="Ligar ou enviar mensagem">
                      <Phone size={14} />
                      {submission.phone}
                    </a>
                  )}
                </div>
              </div>

              <div className="lead-portal-form-responses__answers">{renderAnswers(submission)}</div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
