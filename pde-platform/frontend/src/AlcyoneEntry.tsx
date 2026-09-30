import React, { FormEvent, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./alcyone.css";

const productSlug = "pde-planejado-46";
const apiBase = `/api/pde/agent-validation/v1/products/${productSlug}`;
const sessionStorageKey = "alcyone-agent-validation-session";
const continuityStorageKey = "alcyone-agent-validation-continuity-v1";

type LookCard = {
  id: string;
  title: string;
  imagePath: string;
  whyItWorks: string;
  practicalAdjustment: string;
};

type Session = {
  sessionToken?: string | null;
  productId: number;
  productSlug: string;
  prototypeVersion: string;
  scenarioCode: "ADHERENT" | "RECOVERY" | "SAFETY";
  trafficClass: string;
  mhInternalTest: boolean;
  status: "AUTHORIZED" | "INPUT_READY" | "READY" | "BLOCKED";
  input?: {
    occasion?: string;
    eventDate?: string;
    preferences?: string[];
    constraints?: string[];
    pieceReferences?: string[];
  } | null;
  looks: LookCard[];
  selectedLookId?: string | null;
  blocker?: string | null;
  events: string[];
  checkoutMode: string;
  finished: boolean;
  resultPackageId?: string | null;
  resultPackageFingerprint?: string | null;
  sessionExpiresAt: string;
  continuityPolicyVersion?: string | null;
  policyAcknowledged: boolean;
  providerCalls: number;
  providerCostUsd: number;
};

type Contract = {
  prototypeVersion: string;
  fixtureContract: string;
  checkoutMode: string;
  continuityPolicyVersion: string;
  published: boolean;
  paymentEnabled: boolean;
  mediaSpendBrl: number;
  providerCallsAuthorized: number;
};

type ContinuityCredential = {
  policyVersion: string;
  policyAcknowledgedAt: string;
  continuityExpiresAt: string;
  resultPackageId: string;
  continuationCredential: string;
};

type ContinuityResume = {
  sessionToken: string;
  sessionExpiresAt: string;
  continuationCredential: string;
  continuityExpiresAt: string;
  resultPackageId: string;
  session: Session;
};

type StoredContinuity = {
  credential: string;
  policyVersion: string;
  resultPackageId: string;
};

type ResultPackage = {
  resultPackageId: string;
  resultPackageFingerprint: string;
  looks: LookCard[];
};

type ApiError = { message?: string; error?: string };

/** Renderiza uma experiência privada de decisão de look com continuidade autenticada. */
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
  const [interestReason, setInterestReason] = useState("");
  const [preferenceReason, setPreferenceReason] = useState("");
  const [policyAcknowledged, setPolicyAcknowledged] = useState(false);
  const [hasContinuity, setHasContinuity] = useState(() =>
    Boolean(readContinuity()),
  );

  useEffect(() => {
    void initialize();
  }, []);

  /** Carrega o contrato, tenta a sessão curta e depois a credencial rotativa salva. */
  async function initialize() {
    setLoading(true);
    setError("");
    try {
      const currentContract = await request<Contract>(`${apiBase}/contract`);
      setContract(currentContract);
      const token = window.sessionStorage.getItem(sessionStorageKey);
      if (token) {
        try {
          const restored = await sessionRequest<Session>(token, "/session");
          setSession(restored);
          hydrate(restored);
          return;
        } catch {
          window.sessionStorage.removeItem(sessionStorageKey);
        }
      }
      const continuity = readContinuity();
      if (continuity) await resumeWith(continuity);
    } catch (cause) {
      setError(message(cause));
    } finally {
      setLoading(false);
    }
  }

  /** Repõe a entrada persistida sem expor credenciais na URL ou no corpo da página. */
  function hydrate(restored: Session) {
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
      const updated = await sessionRequest<Session>(token, "/input", {
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
      setSession(
        await sessionRequest<Session>(token, "/generate", { method: "POST" }),
      );
    });
  }

  /** Registra um marco explícito e mantém cada dependência observável. */
  async function record(
    eventType: string,
    details: Record<string, unknown> = {},
  ) {
    await run(async (token) => {
      setSession(
        await sessionRequest<Session>(token, "/events", {
          method: "POST",
          body: JSON.stringify({ eventType, ...details }),
        }),
      );
    });
  }

  /** Cria uma credencial opaca somente após o aceite da política interna versionada. */
  async function createContinuity() {
    await run(async (token) => {
      if (!contract) throw new Error("Contrato de continuidade indisponível.");
      const issued = await sessionRequest<ContinuityCredential>(
        token,
        "/continuity",
        {
          method: "POST",
          body: JSON.stringify({
            policyAcknowledged,
            policyVersion: contract.continuityPolicyVersion,
          }),
        },
      );
      storeContinuity({
        credential: issued.continuationCredential,
        policyVersion: issued.policyVersion,
        resultPackageId: issued.resultPackageId,
      });
      setHasContinuity(true);
      setSession(await sessionRequest<Session>(token, "/session"));
    });
  }

  /** Retoma o mesmo pacote em nova sessão e comprova a leitura antes de liberar a jornada. */
  async function resumeContinuity() {
    const continuity = readContinuity();
    if (!continuity) {
      setError(
        "A credencial de continuidade não está disponível neste navegador.",
      );
      return;
    }
    setWorking(true);
    setError("");
    try {
      await resumeWith(continuity);
    } catch (cause) {
      setError(message(cause));
    } finally {
      setWorking(false);
    }
  }

  /** Rotaciona credenciais, autoriza o pacote e registra o retorno somente após seu carregamento. */
  async function resumeWith(continuity: StoredContinuity) {
    window.sessionStorage.removeItem(sessionStorageKey);
    const resumed = await request<ContinuityResume>(
      `${apiBase}/continuity/resume`,
      {
        method: "POST",
        body: JSON.stringify({
          continuationCredential: continuity.credential,
          policyVersion: continuity.policyVersion,
        }),
      },
    );
    window.sessionStorage.setItem(sessionStorageKey, resumed.sessionToken);
    storeContinuity({
      credential: resumed.continuationCredential,
      policyVersion: continuity.policyVersion,
      resultPackageId: resumed.resultPackageId,
    });
    setHasContinuity(true);
    setSession(resumed.session);
    hydrate(resumed.session);
    const result = await sessionRequest<ResultPackage>(
      resumed.sessionToken,
      `/packages/${encodeURIComponent(resumed.resultPackageId)}`,
    );
    if (
      result.resultPackageId !== resumed.resultPackageId ||
      result.looks.length !== 3 ||
      result.resultPackageFingerprint !==
        resumed.session.resultPackageFingerprint
    ) {
      throw new Error(
        "O pacote retomado não corresponde ao resultado preservado.",
      );
    }
    const returned = await sessionRequest<Session>(
      resumed.sessionToken,
      "/events",
      {
        method: "POST",
        body: JSON.stringify({ eventType: "RETURN_COMPLETED" }),
      },
    );
    setSession(returned);
  }

  /** Executa uma operação autenticada com tratamento uniforme e sem registrar a credencial. */
  async function run(operation: (token: string) => Promise<void>) {
    const token = window.sessionStorage.getItem(sessionStorageKey);
    if (!token) {
      setError(
        "A sessão curta expirou. Use a credencial de continuidade para retornar.",
      );
      return;
    }
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
    return (
      <main className="alcyone-shell">
        <p role="status">Preparando ambiente privado…</p>
      </main>
    );
  }
  if (!session) {
    return (
      <main className="alcyone-shell alcyone-locked">
        <p className="alcyone-kicker">ALCYONE · PROTÓTIPO PRIVADO</p>
        <h1>Acesso reservado à homologação automatizada</h1>
        <p>
          Esta superfície não recruta participantes, não vende e não recebe
          tráfego público.
        </p>
        <dl className="alcyone-contract">
          <div>
            <dt>Versão</dt>
            <dd>{contract?.prototypeVersion || "indisponível"}</dd>
          </div>
          <div>
            <dt>Cobrança</dt>
            <dd>desativada</dd>
          </div>
          <div>
            <dt>Mídia</dt>
            <dd>R$ 0,00</dd>
          </div>
        </dl>
        {hasContinuity && (
          <button disabled={working} onClick={resumeContinuity}>
            Retomar pacote autenticado
          </button>
        )}
        {error && (
          <p role="alert" className="alcyone-alert">
            {error}
          </p>
        )}
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
          <p>
            Use referências sintéticas, compare o resultado e preserve a decisão
            sem pesquisa infinita.
          </p>
        </div>
        <div className="alcyone-status" data-testid="agent-validation-mode">
          <span>AGENT VALIDATION</span>
          <strong>{session.scenarioCode}</strong>
          <small>{session.prototypeVersion}</small>
        </div>
      </header>

      {error && (
        <p role="alert" className="alcyone-alert">
          {error}
        </p>
      )}

      {session.finished ? (
        <section className="alcyone-panel alcyone-complete">
          <p className="alcyone-check">✓</p>
          <h2>Homologação concluída</h2>
          <p>
            O cenário foi preservado como evidência técnica, sem prova humana ou
            comercial.
          </p>
          <p className="alcyone-zero">
            0 chamadas pagas · R$ 0,00 de mídia · nenhuma cobrança
          </p>
        </section>
      ) : session.status === "BLOCKED" ? (
        <section className="alcyone-panel alcyone-blocked">
          <p className="alcyone-kicker">LIMITE PROTEGIDO</p>
          <h2>Este pedido ficou fora do protótipo</h2>
          <p role="alert">{session.blocker}</p>
          <p>
            Nenhuma combinação foi criada e nenhuma chamada externa aconteceu.
          </p>
          {!has("SAFETY_LIMIT_BLOCKED") ? (
            <button
              disabled={working}
              onClick={() => record("SAFETY_LIMIT_BLOCKED")}
            >
              Registrar bloqueio seguro
            </button>
          ) : (
            <button
              disabled={working}
              onClick={() => record("AGENT_SCENARIO_COMPLETED")}
            >
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
                <article
                  key={look.id}
                  className={
                    session.selectedLookId === look.id ? "selected" : ""
                  }
                >
                  <img
                    src={look.imagePath}
                    alt={`Composição sintética: ${look.title}`}
                    width="1024"
                    height="1024"
                  />
                  <div>
                    <h3>{look.title}</h3>
                    <p>{look.whyItWorks}</p>
                    <small>{look.practicalAdjustment}</small>
                    {has("VALUE_MOMENT") && !has("READY_RESULT_USED") && (
                      <button
                        disabled={working}
                        onClick={() =>
                          record("READY_RESULT_USED", {
                            selectedLookId: look.id,
                          })
                        }
                      >
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
              <textarea
                id="value-reason"
                value={valueReason}
                onChange={(event) => setValueReason(event.target.value)}
              />
              <button
                disabled={working || valueReason.trim().length < 5}
                onClick={() =>
                  record("VALUE_MOMENT", {
                    confirmed: true,
                    justification: valueReason,
                  })
                }
              >
                Este pacote resolve minha decisão
              </button>
            </section>
          )}

          {has("READY_RESULT_USED") && !has("SAVE_INTEREST_DECLARED") && (
            <section
              className="alcyone-panel alcyone-action"
              data-testid="save-interest-step"
            >
              <label htmlFor="interest-reason">
                Por que vale preservar esta decisão?
              </label>
              <textarea
                id="interest-reason"
                value={interestReason}
                onChange={(event) => setInterestReason(event.target.value)}
              />
              <button
                disabled={working || interestReason.trim().length < 5}
                onClick={() =>
                  record("SAVE_INTEREST_DECLARED", {
                    confirmed: true,
                    justification: interestReason,
                  })
                }
              >
                Quero preservar esta decisão
              </button>
            </section>
          )}

          {has("SAVE_INTEREST_DECLARED") &&
            !has("CONTINUITY_CREDENTIAL_CREATED") && (
              <section
                className="alcyone-panel alcyone-action"
                data-testid="credential-step"
              >
                <h2>Acesso de retorno do agente</h2>
                <label className="alcyone-policy">
                  <input
                    type="checkbox"
                    checked={policyAcknowledged}
                    onChange={(event) =>
                      setPolicyAcknowledged(event.target.checked)
                    }
                  />
                  Reconheço a política interna versionada de continuidade
                  automatizada. Isto não representa aceite humano nem evidência
                  comercial.
                </label>
                <button
                  disabled={working || !policyAcknowledged}
                  onClick={createContinuity}
                >
                  Criar acesso de retorno
                </button>
              </section>
            )}

          {has("CONTINUITY_CREDENTIAL_CREATED") && !has("RETURN_COMPLETED") && (
            <section
              className="alcyone-panel alcyone-action"
              data-testid="authenticated-return-step"
            >
              <h2>Decisão preservada</h2>
              <p>
                A credencial está separada da sessão curta e será rotacionada no
                próximo acesso.
              </p>
              <button
                disabled={working || !hasContinuity}
                onClick={resumeContinuity}
              >
                Retomar pacote autenticado
              </button>
            </section>
          )}

          {has("RETURN_COMPLETED") && !has("PREFERRED_OVER_FREE") && (
            <section
              className="alcyone-panel alcyone-action"
              data-testid="return-complete-step"
            >
              <p className="alcyone-check">
                ✓ Retorno autenticado ao mesmo pacote
              </p>
              <label htmlFor="preference-reason">
                Por que prefere este pacote à pesquisa gratuita?
              </label>
              <textarea
                id="preference-reason"
                value={preferenceReason}
                onChange={(event) => setPreferenceReason(event.target.value)}
              />
              <button
                disabled={working || preferenceReason.trim().length < 5}
                onClick={() =>
                  record("PREFERRED_OVER_FREE", {
                    confirmed: true,
                    alternativeCode: "FREE_SEARCH",
                    justification: preferenceReason,
                  })
                }
              >
                Prefiro isto à pesquisa gratuita
              </button>
            </section>
          )}

          {has("PREFERRED_OVER_FREE") && !has("CHECKOUT_STARTED") && (
            <section className="alcyone-panel alcyone-checkout">
              <h2>Checkout apenas simulado</h2>
              <p>R$ 79,00 · nenhum dado de pagamento será solicitado.</p>
              <button
                disabled={working}
                onClick={() => record("CHECKOUT_STARTED")}
              >
                Abrir checkout simulado de R$ 79
              </button>
            </section>
          )}

          {has("CHECKOUT_STARTED") && (
            <section className="alcyone-panel alcyone-complete">
              <h2>Simulação concluída — nenhuma cobrança realizada</h2>
              {session.scenarioCode === "RECOVERY" ? (
                !has("RECOVERY_COMPLETED") ? (
                  <button
                    disabled={working}
                    onClick={() => record("RECOVERY_COMPLETED")}
                  >
                    Confirmar retomada do mesmo pacote
                  </button>
                ) : (
                  <button
                    disabled={working}
                    onClick={() => record("AGENT_SCENARIO_COMPLETED")}
                  >
                    Concluir cenário interno
                  </button>
                )
              ) : (
                <button
                  disabled={working}
                  onClick={() => record("AGENT_SCENARIO_COMPLETED")}
                >
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
            <label>
              Ocasião
              <input
                value={occasion}
                onChange={(event) => setOccasion(event.target.value)}
                required
              />
            </label>
            <label>
              Data da ocasião
              <input
                type="date"
                value={eventDate}
                onChange={(event) => setEventDate(event.target.value)}
                required
              />
            </label>
            <label>
              Preferências
              <input
                value={preferences}
                onChange={(event) => setPreferences(event.target.value)}
                placeholder="linhas simples, tons frios"
                required
              />
            </label>
            <label>
              Restrições práticas
              <input
                value={constraints}
                onChange={(event) => setConstraints(event.target.value)}
                placeholder="clima ameno, sem salto alto"
                required
              />
            </label>
            <label className="alcyone-full">
              Referências isoladas das peças
              <textarea
                value={pieces}
                onChange={(event) => setPieces(event.target.value)}
                placeholder={"piece-fixture-01\npiece-fixture-02"}
                required
              />
            </label>
            <button type="submit" disabled={working || !inputComplete}>
              Salvar entrada segura
            </button>
          </form>
          {session.status === "INPUT_READY" && (
            <div className="alcyone-ready">
              <p>
                Entrada preservada. O resultado usa somente fixtures estáticas
                versionadas.
              </p>
              <button disabled={working} onClick={generate}>
                Gerar três combinações estáticas
              </button>
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

/** Executa uma chamada usando a sessão curta somente em cabeçalho privado. */
async function sessionRequest<T>(
  token: string,
  path: string,
  init: RequestInit = {},
) {
  return request<T>(`${apiBase}${path}`, {
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
    try {
      error = (await response.json()) as ApiError;
    } catch {
      /* resposta sem JSON */
    }
    throw new Error(
      error.message ||
        error.error ||
        `A operação falhou (HTTP ${response.status}).`,
    );
  }
  return (await response.json()) as T;
}

/** Salva apenas a credencial opaca de continuidade fora da sessão curta. */
function storeContinuity(continuity: StoredContinuity) {
  window.localStorage.setItem(continuityStorageKey, JSON.stringify(continuity));
}

/** Recupera a credencial local somente quando todos os campos esperados existem. */
function readContinuity(): StoredContinuity | null {
  try {
    const raw = window.localStorage.getItem(continuityStorageKey);
    if (!raw) return null;
    const value = JSON.parse(raw) as Partial<StoredContinuity>;
    if (!value.credential || !value.policyVersion || !value.resultPackageId)
      return null;
    return value as StoredContinuity;
  } catch {
    return null;
  }
}

/** Converte uma lista separada por vírgulas em valores não vazios. */
function list(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

/** Converte referências separadas por linha em valores não vazios. */
function lines(value: string) {
  return value
    .split(/\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

/** Produz uma mensagem segura para falhas desconhecidas. */
function message(cause: unknown) {
  return cause instanceof Error
    ? cause.message
    : "Não foi possível concluir a operação.";
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AlcyonePrototype />
  </React.StrictMode>,
);
