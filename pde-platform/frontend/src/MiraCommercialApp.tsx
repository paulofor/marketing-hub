import React, { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { AdaptiveVideoPlayer } from "./AdaptiveVideoPlayer";

const productSlug = "pde-planejado-36";
const endpoint = "/api/pde/mira/commercial/v1";
const commercialVideo = "/media/mira-commercial-demo-v1.mp4";
const commercialVideoHls = "/media/mira-commercial-demo-v1-hls/index.m3u8";
const commercialVideoPoster = "/media/mira-commercial-demo-v1-poster.jpg";

type ProductInput = { name: string; labelDirections: string };
type RoutineCard = {
  productName: string;
  order: number;
  documentedDirection: string;
  safetyNote: string;
};
type Session = {
  experienceVersion: string;
  status: string;
  objective?: string;
  products: ProductInput[];
  routine: RoutineCard[];
  blocker?: string;
  attemptsUsed: number;
  attemptsLimit: number;
  events: string[];
  generatedAt?: string;
  completedAt?: string;
};
type Offer = {
  checkoutUrl: string;
  priceBrl: number;
  primaryCta: string;
  experimentId?: number;
  experienceVersion?: string;
  layoutKey?: string;
};

/** Entrega login, rotina e políticas da primeira versão comercial de Mira. */
export function MiraCommercialApp() {
  const legalPage = legalContent(window.location.pathname);
  const [accessToken, setAccessToken] = useState(
    () =>
      new URLSearchParams(window.location.hash.slice(1))
        .get("access")
        ?.trim() ||
      window.sessionStorage.getItem("mira-commercial-access") ||
      "",
  );
  const [session, setSession] = useState<Session | null>(null);
  const [email, setEmail] = useState("");
  const [objective, setObjective] = useState(
    "Organizar os produtos que já tenho em uma rotina simples",
  );
  const [products, setProducts] = useState<ProductInput[]>([
    { name: "", labelDirections: "" },
    { name: "", labelDirections: "" },
  ]);
  const [offer, setOffer] = useState<Offer | null>(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const pageViewed = useRef(false);
  const checkoutCtaViewed = useRef(false);
  const videoPlayed = useRef(false);
  const videoCompleted = useRef(false);

  useEffect(() => {
    document.title = legalPage?.title || "Mira · sua rotina organizada";
    const robots =
      document.querySelector('meta[name="robots"]') ||
      document.head.appendChild(document.createElement("meta"));
    robots.setAttribute("name", "robots");
    robots.setAttribute(
      "content",
      accessToken || window.location.pathname === "/access"
        ? "noindex, nofollow"
        : "index, follow",
    );
  }, [accessToken, legalPage?.title]);

  useEffect(() => {
    if (!window.location.hash) return;
    window.sessionStorage.setItem("mira-commercial-access", accessToken);
    window.history.replaceState({}, "", "/access");
  }, [accessToken]);

  useEffect(() => {
    if (legalPage || accessToken) return;
    if (!pageViewed.current) {
      pageViewed.current = true;
      void trackPublicEvent("PAGE_VIEW");
    }
    void fetch(`/api/pde/products/${productSlug}/commercial-offer?slotCode=v1`)
      .then(async (response) =>
        response.ok ? ((await response.json()) as Offer) : null,
      )
      .then(setOffer)
      .catch(() => setOffer(null));
  }, [accessToken, legalPage]);

  useEffect(() => {
    if (!offer?.checkoutUrl || checkoutCtaViewed.current) return;
    const cta = document.querySelector<HTMLElement>("[data-checkout-cta]");
    if (!cta) return;
    const recordViewed = () => {
      if (checkoutCtaViewed.current) return;
      checkoutCtaViewed.current = true;
      void trackPublicEvent("CTA_VIEWED", {
        experimentId: offer.experimentId ?? 93,
        placement: "hero",
      });
    };
    if (!("IntersectionObserver" in window)) {
      recordViewed();
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (
          entries.some(
            (entry) => entry.isIntersecting && entry.intersectionRatio >= 0.5,
          )
        ) {
          recordViewed();
          observer.disconnect();
        }
      },
      { threshold: 0.5 },
    );
    observer.observe(cta);
    return () => observer.disconnect();
  }, [offer]);

  useEffect(() => {
    if (!accessToken || legalPage) return;
    let active = true;
    void request<Session>("/session", { method: "GET" }, accessToken)
      .then((value) => {
        if (!active) return;
        setSession(value);
        if (value.objective) setObjective(value.objective);
        if (value.products?.length) setProducts(value.products);
      })
      .catch((cause) => {
        if (!active) return;
        setError(message(cause));
        window.sessionStorage.removeItem("mira-commercial-access");
        setAccessToken("");
      });
    return () => {
      active = false;
    };
  }, [accessToken, legalPage]);

  const attemptsLeft = useMemo(
    () => (session ? session.attemptsLimit - session.attemptsUsed : 2),
    [session],
  );

  async function requestLogin(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await fetch("/api/pde/access/login-link", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          productSlug,
          email,
          experienceVersion: "mira-commercial-v1",
        }),
      });
      if (!response.ok) throw new Error(await responseMessage(response));
      setNotice(
        "Enviamos seu link seguro para o e-mail usado na compra. Confira também spam e promoções.",
      );
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  async function generate(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await request<Session>(
        "/input",
        { method: "PUT", body: JSON.stringify({ objective, products }) },
        accessToken,
      );
      const ready = await request<Session>(
        "/generate",
        { method: "POST" },
        accessToken,
      );
      setSession(ready);
      if (
        ready.status === "READY" &&
        !ready.events.includes("READY_RESULT_USED")
      ) {
        setSession(
          await request<Session>(
            "/events",
            {
              method: "POST",
              body: JSON.stringify({ eventType: "READY_RESULT_USED" }),
            },
            accessToken,
          ),
        );
      }
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  async function mark(eventType: string) {
    setBusy(true);
    setError("");
    try {
      setSession(
        await request<Session>(
          "/events",
          { method: "POST", body: JSON.stringify({ eventType }) },
          accessToken,
        ),
      );
    } catch (cause) {
      setError(message(cause));
    } finally {
      setBusy(false);
    }
  }

  function changeProduct(
    index: number,
    field: keyof ProductInput,
    value: string,
  ) {
    setProducts((current) =>
      current.map((product, position) =>
        position === index ? { ...product, [field]: value } : product,
      ),
    );
  }

  if (legalPage) return <LegalPage {...legalPage} />;
  if (!accessToken) {
    return (
      <main className="mira-commercial-shell">
        <section className="mira-commercial-hero">
          <p className="mira-commercial-kicker">
            Mira · rotina simples para pele madura
          </p>
          <h1>Cuide de você com mais clareza, usando o que já tem.</h1>
          <p className="mira-commercial-lead">
            Mira lê as orientações que você informa e organiza uma ordem
            prática, sem diagnóstico, prescrição ou empurrar novos cosméticos.
          </p>
          <AdaptiveVideoPlayer
            src={commercialVideoHls}
            fallbackSrc={commercialVideo}
            controls
            playsInline
            preload="metadata"
            poster={commercialVideoPoster}
            ariaLabel="Demonstração de Mira"
            onPlaybackEvent={(event) => {
              if (event.type === "play" && !videoPlayed.current) {
                videoPlayed.current = true;
                void trackPublicEvent("VIDEO_PLAY", {
                  experimentId: 93,
                  creativeVariant: "mira-commercial-demo-v1",
                });
              }
              if (event.type === "ended" && !videoCompleted.current) {
                videoCompleted.current = true;
                void trackPublicEvent("VIDEO_COMPLETED", {
                  experimentId: 93,
                  creativeVariant: "mira-commercial-demo-v1",
                });
              }
            }}
          />
          <div className="mira-commercial-proof">
            <strong>Veja antes de decidir:</strong> entrada guiada, resultado
            claro e duas organizações incluídas por R$ 49, em pagamento único.
          </div>
          {offer?.checkoutUrl && (
            <a
              className="mira-commercial-primary"
              data-checkout-cta
              href={offer.checkoutUrl}
              onClick={() => {
                if (!checkoutCtaViewed.current) {
                  checkoutCtaViewed.current = true;
                  void trackPublicEvent("CTA_VIEWED", {
                    experimentId: offer.experimentId ?? 93,
                    placement: "hero",
                  });
                }
                void trackPublicEvent("CHECKOUT_STARTED", {
                  idempotencyKey: stableActionId(
                    "checkout",
                    `${offer.experienceVersion || "mira-commercial-v1"}:${offer.experimentId ?? 93}`,
                  ),
                  experimentId: offer.experimentId ?? 93,
                  priceBrl: offer.priceBrl,
                  checkoutHost: new URL(offer.checkoutUrl).hostname,
                });
              }}
            >
              {offer.primaryCta || "Quero organizar minha rotina por R$ 49"}
            </a>
          )}
        </section>
        <section className="mira-commercial-card" id="acesso">
          <h2>Já comprou? Seu primeiro passo leva menos de um minuto.</h2>
          <p>
            Use o mesmo e-mail informado no pagamento para receber seu link
            seguro.
          </p>
          <form onSubmit={requestLogin}>
            <label htmlFor="mira-email">E-mail da compra</label>
            <input
              id="mira-email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
            <button type="submit" disabled={busy}>
              {busy ? "Verificando..." : "Receber meu link de acesso"}
            </button>
          </form>
          {notice && <p className="mira-commercial-success">{notice}</p>}
          {error && (
            <p className="mira-commercial-error" role="alert">
              {error}
            </p>
          )}
        </section>
        <Footer />
      </main>
    );
  }

  if (!session) {
    return (
      <main className="mira-commercial-shell">
        <p>Preparando sua área segura…</p>
      </main>
    );
  }

  return (
    <main className="mira-commercial-shell">
      <header className="mira-commercial-header">
        <div>
          <span>Mira</span>
          <strong>Sua rotina organizada</strong>
        </div>
        <span>
          {attemptsLeft}{" "}
          {attemptsLeft === 1
            ? "organização disponível"
            : "organizações disponíveis"}
        </span>
      </header>
      {session.status === "READY" ? (
        <section className="mira-commercial-card">
          <p className="mira-commercial-kicker">Sua rotina está pronta</p>
          <h1>Uma ordem simples, baseada nos rótulos que você informou.</h1>
          <ol className="mira-routine-list">
            {session.routine.map((card) => (
              <li key={`${card.order}-${card.productName}`}>
                <span>{card.order / 10}</span>
                <div>
                  <strong>{card.productName}</strong>
                  <p>{card.documentedDirection}</p>
                  <small>{card.safetyNote}</small>
                </div>
              </li>
            ))}
          </ol>
          {!session.events.includes("FIRST_USE") ? (
            <button onClick={() => void mark("FIRST_USE")} disabled={busy}>
              Já usei esta rotina
            </button>
          ) : !session.completedAt ? (
            <button
              onClick={() => void mark("JOURNEY_COMPLETED")}
              disabled={busy}
            >
              Concluir minha primeira aplicação
            </button>
          ) : (
            <p className="mira-commercial-success">
              Primeira aplicação concluída. Sua rotina permanece salva aqui.
            </p>
          )}
          {attemptsLeft > 0 && (
            <button
              className="mira-commercial-secondary"
              onClick={() => setSession({ ...session, status: "INPUT_READY" })}
            >
              Organizar outra combinação
            </button>
          )}
        </section>
      ) : (
        <section className="mira-commercial-card">
          <p className="mira-commercial-kicker">
            Passo único · tenha os rótulos à mão
          </p>
          <h1>Conte o que você já usa.</h1>
          <p>
            Mira organiza somente instruções documentadas. Se faltar informação
            segura, ela explica o que precisa.
          </p>
          <form onSubmit={generate}>
            <label htmlFor="mira-objective">O que você quer facilitar?</label>
            <input
              id="mira-objective"
              value={objective}
              onChange={(event) => setObjective(event.target.value)}
              required
            />
            {products.map((product, index) => (
              <fieldset key={index}>
                <legend>Produto {index + 1}</legend>
                <label>Nome</label>
                <input
                  value={product.name}
                  onChange={(event) =>
                    changeProduct(index, "name", event.target.value)
                  }
                  required
                />
                <label>Orientação escrita no rótulo</label>
                <textarea
                  value={product.labelDirections}
                  onChange={(event) =>
                    changeProduct(index, "labelDirections", event.target.value)
                  }
                  required
                />
              </fieldset>
            ))}
            {products.length < 12 && (
              <button
                type="button"
                className="mira-commercial-secondary"
                onClick={() =>
                  setProducts((current) => [
                    ...current,
                    { name: "", labelDirections: "" },
                  ])
                }
              >
                Adicionar produto
              </button>
            )}
            <button type="submit" disabled={busy}>
              {busy ? "Organizando..." : "Organizar minha rotina"}
            </button>
          </form>
          {session.blocker && (
            <p className="mira-commercial-error" role="alert">
              {session.blocker}
            </p>
          )}
        </section>
      )}
      {error && (
        <p className="mira-commercial-error" role="alert">
          {error}
        </p>
      )}
      <Footer />
    </main>
  );
}

function Footer() {
  return (
    <footer>
      <a href="/terms">Termos</a>
      <a href="/privacy">Privacidade</a>
      <a href="/refund-policy">Reembolso</a>
      <a href="mailto:contato@digicomdigital.com.br">Suporte</a>
    </footer>
  );
}

function LegalPage({
  title,
  paragraphs,
}: {
  title: string;
  paragraphs: string[];
}) {
  return (
    <main className="mira-commercial-shell">
      <section className="mira-commercial-card">
        <p className="mira-commercial-kicker">Mira · Digicom Digital</p>
        <h1>{title}</h1>
        {paragraphs.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
        <a href="/">Voltar para Mira</a>
      </section>
      <Footer />
    </main>
  );
}

function legalContent(path: string) {
  if (path === "/terms")
    return {
      title: "Termos de uso",
      paragraphs: [
        "Mira organiza, em até duas tentativas, os produtos que a cliente informa a partir das orientações documentadas nos rótulos.",
        "Mira não realiza diagnóstico, prescrição ou tratamento e não substitui orientação profissional.",
      ],
    };
  if (path === "/privacy")
    return {
      title: "Privacidade",
      paragraphs: [
        "Os dados são usados para liberar o acesso, salvar a rotina e prestar suporte. A cliente pode solicitar acesso, correção, oposição ou exclusão dentro da área autenticada.",
        "Credenciais de acesso não são incluídas em URLs de servidor, logs públicos ou relatórios comerciais.",
      ],
    };
  if (path === "/refund-policy")
    return {
      title: "Política de reembolso",
      paragraphs: [
        "A compra possui a garantia legal aplicável. Solicite atendimento pelo e-mail contato@digicomdigital.com.br informando o e-mail usado no pagamento.",
        "Quando o provedor confirmar o reembolso, o acesso é encerrado automaticamente.",
      ],
    };
  return null;
}

async function request<T>(
  path: string,
  init: RequestInit,
  token: string,
): Promise<T> {
  const response = await fetch(`${endpoint}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-PDE-Access-Token": token,
      ...(init.headers || {}),
    },
  });
  if (!response.ok) throw new Error(await responseMessage(response));
  return (await response.json()) as T;
}

async function responseMessage(response: Response) {
  try {
    const body = (await response.json()) as {
      message?: string;
      detail?: string;
    };
    return body.message || body.detail || "Não foi possível concluir agora.";
  } catch {
    return "Não foi possível concluir agora.";
  }
}

function message(cause: unknown) {
  return cause instanceof Error
    ? cause.message
    : "Não foi possível concluir agora.";
}

/** Registra o funil público sem bloquear a compra nem incluir credenciais na telemetria. */
async function trackPublicEvent(
  eventType: string,
  metadata: Record<string, unknown> = {},
) {
  const parameters = new URLSearchParams(window.location.search);
  if (parameters.get("pde_analytics")?.toLowerCase() === "off") return;
  const qa =
    parameters.get("mh_test") === "1" || parameters.get("mh_preview") === "qa";
  const trackingUrl = new URL(window.location.href);
  trackingUrl.hash = "";
  try {
    await fetch("/api/pde/access/events", {
      method: "POST",
      keepalive: true,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        productSlug,
        eventType,
        source: qa ? "mh_test" : "pde-mira-commercial",
        pageUrl: trackingUrl.toString(),
        metadata: {
          visitorId: stableTrackingId(
            "mira-commercial-visitor",
            window.localStorage,
          ),
          sessionId: stableTrackingId(
            "mira-commercial-session",
            window.sessionStorage,
          ),
          experienceVersion: "mira-commercial-v1",
          layoutKey: "mira-routine-v1",
          experimentId: 93,
          path:
            window.location.pathname === "/access"
              ? "/access"
              : window.location.pathname,
          referrerUrl: document.referrer,
          deviceType: window.innerWidth < 768 ? "mobile" : "desktop",
          viewportWidth: window.innerWidth,
          viewportHeight: window.innerHeight,
          utmSource: parameters.get("utm_source"),
          utmMedium: parameters.get("utm_medium"),
          utmCampaign: parameters.get("utm_campaign"),
          utmContent: parameters.get("utm_content"),
          utmTerm: parameters.get("utm_term"),
          ...(qa
            ? { mh_internal_test: true, trafficQuality: "INTERNAL_QA" }
            : {}),
          ...metadata,
        },
      }),
    });
  } catch {
    // A telemetria é auxiliar e nunca pode interromper a experiência comercial.
  }
}

/** Mantém identificadores first-party opacos por visitante e sessão. */
function stableTrackingId(key: string, storage: Storage) {
  const existing = storage.getItem(key);
  if (existing) return existing;
  const generated =
    window.crypto?.randomUUID?.() ??
    `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  storage.setItem(key, generated);
  return generated;
}

/** Cria uma idempotência opaca por ação sem persistir checkout ou dado pessoal. */
function stableActionId(action: string, correlation: string) {
  let fingerprint = 2166136261;
  for (let index = 0; index < correlation.length; index += 1) {
    fingerprint ^= correlation.charCodeAt(index);
    fingerprint = Math.imul(fingerprint, 16777619);
  }
  const key = `mira-commercial-action:${action}:${(fingerprint >>> 0).toString(16)}`;
  return `${action}:${stableTrackingId(key, window.sessionStorage)}`;
}
