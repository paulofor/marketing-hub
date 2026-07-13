import { FormEvent, useId, useState } from "react";
import { Send } from "lucide-react";
import "./FashionChatPage.css";

type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  mode?: string;
  sandboxId?: string;
};

const serviceUrl =
  import.meta.env.VITE_FASHION_CHAT_SERVICE_URL || "http://191.252.210.83:8094";

const nameCandidates = [
  "Mia Estilo",
  "Clara Look",
  "Aura Moda",
  "Lia Closet",
  "Bella Combina",
];

function normalizeFashionText(value: string) {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function FashionSuggestionSketch({ suggestion }: { suggestion: string }) {
  const titleId = useId();
  const lowerSuggestion = normalizeFashionText(suggestion);
  const hasDress = lowerSuggestion.includes("vestido");
  const hasCoat =
    lowerSuggestion.includes("casaco") ||
    lowerSuggestion.includes("terceira peca") ||
    lowerSuggestion.includes("blazer");
  const hasBoot =
    lowerSuggestion.includes("bota") || lowerSuggestion.includes("inverno");
  const captionParts = [
    hasDress ? "vestido como peca principal" : "base bem ajustada",
    hasCoat ? "terceira peca leve" : "proporcao limpa",
    hasBoot ? "acabamento de inverno" : "acessorios discretos",
  ];

  return (
    <figure
      className="fashion-chat-sketch"
      aria-label="Croqui ilustrativo da sugestao de look"
    >
      <svg
        className="fashion-chat-sketch-art"
        viewBox="0 0 360 420"
        role="img"
        aria-labelledby={titleId}
      >
        <title id={titleId}>
          Desenho de estilista mostrando a sugestao de look
        </title>
        <rect
          className="fashion-chat-sketch-paper"
          x="8"
          y="8"
          width="344"
          height="404"
          rx="4"
        />
        <path
          className="fashion-chat-sketch-shadow"
          d="M135 375 C170 390 220 388 250 374"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M181 58 C166 62 158 77 161 94 C164 113 181 119 196 109 C209 100 211 78 200 66 C196 61 189 57 181 58 Z"
        />
        <path
          className="fashion-chat-sketch-hair"
          d="M160 88 C157 63 173 47 192 55 C213 63 216 90 202 111 C204 92 195 78 180 74 C169 72 162 77 160 88 Z"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M181 118 C178 145 178 176 181 204"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M181 204 C171 248 159 296 151 357"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M183 204 C201 248 214 297 230 359"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M161 145 C129 163 106 190 86 222"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M202 145 C233 166 255 193 272 225"
        />
        {hasDress ? (
          <path
            className="fashion-chat-sketch-garment"
            d="M152 142 L211 142 C222 202 236 256 255 318 C214 337 172 337 132 318 C147 257 152 204 152 142 Z"
          />
        ) : (
          <>
            <path
              className="fashion-chat-sketch-garment"
              d="M151 142 C170 133 194 133 213 142 L207 215 C187 225 166 224 145 215 Z"
            />
            <path
              className="fashion-chat-sketch-garment"
              d="M147 224 C166 232 188 232 207 224 L225 322 C197 334 170 333 139 322 Z"
            />
          </>
        )}
        {hasCoat ? (
          <>
            <path
              className="fashion-chat-sketch-coat"
              d="M138 142 C122 184 116 244 118 319 C133 329 148 331 164 324 C159 263 160 197 171 143 Z"
            />
            <path
              className="fashion-chat-sketch-coat"
              d="M224 143 C241 185 249 245 250 319 C236 329 220 331 205 324 C208 260 205 196 193 143 Z"
            />
            <path
              className="fashion-chat-sketch-detail"
              d="M171 150 C178 176 183 201 181 224"
            />
            <path
              className="fashion-chat-sketch-detail"
              d="M193 150 C188 177 185 201 186 224"
            />
          </>
        ) : null}
        <path
          className="fashion-chat-sketch-detail"
          d="M151 181 C170 189 195 189 214 181"
        />
        <path
          className="fashion-chat-sketch-detail"
          d="M142 246 C172 258 217 258 242 244"
        />
        {hasBoot ? (
          <>
            <path
              className="fashion-chat-sketch-accessory"
              d="M133 354 L159 354 L158 386 L124 386 C124 375 128 364 133 354 Z"
            />
            <path
              className="fashion-chat-sketch-accessory"
              d="M219 356 L245 356 L253 386 L221 386 C216 374 216 365 219 356 Z"
            />
          </>
        ) : (
          <>
            <path
              className="fashion-chat-sketch-accessory"
              d="M134 357 C145 363 154 364 163 359 L158 382 L123 382 C124 373 128 365 134 357 Z"
            />
            <path
              className="fashion-chat-sketch-accessory"
              d="M220 359 C230 364 239 364 247 359 L258 382 L222 382 C218 373 217 365 220 359 Z"
            />
          </>
        )}
        <path
          className="fashion-chat-sketch-note-line"
          d="M249 93 C280 77 307 77 323 91"
        />
        <path
          className="fashion-chat-sketch-note-line"
          d="M250 122 C285 113 313 116 332 133"
        />
        <path
          className="fashion-chat-sketch-note-line"
          d="M51 291 C77 276 103 277 124 291"
        />
        <circle className="fashion-chat-sketch-pin" cx="246" cy="94" r="4" />
        <circle className="fashion-chat-sketch-pin" cx="248" cy="123" r="4" />
        <circle className="fashion-chat-sketch-pin" cx="127" cy="291" r="4" />
      </svg>
      <figcaption className="fashion-chat-sketch-caption">
        Croqui da sugestao: {captionParts.join(", ")}.
      </figcaption>
    </figure>
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
      const response = await fetch(`${serviceUrl}/api/fashion-chat/messages`, {
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
                  {message.role === "assistant" ? (
                    <FashionSuggestionSketch suggestion={message.text} />
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
