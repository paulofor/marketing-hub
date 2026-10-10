import React, { useEffect, useState, type FormEvent } from "react";
import { PrivateCycleVideo, type PrivateVideo } from "./PrivateCycleVideo";

const endpoint = "/api/pde/mira/candidate/v1";
const storageKey = "mira-candidate-v1-session";
type Product = {
  name: string;
  labelDirections: string;
  sourceUrl?: string | null;
};
type Card = {
  productName: string;
  documentedDirection: string;
  safetyNote: string;
  sourceUrl?: string | null;
};
type Session = {
  videoIntegration?: {
    heroVideo: PrivateVideo;
    integrationFingerprint: string;
  };
  id: string;
  prototypeVersion: string;
  condition: string;
  scenarioCode: string;
  status: string;
  objective: string;
  products: Product[];
  routine: Card[];
  previousResults: { organization: number; routine: Card[] }[];
  blocker?: string;
  organizationsUsed: number;
  organizationsLimit: number;
  events: string[];
  trafficClass: string;
  generationMode: string;
  firstInteractionAt?: string;
};

/** Apresenta a candidata privada com entrada gradual e estado autoritativo do backend. */
export function MiraCandidateApp() {
  const [token, setToken] = useState(() => {
    const secret = new URLSearchParams(window.location.hash.slice(1)).get(
      "access",
    );
    if (secret) {
      sessionStorage.setItem(storageKey, secret);
      history.replaceState(null, "", location.pathname);
    }
    return secret || sessionStorage.getItem(storageKey) || "";
  });
  const [session, setSession] = useState<Session | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [objective, setObjective] = useState("");
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [loaded, setLoaded] = useState(false);
  const [recoveryPending, setRecoveryPending] = useState(false);

  async function api(path: string, method = "GET", body?: unknown) {
    const response = await fetch(endpoint + path, {
      method,
      cache: "no-store",
      headers: { "Content-Type": "application/json", "X-Mira-Session": token },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    if (!response.ok) {
      let message =
        "Não foi possível concluir agora. Sua entrada está preservada; tente retomar.";
      try {
        const data = await response.json();
        message = data.detail || data.message || message;
      } catch {
        /* Mantém orientação sem presumir sucesso. */
      }
      throw new Error(message);
    }
    return (await response.json()) as Session;
  }
  function show(value: Session) {
    setSession(value);
    setEditing(false);
    setObjective(value.objective);
    setProducts(
      value.products.length
        ? value.products
        : Array.from(
            { length: value.condition === "REFERENCE" ? 2 : 1 },
            () => ({ name: "", labelDirections: "" }),
          ),
    );
    setRecoveryPending(
      sessionStorage.getItem("mira-candidate-recovery") === value.id,
    );
  }
  async function refresh() {
    setBusy(true);
    setError("");
    try {
      show(await api("/session"));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
      setLoaded(true);
    }
  }
  useEffect(() => {
    document.title = "Mira · organize os cuidados que você já tem";
    const robots = document.querySelector('meta[name="robots"]');
    robots?.setAttribute("content", "noindex, nofollow, noarchive");
    void refresh();
  }, [token]);
  async function generate(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api("/input", "PUT", { objective, products });
      show(await api("/generate", "POST"));
    } catch (e) {
      if (session)
        sessionStorage.setItem("mira-candidate-recovery", session.id);
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function record(eventType: string) {
    setBusy(true);
    setError("");
    try {
      show(await api("/events", "POST", { eventType }));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function change(index: number, key: keyof Product, value: string) {
    setProducts((items) =>
      items.map((item, i) => (i === index ? { ...item, [key]: value } : item)),
    );
  }
  const ready = session?.status === "READY";
  const minimum = session?.condition === "REFERENCE" ? 2 : 1;
  const completed = session?.events.includes("AGENT_SCENARIO_COMPLETED");
  const consulted = session?.events.includes("READY_RESULT_USED");
  const recovery =
    ready && session?.scenarioCode === "RECOVERY" && recoveryPending;
  function reference(card: Card) {
    if (card.sourceUrl?.startsWith("https://"))
      return (
        <p className="mira-source">
          Referência informada:{" "}
          <a href={card.sourceUrl} target="_blank" rel="noopener noreferrer">
            fabricante de {card.productName}
          </a>
          . O conteúdo não substitui o rótulo da sua embalagem.
        </p>
      );
    return (
      <p className="mira-source">
        Referência: texto do rótulo de {card.productName} informado neste
        acesso. Embalagem e fabricante não verificados.
      </p>
    );
  }
  async function consult() {
    if (!consulted) await record("READY_RESULT_USED");
    document.getElementById("mira-result")?.focus();
  }
  return (
    <main className="mira-candidate">
      <header>
        <strong>Mira</strong>
        <span>Acesso privado · sem cobrança</span>
      </header>
      <h1>Coloque ordem nos cuidados que você já tem</h1>
      <p>
        Conte quais produtos já possui. Veja suas instruções organizadas antes
        de decidir continuar.
      </p>
      {error && <p role="alert">{error}</p>}
      {!session && loaded && (
        <section>
          <h2>Abra seu acesso privado</h2>
          <p>
            Esta versão é reservada aos testes autorizados. Não há compra ou
            campanha ativa.
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const value =
                new FormData(e.currentTarget)
                  .get("access")
                  ?.toString()
                  .trim() || "";
              sessionStorage.setItem(storageKey, value);
              setToken(value);
            }}
          >
            <label>
              Credencial privada *
              <input
                name="access"
                type="password"
                required
                autoComplete="off"
              />
            </label>
            <button disabled={busy}>
              {busy ? "Abrindo…" : "Abrir acesso"}
            </button>
          </form>
        </section>
      )}
      {session && (
        <>
          <p data-testid="agent-validation-mode">
            Teste interno · organização a partir do texto informado
          </p>
          <PrivateCycleVideo
            media={session.videoIntegration?.heroVideo}
            label="Como funciona sua organização"
            fallback="O vídeo não abriu. Você pode continuar e organizar seus produtos normalmente."
          />
          {(!ready || editing) && (
            <form onSubmit={generate}>
              <h2>Conte o mínimo necessário</h2>
              <label htmlFor="mira-objective">
                O que você quer organizar? *
              </label>
              <textarea
                id="mira-objective"
                required
                maxLength={500}
                value={objective}
                onChange={(e) => setObjective(e.target.value)}
              />
              {products.map((product, index) => (
                <fieldset key={index}>
                  <legend>Produto {index + 1}</legend>
                  <label htmlFor={`mira-name-${index}`}>
                    Nome do produto {index + 1} *
                  </label>
                  <input
                    id={`mira-name-${index}`}
                    required
                    maxLength={160}
                    value={product.name}
                    onChange={(e) => change(index, "name", e.target.value)}
                  />
                  <label htmlFor={`mira-directions-${index}`}>
                    Orientação do rótulo {index + 1} *
                  </label>
                  <textarea
                    id={`mira-directions-${index}`}
                    required
                    maxLength={600}
                    value={product.labelDirections}
                    onChange={(e) =>
                      change(index, "labelDirections", e.target.value)
                    }
                  />
                  <details className="mira-document-reference">
                    <summary>
                      Acrescentar referência do produto {index + 1} (opcional)
                    </summary>
                    <label htmlFor={`mira-source-${index}`}>
                      Link do fabricante {index + 1}
                    </label>
                    <input
                      id={`mira-source-${index}`}
                      type="url"
                      maxLength={1200}
                      value={product.sourceUrl || ""}
                      onChange={(e) =>
                        change(index, "sourceUrl", e.target.value)
                      }
                    />
                    <p>
                      Use um endereço HTTPS aplicável a este produto. Não
                      verificamos a embalagem ou a adequação ao seu caso.
                    </p>
                  </details>
                  {products.length > minimum && (
                    <button
                      type="button"
                      className="mira-secondary"
                      disabled={busy}
                      onClick={() =>
                        setProducts((items) =>
                          items.filter((_, i) => i !== index),
                        )
                      }
                    >
                      Remover produto {index + 1}
                    </button>
                  )}
                </fieldset>
              ))}
              <button
                type="button"
                className="mira-secondary"
                disabled={busy || products.length >= 12}
                onClick={() =>
                  setProducts((items) => [
                    ...items,
                    { name: "", labelDirections: "" },
                  ])
                }
              >
                Acrescentar outro produto
              </button>
              <button disabled={busy}>
                {busy ? "Organizando…" : "Gerar rotina segura"}
              </button>
              <p>
                Inclua somente produtos que já possui. Não fazemos diagnóstico,
                prescrição nem indicação de compra.
              </p>
            </form>
          )}
          {session.blocker && (
            <section>
              <h2>Como seguir com segurança</h2>
              <p role="alert">{session.blocker}</p>
              <p>
                Confira o texto do rótulo ou reformule o pedido como organização
                dos seus cuidados.
              </p>
            </section>
          )}
          {ready && !editing && (
            <section>
              <h2 id="mira-result" tabIndex={-1}>
                Sua rotina organizada
              </h2>
              <p>
                Este resultado considera apenas os {session.products.length}{" "}
                produtos informados, sem avaliação clínica.
              </p>
              <button disabled={busy} onClick={consult}>
                Consultar organização
              </button>
              <p className="mira-next-step">
                Releia as instruções quando precisar. Consultar e retomar este
                resultado não usa outra organização.
              </p>
              <div className="mira-routine-grid">
                {session.routine.map((card, index) => (
                  <article key={index}>
                    <h3>{card.productName}</h3>
                    <p>{card.documentedDirection}</p>
                    {reference(card)}
                    <small>{card.safetyNote}</small>
                  </article>
                ))}
              </div>
              <p>
                Você usou {session.organizationsUsed} de{" "}
                {session.organizationsLimit} organizações deste pacote de teste.
              </p>
              {session.organizationsUsed < session.organizationsLimit && (
                <button
                  className="mira-secondary"
                  disabled={busy}
                  onClick={() => setEditing(true)}
                >
                  Preparar segunda organização
                </button>
              )}
              <h3>O que a continuidade acrescenta?</h3>
              <p>
                Uma nova organização para os seus produtos, preservando o
                resultado anterior. A oferta ainda precisa da revisão
                independente. Nenhum pagamento está disponível nesta candidata.
              </p>
            </section>
          )}
          {session.previousResults?.map((result) => (
            <details key={result.organization}>
              <summary>
                Consultar organização anterior {result.organization}
              </summary>
              {result.routine.map((card, index) => (
                <article key={index}>
                  <h3>{card.productName}</h3>
                  <p>{card.documentedDirection}</p>
                  {reference(card)}
                  <small>{card.safetyNote}</small>
                </article>
              ))}
            </details>
          ))}
          <details
            className="mira-internal-controls"
            aria-label="Verificação interna"
          >
            <summary>
              {completed
                ? "Verificação interna concluída"
                : "Verificação interna (somente homologação)"}
            </summary>
            <p>
              Estes registros são do teste por agentes; não são uso humano,
              compra ou satisfação.
            </p>
            {completed ? (
              <h2>Avaliação interna concluída</h2>
            ) : (
              <>
                {session.blocker &&
                  (session.events.includes("SAFETY_LIMIT_BLOCKED") ? (
                    <button
                      className="mira-secondary"
                      disabled={busy}
                      onClick={() => record("AGENT_SCENARIO_COMPLETED")}
                    >
                      Concluir cenário de segurança
                    </button>
                  ) : (
                    <button
                      className="mira-secondary"
                      disabled={busy}
                      onClick={() => record("SAFETY_LIMIT_BLOCKED")}
                    >
                      Registrar limite de segurança
                    </button>
                  ))}
                {recovery && !session.events.includes("RECOVERY_COMPLETED") && (
                  <button
                    className="mira-secondary"
                    disabled={busy || !consulted}
                    onClick={() => record("RECOVERY_COMPLETED")}
                  >
                    Confirmar retomada
                  </button>
                )}
                {ready && (
                  <>
                    <button
                      className="mira-secondary"
                      disabled={
                        busy ||
                        !consulted ||
                        session.events.includes("PREFERRED_OVER_FREE")
                      }
                      onClick={() => record("PREFERRED_OVER_FREE")}
                    >
                      Simular comparação com alternativa gratuita
                    </button>
                    <button
                      className="mira-secondary"
                      disabled={
                        busy ||
                        !session.events.includes("PREFERRED_OVER_FREE") ||
                        session.events.includes("CHECKOUT_STARTED")
                      }
                      onClick={() => record("CHECKOUT_STARTED")}
                    >
                      Simular início da continuidade sem cobrança
                    </button>
                    <button
                      className="mira-secondary"
                      disabled={
                        busy ||
                        !consulted ||
                        (recovery &&
                          !session.events.includes("RECOVERY_COMPLETED"))
                      }
                      onClick={() => record("AGENT_SCENARIO_COMPLETED")}
                    >
                      Concluir cenário
                    </button>
                  </>
                )}
              </>
            )}
          </details>
          {error && (
            <button
              className="mira-secondary"
              disabled={busy}
              onClick={refresh}
            >
              {busy ? "Retomando…" : "Retomar etapa salva"}
            </button>
          )}
          <button
            className="mira-secondary"
            disabled={busy}
            onClick={() => {
              sessionStorage.removeItem(storageKey);
              setToken("");
              setSession(null);
              setLoaded(true);
            }}
          >
            Encerrar e sair
          </button>
        </>
      )}
    </main>
  );
}
