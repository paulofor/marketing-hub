import React, { useEffect, useState } from "react";

type ProductInput = { name: string; labelDirections: string };
type RoutineCard = {
  productName: string;
  order: number;
  documentedDirection: string;
  safetyNote: string;
};
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
  readingFinished: boolean;
};

const endpoint = "/api/pde/mira/private/v1";

/** Renderiza o protótipo privado de Mira sem publicação, cobrança ou mídia. */
export function MiraPrivatePrototype() {
  const [accessToken, setAccessToken] = useState(
    () =>
      new URLSearchParams(window.location.hash.slice(1))
        .get("access")
        ?.trim() || "",
  );
  const [session, setSession] = useState<Session | null>(null);
  const [consent, setConsent] = useState(false);
  const [ageRange, setAgeRange] = useState("45-54");
  const [objective, setObjective] = useState(
    "Organizar os produtos que já possuo em uma rotina simples",
  );
  const [products, setProducts] = useState<ProductInput[]>([
    { name: "", labelDirections: "" },
    { name: "", labelDirections: "" },
  ]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const sessionToken =
    session?.sessionToken ||
    (!accessToken
      ? window.sessionStorage.getItem("mira-private-session")
      : "") ||
    "";

  useEffect(() => {
    if (window.location.hash)
      window.history.replaceState({}, "", "/mira-private");
    if (accessToken) window.sessionStorage.removeItem("mira-private-session");
  }, [accessToken]);

  useEffect(() => {
    // Um novo convite pode chegar por navegação de fragmento sem remontar o React.
    const openNewInvitation = () => {
      if (new URLSearchParams(window.location.hash.slice(1)).get("access"))
        window.location.reload();
    };
    window.addEventListener("hashchange", openNewInvitation);
    return () => window.removeEventListener("hashchange", openNewInvitation);
  }, []);

  useEffect(() => {
    document.title = "Sua rotina, organizada com calma";
    const robots =
      document.querySelector('meta[name="robots"]') ||
      document.head.appendChild(document.createElement("meta"));
    robots.setAttribute("name", "robots");
    robots.setAttribute("content", "noindex, nofollow, noarchive");
  }, []);

  useEffect(() => {
    if (!sessionToken || session) return;
    let active = true;
    void request<Session>("/session", { method: "GET" }, sessionToken)
      .then((restored) => {
        if (active) setSession(restored);
      })
      .catch(() => {
        if (active) window.sessionStorage.removeItem("mira-private-session");
      });
    return () => {
      active = false;
    };
  }, [session, sessionToken]);

  useEffect(() => {
    if (!session) return;
    if (session.ageRange) setAgeRange(session.ageRange);
    if (session.objective) setObjective(session.objective);
    if (session.products.length) setProducts(session.products);
  }, [session]);

  async function begin() {
    setBusy(true);
    setError("");
    try {
      const created = await request<Session>("/access", {
        method: "POST",
        body: JSON.stringify({ accessToken, consentAccepted: consent }),
      });
      window.sessionStorage.setItem(
        "mira-private-session",
        created.sessionToken,
      );
      setSession(created);
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  async function generate() {
    setBusy(true);
    setError("");
    try {
      await request<Session>(
        "/input",
        {
          method: "PUT",
          body: JSON.stringify({ ageRange, objective, products }),
        },
        sessionToken,
      );
      setSession(
        await request<Session>("/generate", { method: "POST" }, sessionToken),
      );
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  async function record(eventType: string, confirmed?: boolean) {
    setBusy(true);
    setError("");
    try {
      setSession(
        await request<Session>(
          "/events",
          { method: "POST", body: JSON.stringify({ eventType, confirmed }) },
          sessionToken,
        ),
      );
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  /** Preserva uma resposta negativa ou uma dificuldade sem inventar sinais positivos. */
  async function finish() {
    setBusy(true);
    setError("");
    try {
      setSession(
        await request<Session>("/finish", { method: "POST" }, sessionToken),
      );
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  if (!session) {
    return (
      <main className="mira-private-shell">
        <p className="mira-private-kicker">
          Sua rotina de cuidados · acesso privado
        </p>
        <h1>Sua rotina, organizada com calma</h1>
        <p>
          Organize os cuidados com os produtos que você já tem, usando as
          instruções dos rótulos. Sem diagnóstico, prescrição ou recomendação de
          novas compras.
        </p>
        <ol aria-label="Como funciona">
          <li>Separe os produtos que já usa e tenha os rótulos à mão.</li>
          <li>
            Informe as orientações de uso e consulte sua rotina organizada.
          </li>
          <li>
            Conte se a experiência ajudou. Respostas negativas também são
            bem-vindas.
          </li>
        </ol>
        <p>
          Abra seu convite individual ou cole abaixo o código recebido. Cada
          convite pertence a uma pessoa; não use o convite de outra
          participante.
        </p>
        <label>
          <span>
            Código do convite <span aria-hidden="true">*</span>
          </span>
          <input
            type="password"
            autoComplete="off"
            value={accessToken}
            maxLength={512}
            onChange={(event) => setAccessToken(event.target.value.trim())}
          />
        </label>
        {!accessToken && (
          <div className="mira-private-alert">
            Ainda não recebeu um convite? Peça o acesso privado ao responsável
            que está acompanhando sua experiência.
          </div>
        )}
        <label className="mira-private-consent">
          <input
            type="checkbox"
            checked={consent}
            onChange={(event) => setConsent(event.target.checked)}
          />{" "}
          Aceito participar desta experiência privada e permitir o registro do
          meu uso com um código de participante, sem informar meu nome.
        </label>
        {error && (
          <div className="mira-private-alert" role="alert">
            {error}
          </div>
        )}
        <button
          className="primary-button"
          disabled={!accessToken || !consent || busy}
          onClick={() => void begin()}
        >
          {busy ? "Preparando…" : "Começar leitura privada"}
        </button>
        <small>
          Não há cobrança nem pedido de dados de pagamento nesta experiência.
        </small>
      </main>
    );
  }

  if (session.status === "READY") {
    const used = session.events.includes("READY_RESULT_USED");
    const preferred = session.events.includes("PREFERRED_OVER_FREE");
    const checkout = session.events.includes("CHECKOUT_STARTED");
    const finished = session.readingFinished;
    return (
      <main className="mira-private-shell">
        <p className="mira-private-kicker">Sua rotina de cuidados</p>
        <h1>Uma ordem simples para consultar</h1>
        <p>
          Esta organização usa somente o texto de rótulo informado e não
          substitui avaliação profissional.
        </p>
        <div className="mira-routine-grid">
          {session.routine.map((card, index) => (
            <article key={card.productName}>
              <span>Passo {index + 1}</span>
              <h2>{card.productName}</h2>
              <p>{card.documentedDirection}</p>
              <small>{card.safetyNote}</small>
            </article>
          ))}
        </div>
        <button
          className="primary-button"
          disabled={used || busy || finished}
          onClick={() => void record("READY_RESULT_USED")}
        >
          {used ? "Resultado consultado" : "Marcar uma parte como consultada"}
        </button>
        {used && (!finished || preferred) && (
          <section className="mira-private-question">
            <h2>
              Isso foi mais útil que organizar conteúdos gratuitos por conta
              própria?
            </h2>
            <button
              className="secondary-button"
              disabled={preferred || busy}
              onClick={() => void record("PREFERRED_OVER_FREE", true)}
            >
              {preferred
                ? "Preferência registrada"
                : "Sim, prefiro a rotina pronta"}
            </button>
          </section>
        )}
        {used && !preferred && !finished && (
          <button
            className="secondary-button"
            disabled={busy}
            onClick={() => void finish()}
          >
            Não, prefiro a alternativa gratuita
          </button>
        )}
        {preferred && !finished && !checkout && (
          <section className="mira-private-question">
            <h2>Você consideraria avançar por R$ 49?</h2>
            <p>
              Simulação de intenção: não há cobrança nem dados de pagamento.
            </p>
            <button
              className="primary-button"
              disabled={checkout || busy}
              onClick={() => void record("CHECKOUT_STARTED")}
            >
              {checkout
                ? "Simulação concluída"
                : "Simular avanço — sem cobrança"}
            </button>
          </section>
        )}
        {preferred && !finished && !checkout && (
          <button
            className="secondary-button"
            disabled={busy}
            onClick={() => void finish()}
          >
            Não avançaria por esse valor
          </button>
        )}
        {finished && (
          <div className="mira-private-success">
            Leitura encerrada. Sua resposta foi preservada. Nenhuma compra foi
            realizada. Obrigada por compartilhar sua opinião. Avise a pessoa que
            acompanha sua experiência que você terminou.
          </div>
        )}
        {checkout && (
          <>
            <button className="primary-button" disabled>
              Simulação concluída
            </button>
            {!finished && (
              <>
                <p>Nenhuma compra foi realizada.</p>
                <button
                  className="secondary-button"
                  disabled={busy}
                  onClick={() => void finish()}
                >
                  Encerrar leitura
                </button>
              </>
            )}
          </>
        )}
        {!finished && !used && (
          <button
            className="secondary-button"
            disabled={busy}
            onClick={() => void finish()}
          >
            Não consegui usar; encerrar leitura
          </button>
        )}
        {error && (
          <div className="mira-private-alert" role="alert">
            {error}
          </div>
        )}
      </main>
    );
  }

  if (session.readingFinished)
    return (
      <main className="mira-private-shell">
        <h1>Leitura encerrada</h1>
        <p>
          Sua dificuldade foi registrada para melhorar esta experiência. Nenhuma
          compra foi realizada. Avise a pessoa que acompanha sua experiência que
          você terminou.
        </p>
      </main>
    );

  return (
    <main className="mira-private-shell">
      <p className="mira-private-kicker">Entrada guiada · 3 passos</p>
      <h1>Conte o mínimo necessário</h1>
      <label>
        Faixa etária
        <select
          value={ageRange}
          onChange={(event) => setAgeRange(event.target.value)}
        >
          <option>35-44</option>
          <option>45-54</option>
          <option>55-60</option>
        </select>
      </label>
      <label>
        Objetivo de autocuidado
        <textarea
          value={objective}
          maxLength={500}
          onChange={(event) => setObjective(event.target.value)}
        />
      </label>
      <h2>Produtos e orientação do rótulo</h2>
      {products.map((product, index) => (
        <fieldset key={index}>
          <legend>Produto {index + 1}</legend>
          <label>
            Nome
            <input
              value={product.name}
              onChange={(event) =>
                setProducts(
                  replace(products, index, {
                    ...product,
                    name: event.target.value,
                  }),
                )
              }
            />
          </label>
          <label>
            Como o rótulo orienta usar
            <textarea
              value={product.labelDirections}
              onChange={(event) =>
                setProducts(
                  replace(products, index, {
                    ...product,
                    labelDirections: event.target.value,
                  }),
                )
              }
            />
          </label>
        </fieldset>
      ))}
      <button
        className="secondary-button"
        disabled={busy || products.length >= 12}
        onClick={() =>
          setProducts([...products, { name: "", labelDirections: "" }])
        }
      >
        Adicionar produto
      </button>
      {session.blocker && (
        <div className="mira-private-alert" role="alert">
          {session.blocker}
        </div>
      )}
      {error && (
        <div className="mira-private-alert" role="alert">
          {error}
        </div>
      )}
      <button
        className="primary-button"
        disabled={
          busy ||
          products.some(
            (product) =>
              !product.name.trim() || !product.labelDirections.trim(),
          )
        }
        onClick={() => void generate()}
      >
        {busy ? "Organizando…" : "Gerar rotina segura"}
      </button>
      <button
        className="secondary-button"
        disabled={busy}
        onClick={() => void finish()}
      >
        Não consegui continuar; encerrar leitura
      </button>
    </main>
  );
}

/** Substitui somente o produto editado sem misturar entradas da sessão. */
function replace(values: ProductInput[], index: number, value: ProductInput) {
  return values.map((current, currentIndex) =>
    currentIndex === index ? value : current,
  );
}

/** Chama exclusivamente a API do próprio módulo e preserva sua mensagem controlada. */
async function request<T>(
  path: string,
  init: RequestInit,
  sessionToken = "",
): Promise<T> {
  const response = await fetch(endpoint + path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(sessionToken ? { "X-Mira-Session": sessionToken } : {}),
    },
  });
  const body = await response.json();
  if (!response.ok)
    throw new Error(
      body.error || "Não foi possível continuar a leitura privada.",
    );
  return body as T;
}

/** Converte uma falha desconhecida em orientação legível. */
function message(cause: unknown) {
  return cause instanceof Error
    ? cause.message
    : "Não foi possível continuar a leitura privada.";
}
