import { MUSA_V15_EXPERIENCE_VERSION } from "./musaCommercialRoutes";

let volatileSessionId: string | undefined;

/** Identificador apenas analítico, sem função de autenticação; funciona também em prévias HTTP. */
function newSessionId() {
  return (
    window.crypto?.randomUUID?.() ??
    `musa-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
  );
}

/** Mede somente esta candidata e conserva marcadores de QA sem atribuir eventos ao experimento antigo. */
export function trackStyleEvent(eventType: string, requestId = "") {
  const params = new URLSearchParams(location.search);
  const qa =
    params.get("mh_preview") === "qa" || params.get("pde_analytics") === "off";
  let sessionId: string;
  try {
    sessionId =
      sessionStorage.getItem("musa-style-session") ||
      volatileSessionId ||
      newSessionId();
    sessionStorage.setItem("musa-style-session", sessionId);
  } catch {
    sessionId = volatileSessionId || newSessionId();
  }
  volatileSessionId = sessionId;
  const metadata: Record<string, unknown> = {
    sessionId,
    experienceVersion: MUSA_V15_EXPERIENCE_VERSION,
    layoutKey: "personal-style-v1",
    funnelVersion: "musa-personal-style-v1",
    deviceType: innerWidth < 700 ? "MOBILE" : "DESKTOP",
    userAgent: navigator.userAgent,
    requestId,
    idempotencyKey: `${sessionId}:${eventType}:${requestId}`,
    ...(qa ? { trafficType: "INTERNAL_QA", testMode: true } : {}),
  };
  for (const [key, value] of params.entries()) {
    if (
      ["utm_source", "utm_medium", "utm_campaign", "utm_content"].includes(key)
    ) {
      metadata[
        key.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase())
      ] = value.slice(0, 200);
    }
  }
  return fetch("/api/pde/access/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    keepalive: true,
    body: JSON.stringify({
      productSlug: "metodo-musa-7-dias",
      eventType,
      source: qa ? "INTERNAL_QA" : "pde-platform-frontend",
      pageUrl: `${location.origin}${location.pathname}${qa ? "?mh_preview=qa" : ""}`,
      metadata,
    }),
  }).catch(() => undefined);
}
