import React, { useEffect, useState } from "react";

type ProductInput = { name: string; labelDirections: string };
type RoutineCard = { productName: string; order: number; documentedDirection: string; safetyNote: string };
type Session = {
  sessionToken: string;
  participantReference: string;
  trafficClass: string;
  status: string;
  ageRange?: string;
  objective?: string;
  products: ProductInput[];
  routine: RoutineCard[];
  blocker?: string;
  events: string[];
  prototypeVersion: string;
  checkoutMode: string;
};

const endpoint = "/api/pde/mira/private/v1";

/** Renderiza o protótipo privado de Mira sem publicação, cobrança ou mídia. */
export function MiraPrivatePrototype() {
  const [accessToken] = useState(() => {
    const token = window.location.pathname.split("/").filter(Boolean)[1] || "";
    if (token) window.history.replaceState({}, "", "/mira-private");
    return token;
  });
  const [session, setSession] = useState<Session | null>(null);
  const [consent, setConsent] = useState(false);
  const [ageRange, setAgeRange] = useState("45-54");
  const [objective, setObjective] = useState("Organizar os produtos que já possuo em uma rotina simples");
  const [products, setProducts] = useState<ProductInput[]>([
    { name: "", labelDirections: "" },
    { name: "", labelDirections: "" },
  ]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const sessionToken = session?.sessionToken || window.sessionStorage.getItem("mira-private-session") || "";

  useEffect(() => {
    document.title = "Mira · validação privada";
    const robots = document.querySelector('meta[name="robots"]') || document.head.appendChild(document.createElement("meta"));
    robots.setAttribute("name", "robots");
    robots.setAttribute("content", "noindex, nofollow, noarchive");
  }, []);

  useEffect(() => {
    if (!sessionToken || session) return;
    void request<Session>("/session", { method: "GET" }, sessionToken).then(setSession).catch(() => {
      window.sessionStorage.removeItem("mira-private-session");
    });
  }, [session, sessionToken]);

  async function begin() {
    setBusy(true); setError("");
    try {
      const created = await request<Session>("/access", {
        method: "POST", body: JSON.stringify({ accessToken, consentAccepted: consent }),
      });
      window.sessionStorage.setItem("mira-private-session", created.sessionToken);
      setSession(created);
    } catch (cause) { setError(message(cause)); } finally { setBusy(false); }
  }

  async function generate() {
    setBusy(true); setError("");
    try {
      await request<Session>("/input", { method: "PUT", body: JSON.stringify({ ageRange, objective, products }) }, sessionToken);
      setSession(await request<Session>("/generate", { method: "POST" }, sessionToken));
    } catch (cause) { setError(message(cause)); } finally { setBusy(false); }
  }

  async function record(eventType: string, confirmed?: boolean) {
    setBusy(true); setError("");
    try {
      setSession(await request<Session>("/events", { method: "POST", body: JSON.stringify({ eventType, confirmed }) }, sessionToken));
    } catch (cause) { setError(message(cause)); } finally { setBusy(false); }
  }

  if (!session) {
    return <main className="mira-private-shell">
      <p className="mira-private-kicker">Mira · validação privada</p>
      <h1>Sua rotina, organizada com calma</h1>
      <p>Use somente informações dos produtos que você já possui. Mira organiza o que está documentado, sem diagnosticar, prescrever ou recomendar compras.</p>
      {!accessToken && <div className="mira-private-alert">Abra o acesso privado individual recebido para esta leitura.</div>}
      <label className="mira-private-consent"><input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} /> Aceito participar desta leitura privada e registrar eventos de uso pseudonimizados.</label>
      {error && <div className="mira-private-alert" role="alert">{error}</div>}
      <button className="primary-button" disabled={!accessToken || !consent || busy} onClick={() => void begin()}>{busy ? "Preparando…" : "Começar leitura privada"}</button>
      <small>Sem pagamento, publicação, anúncio ou contato automatizado.</small>
    </main>;
  }

  if (session.status === "READY") {
    const used = session.events.includes("READY_RESULT_USED");
    const preferred = session.events.includes("PREFERRED_OVER_FREE");
    const checkout = session.events.includes("CHECKOUT_STARTED");
    return <main className="mira-private-shell">
      <p className="mira-private-kicker">Rotina pronta · {session.prototypeVersion}</p>
      <h1>Uma ordem simples para consultar</h1>
      <p>Esta organização usa somente o texto de rótulo informado e não substitui avaliação profissional.</p>
      <div className="mira-routine-grid">{session.routine.map((card, index) => <article key={card.productName}>
        <span>Passo {index + 1}</span><h2>{card.productName}</h2><p>{card.documentedDirection}</p><small>{card.safetyNote}</small>
      </article>)}</div>
      <button className="primary-button" disabled={used || busy} onClick={() => void record("READY_RESULT_USED")}>{used ? "Resultado consultado" : "Marcar uma parte como consultada"}</button>
      {used && <section className="mira-private-question"><h2>Isso foi mais útil que organizar conteúdos gratuitos por conta própria?</h2>
        <button className="secondary-button" disabled={preferred || busy} onClick={() => void record("PREFERRED_OVER_FREE", true)}>{preferred ? "Preferência registrada" : "Sim, prefiro a rotina pronta"}</button></section>}
      {preferred && <section className="mira-private-question"><h2>Você consideraria avançar por R$ 49?</h2><p>Simulação de intenção: não há cobrança nem dados de pagamento.</p>
        <button className="primary-button" disabled={checkout || busy} onClick={() => void record("CHECKOUT_STARTED")}>{checkout ? "Simulação concluída" : "Simular avanço — sem cobrança"}</button></section>}
      {checkout && <div className="mira-private-success">Leitura concluída. Nenhuma compra foi realizada.</div>}
      {error && <div className="mira-private-alert" role="alert">{error}</div>}
    </main>;
  }

  return <main className="mira-private-shell">
    <p className="mira-private-kicker">Entrada guiada · 3 passos</p><h1>Conte o mínimo necessário</h1>
    <label>Faixa etária<select value={ageRange} onChange={(event) => setAgeRange(event.target.value)}><option>35-44</option><option>45-54</option><option>55-60</option></select></label>
    <label>Objetivo de autocuidado<textarea value={objective} maxLength={500} onChange={(event) => setObjective(event.target.value)} /></label>
    <h2>Produtos e orientação do rótulo</h2>
    {products.map((product, index) => <fieldset key={index}><legend>Produto {index + 1}</legend>
      <label>Nome<input value={product.name} onChange={(event) => setProducts(replace(products, index, { ...product, name: event.target.value }))} /></label>
      <label>Como o rótulo orienta usar<textarea value={product.labelDirections} onChange={(event) => setProducts(replace(products, index, { ...product, labelDirections: event.target.value }))} /></label>
    </fieldset>)}
    <button className="secondary-button" onClick={() => setProducts([...products, { name: "", labelDirections: "" }])}>Adicionar produto</button>
    {session.blocker && <div className="mira-private-alert" role="alert">{session.blocker}</div>}
    {error && <div className="mira-private-alert" role="alert">{error}</div>}
    <button className="primary-button" disabled={busy || products.some((product) => !product.name.trim() || !product.labelDirections.trim())} onClick={() => void generate()}>{busy ? "Organizando…" : "Gerar rotina segura"}</button>
  </main>;
}

/** Substitui somente o produto editado sem misturar entradas da sessão. */
function replace(values: ProductInput[], index: number, value: ProductInput) {
  return values.map((current, currentIndex) => currentIndex === index ? value : current);
}

/** Chama exclusivamente a API do próprio módulo e preserva sua mensagem controlada. */
async function request<T>(path: string, init: RequestInit, sessionToken = ""): Promise<T> {
  const response = await fetch(endpoint + path, { ...init, headers: { "Content-Type": "application/json", ...(sessionToken ? { "X-Mira-Session": sessionToken } : {}) } });
  const body = await response.json();
  if (!response.ok) throw new Error(body.error || "Não foi possível continuar a leitura privada.");
  return body as T;
}

/** Converte uma falha desconhecida em orientação legível. */
function message(cause: unknown) {
  return cause instanceof Error ? cause.message : "Não foi possível continuar a leitura privada.";
}
