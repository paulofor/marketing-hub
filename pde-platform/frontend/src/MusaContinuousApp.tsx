import React, { useEffect, useRef, useState } from "react";
import "./musaContinuous.css";
const base = "/api/pde/vega/consultor/v1";
type Answers = {
  occasion: string;
  existingSelection: string;
  stylePreference: string;
  reflection?: string;
};
type Orientation = {
  operationId: string;
  status: string;
  answers: Answers;
  action: string;
  reason: string;
  limitation: string;
  continuity: string;
  parentChoiceId?: string;
};
type Workspace = {
  sessionId: string;
  productId: number;
  cycleId: number;
  experimentId: number;
  expiresAt: string;
  revision: number;
  draft: Partial<Answers>;
  choices: Record<string, Orientation>;
  preference: string;
  consented: boolean;
};
const blank: Answers = {
  occasion: "",
  existingSelection: "",
  stylePreference: "",
};
const preferences = [
  "Conforto e praticidade",
  "Cor e criatividade",
  "Discrição",
  "Delicadeza",
  "Elegância",
  "Ainda estou descobrindo",
  "Quero manter como está",
];

/** Oferece valor inicial e continuidade privada sem transportar credenciais em armazenamento local. */
export default function MusaContinuousApp() {
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [answers, setAnswers] = useState<Answers>(blank);
  const [step, setStep] = useState(0);
  const [result, setResult] = useState<Orientation | null>(null);
  const [parent, setParent] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [key, setKey] = useState("");
  const [cycleId, setCycleId] = useState("22");
  const [privateLink, setPrivateLink] = useState("");
  const pending = useRef<{ key: string; input: object } | null>(null);
  const field = useRef<HTMLTextAreaElement>(null);
  async function api(path: string, payload?: unknown) {
    const response = await fetch(base + path, {
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      ...(payload !== undefined
        ? { method: "POST", body: JSON.stringify(payload) }
        : {}),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok)
      throw new Error(
        data.detail ||
          data.message ||
          `Não foi possível concluir (${response.status}). Seus dados confirmados foram preservados.`,
      );
    return data;
  }
  async function load(restore = false) {
    const value = (await api("/workspace")) as Workspace;
    setWorkspace(value);
    if (restore) setAnswers({ ...blank, ...value.draft });
    return value;
  }
  async function command(action: string, payload: unknown) {
    return api("/commands", { action, payload });
  }
  async function run(work: () => Promise<void>) {
    if (busy) return;
    setBusy(true);
    setMessage("");
    try {
      await work();
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Conexão interrompida. Consulte novamente antes de repetir.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function login(accessKey: string) {
    setWorkspace(null);
    setAnswers(blank);
    setResult(null);
    setParent("");
    setAccepted(false);
    setPrivateLink("");
    pending.current = null;
    await api("/login", { accessKey });
    setKey("");
    await load(true);
  }
  useEffect(() => {
    const openAccess = () => {
      const accessKey = new URLSearchParams(location.hash.slice(1)).get(
        "access",
      );
      history.replaceState(null, "", location.pathname);
      if (accessKey) void run(() => login(accessKey));
      else
        void run(async () => {
          await load(true);
        });
    };
    openAccess();
    window.addEventListener("hashchange", openAccess);
    return () => window.removeEventListener("hashchange", openAccess);
  }, []);
  useEffect(() => {
    field.current?.focus();
  }, [step]);
  async function saveDraft(nextStep?: number) {
    if (!workspace) return;
    await command("draft", { answers, revision: workspace.revision });
    await load();
    if (nextStep !== undefined) setStep(nextStep);
    else
      setMessage("Rascunho guardado. Você pode sair e continuar neste acesso.");
  }
  async function generate() {
    if (!workspace) return;
    const input = pending.current?.input || {
      operationKey: crypto.randomUUID(),
      revision: workspace.revision,
      answers,
      parentChoiceId: parent,
    };
    pending.current = {
      key: (input as { operationKey: string }).operationKey,
      input,
    };
    let orientation: Orientation;
    try {
      orientation = await command("generate", input);
    } catch (error) {
      try {
        orientation = await api(`/operations/${pending.current.key}`);
      } catch {
        throw error;
      }
    }
    pending.current = null;
    setResult(orientation);
    await load();
    if (orientation.status === "COMPLETED") {
      await new Promise<void>((resolve) =>
        requestAnimationFrame(() => requestAnimationFrame(() => resolve())),
      );
      try {
        await command("event", {
          eventType: "VALUE_MOMENT",
          operationId: orientation.operationId,
        });
      } catch {
        setMessage(
          "Orientação preservada. A confirmação de exibição falhou; consulte a orientação novamente para confirmar.",
        );
      }
    }
  }
  const choiceEntries = workspace ? Object.entries(workspace.choices) : [];
  return (
    <main className="musa-continuous">
      <header>
        <a href={location.pathname} aria-label="MUSA início">
          MUSA<span>Seu estilo todos os dias</span>
        </a>
        <span className="private-badge">Homologação privada</span>
      </header>
      <section className="musa-intro">
        <p className="eyebrow">Use o que você já tem</p>
        <h1>
          Uma escolha com a sua cara.
          <br />
          Hoje e na próxima ocasião.
        </h1>
        <p>
          Conte a situação, receba uma orientação e guarde o que fizer sentido
          para você.
        </p>
      </section>
      {message && (
        <div className="musa-message" role="alert">
          {message}
        </div>
      )}
      {!workspace ? (
        <section className="musa-panel">
          <h2>Entre no seu espaço privado</h2>
          <p>
            O acesso vale 90 dias por emissão. Seus dados ficam separados dos
            demais acessos.
          </p>
          <label>
            Chave de acesso privado
            <input
              type="password"
              autoComplete="off"
              value={key}
              onChange={(e) => setKey(e.target.value)}
            />
          </label>
          <button
            disabled={busy || !key}
            onClick={() => void run(() => login(key))}
          >
            Entrar no meu espaço
          </button>
          <details>
            <summary>Operação da homologação</summary>
            <p>Emissão restrita ao operador autenticado do harness.</p>
            <label>
              Ciclo
              <input
                inputMode="numeric"
                value={cycleId}
                onChange={(e) => setCycleId(e.target.value)}
              />
            </label>
            <button
              disabled={busy}
              onClick={() =>
                void run(async () => {
                  const access = await api(
                    `/internal/cycles/${Number(cycleId)}/access`,
                    {},
                  );
                  setPrivateLink(
                    `${location.origin}${location.pathname}#access=${access.accessKey}`,
                  );
                })
              }
            >
              Emitir acesso privado
            </button>
            {privateLink && <a href={privateLink}>Abrir acesso emitido</a>}
          </details>
        </section>
      ) : (
        <>
          <div className="musa-context">
            <span>
              Acesso até{" "}
              {new Date(workspace.expiresAt).toLocaleDateString("pt-BR", {
                timeZone: "UTC",
              })}
            </span>
            <button
              className="secondary"
              disabled={busy}
              onClick={() =>
                void run(async () => {
                  await api("/logout", {});
                  setWorkspace(null);
                  setParent("");
                  pending.current = null;
                  setAnswers(blank);
                  setResult(null);
                  setPrivateLink("");
                })
              }
            >
              Sair
            </button>
          </div>
          {!workspace.consented ? (
            <section className="musa-panel">
              <h2>Um espaço de teste protegido</h2>
              <p>
                Use somente dados sintéticos. Respostas e escolhas ficam neste
                acesso para você retomar. Não inclua fotos, documentos,
                informações de saúde ou dados de outras pessoas. Não há cobrança
                nem envio a provedores de IA.
              </p>
              <label className="check">
                <input
                  type="checkbox"
                  checked={accepted}
                  onChange={(e) => setAccepted(e.target.checked)}
                />
                Li o aviso e vou usar somente dados sintéticos nesta
                homologação.
              </label>
              <button
                disabled={busy || !accepted}
                onClick={() =>
                  void run(async () => {
                    await command("consent", { accepted: true });
                    await load();
                  })
                }
              >
                Começar minha orientação
              </button>
            </section>
          ) : (
            <div className="musa-columns">
              <section className="musa-panel" aria-label="Sua orientação">
                <p className="eyebrow">
                  {parent
                    ? "Uma nova ocasião · escolha anterior preservada"
                    : "Seu primeiro passo"}
                </p>
                <p>Passo {step + 1} de 3</p>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    void run(async () => {
                      if (step < 2) await saveDraft(step + 1);
                      else await generate();
                    });
                  }}
                >
                  {step === 0 && (
                    <label>
                      Para qual ocasião você quer escolher uma combinação?
                      <textarea
                        ref={field}
                        required
                        maxLength={160}
                        rows={3}
                        value={answers.occasion}
                        onChange={(e) => {
                          pending.current = null;
                          setAnswers({ ...answers, occasion: e.target.value });
                          setResult(null);
                        }}
                        placeholder="Ex.: almoço com amigas"
                      />
                    </label>
                  )}
                  {step === 1 && (
                    <label>
                      Quais peças você tem e como consegue usá-las?
                      <textarea
                        ref={field}
                        required
                        maxLength={300}
                        rows={5}
                        value={answers.existingSelection}
                        onChange={(e) => {
                          pending.current = null;
                          setAnswers({
                            ...answers,
                            existingSelection: e.target.value,
                          });
                          setResult(null);
                        }}
                        placeholder="Ex.: camisa, calça, lenço e bolsa. Posso usar o lenço no pescoço ou na alça da bolsa."
                      />
                      <small>
                        Usamos somente o que você declarar. Cobertura atual:
                        lenço e suas formas de uso, mangas dobráveis, camada
                        removível, alternativas de sapatos ou manter sua
                        combinação.
                      </small>
                    </label>
                  )}
                  {step === 2 && (
                    <label>
                      O que você prefere nessa combinação?
                      <select
                        required
                        value={answers.stylePreference}
                        onChange={(e) => {
                          pending.current = null;
                          setAnswers({
                            ...answers,
                            stylePreference: e.target.value,
                          });
                          setResult(null);
                        }}
                      >
                        <option value="">
                          Escolha o que faz sentido para você
                        </option>
                        {preferences.map((value) => (
                          <option key={value}>{value}</option>
                        ))}
                      </select>
                      <small>
                        Você também pode manter sua combinação ou explorar sem
                        rótulo.
                      </small>
                    </label>
                  )}
                  <div className="musa-actions">
                    {step > 0 && (
                      <button
                        type="button"
                        className="secondary"
                        disabled={busy}
                        onClick={() => setStep(step - 1)}
                      >
                        Voltar
                      </button>
                    )}
                    <button disabled={busy}>
                      {busy
                        ? "Preservando suas escolhas…"
                        : step < 2
                          ? "Próximo"
                          : "Receber orientação"}
                    </button>
                  </div>
                </form>
                <button
                  className="text-button"
                  disabled={busy}
                  onClick={() => void run(() => saveDraft())}
                >
                  Guardar rascunho para continuar depois
                </button>
                {result && (
                  <article
                    className="orientation"
                    aria-label="Resultado da orientação"
                  >
                    {result.status === "COMPLETED" ? (
                      <>
                        <p className="eyebrow">Sua orientação</p>
                        <h2>{result.answers.occasion}</h2>
                        <p>{result.action}</p>
                        {result.continuity && <p>{result.continuity}</p>}
                        <details>
                          <summary>Por que essa escolha?</summary>
                          <p>{result.reason}</p>
                          <p>Peças: {result.answers.existingSelection}</p>
                        </details>
                        <div className="musa-actions">
                          <button
                            disabled={
                              busy || !!workspace.choices[result.operationId]
                            }
                            onClick={() =>
                              void run(async () => {
                                await command("save", {
                                  operationId: result.operationId,
                                  revision: workspace.revision,
                                });
                                await load();
                                setMessage(
                                  "Escolha salva. Retome pelo seu repertório para outra ocasião.",
                                );
                              })
                            }
                          >
                            {workspace.choices[result.operationId]
                              ? "Escolha salva"
                              : "Salvar minha escolha"}
                          </button>
                          <button
                            className="secondary"
                            disabled={busy}
                            onClick={() =>
                              void run(async () => {
                                await command("event", {
                                  eventType: "READY_RESULT_USED",
                                  operationId: result.operationId,
                                });
                                setMessage(
                                  "Sua ação de uso foi registrada, separadamente do salvamento.",
                                );
                              })
                            }
                          >
                            Usar esta orientação
                          </button>
                        </div>
                        <button
                          className="text-button"
                          onClick={() => {
                            setResult(null);
                            setMessage(
                              "Tudo bem manter sua escolha. Você decide o que vale usar.",
                            );
                          }}
                        >
                          Prefiro não usar agora
                        </button>
                      </>
                    ) : (
                      <>
                        <h2>Vamos ajustar a entrada</h2>
                        <p role="alert">{result.limitation}</p>
                        <button
                          className="secondary"
                          onClick={() => setStep(1)}
                        >
                          Revisar minhas peças
                        </button>
                      </>
                    )}
                  </article>
                )}
              </section>
              <aside className="musa-panel">
                <p className="eyebrow">O que vale repetir</p>
                <h2>Seu repertório</h2>
                <p>
                  Guarde uma escolha para adaptá-la à próxima ocasião sem contar
                  tudo de novo.
                </p>
                {choiceEntries.length === 0 && (
                  <p>
                    Nenhuma escolha salva ainda. O rascunho fica separado do
                    repertório.
                  </p>
                )}
                {choiceEntries.map(([id, choice]) => (
                  <article className="saved-choice" key={id}>
                    <h3>{choice.answers.occasion}</h3>
                    <p>{choice.action}</p>
                    <button
                      className="secondary"
                      disabled={busy}
                      onClick={() => {
                        setAnswers({
                          ...choice.answers,
                          occasion: "",
                          stylePreference:
                            workspace.preference ||
                            choice.answers.stylePreference,
                        });
                        setParent(id);
                        setStep(0);
                        setResult(null);
                        setMessage(
                          "Peças recuperadas. Conte a nova ocasião e altere somente o que mudou.",
                        );
                      }}
                    >
                      Adaptar para outra ocasião
                    </button>
                  </article>
                ))}
                <button
                  className="text-button"
                  disabled={busy}
                  onClick={() =>
                    void run(async () => {
                      await load();
                      setMessage(
                        "Repertório atualizado. As escolhas anteriores foram preservadas.",
                      );
                    })
                  }
                >
                  Atualizar repertório
                </button>
              </aside>
            </div>
          )}
          <footer>
            <p>
              Base gratuita e premium opcional em estudo. Preço e condições
              comerciais ainda serão definidos. Esta homologação não oferece
              assinatura nem cobrança.
            </p>
            <p>
              Dados de teste segregados · Sem campanha ou produção audiovisual
            </p>
          </footer>
        </>
      )}
    </main>
  );
}
