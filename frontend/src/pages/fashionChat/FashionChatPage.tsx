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

function FashionSuggestionSketch({ suggestion }: { suggestion: string }) {
  const titleId = useId();
  const lowerSuggestion = normalizeFashionText(suggestion);
  const hasDress =
    lowerSuggestion.includes("vestido") ||
    lowerSuggestion.includes("evento") ||
    lowerSuggestion.includes("festa");
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
          className="fashion-chat-sketch-board-line"
          d="M42 39 C90 28 151 29 206 38"
        />
        <path
          className="fashion-chat-sketch-board-line"
          d="M261 45 C289 36 318 39 337 54"
        />
        <path
          className="fashion-chat-sketch-shadow"
          d="M115 381 C157 397 226 396 270 379"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M181 42 C164 46 155 64 158 84 C161 105 178 116 195 106 C211 96 214 70 202 56 C197 49 189 42 181 42 Z"
        />
        <path
          className="fashion-chat-sketch-hair"
          d="M158 83 C151 55 171 33 195 45 C220 57 225 90 202 114 C205 88 194 69 178 64 C166 62 159 70 158 83 Z"
        />
        <path
          className="fashion-chat-sketch-face-detail"
          d="M178 78 C182 76 187 76 191 79 M181 93 C185 96 190 96 194 92"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M181 105 C177 143 177 181 181 217"
        />
        <path
          className="fashion-chat-sketch-detail"
          d="M169 118 C178 125 192 125 202 118"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M181 217 C169 262 151 313 135 374"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M184 217 C204 262 225 314 249 375"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M160 133 C128 154 102 186 76 229"
        />
        <path
          className="fashion-chat-sketch-line"
          d="M204 133 C238 154 266 188 289 231"
        />
        {hasDress ? (
          <>
            <path
              className="fashion-chat-sketch-garment"
              d="M151 129 C169 117 197 117 216 130 C219 183 237 252 280 354 C231 380 166 381 104 354 C135 258 149 191 151 129 Z"
            />
            <path
              className="fashion-chat-sketch-color-wash"
              d="M154 153 C172 164 196 165 214 153 C222 213 241 279 262 343 C221 363 172 364 125 343 C143 276 151 211 154 153 Z"
            />
            <path
              className="fashion-chat-sketch-floral"
              d="M160 178 C170 166 182 167 185 181 C177 189 167 189 160 178 Z M205 219 C216 207 229 210 231 224 C221 232 211 230 205 219 Z M146 292 C158 278 173 281 175 296 C164 305 153 302 146 292 Z M220 316 C232 305 246 309 247 324 C235 332 225 328 220 316 Z"
            />
            <path
              className="fashion-chat-sketch-detail"
              d="M151 164 C172 174 196 174 216 163 M135 254 C174 274 223 274 253 252 M122 340 C169 360 225 360 267 340"
            />
          </>
        ) : (
          <>
            <path
              className="fashion-chat-sketch-garment"
              d="M149 130 C170 120 197 120 218 130 L210 218 C188 230 164 229 141 217 Z"
            />
            <path
              className="fashion-chat-sketch-garment"
              d="M139 226 C164 236 190 236 213 226 L237 337 C203 353 164 352 124 336 Z"
            />
            <path
              className="fashion-chat-sketch-color-wash"
              d="M153 146 C170 139 193 139 209 146 L205 206 C186 214 168 214 149 206 Z"
            />
          </>
        )}
        {hasCoat ? (
          <>
            <path
              className="fashion-chat-sketch-coat"
              d="M136 130 C117 181 107 253 108 338 C126 349 146 350 164 341 C157 270 159 197 171 131 Z"
            />
            <path
              className="fashion-chat-sketch-coat"
              d="M225 131 C248 181 260 254 262 338 C245 349 224 350 204 341 C210 270 206 196 193 131 Z"
            />
            <path
              className="fashion-chat-sketch-detail"
              d="M171 140 C178 172 183 202 181 231"
            />
            <path
              className="fashion-chat-sketch-detail"
              d="M193 140 C188 172 185 202 186 231"
            />
          </>
        ) : null}
        <path
          className="fashion-chat-sketch-detail"
          d="M149 168 C171 177 195 177 217 168"
        />
        <path
          className="fashion-chat-sketch-detail"
          d="M126 254 C165 270 222 270 252 252"
        />
        {hasBoot ? (
          <>
            <path
              className="fashion-chat-sketch-accessory"
              d="M119 365 L149 365 L148 397 L108 397 C108 385 112 374 119 365 Z"
            />
            <path
              className="fashion-chat-sketch-accessory"
              d="M238 366 L266 366 L276 397 L240 397 C234 385 233 374 238 366 Z"
            />
          </>
        ) : (
          <>
            <path
              className="fashion-chat-sketch-accessory"
              d="M119 368 C132 375 143 375 153 369 L148 394 L106 394 C108 384 112 375 119 368 Z"
            />
            <path
              className="fashion-chat-sketch-accessory"
              d="M238 370 C249 376 260 376 269 370 L282 394 L241 394 C236 384 235 376 238 370 Z"
            />
          </>
        )}
        <path
          className="fashion-chat-sketch-pencil"
          d="M55 333 L96 292 L106 302 L65 343 Z"
        />
        <path
          className="fashion-chat-sketch-pencil-tip"
          d="M96 292 L112 286 L106 302 Z"
        />
        <path
          className="fashion-chat-sketch-note-line"
          d="M249 88 C280 72 310 73 330 88"
        />
        <path
          className="fashion-chat-sketch-note-line"
          d="M251 118 C287 107 316 111 337 129"
        />
        <path
          className="fashion-chat-sketch-note-line"
          d="M43 269 C70 255 101 257 126 274"
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

export function shouldRenderFashionSketch(message: ChatMessage) {
  if (message.role !== "assistant") {
    return false;
  }
  const normalized = normalizeFashionText(message.text);
  return (
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
                  {shouldRenderFashionSketch(message) ? (
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
