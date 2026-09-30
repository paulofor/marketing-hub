import React, { FormEvent, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./alcyone.css";

const productSlug = "pde-planejado-46";
const apiBase = `/api/pde/agent-validation/v1/products/${productSlug}`;
const sessionStorageKey = "alcyone-agent-validation-session";

type LookCard = {
  id: string;
  title: string;
  imagePath: string;
  whyItWorks: string;
  practicalAdjustment: string;
};

type Session = {
  sessionToken: string;
  productId: number;
  productSlug: string;
  prototypeVersion: string;
  scenarioCode: "ADHERENT" | "RECOVERY" | "SAFETY";
  trafficClass: string;
  mhInternalTest: boolean;
  status: "AUTHORIZED" | "INPUT_READY" | "READY" | "BLOCKED";
  looks: LookCard[];
  selectedLookId?: string | null;
  blocker?: string | null;
  events: string[];
  checkoutMode: string;
  finished: boolean;
  resultPackageFingerprint?: string | null;
  providerCalls: number;
  providerCostUsd: number;
};

type Contract = {
  prototypeVersion: string;
  fixtureContract: string;
  checkoutMode: string;
  published: boolean;
  paymentEnabled: boolean;
  mediaSpendBrl: number;
  providerCallsAuthorized: number;
};

type ApiError = { message?: string };

/** Renderiza uma experiência de decisão de look restrita à homologação automatizada. */
function AlcyonePrototype() {
  const [contract, setContract] = useState<Contract | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [occasion, setOccasion] = useState("");
  const [eventDate, setEventDate] = useState("");
  const [preferences, setPreferences] = useState("");
  const [constraints, setConstraints] = useState("");
  const [pieces, setPieces] = useState("");
  const [valueReason, setValueReason] = useState("");
  const [preferenceReason, setPreferenceReason] = useState("");

  useEffect(() => {
    void initialize();
  }, []);

  /** Carrega o contrato público e recupera somente a sessão provisionada pelo harness. */
  async function initialize() {
    setLoading(true);
    try {
      const currentContract = await request<Contract>(`${apiBase}/contract`);
      setContract(currentContract);
      const token = window.sessionStorage.getItem(sessionStorageKey);
      if (token) {
        const restored = await sessionRequest(token, "/session");
        setSession(restored);
        hydrate(restored);
      }
    } catch (cause) {
      setError(message(cause));
    } finally {
      setLoading(false);
    }
  }

  /** Repõe a entrada persistida sem expor o token na URL ou no corpo da página. */
  function hydrate(restored: Session & { input?: {
    occasion?: string;
    eventDate?: string;
    preferences?: string[];
    constraints?: string[];
    pieceReferences?: string[];
  } | null }) {
    if (!restored.input) return;
    setOccasion(restored.input.occasion || "");
    setEventDate(restored.input.eventDate || "");
    setPreferences((restored.input.preferences || []).join(", "));
    setConstraints((restored.input.constraints || []).join(", "));
    setPieces((restored.input.pieceReferences || []).join("\n"));
  }

  /** Persiste a entrada mínima do cenário sem aceitar fotos ou dados pessoais. */
  async function submitInput(event: FormEvent) {
    event.preventDefault();
    await run(async (token) => {
      const updated = await sessionRequest(token, "/input", {
        method: "PUT",
        body: JSON.stringify({
          occasion,
          eventDate,
          preferences: list(preferences),
          constraints: list(constraints),
          pieceReferences: lines(pieces),
        }),
      });
      setSession(updated);
    });
  }

  /** Solicita o pacote estático já autorizado, sem provedor externo. */
  async function generate() {
    await run(async (token) => {
      setSession(await sessionRequest(token, "/generate", { method: "POST" }));
    });
  }

  /** Registra um marco explícito e mantém cada dependência observável. */
  async function record(
    eventType: string,
    details: Record<string, unknown> = {},
  ) {
    await run(async (token) => {
      setSession(
        await sessionRequest(token, "/events", {
          method: "POST",
          body: JSON.stringify({ eventType, ...details }),
        }),
      );
    });
  }

  /** Executa uma operação com tratamento uniforme e sem registrar a credencial. */
  async function run(operation: (token: string) => Promise<void>) {
    const token = window.sessionStorage.getItem(sessionStorageKey);
    if (!token) return;
    setWorking(true);
    setError("");
    try {
      await operation(token);
    } catch (cause) {
      setError(message(cause));
    } finally {
      setWorking(false);
    }
  }

  const inputComplete = useMemo(
    () =>
      occasion.trim().length >= 3 &&
      Boolean(eventDate) &&
      list(preferences).length > 0 &&
      list(constraints).length > 0 &&
      lines(pieces).length >= 2,
    [occasion, eventDate, preferences, constraints, pieces],
  );

  if (loading) {
    return <main className="alcyone-shell"><p role="status">Preparando ambiente privado…</p></main>;
  }
  if (!session) {
    return (
      <main className="alcyone-shell alcyone-locked">
        <p className="alcyone-kicker">ALCYONE · PROTÓTIPO PRIVADO</p>
        <h1>Acesso reservado à homologação automatizada</h1>
        <p>Esta superfície não recruta participantes, não vende e não recebe tráfego público.</p>
        <dl className="alcyone-contract">
          <div><dt>Versão</dt><dd>{contract?.prototypeVersion || "indisponível"}</dd></div>
          <div><dt>Cobrança</dt><dd>desativada</dd></div>
          <div><dt>Mídia</dt><dd>R$ 0,00</dd></div>
        </dl>
        {error && <p role="alert" className="alcyone-alert">{error}</p>}
      </main>
    );
  }

  const has = (event: string) => session.events.includes(event);
  return (
    <main className="alcyone-shell">
      <header className="alcyone-header">
        <div>
          <p className="alcyone-kicker">ALCYONE · DECISÃO DE LOOK</p>
          <h1>Três caminhos claros para a sua ocasião</h1>
          <p>Use referências sintéticas, compare o resultado e preserve a decisão sem pesquisa infinita.</p>
        </div>
        <div className="alcyone-status" data-testid="agent-validation-mode">
          <span>AGENT VALIDATION</span>
          <strong>{session.scenarioCode}</strong>
          <small>{session.prototypeVersion}</small>
        </div>
      </header>

      {error && <p role="alert" className="alcyone-alert">{error}</p>}

      {session.finished ? (
        <section className="alcyone-panel alcyone-complete">
          <p className="alcyone-check">✓</p>
          <h2>Homologação concluída</h2>
          <p>O cenário foi preservado como evidência técnica, sem prova humana ou comercial.</p>
          <p className="alcyone-zero">0 chamadas pagas · R$ 0,00 de mídia · nenhuma cobrança</p>
        </section>
      ) : session.status === "BLOCKED" ? (
        <section className="alcyone-panel alcyone-blocked">
          <p className="alcyone-kicker">LIMITE PROTEGIDO</p>
          <h2>Este pedido ficou fora do protótipo</h2>
          <p role="alert">{session.blocker}</p>
          <p>Nenhuma combinação foi criada e nenhuma chamada externa aconteceu.</p>
          {!has("SAFETY_LIMIT_BLOCKED") ? (
            <button disabled={working} onClick={() => record("SAFETY_LIMIT_BLOCKED")}>
              Registrar bloqueio seguro
            </button>
          ) : (
            <button disabled={working} onClick={() => record("AGENT_SCENARIO_COMPLETED")}>
              Concluir cenário de segurança
            </button>
          )}
        </section>
      ) : session.status === "READY" ? (
        <>
          <section className="alcyone-panel">
            <p className="alcyone-kicker">PACOTE ESTÁTICO · 3 RESULTADOS</p>
            <h2>Três combinações para sua ocasião</h2>
            <div className="alcyone-look-grid">
              {session.looks.map((look) => (
                <article key={look.id} className={session.selectedLookId === look.id ? "selected" : ""}>
                  <img src={look.imagePath} alt={`Composição sintética: ${look.title}`} width="1024" height="1024" />
                  <div>
                    <h3>{look.title}</h3>
                    <p>{look.whyItWorks}</p>
                    <small>{look.practicalAdjustment}</small>
                    {has("VALUE_MOMENT") && !has("READY_RESULT_USED") && (
                      <button disabled={working} onClick={() => record("READY_RESULT_USED", { selectedLookId: look.id })}>
                        Escolher combinação {look.id.slice(-1)}
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          </section>

          {!has("VALUE_MOMENT") && (
            <section className="alcyone-panel alcyone-action">
              <label htmlFor="value-reason">O que tornou o pacote útil?</label>
              <textarea id="value-reason" value={valueReason} onChange={(event) => setValueReason(event.target.value)} />
              <button disabled={working || valueReason.trim().length < 5} onClick={() => record("VALUE_MOMENT", { confirmed: true, justification: valueReason })}>
                Este pacote resolve minha decisão
              </button>
            </section>
          )}

          {has("READY_RESULT_USED") && !has("PREFERRED_OVER_FREE") && (
            <section className="alcyone-panel alcyone-action">
              <label htmlFor="preference-reason">Por que prefere este pacote à pesquisa gratuita?</label>
              <textarea id="preference-reason" value={preferenceReason} onChange={(event) => setPreferenceReason(event.target.value)} />
              <button disabled={working || preferenceReason.trim().length < 5} onClick={() => record("PREFERRED_OVER_FREE", { confirmed: true, alternativeCode: "FREE_SEARCH", justification: preferenceReason })}>
                Prefiro isto à pesquisa gratuita
              </button>
            </section>
          )}

          {has("PREFERRED_OVER_FREE") && !has("CHECKOUT_STARTED") && (
            <section className="alcyone-panel alcyone-checkout">
              <h2>Checkout apenas simulado</h2>
              <p>R$ 79,00 · nenhum dado de pagamento será solicitado.</p>
              <button disabled={working} onClick={() => record("CHECKOUT_STARTED")}>
                Abrir checkout simulado de R$ 79
              </button>
            </section>
          )}

          {has("CHECKOUT_STARTED") && (
            <section className="alcyone-panel alcyone-complete">
              <h2>Simulação concluída — nenhuma cobrança realizada</h2>
              {session.scenarioCode === "RECOVERY" ? (
                !has("RECOVERY_COMPLETED") ? (
                  <button disabled={working} onClick={() => record("RECOVERY_COMPLETED")}>
                    Confirmar retomada do mesmo pacote
                  </button>
                ) : (
                  <button disabled={working} onClick={() => record("AGENT_SCENARIO_COMPLETED")}>
                    Concluir cenário interno
                  </button>
                )
              ) : (
                <button disabled={working} onClick={() => record("AGENT_SCENARIO_COMPLETED")}>
                  Concluir cenário interno
                </button>
              )}
            </section>
          )}
        </>
      ) : (
        <section className="alcyone-panel">
          <p className="alcyone-kicker">ENTRADA MÍNIMA · SEM FOTO CORPORAL</p>
          <h2>Conte o mínimo necessário</h2>
          <form onSubmit={submitInput} className="alcyone-form">
            <label>Ocasião<input value={occasion} onChange={(event) => setOccasion(event.target.value)} required /></label>
            <label>Data da ocasião<input type="date" value={eventDate} onChange={(event) => setEventDate(event.target.value)} required /></label>
            <label>Preferências<input value={preferences} onChange={(event) => setPreferences(event.target.value)} placeholder="linhas simples, tons frios" required /></label>
            <label>Restrições práticas<input value={constraints} onChange={(event) => setConstraints(event.target.value)} placeholder="clima ameno, sem salto alto" required /></label>
            <label className="alcyone-full">Referências isoladas das peças<textarea value={pieces} onChange={(event) => setPieces(event.target.value)} placeholder={"piece-fixture-01\npiece-fixture-02"} required /></label>
            <button type="submit" disabled={working || !inputComplete}>Salvar entrada segura</button>
          </form>
          {session.status === "INPUT_READY" && (
            <div className="alcyone-ready">
              <p>Entrada preservada. O resultado usa somente fixtures estáticas versionadas.</p>
              <button disabled={working} onClick={generate}>Gerar três combinações estáticas</button>
            </div>
          )}
        </section>
      )}

      <footer>
        <span>{contract?.fixtureContract}</span>
        <span>{session.providerCalls} chamadas externas</span>
        <span>sem contato · sem venda · sem mídia</span>
      </footer>
    </main>
  );
}

/** Executa uma chamada de sessão usando a credencial somente em cabeçalho privado. */
async function sessionRequest(token: string, path: string, init: RequestInit = {}) {
  return request<Session>(`${apiBase}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-PDE-Agent-Session": token,
      ...(init.headers || {}),
    },
  });
}

/** Exige JSON bem-sucedido e converte o erro da API em mensagem curta. */
async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init.headers || {}) },
  });
  if (!response.ok) {
    let error: ApiError = {};
    try { error = (await response.json()) as ApiError; } catch { /* resposta sem JSON */ }
    throw new Error(error.message || `A operação falhou (HTTP ${response.status}).`);
  }
  return (await response.json()) as T;
}

/** Converte uma lista separada por vírgulas em valores não vazios. */
function list(value: string) {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

/** Converte referências separadas por linha em valores não vazios. */
function lines(value: string) {
  return value.split(/\n/).map((item) => item.trim()).filter(Boolean);
}

/** Produz uma mensagem segura para falhas desconhecidas. */
function message(cause: unknown) {
  return cause instanceof Error ? cause.message : "Não foi possível concluir a operação.";
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode><AlcyonePrototype /></React.StrictMode>,
);
