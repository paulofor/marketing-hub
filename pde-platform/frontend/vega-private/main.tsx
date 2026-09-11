import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
const API = "/api/pde/vega/private/v1";
const storageKey = "vega-private-session-v1";
type Card = {
  action: string;
  application: string;
  occasion: string;
  selfAssessmentPrompt: string;
  cardId: string;
};
type Session = {
  id: string;
  state: string;
  generationStatus?: string;
  card?: Card;
  error?: string;
  preference?: string;
  events: Record<string, unknown>;
  input?: {
    occasion?: string;
    existingSelection?: string;
    optionalNote?: string;
  };
  sessionToken?: string;
  prototypeVersion: string;
};
const params = new URLSearchParams(location.hash.slice(1));
const invitation = params.get("access");
if (location.hash) history.replaceState(null, "", location.pathname);
function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [consent, setConsent] = useState(false);
  const [saved, setSaved] = useState(false);
  async function api(path: string, body?: unknown) {
    const token = localStorage.getItem(storageKey) || "";
    const response = await fetch(API + path, {
      method: body === undefined ? "GET" : "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { "X-Vega-Session": token } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: "no-store",
    });
    if (!response.ok) {
      let data;
      try {
        data = await response.json();
      } catch {}
      throw new Error(
        data?.detail ||
          data?.message ||
          (response.status === 401
            ? "Seu acesso expirou. Abra novamente seu convite privado."
            : "Não foi possível registrar agora. Sua última etapa foi preservada; tente novamente."),
      );
    }
    return (await response.json()) as Session;
  }
  async function act(path: string, body: unknown = {}) {
    setBusy(true);
    setError("");
    try {
      const data = await api(path, body);
      if (data.sessionToken)
        localStorage.setItem(storageKey, data.sessionToken);
      setSession(data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  useEffect(() => {
    let active = true;
    if (invitation) {
      setLoading(false);
      return;
    }
    api("/session")
      .then((data) => {
        if (active) setSession(data);
      })
      .catch((e) => {
        if (active && localStorage.getItem(storageKey)) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  const pending =
    session && ["QUEUED", "RUNNING"].includes(session.generationStatus || "");
  useEffect(() => {
    if (!pending) return;
    let active = true;
    const timer = setInterval(() => {
      api("/session")
        .then((data) => {
          if (active) {
            setSession(data);
            setError("");
          }
        })
        .catch(() => {
          if (active)
            setError(
              "A conexão foi interrompida. Seu contexto está salvo; estamos tentando recuperar o andamento.",
            );
        });
    }, 1500);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [pending]);
  const card = session?.card;
  const events = session?.events || {};
  async function generate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    await act("/generate", {
      occasion: form.get("occasion"),
      existingSelection: form.get("existingSelection"),
      optionalNote: form.get("optionalNote") || "",
    });
  }
  return (
    <main>
      <header>
        <span className="brand">MUSA</span>
        <span className="small">Experiência privada</span>
      </header>
      <div className="intro">
        <span className="eyebrow">O que você já tem, de um jeito seu</span>
        <h1>Seu primeiro ajuste</h1>
        <p>
          Uma pequena mudança na roupa que você escolheu. Clara, prática e sem
          precisar de nada novo.
        </p>
      </div>
      {error && (
        <div role="alert" className="notice error">
          {error}
        </div>
      )}
      {loading ? (
        <p role="status">Recuperando seu ajuste…</p>
      ) : !session ? (
        <section className="panel">
          <h2>Um momento só seu</h2>
          <p>
            Esta experiência usa um convite individual. Suas escolhas e seu
            ajuste ficam salvos por até sete dias. Não há compra ou cobrança.
          </p>
          {invitation ? (
            <>
              <label className="consent">
                <input
                  type="checkbox"
                  checked={consent}
                  onChange={(e) => setConsent(e.target.checked)}
                />
                Concordo em participar desta experiência privada e salvar minhas
                escolhas.
              </label>
              <button
                disabled={!consent || busy}
                onClick={() =>
                  act("/access", {
                    accessToken: invitation,
                    consentAccepted: true,
                  })
                }
              >
                {busy ? "Abrindo…" : "Acessar meu ajuste"}
              </button>
            </>
          ) : (
            <p>Abra o link do seu convite para começar ou retomar.</p>
          )}
        </section>
      ) : (
        <>
          {!events.EXPERIENCE_STARTED ? (
            <section className="panel">
              <h2>Vamos usar o que está ao seu alcance</h2>
              <p>
                Escolha a ocasião, conte qual roupa já separou e receba uma ação
                simples para experimentar. Você decide se ela faz sentido para
                você.
              </p>
              <button disabled={busy} onClick={() => act("/start")}>
                {busy ? "Iniciando…" : "Começar"}
              </button>
            </section>
          ) : !card ? (
            <section className="panel">
              <h2>Para qual momento?</h2>
              {session.generationStatus === "BLOCKED" && (
                <div className="notice" role="status">
                  <h3>Podemos ajustar o caminho</h3>
                  <p>{session.error}</p>
                  <p>
                    Reformule abaixo usando apenas a roupa que você já possui.
                  </p>
                </div>
              )}
              {session.generationStatus === "FAILED" && (
                <p className="notice" role="alert">
                  {session.error}
                </p>
              )}
              <form onSubmit={generate}>
                <label>
                  Ocasião *
                  <select
                    name="occasion"
                    required
                    defaultValue={session.input?.occasion || ""}
                    disabled={busy || !!pending}
                  >
                    <option value="">Escolha uma ocasião</option>
                    <option>Almoço informal</option>
                    <option>Dia de trabalho</option>
                    <option>Passeio</option>
                    <option>Encontro com pessoas próximas</option>
                  </select>
                </label>
                <label>
                  Qual roupa ou combinação você já escolheu? *
                  <input
                    name="existingSelection"
                    required
                    maxLength={300}
                    defaultValue={session.input?.existingSelection || ""}
                    placeholder="Ex.: camisa branca e calça preta"
                    disabled={busy || !!pending}
                  />
                </label>
                <label>
                  Algo que gostaria de sentir diferente?{" "}
                  <span className="small">(opcional)</span>
                  <input
                    name="optionalNote"
                    maxLength={300}
                    defaultValue={session.input?.optionalNote || ""}
                    placeholder="Ex.: quero me sentir menos formal"
                    disabled={busy || !!pending}
                  />
                </label>
                <button disabled={busy || !!pending}>
                  {busy
                    ? "Enviando…"
                    : pending
                      ? "Preparando seu ajuste…"
                      : session.generationStatus === "FAILED"
                        ? "Tentar gerar novamente"
                        : session.generationStatus === "BLOCKED"
                          ? "Gerar com meu novo contexto"
                          : "Criar meu ajuste"}
                </button>
              </form>
              {pending && (
                <p role="status" className="small">
                  Seu contexto está salvo. O ajuste pode levar alguns minutos;
                  você pode voltar a esta página.
                </p>
              )}
            </section>
          ) : (
            <>
              <article className="panel card-result">
                <div className="eyebrow">Seu ajuste está salvo</div>
                <h2>{card.action}</h2>
                <p className="application">{card.application}</p>
                <div className="occasion">Para: {card.occasion}</div>
                <h3>Perceba por você</h3>
                <p>{card.selfAssessmentPrompt}</p>
                <p className="small">
                  Use só o que já tem e preserve seu conforto. Não existe um
                  jeito certo de agradar outras pessoas.
                </p>
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={async () => {
                    setBusy(true);
                    try {
                      setSession(await api("/session"));
                      setSaved(true);
                    } catch (e) {
                      setError((e as Error).message);
                    } finally {
                      setBusy(false);
                    }
                  }}
                >
                  Meu ajuste salvo
                </button>
                {saved && (
                  <p role="status" className="small">
                    Este é o mesmo ajuste, pronto para consultar novamente.
                  </p>
                )}
              </article>
              {!events.VALUE_MOMENT ? (
                <section className="panel">
                  <h2>Ficou claro como aplicar?</h2>
                  <button
                    disabled={busy}
                    onClick={() =>
                      act("/events", {
                        eventType: "VALUE_MOMENT",
                        answer: "Entendi e consigo aplicar",
                      })
                    }
                  >
                    Entendi e consigo aplicar
                  </button>
                  <p className="small">
                    Se ainda não ficou claro, você pode reler o cartão sem
                    confirmar.
                  </p>
                </section>
              ) : !events.READY_RESULT_USED ? (
                <section className="panel">
                  <h2>Experimente e perceba</h2>
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      const data = new FormData(e.currentTarget);
                      act("/events", {
                        eventType: "READY_RESULT_USED",
                        answer: data.get("answer"),
                      });
                    }}
                  >
                    <label>
                      Depois de aplicar, como ficou para você? *
                      <textarea
                        name="answer"
                        required
                        maxLength={500}
                        placeholder="Conte o que percebeu no conforto e na ocasião."
                      />
                    </label>
                    <button disabled={busy}>Apliquei este ajuste</button>
                  </form>
                </section>
              ) : (
                <>
                  {!session.preference ? (
                    <section className="panel">
                      <h2>Qual caminho faz mais sentido para você?</h2>
                      <p>
                        As duas escolhas são válidas. Escolha a que preferiria
                        usar neste momento.
                      </p>
                      <div className="choices">
                        <button
                          className="secondary"
                          disabled={busy}
                          onClick={() =>
                            act("/events", {
                              eventType: "PREFERENCE",
                              answer: "CARD",
                            })
                          }
                        >
                          Usar um cartão como este
                        </button>
                        <button
                          className="secondary"
                          disabled={busy}
                          onClick={() =>
                            act("/events", {
                              eventType: "PREFERENCE",
                              answer: "FREE",
                            })
                          }
                        >
                          Experimentar sozinha com conteúdo gratuito
                        </button>
                      </div>
                    </section>
                  ) : (
                    <p className="notice">
                      Sua preferência foi registrada. Obrigada por contar como
                      foi para você.
                    </p>
                  )}
                  <section className="panel">
                    <h2>Conhecer uma possível continuidade</h2>
                    <p>
                      A proposta de continuidade é uma jornada dos Dias 2 a 7,
                      por R$ 67 em pagamento único. Nesta experiência você só
                      pode explorar uma simulação.
                    </p>
                    {events.CHECKOUT_STARTED ? (
                      <div className="notice" role="status">
                        <h3>Simulação concluída</h3>
                        <p>
                          Nenhuma compra foi feita. Não há cobrança, pedido ou
                          acesso pago. Seu ajuste continua salvo acima.
                        </p>
                      </div>
                    ) : (
                      <button
                        disabled={busy}
                        onClick={() =>
                          act("/events", {
                            eventType: "CHECKOUT_STARTED",
                            answer:
                              "Abrir voluntariamente a simulação de R$ 67",
                          })
                        }
                      >
                        Explorar simulação sem cobrança
                      </button>
                    )}
                  </section>
                </>
              )}
            </>
          )}
          <footer>
            <p>
              Seu conforto e sua escolha vêm primeiro. Esta experiência não
              recomenda novas compras nem avalia seu corpo.
            </p>
            {session.state === "FINISHED" ? (
              <p role="status">
                Leitura encerrada. Seu ajuste continua disponível durante a
                validade do convite.
              </p>
            ) : (
              <button
                className="text-button"
                disabled={busy || !!pending}
                onClick={() => act("/finish")}
              >
                Encerrar leitura
              </button>
            )}
          </footer>
        </>
      )}
    </main>
  );
}
createRoot(document.getElementById("root")!).render(<App />);
