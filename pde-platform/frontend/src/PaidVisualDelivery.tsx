import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./paid-visual.css";

type Input = {
  occasion: string;
  pieces: string[];
  preferences: string;
  constraints: string;
};
type Delivery = {
  jobId: string;
  status: string;
  input: Input;
  imageBase64?: string;
  occasionAt?: string;
};
type Conditions = {
  supportEmail?: string;
  termsUrl?: string;
  privacyUrl?: string;
  refundPolicyUrl?: string;
};
type Session = Conditions & {
  productName: string;
  productVersion: string;
  expiresAt: string;
  regenerationAvailable: boolean;
  deliveries: Delivery[];
};
type Offer = Conditions & {
  productName: string;
  selling: boolean;
  priceBrl: number;
  checkoutUrl?: string;
};
const api = "/api/pde/paid-visual/v1";

type Policy = "terms" | "privacy" | "refund";

/** Explica as condições da entrega usando o preço e o suporte do contrato publicado. */
function DeliveryPolicy({
  policy,
  offer,
  experiment,
  error,
}: {
  policy: Policy;
  offer: Offer | null;
  experiment: string;
  error: string;
}) {
  const titles = {
    terms: "Condições da entrega",
    privacy: "Privacidade da personalização",
    refund: "Cancelamento e reembolso",
  };
  const paragraphs =
    policy === "terms"
      ? [
          `Um pagamento único de ${new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(offer?.priceBrl || 0)} inclui um painel com três combinações visuais para a ocasião e as peças descritas, com justificativas práticas e uma regeneração opcional.`,
          "O acesso começa após a confirmação do pagamento e dura até sete dias, encerrando antes se chegar o horário da ocasião informado. Salve sua imagem enquanto o acesso estiver disponível. O suporte da compra permanece disponível por sete dias.",
          "As imagens são ilustrações produzidas com inteligência artificial. Não são prova de caimento no seu corpo nem reprodução exata dos tecidos. Confira se as sugestões correspondem às peças que você possui.",
          "Uma falha técnica não exige uma nova compra. Procure o suporte para recuperar a entrega ou resolver a contratação. O pagamento e o acesso pertencem ao e-mail usado na compra.",
        ]
      : policy === "privacy"
        ? [
            "Usamos o e-mail da compra, os dados do pagamento e as informações fornecidas sobre ocasião, roupas e preferências para confirmar seu direito de acesso, gerar as combinações, recuperar resultados e prestar suporte.",
            "O processamento envolve os serviços de pagamento, e-mail, armazenamento e inteligência artificial utilizados na entrega. Não envie fotos do corpo, dados de saúde, documentos ou informações identificáveis de terceiros.",
            "O navegador guarda a credencial de acesso na sessão local. Não compartilhe o link de acesso recebido por e-mail. Registros de compra, geração e reembolso são preservados para cumprimento do contrato, auditoria e obrigações aplicáveis.",
            "Para solicitar acesso, correção ou exclusão de dados quando cabível, use o contato abaixo. Consulte também a política institucional da Digicom Digital.",
          ]
        : [
            "Compras a distância preservam o direito de arrependimento no prazo legal aplicável. Iniciar a geração ou acessar a imagem não exige renúncia a esse direito.",
            "Solicite cancelamento ou relate uma falha ao suporte abaixo, informando o e-mail usado na compra e a referência do pagamento. Não envie dados completos do cartão.",
            "Se a entrega não funcionar conforme contratado, procure o suporte para correção ou reembolso, respeitados seus direitos. A confirmação de reembolso ou chargeback encerra o acesso pago e novas gerações, sem impedir atendimento ou pedidos de privacidade.",
          ];
  return (
    <main className="visual-paid">
      <a
        href={`/paid-visual.html?experiment=${encodeURIComponent(experiment)}`}
      >
        ← Voltar para a entrega
      </a>
      <h1>{titles[policy]}</h1>
      {offer ? (
        <>
          <p>{offer.productName}</p>
          {paragraphs.map((paragraph) => (
            <p key={paragraph}>{paragraph}</p>
          ))}
          <p>
            Suporte:{" "}
            <a href={"mailto:" + offer.supportEmail}>{offer.supportEmail}</a>
          </p>
          <p>
            Digicom Digital · PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA ·
            CNPJ 25.215.414/0001-69.
          </p>
          <a
            href="https://www.digicomdigital.com.br/privacy-policy/"
            target="_blank"
            rel="noreferrer"
          >
            Política de privacidade institucional
          </a>
        </>
      ) : (
        <p role="status">{error || "Carregando as condições da oferta…"}</p>
      )}
    </main>
  );
}

/** Mantém a credencial somente no fragmento recebido e na sessão local desta oferta. */
function readToken(experiment: string) {
  const fragment = new URLSearchParams(location.hash.slice(1));
  const key = "paid-visual:" + experiment;
  const token = fragment.get("access") || sessionStorage.getItem(key) || "";
  if (token) sessionStorage.setItem(key, token);
  if (fragment.has("access"))
    history.replaceState(null, "", location.pathname + location.search);
  return token;
}

/** Apresenta acesso e entrega da própria compra, sem cobrar nem gerar antes da confirmação. */
export function PaidVisualDelivery() {
  const experiment =
    new URLSearchParams(location.search).get("experiment") || "";
  const requestedPolicy = new URLSearchParams(location.search).get("policy");
  const policy = ["terms", "privacy", "refund"].includes(requestedPolicy || "")
    ? (requestedPolicy as Policy)
    : null;
  const [token, setToken] = useState(() => {
    const access = readToken(experiment);
    return policy ? "" : access;
  });
  const [offer, setOffer] = useState<Offer | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState(false);
  const [occasion, setOccasion] = useState("");
  const [pieces, setPieces] = useState("");
  const [preferences, setPreferences] = useState("");
  const [constraints, setConstraints] = useState("");
  const [occasionAt, setOccasionAt] = useState("");
  const [email, setEmail] = useState("");
  const [operation, setOperation] = useState(() => crypto.randomUUID());
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const changed = () => {
      const access = readToken(experiment);
      setToken(policy ? "" : access);
    };
    window.addEventListener("hashchange", changed);
    return () => window.removeEventListener("hashchange", changed);
  }, [experiment, policy]);
  async function request(path: string, body?: unknown) {
    const response = await fetch(api + path, {
      method: body === undefined ? "GET" : "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { "X-PDE-Access-Token": token } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const data = await response.json().catch(() => ({
      message: "Não foi possível concluir agora. Seu pedido foi preservado.",
    }));
    if (!response.ok)
      throw new Error(
        data.detail ||
          data.message ||
          "Não foi possível concluir agora. Seu pedido foi preservado.",
      );
    return data;
  }
  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;
    async function load() {
      try {
        if (token) {
          const result: Session = await request("/session");
          if (cancelled) return;
          setSession(result);
          setError("");
          if (
            result.deliveries.some((d) =>
              ["PDE_QUEUED", "PDE_RUNNING", "PDE_RAW_RECEIVED"].includes(
                d.status,
              ),
            )
          )
            timer = setTimeout(load, 4000);
        } else if (/^[0-9]+$/.test(experiment)) {
          const result = await request(`/experiments/${experiment}/offer`);
          if (!cancelled) setOffer(result);
        }
      } catch (ex) {
        if (!cancelled) {
          setSession(null);
          setError(
            ex instanceof Error ? ex.message : "Acesso indisponível agora.",
          );
        }
      }
    }
    void load();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [token, revision]);
  async function login(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const result = await request(`/experiments/${experiment}/login`, {
        email,
      });
      setMessage(result.message);
    } catch (ex) {
      setError((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const original = session?.deliveries[0];
      const result = await request("/session/submit", {
        operationKey: operation,
        regeneration: !!original,
        occasionAt: original?.occasionAt || new Date(occasionAt).toISOString(),
        input: {
          occasion,
          pieces: pieces
            .split("\n")
            .map((p) => p.trim())
            .filter(Boolean),
          preferences,
          constraints,
        },
      });
      setSession(result);
      setEditing(false);
      setRevision((x) => x + 1);
    } catch (ex) {
      setError((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function regenerate() {
    const original = session!.deliveries[0];
    setOccasion(original.input.occasion);
    setPieces(original.input.pieces.join("\n"));
    setPreferences(original.input.preferences);
    setConstraints(original.input.constraints);
    setOperation(crypto.randomUUID());
    setEditing(true);
  }
  async function reconcile(jobId: string) {
    setBusy(true);
    setError("");
    try {
      await request(`/session/${jobId}/reconcile`, {});
      setRevision((x) => x + 1);
    } catch (ex) {
      setError((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }
  if (policy)
    return (
      <DeliveryPolicy
        policy={policy}
        offer={offer}
        experiment={experiment}
        error={error}
      />
    );
  return (
    <main className="visual-paid">
      <header>
        <p className="eyebrow">ALCYONE · LOOK PARA A OCASIÃO</p>
        <h1>
          O convite chegou.
          <br />
          Escolha seu look com mais clareza.
        </h1>
        <p>
          Três combinações visuais para uma ocasião, a partir das peças que você
          já tem.
        </p>
      </header>
      {error && (
        <div role="alert" className="notice">
          {error}
          <button onClick={() => setRevision((x) => x + 1)}>
            Consultar novamente
          </button>
        </div>
      )}
      {message && (
        <p role="status" className="notice">
          {message}
        </p>
      )}
      {!session && (
        <section className="access-panel">
          <div>
            <h2>Já comprou?</h2>
            <p>
              Use o mesmo e-mail do pagamento para receber seu acesso e
              recuperar sua entrega.
            </p>
            <form onSubmit={login}>
              <label>
                E-mail da compra
                <input
                  type="email"
                  required
                  maxLength={254}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email"
                />
              </label>
              <button disabled={busy || !experiment}>
                {busy ? "Enviando…" : "Receber meu acesso"}
              </button>
            </form>
            {token && (
              <button
                className="secondary"
                onClick={() => {
                  sessionStorage.removeItem("paid-visual:" + experiment);
                  setToken("");
                  setError("");
                }}
              >
                Usar outro acesso
              </button>
            )}
          </div>
          <aside>
            <h2>Uma decisão mais fácil para sair</h2>
            <ul>
              <li>Três propostas com justificativa prática.</li>
              <li>Uma regeneração para ajustar suas preferências.</li>
              <li>
                Acesso por sete dias ou até a ocasião, o que vier primeiro.
              </li>
            </ul>
            <p>
              As imagens são visualizações ilustrativas por IA. Confira cores,
              peças e caimento reais antes de se vestir.
            </p>
            {offer?.selling && offer.checkoutUrl ? (
              <a className="primary" href={offer.checkoutUrl}>
                Escolher minhas combinações ·{" "}
                {offer.priceBrl.toLocaleString("pt-BR", {
                  style: "currency",
                  currency: "BRL",
                })}
              </a>
            ) : (
              <p className="muted">
                A oferta está em preparação. As compras ainda não foram abertas.
              </p>
            )}
          </aside>
        </section>
      )}
      {session && (
        <section>
          <div className="session-heading">
            <h2>Sua entrega</h2>
            <p>
              Acesso até {new Date(session.expiresAt).toLocaleString("pt-BR")}
            </p>
          </div>
          {session.deliveries.map((delivery, index) => (
            <article className="delivery" key={delivery.jobId}>
              <h3>
                {index === 0 ? "Suas primeiras combinações" : "Seu ajuste"}
              </h3>
              {delivery.imageBase64 && (
                <>
                  <img
                    alt={`Três combinações de roupas para ${delivery.input.occasion}`}
                    src={"data:image/png;base64," + delivery.imageBase64}
                  />
                  <a
                    download="minhas-combinacoes.png"
                    href={"data:image/png;base64," + delivery.imageBase64}
                  >
                    Baixar minhas combinações
                  </a>
                </>
              )}
              {!delivery.imageBase64 && (
                <p role="status">
                  {["PDE_QUEUED", "PDE_RUNNING", "PDE_RAW_RECEIVED"].includes(
                    delivery.status,
                  )
                    ? "Estamos preparando suas combinações. Você pode voltar por este mesmo acesso."
                    : "Sua tentativa foi preservada e precisa de conferência. Não repetimos a geração automaticamente."}
                </p>
              )}
              {["PDE_RAW_RECEIVED", "PDE_COST_PENDING"].includes(
                delivery.status,
              ) && (
                <button
                  disabled={busy}
                  onClick={() => reconcile(delivery.jobId)}
                >
                  Recuperar resultado preservado
                </button>
              )}
            </article>
          ))}
          {session.regenerationAvailable &&
            session.deliveries[0]?.status === "PDE_COMPLETED" &&
            !editing && (
              <button onClick={regenerate}>Usar minha regeneração</button>
            )}
          {(session.deliveries.length === 0 || editing) && (
            <form className="personalization" onSubmit={submit}>
              <h3>
                {editing
                  ? "Ajuste para a mesma ocasião"
                  : "Conte o essencial para seu look"}
              </h3>
              <label>
                Qual é a ocasião?
                <input
                  required
                  maxLength={500}
                  value={occasion}
                  onChange={(e) => setOccasion(e.target.value)}
                  placeholder="Ex.: jantar informal ao ar livre"
                />
              </label>
              {!editing && (
                <label>
                  Quando será?
                  <input
                    type="datetime-local"
                    required
                    value={occasionAt}
                    onChange={(e) => setOccasionAt(e.target.value)}
                  />
                </label>
              )}
              <label>
                Quais peças você tem? Uma por linha, de 2 a 12.
                <textarea
                  required
                  value={pieces}
                  onChange={(e) => setPieces(e.target.value)}
                  maxLength={1800}
                  rows={5}
                  placeholder={"Camisa branca\nCalça preta\nTênis branco"}
                />
              </label>
              <label>
                O que você prefere?
                <input
                  required
                  maxLength={500}
                  value={preferences}
                  onChange={(e) => setPreferences(e.target.value)}
                  placeholder="Ex.: conforto, cores neutras"
                />
              </label>
              <label>
                Algum cuidado prático?
                <input
                  maxLength={500}
                  value={constraints}
                  onChange={(e) => setConstraints(e.target.value)}
                  placeholder="Ex.: frio à noite; evitar salto"
                />
              </label>
              <p className="muted">
                Não envie fotos do corpo, dados de saúde ou informações
                sensíveis.
              </p>
              <button disabled={busy}>
                {busy
                  ? "Registrando…"
                  : editing
                    ? "Gerar meu ajuste"
                    : "Preparar minhas três combinações"}
              </button>
            </form>
          )}
        </section>
      )}
      <footer>
        Visualização ilustrativa. A entrega considera apenas as peças e
        informações que você descreveu.
        {(session || offer)?.supportEmail && (
          <p>
            Precisa de ajuda?{" "}
            <a href={"mailto:" + (session || offer)?.supportEmail}>
              {(session || offer)?.supportEmail}
            </a>
          </p>
        )}
        <nav aria-label="Condições da compra">
          {(
            [
              ["termsUrl", "Termos"],
              ["privacyUrl", "Privacidade"],
              ["refundPolicyUrl", "Reembolso"],
            ] as const
          ).map(([key, label]) => {
            const href = (session || offer)?.[key];
            return href?.startsWith("https://") ? (
              <a key={key} href={href} target="_blank" rel="noreferrer">
                {label}{" "}
              </a>
            ) : null;
          })}
        </nav>
      </footer>
    </main>
  );
}
createRoot(document.getElementById("root")!).render(<PaidVisualDelivery />);
