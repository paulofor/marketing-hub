import { FormEvent, useEffect, useId, useRef, useState } from "react";
import type {
  ConsultantConversationMessage,
  ConsultantTransport,
} from "./types";
import "./consultant-chat.css";

const DEFAULT_MAX_IMAGE_BYTES = 15 * 1024 * 1024;
const ACCEPTED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

export type ConsultantChatProps = {
  consultantName: string;
  greeting: string;
  transport: ConsultantTransport;
  initialMessages?: ConsultantConversationMessage[];
  photoPrompt?: string;
  submitLabel?: string;
  maxImageBytes?: number;
};

/**
 * Experiência conversacional mobile-first que delega identidade, memória e execução ao backend PDE.
 */
export function ConsultantChat({
  consultantName,
  greeting,
  transport,
  initialMessages = [],
  photoPrompt = "Adicionar foto",
  submitLabel = "Pedir orientação",
  maxImageBytes = DEFAULT_MAX_IMAGE_BYTES,
}: ConsultantChatProps) {
  const inputId = useId();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const retainedImageUrls = useRef<string[]>([]);
  const [messages, setMessages] = useState<ConsultantConversationMessage[]>([
    {
      id: "consultant-greeting",
      role: "CONSULTANT",
      text: greeting,
    },
    ...initialMessages,
  ]);
  const [draft, setDraft] = useState("");
  const [image, setImage] = useState<File>();
  const [imageConsent, setImageConsent] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(
    () => () => {
      retainedImageUrls.current.forEach((url) => URL.revokeObjectURL(url));
    },
    [],
  );

  function selectImage(file?: File) {
    setError(undefined);
    setImageConsent(false);
    if (!file) {
      setImage(undefined);
      return;
    }
    if (!ACCEPTED_IMAGE_TYPES.has(file.type)) {
      setImage(undefined);
      setError("Envie uma imagem JPEG, PNG ou WebP.");
      return;
    }
    if (file.size <= 0 || file.size > maxImageBytes) {
      setImage(undefined);
      setError("A imagem excede o limite configurado para este produto.");
      return;
    }
    setImage(file);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    const message = draft.trim();
    if (!message && !image) {
      setError("Escreva uma mensagem ou adicione uma foto.");
      return;
    }
    if (image && !imageConsent) {
      setError("Autorize o uso da foto para esta orientação.");
      return;
    }

    setPending(true);
    setError(undefined);
    try {
      const response = await transport({ message, image, imageConsent });
      const interactionId = crypto.randomUUID();
      const imageUrl = image ? URL.createObjectURL(image) : undefined;
      if (imageUrl) retainedImageUrls.current.push(imageUrl);
      setMessages((current) => [
        ...current,
        {
          id: `${interactionId}-customer`,
          role: "CUSTOMER",
          text: message || "Foto enviada para análise.",
          imageUrl,
        },
        {
          id: `${interactionId}-consultant`,
          role: "CONSULTANT",
          text: response.message,
          recommendation: response.recommendation,
          why: response.why,
          nextQuestion: response.nextQuestion,
          blocker: response.blocker,
        },
      ]);
      setDraft("");
      setImage(undefined);
      setImageConsent(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    } catch {
      setError(
        "Não foi possível concluir agora. Sua mensagem e foto continuam aqui para tentar novamente.",
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <section
      className="pde-consultant"
      aria-label={`Conversa com ${consultantName}`}
    >
      <header className="pde-consultant__header">
        <span className="pde-consultant__presence" aria-hidden="true" />
        <div>
          <strong>{consultantName}</strong>
          <span>Consultoria personalizada</span>
        </div>
      </header>

      <div
        className="pde-consultant__messages"
        aria-live="polite"
        aria-busy={pending}
      >
        {messages.map((message) => (
          <article
            className={`pde-consultant__message pde-consultant__message--${message.role.toLowerCase()}`}
            key={message.id}
          >
            {message.imageUrl && (
              <img src={message.imageUrl} alt="Foto enviada nesta conversa" />
            )}
            <p>{message.text}</p>
            {message.recommendation && (
              <div className="pde-consultant__recommendation">
                <strong>Minha recomendação</strong>
                <p>{message.recommendation}</p>
                {message.why && <small>{message.why}</small>}
              </div>
            )}
            {message.blocker?.blocked && (
              <div className="pde-consultant__blocker" role="alert">
                <strong>Preciso pausar aqui</strong>
                {message.blocker.reason && <p>{message.blocker.reason}</p>}
                {message.blocker.userGuidance && (
                  <p>{message.blocker.userGuidance}</p>
                )}
                {message.blocker.helpLinks.map((link) => (
                  <a href={link} key={link} target="_blank" rel="noreferrer">
                    Abrir orientação
                  </a>
                ))}
              </div>
            )}
            {message.nextQuestion && <p>{message.nextQuestion}</p>}
          </article>
        ))}
        {pending && (
          <div className="pde-consultant__thinking" role="status">
            {consultantName} está analisando com cuidado…
          </div>
        )}
      </div>

      <form className="pde-consultant__composer" onSubmit={submit}>
        <label htmlFor={inputId}>Conte o que você precisa agora</label>
        <textarea
          id={inputId}
          rows={3}
          maxLength={20_000}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Ex.: tenho uma reunião hoje e estou em dúvida entre duas opções"
          disabled={pending}
        />

        <div className="pde-consultant__actions">
          <label className="pde-consultant__photo">
            <span>{photoPrompt}</span>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              capture="environment"
              disabled={pending}
              onChange={(event) => selectImage(event.target.files?.[0])}
            />
          </label>
          <button type="submit" disabled={pending}>
            {pending ? "Analisando…" : submitLabel}
          </button>
        </div>

        {image && (
          <div className="pde-consultant__selected-image">
            <p className="pde-consultant__file">
              Foto selecionada: <strong>{image.name}</strong>
            </p>
            <label className="pde-consultant__consent">
              <input
                type="checkbox"
                checked={imageConsent}
                disabled={pending}
                onChange={(event) => setImageConsent(event.target.checked)}
              />
              Autorizo usar esta foto somente para esta orientação.
            </label>
          </div>
        )}
        {error && (
          <p className="pde-consultant__error" role="alert">
            {error}
          </p>
        )}
        <small className="pde-consultant__privacy">
          O backend PDE deve registrar o consentimento, isolar a mídia por
          cliente e respeitar a política de retenção do produto.
        </small>
      </form>
    </section>
  );
}
