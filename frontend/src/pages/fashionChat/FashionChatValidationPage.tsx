import { ExternalLink, RefreshCw, ShieldCheck, ShieldX } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  FashionChatAccountStatus,
  useFashionChatValidationStatus,
  useStartFashionChatLogin,
} from "../../api/fashionChatValidation";
import "./FashionChatValidationPage.css";

const ACCOUNT_LABELS: Record<FashionChatAccountStatus, string> = {
  AUTHENTICATED: "Autenticado",
  NOT_AUTHENTICATED: "Login necessário",
  UNAVAILABLE: "Serviço indisponível",
  UNKNOWN: "Sem confirmação",
};

const ACCOUNT_BADGES: Record<FashionChatAccountStatus, string> = {
  AUTHENTICATED: "text-bg-success",
  NOT_AUTHENTICATED: "text-bg-warning",
  UNAVAILABLE: "text-bg-danger",
  UNKNOWN: "text-bg-secondary",
};

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Sem validação";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function FashionChatValidationPage() {
  const statusQuery = useFashionChatValidationStatus();
  const loginMutation = useStartFashionChatLogin();
  const status = statusQuery.data;
  const login = loginMutation.data;
  const isAuthenticated = status?.accountStatus === "AUTHENTICATED";
  const canOpenLogin = Boolean(login?.verificationUri);

  return (
    <div className="fashion-chat-validation-page">
      <div className="fashion-chat-validation-page__header">
        <div>
          <PageTitle>Validação do Chat Moda</PageTitle>
          <p className="text-muted mb-0">
            Verificação operacional da sessão ChatGPT usada pelo serviço de
            atendimento de moda.
          </p>
        </div>
        <button
          className="btn btn-outline-primary"
          type="button"
          onClick={() => statusQuery.refetch()}
          disabled={statusQuery.isFetching}
        >
          {statusQuery.isFetching ? (
            <span
              className="spinner-border spinner-border-sm"
              aria-hidden="true"
            />
          ) : (
            <RefreshCw size={18} aria-hidden="true" />
          )}
          Validar agora
        </button>
      </div>

      {statusQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível consultar o backend de validação do Chat Moda.
        </div>
      ) : null}

      <div className="row g-3 mb-4">
        <div className="col-lg-4">
          <StatusCard
            title="Serviço"
            badge={status?.ready ? "Pronto" : "Não pronto"}
            badgeClass={status?.ready ? "text-bg-success" : "text-bg-danger"}
            detail={
              status
                ? `HTTP ${status.readyHttpStatus ?? "sem resposta"}`
                : "Consultando"
            }
          />
        </div>
        <div className="col-lg-4">
          <StatusCard
            title="Conta ChatGPT"
            badge={
              status ? ACCOUNT_LABELS[status.accountStatus] : "Consultando"
            }
            badgeClass={
              status
                ? ACCOUNT_BADGES[status.accountStatus]
                : "text-bg-secondary"
            }
            detail={
              isAuthenticated
                ? "O Chat Moda pode responder usando Codex/ChatGPT."
                : "A sessão precisa ser autenticada para sair do fallback local."
            }
          />
        </div>
        <div className="col-lg-4">
          <StatusCard
            title="Última validação"
            badge={formatDateTime(status?.checkedAt)}
            badgeClass="text-bg-light"
            detail={status?.serviceBaseUrl ?? "Serviço ainda não consultado"}
          />
        </div>
      </div>

      {!isAuthenticated ? (
        <section className="fashion-chat-validation-page__panel">
          <div className="fashion-chat-validation-page__panel-header">
            <div>
              <h2>Autenticar serviço</h2>
              <p>
                Inicie o login, abra o link retornado e informe o código. Depois
                valide novamente a prontidão do serviço.
              </p>
            </div>
            <button
              className="btn btn-primary"
              type="button"
              onClick={() => loginMutation.mutate()}
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  aria-hidden="true"
                />
              ) : (
                <ShieldCheck size={18} aria-hidden="true" />
              )}
              Iniciar login
            </button>
          </div>

          {loginMutation.isError || login?.errorMessage ? (
            <div className="alert alert-danger" role="alert">
              {login?.errorMessage ?? "Falha ao iniciar o login do Chat Moda."}
            </div>
          ) : null}

          {login ? (
            <div className="fashion-chat-validation-page__login-box">
              <div>
                <span className="fashion-chat-validation-page__label">
                  Código
                </span>
                <strong>{login.userCode ?? "Não informado"}</strong>
              </div>
              <div>
                <span className="fashion-chat-validation-page__label">
                  Link de autenticação
                </span>
                {canOpenLogin ? (
                  <a
                    className="btn btn-outline-primary"
                    href={login.verificationUri ?? undefined}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <ExternalLink size={18} aria-hidden="true" />
                    Abrir login
                  </a>
                ) : (
                  <strong>Não informado</strong>
                )}
              </div>
              <div>
                <span className="fashion-chat-validation-page__label">
                  Expiração
                </span>
                <strong>
                  {login.expiresIn ? `${login.expiresIn}s` : "Não informada"}
                </strong>
              </div>
            </div>
          ) : null}
        </section>
      ) : (
        <section className="fashion-chat-validation-page__ready">
          <ShieldCheck size={22} aria-hidden="true" />
          <div>
            <strong>Chat Moda autenticado.</strong>
            <span>
              O serviço está apto a usar a sessão ChatGPT configurada.
            </span>
          </div>
        </section>
      )}

      {status?.accountError || status?.readyError ? (
        <section className="fashion-chat-validation-page__error">
          <ShieldX size={20} aria-hidden="true" />
          <div>
            <strong>Detalhe técnico retornado pelo backend</strong>
            <span>{status.accountError ?? status.readyError}</span>
          </div>
        </section>
      ) : null}
    </div>
  );
}

function StatusCard({
  title,
  badge,
  badgeClass,
  detail,
}: {
  title: string;
  badge: string;
  badgeClass: string;
  detail: string;
}) {
  return (
    <div className="fashion-chat-validation-page__card">
      <span>{title}</span>
      <strong className={`badge ${badgeClass}`}>{badge}</strong>
      <p>{detail}</p>
    </div>
  );
}
