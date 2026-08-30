import { readFile } from "node:fs/promises";

const MANDATORY_COLLECTIONS = ["gartner", "ia-aplicada"];
const B2C_COLLECTION = "momentos-de-compra-b2c";
let cachedLibrary;

/** Seleciona trechos aderentes e registra a cobertura integral das coleções vivas consultadas. */
export async function selectResearchLibraryContext(job, options = {}) {
  const library = await loadLibrary(options.indexUrl);
  const eligible = library.documents.filter((document) => document.evidenceEligible);
  const requiredCollections = [
    ...MANDATORY_COLLECTIONS,
    ...(isConsumerInstagram(job) ? [B2C_COLLECTION] : []),
  ];
  const terms = meaningfulTerms(
    [
      job.theme,
      job.targetAudience,
      job.objective,
      job.acquisitionChannel,
      job.referenceSources,
    ]
      .filter(Boolean)
      .join(" "),
  );
  const scored = eligible
    .map((document) => ({ document, score: relevance(document, terms) }))
    .sort(
      (left, right) =>
        right.score - left.score ||
        String(right.document.date || "").localeCompare(
          String(left.document.date || ""),
        ) ||
        left.document.path.localeCompare(right.document.path),
    );
  const selected = [];
  for (const collection of requiredCollections) {
    const candidate = scored.find(
      (item) =>
        item.document.collection === collection &&
        !selected.includes(item.document),
    );
    if (candidate) selected.push(candidate.document);
  }
  for (const item of scored) {
    if (selected.length >= Number(options.limit || 7)) break;
    if (!selected.includes(item.document)) selected.push(item.document);
  }
  const maxExcerptChars = Number(options.maxExcerptChars || 2200);
  return {
    evidence: selected.map((document, index) => ({
      evidenceId: `R${index + 1}`,
      sourceType: "REPOSITORY_RESEARCH",
      path: document.path,
      collection: document.collection,
      title: document.title,
      date: document.date,
      sha256: document.sha256,
      excerpt: relevantExcerpt(document.content, terms, maxExcerptChars),
    })),
    coverage: requiredCollections.map((collection) => {
      const documents = eligible.filter(
        (document) => document.collection === collection,
      );
      return {
        collection,
        status: documents.length > 0 ? "CONSULTED" : "EMPTY",
        documentCount: documents.length,
        documents: documents.map((document) => ({
          path: document.path,
          sha256: document.sha256,
          date: document.date,
        })),
      };
    }),
  };
}

/** Carrega uma única vez o índice versionado presente na imagem do executor. */
async function loadLibrary(indexUrl) {
  if (indexUrl) {
    return JSON.parse(await readFile(indexUrl, "utf8"));
  }
  if (!cachedLibrary) {
    cachedLibrary = JSON.parse(
      await readFile(
        new URL("../research-library/index.json", import.meta.url),
        "utf8",
      ),
    );
  }
  return cachedLibrary;
}

/** Pontua título, caminho e conteúdo sem converter recorrência em prova comercial. */
function relevance(document, terms) {
  const title = normalize(`${document.collection} ${document.title}`);
  const content = normalize(document.content);
  return terms.reduce(
    (score, term) =>
      score +
      (title.includes(term) ? 8 : 0) +
      Math.min(5, occurrences(content, term)),
    0,
  );
}

/** Recorta o trecho próximo à primeira correspondência e preserva contexto suficiente. */
function relevantExcerpt(content, terms, maxChars) {
  const normalizedContent = normalize(content);
  const firstMatch = terms
    .map((term) => normalizedContent.indexOf(term))
    .filter((index) => index >= 0)
    .sort((left, right) => left - right)[0];
  const center = firstMatch === undefined ? 0 : firstMatch;
  const start = Math.max(0, center - Math.floor(maxChars / 4));
  const end = Math.min(content.length, start + maxChars);
  return content.slice(start, end).trim();
}

/** Extrai palavras informativas do briefing para escolher contexto sem depender de modelo. */
function meaningfulTerms(value) {
  return [
    ...new Set(
      normalize(value)
        .split(/[^\p{L}\p{N}]+/u)
        .filter((term) => term.length >= 4)
        .filter(
          (term) =>
            ![
              "para",
              "como",
              "mais",
              "pelo",
              "pela",
              "entre",
              "mercado",
              "instagram",
            ].includes(term),
        ),
    ),
  ].slice(0, 24);
}

/** Normaliza acentos somente para comparação local, preservando o texto original no artefato. */
function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase();
}

/** Conta recorrências simples para ordenar documentos, sem produzir métrica de demanda. */
function occurrences(content, term) {
  let count = 0;
  let offset = 0;
  while ((offset = content.indexOf(term, offset)) >= 0) {
    count += 1;
    offset += term.length;
  }
  return count;
}

/** Usa o tipo explícito do ciclo e mantém compatibilidade com briefings anteriores. */
function isConsumerInstagram(job) {
  return (
    /instagram/i.test(String(job?.acquisitionChannel || "")) &&
    (job?.marketType === "B2C" ||
      /\bb2c\b|consumidor|pessoa f[ií]sica/i.test(
        `${job?.commercialConstraints || ""} ${job?.targetAudience || ""}`,
      ))
  );
}
