const PAIN_TERMS = [
  "dificuldade",
  "problema",
  "erro",
  "medo",
  "insegurança",
  "inseguranca",
  "frustração",
  "frustracao",
  "não consigo",
  "nao consigo",
  "como fazer",
  "reclamação",
  "reclamacao",
  "review",
];

const UNMET_TERMS = [
  "caro",
  "complicado",
  "demorado",
  "confuso",
  "manual",
  "consultoria",
  "não resolve",
  "nao resolve",
  "difícil",
  "dificil",
];

const HIGH_RISK_TERMS = [
  "cura",
  "tratamento",
  "diagnóstico médico",
  "diagnostico medico",
  "renda garantida",
  "lucro garantido",
  "processo judicial",
];

export function buildSearchQueries(job) {
  const base = [job.theme, job.targetAudience].filter(Boolean).join(" ");
  return [
    `${base} dificuldade problema`,
    `${base} reclamação review`,
    `${base} como resolver`,
    `${base} caro complicado`,
  ].map((query) => query.trim());
}

export function analyzeSearchResults(job, results) {
  const evidence = results.slice(0, 12).map((result) => ({
    title: result.title,
    url: result.url,
    snippet: result.snippet,
  }));
  const combined = evidence
    .map((item) => `${item.title} ${item.snippet}`)
    .join(" ")
    .toLowerCase();
  const painHits = countHits(combined, PAIN_TERMS);
  const unmetHits = countHits(combined, UNMET_TERMS);
  const highRiskHits = countHits(combined, HIGH_RISK_TERMS);
  const independentDomains = new Set(
    evidence.map((item) => safeDomain(item.url)).filter(Boolean),
  ).size;
  const scaleScore = Math.min(35, independentDomains * 7 + painHits * 3);
  const unmetScore = Math.min(30, unmetHits * 5);
  const pdeScore = highRiskHits > 0 ? 5 : 25;
  const score = Math.min(100, scaleScore + unmetScore + pdeScore + 10);
  const decision =
    highRiskHits > 0
      ? "HUMAN_REVIEW"
      : score >= 70 && independentDomains >= 2
        ? "APPROVE"
        : score >= 45
          ? "RESEARCH_MORE"
          : "REJECT";

  return {
    decisionSummary: `Ciclo pesquisado com ${evidence.length} evidências públicas e ${independentDomains} domínios independentes. Principal decisão: ${decision}.`,
    opportunities: [
      {
        name: `PDE de alívio para ${job.theme}`,
        primaryAudience: job.targetAudience || job.theme,
        rootPain: `O público demonstra esforço recorrente para resolver ${job.theme} com clareza e baixo risco.`,
        practicalPain:
          "A dor aparece como excesso de tentativa, comparação, busca por orientação e dificuldade de transformar informação em ação.",
        emotionalPain:
          "A fricção tende a gerar insegurança, medo de errar e sensação de estar sozinho na decisão.",
        scaleEvidence: `${independentDomains} domínios independentes e ${painHits} sinais de dor recorrente foram encontrados nos resultados públicos.`,
        unmetnessEvidence: `${unmetHits} sinais sugerem soluções caras, confusas, demoradas ou incompletas.`,
        pdeExperience:
          "Experiência guiada em que o usuário informa sua situação, recebe diagnóstico simples, plano de ação e primeiro antes/depois aplicável.",
        firstCampaignAngle: `Pare de tentar resolver ${job.theme} no improviso: veja em poucos minutos qual é o próximo passo mais seguro.`,
        commercialRisk:
          highRiskHits > 0
            ? "Tema contém sinais sensíveis e exige revisão humana antes de qualquer experimento."
            : "Risco principal é a dor ainda estar ampla demais; validar linguagem específica antes de campanha.",
        evidenceJson: JSON.stringify(evidence),
        score,
        decision,
      },
    ],
  };
}

export function normalizeDuckDuckGoResponse(payload) {
  const related = Array.isArray(payload?.RelatedTopics)
    ? payload.RelatedTopics
    : [];
  return related.flatMap((item) => {
    if (Array.isArray(item.Topics)) {
      return item.Topics.map(toSearchResult).filter(Boolean);
    }
    const normalized = toSearchResult(item);
    return normalized ? [normalized] : [];
  });
}

function toSearchResult(item) {
  if (!item?.FirstURL || !item?.Text) {
    return null;
  }
  return {
    title: item.Text.split(" - ")[0].slice(0, 160),
    url: item.FirstURL,
    snippet: item.Text,
  };
}

function countHits(text, terms) {
  return terms.reduce((total, term) => total + (text.includes(term) ? 1 : 0), 0);
}

function safeDomain(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}
