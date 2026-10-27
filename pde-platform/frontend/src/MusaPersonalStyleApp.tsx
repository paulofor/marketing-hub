import React, { useEffect, useRef, useState } from "react";
import { pdeAccessHeaders } from "./pdeAccessAuthorization";
import {
  MUSA_V15_EXPERIENCE_VERSION,
  MUSA_V16_EXPERIENCE_VERSION,
  resolveMusaCommercialRoute,
  musaCommercialPath,
} from "./musaCommercialRoutes";
import type { ProductExperience, Mission } from "./musaExperiences";
import "./musaPersonalStyle.css";
import { trackStyleEvent } from "./musaStyleTracking";

const contextual =
  resolveMusaCommercialRoute(location.pathname) === MUSA_V16_EXPERIENCE_VERSION;
const version = contextual
  ? MUSA_V16_EXPERIENCE_VERSION
  : MUSA_V15_EXPERIENCE_VERSION;
const apiBase = contextual ? "/api/pde/vega/style/v1/" : "/api/pde/";
const slug = "metodo-musa-7-dias";
const storageKey = `musa:${version}`;
type Answers = {
  occasion: string;
  existingSelection: string;
  stylePreference: string;
  reflection?: string;
};
type Guidance = {
  requestId: string;
  status: string;
  headline: string;
  summary: string;
  signals: string[];
  microActions: string[];
  caution: string;
  errorMessage?: string;
};
type Workspace = {
  product: ProductExperience;
  experienceVersion: string;
  email: string;
  subscriptionStatus: string;
  progressPercent: number;
  completedMissionIds: string[];
  started?: boolean;
  latestResults?: Record<string, { latestRequestId?: string }>;
  missionInteractions: {
    missionId: string;
    questionKey: string;
    answerText: string;
  }[];
};
const empty: Answers = {
  occasion: "",
  existingSelection: "",
  stylePreference: "",
};

async function api<T>(
  path: string,
  data?: unknown,
  token?: string,
): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    method: data === undefined ? "GET" : "POST",
    headers:
      token && !contextual
        ? pdeAccessHeaders(token, { json: data !== undefined })
        : { "Content-Type": "application/json" },
    ...(data === undefined ? {} : { body: JSON.stringify(data) }),
  });
  if (!response.ok && contextual) {
    const body = await response.json().catch(() => ({}));
    throw new Error(
      body.message ||
        body.detail ||
        (response.status === 401
          ? "Este acesso terminou ou não está disponível. Seus registros anteriores foram preservados."
          : "Não foi possível concluir. Confira as escolhas; os registros já salvos foram preservados."),
    );
  }
  if (!response.ok)
    throw new Error(
      response.status === 403
        ? "Este acesso não permite continuar. Entre com o e-mail da compra para recuperar sua jornada."
        : "Não conseguimos concluir agora. Confira as respostas e tente novamente; o formulário foi preservado.",
    );
  return response.status === 204 ? (undefined as T) : response.json();
}

function readSaved(): {
  answers?: Answers;
  requestId?: string;
  operationKey?: string;
  missionId?: string;
} {
  try {
    return JSON.parse(sessionStorage.getItem(storageKey) || "{}");
  } catch {
    return {};
  }
}
function storeSaved(value: unknown) {
  try {
    sessionStorage.setItem(storageKey, JSON.stringify(value));
  } catch {
    /* A sessão continua mesmo sem armazenamento no navegador. */
  }
}
function download(text: string, filename: string) {
  const url = URL.createObjectURL(
    new Blob([text], { type: "text/plain;charset=utf-8" }),
  );
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Apresenta a experiência de estilo e reutiliza exclusivamente a jornada persistida desta versão. */
export default function MusaPersonalStyleApp() {
  const [product, setProduct] = useState<ProductExperience>();
  const [answers, setAnswers] = useState<Answers>(
    () => readSaved().answers || empty,
  );
  const [guidance, setGuidance] = useState<Guidance>();
  const [workspace, setWorkspace] = useState<Workspace>();
  const [token] = useState(() =>
    contextual
      ? "private-cookie"
      : new URLSearchParams(location.hash.slice(1)).get("access") || "",
  );
  const [missionId, setMissionId] = useState("");
  const selectionRevision = useRef(0);
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const mission = workspace?.product.missions.find((m) => m.id === missionId);
  const locked =
    !!mission && mission.day > 1 && workspace?.subscriptionStatus !== "ACTIVE";
  const fields =
    mission?.interaction?.fields ||
    product?.publicDiagnosticQuestions?.map((q) => ({
      key: q.key,
      label: q.question,
      options: q.options,
      placeholder: "",
    })) ||
    [];

  async function selectMission(next: Mission, current: Workspace) {
    const selectedRevision = ++selectionRevision.current;
    if (contextual && readSaved().missionId !== next.id)
      storeSaved({ answers: readSaved().answers, missionId: next.id });
    const prior: Record<string, string> = {};
    for (const day of current.product.missions.filter(
      (m) => m.day <= next.day,
    )) {
      for (const row of current.missionInteractions.filter(
        (r) => r.missionId === day.id,
      ))
        prior[row.questionKey] = row.answerText;
    }
    const ownReflection =
      current.missionInteractions.find(
        (r) => r.missionId === next.id && r.questionKey === "reflection",
      )?.answerText || "";
    setAnswers({
      ...empty,
      ...readSaved().answers,
      ...prior,
      reflection: ownReflection,
    });
    setMissionId(next.id);
    setGuidance(undefined);
    setError("");
    const resultId = current.latestResults?.[next.id]?.latestRequestId;
    if (contextual && resultId) {
      try {
        const saved = await api<Guidance>(
          `access/ai-guidance/${encodeURIComponent(resultId)}`,
        );
        if (selectedRevision !== selectionRevision.current) return;
        if (saved.status === "COMPLETED") setGuidance(saved);
        else if (saved.errorMessage) setError(saved.errorMessage);
      } catch (e) {
        if (selectedRevision === selectionRevision.current)
          setError((e as Error).message);
      }
    }
  }
  async function reloadWorkspace() {
    const current = await api<Workspace>("access/workspace", undefined, token);
    if (
      current.experienceVersion !== version ||
      current.product.experienceVersion !== version
    )
      throw new Error(
        "Este link pertence a outra versão da sua jornada. Use o endereço recebido por e-mail.",
      );
    setWorkspace(current);
    setProduct(current.product);
    const next =
      current.product.missions.find(
        (m) => !current.completedMissionIds.includes(m.id),
      ) || current.product.missions[6];
    if (next) await selectMission(next, current);
  }

  useEffect(() => {
    if (!token) void trackStyleEvent("PAGE_VIEW");
    if (token) {
      reloadWorkspace().catch((e) => setError(e.message));
      return;
    }
    api<ProductExperience>(`products/${slug}?experienceVersion=${version}`)
      .then((p) => {
        if (p.experienceVersion !== version)
          throw new Error(
            "Esta experiência ainda não está disponível. Tente novamente mais tarde.",
          );
        setProduct(p);
      })
      .catch((e) => setError(e.message));
    const id = readSaved().requestId;
    if (id)
      api<Guidance>(`public/presence-diagnostic/${encodeURIComponent(id)}`)
        .then((g) => {
          if (g.status === "COMPLETED") setGuidance(g);
        })
        .catch(() =>
          setMessage(
            "O resultado anterior não pôde ser recuperado agora. Suas escolhas continuam no formulário.",
          ),
        );
  }, [token]);

  function change(key: string, value: string) {
    selectionRevision.current++;
    const next = { ...answers, [key]: value };
    setAnswers(next);
    setGuidance(undefined);
    setError("");
    if (!token || contextual) storeSaved({ answers: next, missionId });
  }
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    setGuidance(undefined);
    let operationKey = readSaved().operationKey;
    if (contextual && !operationKey) {
      operationKey = Array.from(
        crypto.getRandomValues(new Uint8Array(16)),
        (byte) => byte.toString(16).padStart(2, "0"),
      ).join("");
      storeSaved({ answers, operationKey, missionId });
    }
    try {
      const result =
        token && mission
          ? await api<Guidance>(
              `access/missions/${mission.id}/ai-guidance`,
              {
                experienceVersion: version,
                ...(contextual
                  ? { operationKey }
                  : { guidanceType: mission.interaction?.guidanceType }),
                answers,
              },
              token,
            )
          : await api<Guidance>("public/presence-diagnostic", {
              experienceVersion: version,
              answers,
            });
      if (result.status !== "COMPLETED")
        throw new Error(
          result.errorMessage ||
            "A orientação ainda não foi concluída. Suas respostas foram preservadas.",
        );
      setGuidance(result);
      if (!token) void trackStyleEvent("VALUE_MOMENT", result.requestId);
      if (!token || contextual)
        storeSaved({
          answers,
          requestId: result.requestId,
          operationKey,
          missionId,
        });
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function complete() {
    if (!mission || !answers.reflection?.trim()) return;
    setBusy(true);
    setError("");
    try {
      await api(
        `access/missions/${mission.id}/interactions`,
        { answers },
        token,
      );
      await api(`access/missions/${mission.id}/complete`, {}, token);
      await reloadWorkspace();
      setMessage("Seu dia e suas escolhas ficaram salvos.");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function sendAccess(event: React.FormEvent, existing: boolean) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await api<{ accessUrl?: string }>(
        `access/${existing ? "login-link" : "magic-link"}`,
        { productSlug: slug, email, experienceVersion: version },
      );
      setMessage(
        "Confira seu e-mail para continuar. Use o link recebido para guardar suas escolhas.",
      );
      if (import.meta.env.DEV && response.accessUrl)
        location.assign(response.accessUrl);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function exportJourney() {
    if (!workspace) return;
    download(
      [
        workspace.product.name,
        ...workspace.product.missions.flatMap((m) => [
          `\nDia ${m.day}: ${m.title}`,
          ...workspace.missionInteractions
            .filter((r) => r.missionId === m.id)
            .map(
              (r) =>
                `${r.questionKey === "reflection" ? "Meu registro" : r.questionKey === "stylePreference" ? "Minha preferência" : r.questionKey === "occasion" ? "Ocasião" : "Minhas peças"}: ${r.answerText}`,
            ),
        ]),
      ].join("\n"),
      "meu-estilo-musa.txt",
    );
  }
  async function material(url: string) {
    setError("");
    try {
      const response = await fetch(url, {
        headers: contextual ? {} : pdeAccessHeaders(token),
      });
      if (!response.ok)
        throw new Error("O material exige acesso completo vigente.");
      const blob = await response.blob();
      const link = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = link;
      a.download = url.split("/").pop() || "material";
      a.click();
      setTimeout(() => URL.revokeObjectURL(link), 1000);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function begin() {
    setError("");
    try {
      await api("start", {});
      await reloadWorkspace();
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function signal(eventType: string) {
    if (!guidance) return;
    setError("");
    try {
      await api("events", { eventType, requestId: guidance.requestId });
      setMessage(
        eventType === "CHECKOUT_STARTED"
          ? "Você pode conhecer a continuidade por R$ 67, pagamento único, com sete dias e três materiais. Esta prévia não realiza cobranças nem libera uma compra."
          : "Sua ação ficou registrada nesta jornada.",
      );
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <main className="musa-style">
      <header className="style-header">
        <a href={musaCommercialPath(version)} aria-label="MUSA início">
          MUSA<span>seu estilo, do seu jeito</span>
        </a>
        {workspace ? (
          <span>Minha jornada · {workspace.progressPercent}%</span>
        ) : (
          <a href="#style-login">Já comecei</a>
        )}
      </header>
      {(!workspace || (contextual && !workspace.started)) && (
        <section className="style-hero">
          <div>
            <span className="style-eyebrow">VEGA · ESTILO PESSOAL</span>
            <h1>Seu guarda-roupa pode ter mais a sua cara.</h1>
            <p>
              Você não precisa se vestir como outra pessoa. Comece com o que já
              tem e descubra um jeito de usar que combina com você.
            </p>
            {contextual ? (
              <button className="style-button" onClick={begin}>
                Experimentar com minhas roupas
              </button>
            ) : (
              <a className="style-button" href="#style-start">
                Experimentar com minhas roupas
              </a>
            )}
            <small>
              Uma sugestão gratuita. Sem foto, cadastro ou cartão para começar.
            </small>
          </div>
          <aside className="style-mood" aria-label="Preferências possíveis">
            <span className="style-eyebrow">
              NÃO EXISTE UM ÚNICO JEITO CERTO
            </span>
            <p>Hoje eu quero…</p>
            <div>
              <span>conforto</span>
              <span>cor</span>
              <span>discrição</span>
              <span>elegância</span>
              <span>ser eu.</span>
            </div>
            <small>
              Seu gosto pode mudar. A orientação acompanha sua escolha.
            </small>
          </aside>
        </section>
      )}
      {error && (
        <p role="alert" className="style-alert">
          {error}
        </p>
      )}
      {message && (
        <p role="status" className="style-notice">
          {message}
        </p>
      )}
      {!product && !error && <p role="status">Carregando sua experiência…</p>}
      {workspace && (!contextual || workspace.started) && (
        <section className="style-journey">
          <h1>Seu estilo em 7 dias</h1>
          <p>Reaproveite o que funcionou. Mude o que não tem a sua cara.</p>
          <nav aria-label="Dias da jornada">
            {workspace.product.missions.map((m) => (
              <button
                key={m.id}
                disabled={busy}
                aria-current={missionId === m.id ? "step" : undefined}
                onClick={() => selectMission(m, workspace)}
              >
                {workspace.completedMissionIds.includes(m.id) ? "✓ " : ""}Dia{" "}
                {m.day}
              </button>
            ))}
          </nav>
          <button className="style-secondary" onClick={exportJourney}>
            Baixar meus registros
          </button>
        </section>
      )}
      {product && (!contextual || workspace?.started) && (
        <section
          className="style-panel"
          id="style-start"
          aria-label="Sua orientação de estilo"
        >
          <div>
            <span className="style-eyebrow">
              {mission ? `DIA ${mission.day}` : "COMECE POR UMA COMBINAÇÃO"}
            </span>
            <h2>{mission?.title || "O que tem a sua cara hoje?"}</h2>
            <p>
              {mission?.action ||
                "Três escolhas para uma sugestão que você pode testar agora."}
            </p>
          </div>
          {locked ? (
            <div className="style-lock">
              <h3>Sua jornada está guardada</h3>
              <p>
                Os dias 2 a 7 e os materiais precisam de acesso completo
                vigente. O primeiro dia continua disponível.
              </p>
              <p>A compra desta nova experiência ainda não está disponível.</p>
            </div>
          ) : (
            <>
              <form onSubmit={submit} className="style-form">
                {fields.map((field) => (
                  <label key={field.key}>
                    {field.label}
                    {field.options.length ? (
                      <select
                        disabled={busy}
                        required
                        value={answers[field.key as keyof Answers] || ""}
                        onChange={(e) => change(field.key, e.target.value)}
                      >
                        <option value="">Escolha para hoje</option>
                        {field.options.map((o) => (
                          <option key={o}>{o}</option>
                        ))}
                      </select>
                    ) : (
                      <textarea
                        disabled={busy}
                        required
                        rows={2}
                        maxLength={field.key === "occasion" ? 160 : 300}
                        value={answers[field.key as keyof Answers] || ""}
                        placeholder={
                          contextual
                            ? field.key === "occasion"
                              ? "Ex.: almoçar e conversar com amigas"
                              : "Ex.: camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça"
                            : field.key === "occasion"
                              ? "Ex.: trabalhar e depois encontrar amigos"
                              : "Ex.: camiseta azul, jeans e tênis"
                        }
                        onChange={(e) => change(field.key, e.target.value)}
                      />
                    )}
                  </label>
                ))}
                <button className="style-button" disabled={busy}>
                  {busy ? "Preparando…" : "Ver minha orientação"}
                </button>
              </form>
              {guidance && (
                <article
                  className="style-result"
                  aria-label="Sua orientação pronta"
                >
                  <span className="style-eyebrow">PARA EXPERIMENTAR HOJE</span>
                  <h2>{guidance.headline}</h2>
                  <p>{guidance.summary}</p>
                  <ol>
                    {guidance.microActions.map((a) => (
                      <li key={a}>{a}</li>
                    ))}
                  </ol>
                  <details>
                    <summary>Escolhas que orientaram esta sugestão</summary>
                    <ul>
                      {guidance.signals.map((s) => (
                        <li key={s}>{s}</li>
                      ))}
                    </ul>
                    <p>{guidance.caution}</p>
                  </details>
                  <button
                    className="style-secondary"
                    onClick={() =>
                      download(
                        [
                          guidance.headline,
                          guidance.summary,
                          ...guidance.microActions,
                        ].join("\n\n"),
                        "minha-orientacao-musa.txt",
                      )
                    }
                  >
                    Guardar minha orientação
                  </button>
                  {contextual && (
                    <div>
                      <button
                        className="style-secondary"
                        onClick={() => signal("VALUE_MOMENT")}
                      >
                        Entendi como aplicar
                      </button>
                      <button
                        className="style-secondary"
                        onClick={() => signal("READY_RESULT_USED")}
                      >
                        Apliquei este ajuste
                      </button>
                      <button
                        className="style-secondary"
                        onClick={() => signal("CHECKOUT_STARTED")}
                      >
                        Conhecer a continuidade
                      </button>
                    </div>
                  )}
                </article>
              )}
              {workspace && guidance && (
                <div className="style-reflection">
                  <label>
                    O que funcionou para você? O que quer manter ou mudar?
                    <textarea
                      rows={3}
                      maxLength={300}
                      value={answers.reflection || ""}
                      onChange={(e) =>
                        setAnswers({ ...answers, reflection: e.target.value })
                      }
                      placeholder="Pode ser manter tudo como está. Sua escolha é válida."
                    />
                  </label>
                  <button
                    className="style-button"
                    disabled={busy || !answers.reflection?.trim()}
                    onClick={complete}
                  >
                    Salvar e concluir este dia
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      )}
      {!workspace && (
        <section className="style-continuity">
          <span className="style-eyebrow">DE UMA SUGESTÃO A UM JEITO SEU</span>
          <h2>Guarde o que funciona. Use de novo amanhã.</h2>
          <p>
            A jornada de sete dias conecta suas escolhas: testar, adaptar à
            rotina e criar uma fórmula pessoal que você pode repetir. Você leva
            seus registros, um mapa de preferências, um checklist e uma planilha
            da semana.
          </p>
          <div className="style-steps">
            <span>
              01
              <br />
              Experimentar
            </span>
            <span>
              02—06
              <br />
              Descobrir e adaptar
            </span>
            <span>
              07
              <br />
              Criar sua fórmula
            </span>
          </div>
          <p>
            <strong>R$ 67, pagamento único.</strong> Dias 2 a 7, três materiais
            e acesso por 90 dias. Sem assinatura.
          </p>
          <small>
            A compra desta nova experiência ainda não está disponível. Você pode
            experimentar a primeira orientação e o Dia 1 gratuitamente.
          </small>
          {guidance && (
            <form onSubmit={(e) => sendAccess(e, false)}>
              <label>
                Guardar minhas escolhas e começar o Dia 1
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Seu e-mail"
                />
              </label>
              <button className="style-button" disabled={busy}>
                Receber meu acesso gratuito
              </button>
            </form>
          )}
        </section>
      )}
      {workspace?.subscriptionStatus === "ACTIVE" && (
        <section className="style-materials">
          <h2>Para levar para a rotina</h2>
          {workspace.product.supportMaterials.map((m) => (
            <article key={m.url}>
              <h3>{m.title}</h3>
              <p>{m.description}</p>
              <button
                className="style-secondary"
                onClick={() => material(m.url)}
              >
                Baixar {m.title}
              </button>
            </article>
          ))}
        </section>
      )}
      {!workspace && (
        <details id="style-login" className="style-login">
          <summary>Já comecei: recuperar meu acesso</summary>
          <form onSubmit={(e) => sendAccess(e, true)}>
            <label>
              E-mail usado nesta jornada
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
            <button className="style-button" disabled={busy}>
              Receber link de acesso
            </button>
          </form>
        </details>
      )}
      <footer>
        <p>MUSA · Seu estilo, com as suas roupas.</p>
        <p>
          Seu gosto guia a experiência. As sugestões não avaliam corpo,
          personalidade ou reação de outras pessoas.
        </p>
        <a href="mailto:contato@digicomdigital.com.br">Suporte</a> ·{" "}
        <a href="#style-privacy">Suas escolhas e seus dados</a>
        <details id="style-privacy">
          <summary>Como guardamos suas escolhas</summary>
          <p>
            {contextual
              ? "Guardamos suas respostas e seu progresso para recuperar esta prévia. Não pedimos e-mail."
              : "Guardamos seu e-mail, suas respostas e seu progresso para recuperar esta jornada."}{" "}
            Não precisamos de fotos, informações sobre seu corpo ou dados de
            outras pessoas. Para consultar, corrigir ou excluir seus dados, fale
            com contato@digicomdigital.com.br. Você pode baixar seus registros
            na área da jornada.
          </p>
          <p>
            O primeiro dia é gratuito. A continuidade proposta custa R$ 67, em
            pagamento único, com acesso por 90 dias e sem renovação automática.
            A compra ainda não está disponível nesta versão.
          </p>
        </details>
      </footer>
    </main>
  );
}
