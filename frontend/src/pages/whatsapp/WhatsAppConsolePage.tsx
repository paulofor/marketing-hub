import { useEffect, useMemo, useState } from "react";
import { useWhatsAppAccounts } from "../../api/whatsapp/useWhatsAppAccounts";
import { useSaveWhatsAppAccount } from "../../api/whatsapp/useSaveWhatsAppAccount";
import { useSendWhatsAppMessage } from "../../api/whatsapp/useSendWhatsAppMessage";
import { useWhatsAppMessages } from "../../api/whatsapp/useWhatsAppMessages";
import type {
  SaveWhatsAppAccountInput,
  SendWhatsAppMessageInput,
  WhatsAppMessage,
  WhatsAppMessageDirection,
  WhatsAppMessageType,
} from "../../api/whatsapp/types";

interface AccountFormState {
  id?: number;
  displayName: string;
  phoneNumber: string;
  phoneNumberId: string;
  businessAccountId: string;
  accessToken: string;
  verifyToken: string;
  baseUrl: string;
  active: boolean;
}

const DEFAULT_ACCOUNT_FORM: AccountFormState = {
  displayName: "",
  phoneNumber: "",
  phoneNumberId: "",
  businessAccountId: "",
  accessToken: "",
  verifyToken: "",
  baseUrl: "",
  active: true,
};

const MESSAGE_TYPES: { value: WhatsAppMessageType; label: string }[] = [
  { value: "TEXT", label: "Texto" },
  { value: "IMAGE", label: "Imagem" },
];

const DIRECTION_OPTIONS: {
  value: WhatsAppMessageDirection | "ALL";
  label: string;
}[] = [
  { value: "ALL", label: "Todas" },
  { value: "OUTBOUND", label: "Saída" },
  { value: "INBOUND", label: "Entrada" },
];

function formatDate(value: string | null | undefined) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function renderJsonSnippet(payload?: string | null) {
  if (!payload) {
    return null;
  }
  try {
    const parsed = JSON.parse(payload);
    return JSON.stringify(parsed, null, 2);
  } catch (error) {
    return payload;
  }
}

export default function WhatsAppConsolePage() {
  const { data: accountsData, isLoading: accountsLoading } = useWhatsAppAccounts();
  const saveAccount = useSaveWhatsAppAccount();
  const sendMessage = useSendWhatsAppMessage();

  const [accountForm, setAccountForm] = useState<AccountFormState>(
    DEFAULT_ACCOUNT_FORM,
  );
  const [messageForm, setMessageForm] = useState<SendWhatsAppMessageInput>({
    to: "",
    type: "TEXT",
    textBody: "",
    imageUrl: "",
    caption: "",
  });
  const [directionFilter, setDirectionFilter] = useState<
    WhatsAppMessageDirection | "ALL"
  >("ALL");
  const [page, setPage] = useState(0);

  const {
    data: messagePage,
    isLoading: messagesLoading,
    isFetching: messagesFetching,
  } = useWhatsAppMessages({ page, size: 25, direction: directionFilter });

  const messages: WhatsAppMessage[] = messagePage?.content ?? [];
  const totalPages = messagePage?.totalPages ?? 0;

  useEffect(() => {
    if (accountsData && accountsData.length > 0) {
      const account = accountsData[0];
      setAccountForm({
        id: account.id,
        displayName: account.displayName,
        phoneNumber: account.phoneNumber ?? "",
        phoneNumberId: account.phoneNumberId,
        businessAccountId: account.businessAccountId ?? "",
        accessToken: account.accessToken ?? "",
        verifyToken: account.verifyToken ?? "",
        baseUrl: account.baseUrl ?? "",
        active: account.active,
      });
    } else if (!accountsLoading && !saveAccount.isPending) {
      setAccountForm(DEFAULT_ACCOUNT_FORM);
    }
  }, [accountsData, accountsLoading, saveAccount.isPending]);

  useEffect(() => {
    if (sendMessage.isSuccess) {
      setMessageForm((current) => ({
        ...current,
        textBody: "",
        imageUrl: "",
        caption: "",
      }));
    }
  }, [sendMessage.isSuccess]);

  const handleAccountInputChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const { name, value, type, checked } = event.target;
    setAccountForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleAccountSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const payload: SaveWhatsAppAccountInput = {
      id: accountForm.id,
      displayName: accountForm.displayName.trim(),
      phoneNumber: accountForm.phoneNumber.trim() || undefined,
      phoneNumberId: accountForm.phoneNumberId.trim(),
      businessAccountId: accountForm.businessAccountId.trim() || undefined,
      accessToken: accountForm.accessToken.trim() || undefined,
      verifyToken: accountForm.verifyToken.trim() || undefined,
      baseUrl: accountForm.baseUrl.trim() || undefined,
      active: accountForm.active,
    };
    saveAccount.mutate(payload);
  };

  const handleMessageInputChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = event.target;
    setMessageForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleMessageTypeChange = (
    event: React.ChangeEvent<HTMLSelectElement>,
  ) => {
    const nextType = event.target.value as WhatsAppMessageType;
    setMessageForm((current) => ({
      ...current,
      type: nextType,
    }));
  };

  const handleSendMessage = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    sendMessage.mutate({
      ...messageForm,
      to: messageForm.to.trim(),
      textBody: messageForm.textBody?.trim() || undefined,
      imageUrl: messageForm.imageUrl?.trim() || undefined,
      caption: messageForm.caption?.trim() || undefined,
    });
  };

  const handleDirectionChange = (
    event: React.ChangeEvent<HTMLSelectElement>,
  ) => {
    setDirectionFilter(event.target.value as WhatsAppMessageDirection | "ALL");
    setPage(0);
  };

  const canGoPrevious = page > 0;
  const canGoNext = totalPages > 0 && page < totalPages - 1;

  const isImageMessage = messageForm.type === "IMAGE";

  const emptyState = useMemo(() => {
    if (messagesLoading) {
      return null;
    }
    if (messages.length === 0) {
      return (
        <div className="p-4 text-center text-muted">
          Nenhuma mensagem registrada até o momento.
        </div>
      );
    }
    return null;
  }, [messagesLoading, messages]);

  return (
    <div className="container py-4">
      <h1 className="mb-4">Console do WhatsApp</h1>
      <div className="row g-4">
        <div className="col-lg-5">
          <div className="card mb-4">
            <div className="card-header">
              <h2 className="h5 mb-0">Configuração da conta</h2>
            </div>
            <form onSubmit={handleAccountSubmit} className="card-body">
              <div className="mb-3">
                <label className="form-label">Nome de exibição *</label>
                <input
                  name="displayName"
                  type="text"
                  className="form-control"
                  value={accountForm.displayName}
                  onChange={handleAccountInputChange}
                  required
                  disabled={saveAccount.isPending}
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Telefone comercial</label>
                <input
                  name="phoneNumber"
                  type="text"
                  className="form-control"
                  value={accountForm.phoneNumber}
                  onChange={handleAccountInputChange}
                  placeholder="Ex.: +5511987654321"
                  disabled={saveAccount.isPending}
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Phone Number ID *</label>
                <input
                  name="phoneNumberId"
                  type="text"
                  className="form-control"
                  value={accountForm.phoneNumberId}
                  onChange={handleAccountInputChange}
                  required
                  disabled={saveAccount.isPending}
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Business Account ID</label>
                <input
                  name="businessAccountId"
                  type="text"
                  className="form-control"
                  value={accountForm.businessAccountId}
                  onChange={handleAccountInputChange}
                  disabled={saveAccount.isPending}
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Access Token *</label>
                <input
                  name="accessToken"
                  type="password"
                  className="form-control"
                  value={accountForm.accessToken}
                  onChange={handleAccountInputChange}
                  required
                  disabled={saveAccount.isPending}
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Verify Token</label>
                <input
                  name="verifyToken"
                  type="text"
                  className="form-control"
                  value={accountForm.verifyToken}
                  onChange={handleAccountInputChange}
                  placeholder="Utilizado para validar o webhook"
                  disabled={saveAccount.isPending}
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Base URL</label>
                <input
                  name="baseUrl"
                  type="text"
                  className="form-control"
                  value={accountForm.baseUrl}
                  onChange={handleAccountInputChange}
                  placeholder="https://graph.facebook.com/v18.0"
                  disabled={saveAccount.isPending}
                />
                <div className="form-text">
                  Deixe em branco para usar a URL padrão da Meta.
                </div>
              </div>
              <div className="form-check form-switch mb-3">
                <input
                  id="whatsapp-account-active"
                  name="active"
                  className="form-check-input"
                  type="checkbox"
                  checked={accountForm.active}
                  onChange={handleAccountInputChange}
                  disabled={saveAccount.isPending}
                />
                <label className="form-check-label" htmlFor="whatsapp-account-active">
                  Conta ativa
                </label>
              </div>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={saveAccount.isPending}
              >
                {saveAccount.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" role="status" />
                )}
                Salvar conta
              </button>
            </form>
          </div>

          <div className="card">
            <div className="card-header">
              <h2 className="h5 mb-0">Enviar mensagem</h2>
            </div>
            <form onSubmit={handleSendMessage} className="card-body">
              <div className="mb-3">
                <label className="form-label">Destino *</label>
                <input
                  name="to"
                  type="text"
                  className="form-control"
                  value={messageForm.to}
                  onChange={handleMessageInputChange}
                  required
                  disabled={sendMessage.isPending}
                  placeholder="Ex.: +5511987654321"
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Tipo de mensagem *</label>
                <select
                  name="type"
                  className="form-select"
                  value={messageForm.type}
                  onChange={handleMessageTypeChange}
                  disabled={sendMessage.isPending}
                >
                  {MESSAGE_TYPES.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              {isImageMessage ? (
                <>
                  <div className="mb-3">
                    <label className="form-label">URL da imagem *</label>
                    <input
                      name="imageUrl"
                      type="url"
                      className="form-control"
                      value={messageForm.imageUrl ?? ""}
                      onChange={handleMessageInputChange}
                      required
                      disabled={sendMessage.isPending}
                      placeholder="https://"
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Legenda</label>
                    <input
                      name="caption"
                      type="text"
                      className="form-control"
                      value={messageForm.caption ?? ""}
                      onChange={handleMessageInputChange}
                      disabled={sendMessage.isPending}
                    />
                  </div>
                </>
              ) : (
                <div className="mb-3">
                  <label className="form-label">Mensagem *</label>
                  <textarea
                    name="textBody"
                    className="form-control"
                    rows={4}
                    value={messageForm.textBody ?? ""}
                    onChange={handleMessageInputChange}
                    required
                    disabled={sendMessage.isPending}
                  />
                </div>
              )}
              <button
                type="submit"
                className="btn btn-success"
                disabled={sendMessage.isPending}
              >
                {sendMessage.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" role="status" />
                )}
                Enviar mensagem
              </button>
            </form>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-header d-flex flex-wrap gap-3 justify-content-between align-items-center">
              <div>
                <h2 className="h5 mb-0">Mensagens</h2>
                <p className="text-muted mb-0">
                  Histórico de recebimento e envio pelo WhatsApp Cloud API.
                </p>
              </div>
              <div className="d-flex align-items-center gap-2">
                <label className="form-label mb-0" htmlFor="whatsapp-direction-filter">
                  Direção
                </label>
                <select
                  id="whatsapp-direction-filter"
                  className="form-select form-select-sm"
                  value={directionFilter}
                  onChange={handleDirectionChange}
                >
                  {DIRECTION_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="card-body p-0">
              {messagesLoading ? (
                <div className="p-4 text-center">
                  <div className="spinner-border" role="status" />
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead className="table-light">
                      <tr>
                        <th style={{ minWidth: "160px" }}>Data</th>
                        <th>Direção</th>
                        <th>Tipo</th>
                        <th>Contato</th>
                        <th>Conteúdo</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {messages.map((message) => (
                        <MessageRow key={message.id} message={message} />
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {emptyState}
            </div>
            <div className="card-footer d-flex justify-content-between align-items-center">
              <button
                className="btn btn-outline-secondary btn-sm"
                type="button"
                onClick={() => setPage((current) => Math.max(current - 1, 0))}
                disabled={!canGoPrevious || messagesFetching}
              >
                Anterior
              </button>
              <span className="text-muted">
                Página {totalPages === 0 ? 0 : page + 1} de {totalPages}
              </span>
              <button
                className="btn btn-outline-secondary btn-sm"
                type="button"
                onClick={() =>
                  setPage((current) => (canGoNext ? current + 1 : current))
                }
                disabled={!canGoNext || messagesFetching}
              >
                Próxima
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

interface MessageRowProps {
  message: WhatsAppMessage;
}

function MessageRow({ message }: MessageRowProps) {
  const timestamp = formatDate(
    message.messageTimestamp ?? message.sentAt ?? message.receivedAt ?? message.createdAt,
  );
  const directionLabel = message.direction === "OUTBOUND" ? "Saída" : "Entrada";
  const badgeClass = message.direction === "OUTBOUND" ? "bg-success" : "bg-info";
  const contact = message.direction === "OUTBOUND" ? message.toNumber : message.fromNumber;
  const status = message.status ?? "-";
  const payloadSnippet = renderJsonSnippet(
    message.statusPayloadJson ?? message.payloadJson ?? message.contextJson,
  );

  return (
    <tr>
      <td>{timestamp}</td>
      <td>
        <span className={`badge ${badgeClass}`}>{directionLabel}</span>
      </td>
      <td>{message.messageType ?? "-"}</td>
      <td>{contact ?? "-"}</td>
      <td style={{ whiteSpace: "pre-wrap" }}>
        {message.imageUrl ? (
          <div>
            <a href={message.imageUrl} target="_blank" rel="noreferrer">
              Abrir imagem
            </a>
            <div className="mt-2">
              <img
                src={message.imageUrl}
                alt={message.caption ?? "Imagem do WhatsApp"}
                className="img-fluid rounded"
                style={{ maxWidth: "140px" }}
              />
            </div>
            {message.caption && (
              <p className="small text-muted mt-2 mb-0">{message.caption}</p>
            )}
          </div>
        ) : (
          message.textBody ?? "-"
        )}
        {payloadSnippet && (
          <details className="mt-2">
            <summary>Payload</summary>
            <pre className="small bg-light p-2 rounded mt-1 mb-0">
              {payloadSnippet}
            </pre>
          </details>
        )}
      </td>
      <td>
        <div>{status}</div>
        {message.errorMessage && (
          <div className="text-danger small">{message.errorMessage}</div>
        )}
      </td>
    </tr>
  );
}
