import { createHash } from "node:crypto";

export const PUBLIC_EVIDENCE_POLICY = "PUBLIC_SOURCES_V1";
const roles = new Set([
  "PUBLIC_CUSTOMER_REPORT",
  "SELLER_CLAIM",
  "EDITORIAL",
  "SCIENTIFIC",
  "OTHER",
]);
const actions = new Set([
  "PURCHASE_REPORTED",
  "ABANDONMENT_REPORTED",
  "USE_REPORTED",
  "FRUSTRATION_REPORTED",
  "UNKNOWN",
]);

/** Confere suporte coletado e independência sem transformar relato público em entrevista ou venda. */
export function assessPublicEvidence(
  observations,
  evidence,
  referencedIds = [],
) {
  if (!Array.isArray(observations))
    throw new Error(
      "Pesquisa pública exige publicObservations, ainda que vazio",
    );
  const sources = new Map(evidence.map((item) => [item.evidenceId, item]));
  const seen = new Set();
  const domains = new Set();
  const supported = observations.map((item) => {
    const source = sources.get(item.evidenceId);
    const excerpt = String(item.supportingExcerpt || "").trim();
    if (
      !source ||
      !referencedIds.includes(item.evidenceId) ||
      seen.has(item.evidenceId) ||
      !roles.has(item.sourceRole) ||
      !actions.has(item.reportedAction) ||
      excerpt.length < 15 ||
      excerpt.length > 500 ||
      !String(source.snippet || "").includes(excerpt) ||
      !String(item.limitation || "").trim()
    ) {
      throw new Error(
        "Observação pública sem suporte exato, papel válido ou referência única da candidata",
      );
    }
    const url = new URL(source.url);
    if (
      !["http:", "https:"].includes(url.protocol) ||
      !Number.isFinite(Date.parse(source.retrievedAt))
    ) {
      throw new Error(
        "Observação pública exige URL pública e data de coleta válida",
      );
    }
    seen.add(item.evidenceId);
    const host = url.hostname.toLowerCase();
    const domain = host
      .split(".")
      .slice(/\.(com|net|org|co|gov|ac|edu)\.[a-z]{2}$/.test(host) ? -3 : -2)
      .join(".");
    if (
      item.sourceRole === "PUBLIC_CUSTOMER_REPORT" &&
      item.reportedAction !== "UNKNOWN"
    )
      domains.add(domain);
    return {
      ...item,
      url: source.url,
      retrievedAt: source.retrievedAt,
      evidenceScope: "SEARCH_EXCERPT_ONLY",
      classification: "MODEL_CLASSIFIED",
      sourceSha256: createHash("sha256")
        .update(String(source.snippet))
        .digest("hex"),
    };
  });
  return {
    policy: PUBLIC_EVIDENCE_POLICY,
    observations: supported,
    independentBehavioralDomains: [...domains].sort(),
    ready: domains.size >= 2,
    limitation:
      "Relatos públicos e trechos de busca são sinais de terceiros; não verificam identidade, pagamento, utilidade ou vendas do nosso produto.",
  };
}
