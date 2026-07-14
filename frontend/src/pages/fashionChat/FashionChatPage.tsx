import { FormEvent, useState } from "react";
import { Send } from "lucide-react";
import "./FashionChatPage.css";

type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  shouldGenerateImage?: boolean;
  visualBrief?: string;
  imagePrompt?: string;
  imageUrl?: string;
  imageError?: string;
  mode?: string;
  sandboxId?: string;
};

const nameCandidates = [
  "Mia Estilo",
  "Clara Look",
  "Aura Moda",
  "Lia Closet",
  "Bella Combina",
];

export const fashionChatMessageEndpoint = "/api/fashion-chat/messages";

function normalizeFashionText(value: string) {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function FashionGeneratedVisual({ message }: { message: ChatMessage }) {
  if (!message.imageUrl && !message.visualBrief && !message.imagePrompt) {
    return null;
  }

  return (
    <figure className="fashion-chat-visual">
      {message.imageUrl ? (
        <img
          className="fashion-chat-visual-image"
          src={message.imageUrl}
          alt={message.visualBrief || "Imagem de moda gerada para a conversa"}
        />
      ) : null}
      {message.visualBrief ? (
        <figcaption className="fashion-chat-visual-caption">
          {message.visualBrief}
        </figcaption>
      ) : null}
      {!message.imageUrl && message.imagePrompt ? (
        <details className="fashion-chat-visual-prompt">
          <summary>Prompt da imagem contextual</summary>
          <p>{message.imagePrompt}</p>
        </details>
      ) : null}
      {!message.imageUrl && message.imageError ? (
        <p className="fashion-chat-visual-error">
          A imagem contextual nao foi gerada agora: {message.imageError}
        </p>
      ) : null}
    </figure>
  );
}

export function shouldRenderFashionVisual(message: ChatMessage) {
  if (message.role !== "assistant") {
    return false;
  }
  const normalized = normalizeFashionText(message.text);
  return (
    Boolean(message.imageUrl || message.visualBrief || message.imagePrompt) &&
    !normalized.includes("nao consegui acionar") &&
    !normalized.includes("erro de conexao") &&
    !normalized.includes("http 502")
  );
}

export function createMessageId() {
  if (
    typeof crypto !== "undefined" &&
    typeof crypto.randomUUID === "function"
  ) {
    return crypto.randomUUID();
  }

  return `msg-${Date.now().toString(36)}-${Math.random()
    .toString(36)
    .slice(2, 10)}`;
}

export default function FashionChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [status, setStatus] = useState("Pronto para testar");

  async function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const text = input.trim();
    if (!text || isSending) {
      return;
    }

    const userMessage: ChatMessage = {
      id: createMessageId(),
      role: "user",
      text,
    };
    setMessages((current) => [...current, userMessage]);
    setInput("");
    setIsSending(true);
    setStatus("Consultando especialista");

    try {
      const response = await fetch(fashionChatMessageEndpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          customerId: "marketing-hub-pilot",
          message: text,
        }),
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const payload = await response.json();
      setMessages((current) => [
        ...current,
        {
          id: createMessageId(),
          role: "assistant",
          text: payload.answer || "Nao foi possivel responder agora.",
          shouldGenerateImage: payload.shouldGenerateImage,
          visualBrief: payload.visualBrief,
          imagePrompt: payload.imagePrompt,
          imageUrl: payload.imageUrl,
          imageError: payload.imageError,
          mode: payload.mode,
          sandboxId: payload.sandboxId,
        },
      ]);
      setStatus(payload.mode || "Respondido");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha no chat";
      setMessages((current) => [
        ...current,
        {
          id: createMessageId(),
          role: "assistant",
          text: `Nao consegui acionar o modulo de moda agora. Detalhe: ${message}`,
        },
      ]);
      setStatus("Erro de conexao");
    } finally {
      setIsSending(false);
    }
  }

  return (
    <div className="fashion-chat-page">
      <div className="fashion-chat-header">
        <div>
          <h1 className="fashion-chat-title">Especialista em Moda</h1>
          <p className="fashion-chat-subtitle">
            Piloto de produto conversacional AI Sandbox: pergunta do cliente,
            pesquisa de moda e resposta curta para orientar decisao.
          </p>
          <div
            className="fashion-chat-name-candidates"
            aria-label="Nomes candidatos para o chat consultor de moda"
          >
            <span className="fashion-chat-name-label">Nomes em avaliacao:</span>
            {nameCandidates.map((name) => (
              <span className="fashion-chat-name-pill" key={name}>
                {name}
              </span>
            ))}
          </div>
        </div>
        <div className="fashion-chat-status">
          <strong>Status</strong>
          <div>{status}</div>
        </div>
      </div>

      <section className="fashion-chat-panel">
        <div className="fashion-chat-messages">
          {messages.length === 0 ? (
            <div className="fashion-chat-empty">
              <div>
                <strong>Digite uma pergunta de moda.</strong>
                <div>Ex.: que roupa usar em uma reuniao casual?</div>
              </div>
            </div>
          ) : (
            messages.map((message) => (
              <div
                className={`fashion-chat-message fashion-chat-message--${message.role}`}
                key={message.id}
              >
                <div className="fashion-chat-bubble">
                  {message.text}
                  {shouldRenderFashionVisual(message) ? (
                    <FashionGeneratedVisual message={message} />
                  ) : null}
                  {message.role === "assistant" &&
                  (message.mode || message.sandboxId) ? (
                    <div className="fashion-chat-meta">
                      {message.mode ? `modo: ${message.mode}` : null}
                      {message.mode && message.sandboxId ? " | " : null}
                      {message.sandboxId
                        ? `sandbox: ${message.sandboxId}`
                        : null}
                    </div>
                  ) : null}
                </div>
              </div>
            ))
          )}
        </div>

        <form className="fashion-chat-form" onSubmit={sendMessage}>
          <textarea
            className="form-control fashion-chat-input"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="Pergunte algo sobre look, ocasiao, cores, estilo ou combinacao..."
          />
          <button
            className="btn btn-primary"
            type="submit"
            disabled={isSending}
          >
            <Send size={18} aria-hidden="true" />{" "}
            {isSending ? "Enviando" : "Enviar"}
          </button>
        </form>
      </section>
    </div>
  );
}
