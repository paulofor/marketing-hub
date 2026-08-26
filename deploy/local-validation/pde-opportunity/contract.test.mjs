import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import test from "node:test";
import {
  buildFinalDecision,
  selectActiveResearch,
  validateFunctionalResult,
  validateResearchInput,
} from "./contract.mjs";
import {
  attachLiveArticleInspirations,
  loadLiveArticleInspirations,
} from "./live-inspirations.mjs";

test("valida o ciclo v5 com coleções vivas e Hotmart sem inventar vendas", async () => {
  const repositoryRoot = fileURLToPath(new URL("../../..", import.meta.url));
  const input = JSON.parse(
    await readFile(new URL("./inputs/LOCAL_QA-2026-08-26-v5.json", import.meta.url), "utf8"),
  );
  const inventory = await loadLiveArticleInspirations(
    repositoryRoot,
    new Date("2026-08-26T12:00:00Z"),
  );
  const research = selectActiveResearch(attachLiveArticleInspirations(input, inventory));

  assert.doesNotThrow(() => validateResearchInput(research));

  const withoutGartner = structuredClone(research);
  withoutGartner.inspirations.articles = withoutGartner.inspirations.articles.filter(
    (article) => article.origin !== "GARTNER",
  );
  assert.throws(
    () => validateResearchInput(withoutGartner),
    /coleção viva GARTNER/,
  );

  const fakeSale = structuredClone(research);
  fakeSale.inspirations.hotmartProducts[0].tractionIsNotSale = false;
  assert.throws(
    () => validateResearchInput(fakeSale),
    /não pode transformar score ou temperatura em venda/,
  );
});

test("separa fontes históricas das oportunidades ativas", () => {
  const research = researchFixture();
  research.sources.push({
    id: "D-archived",
    candidateName: "D",
    sourceType: "COMMUNITY",
    pathway: "PUBLIC_PAIN",
    supports: ["RECURRENCE"],
    paid: false,
    offerKey: "",
  });

  const active = selectActiveResearch(research);

  assert.equal(active.sources.some((source) => source.id === "D-archived"), false);
  assert.equal(research.sources.some((source) => source.id === "D-archived"), true);
  assert.deepEqual(active.auditFacts, {
    activeCandidateCount: 3,
    activeSourceCount: 18,
    paidOfferCount: 12,
    paidOffersByCandidate: { A: 4, B: 4, C: 4 },
  });
  assert.doesNotThrow(() => validateResearchInput(active));
});

test("bloqueia adulteração dos fatos auditáveis calculados pelo executor", () => {
  const research = selectActiveResearch(researchFixture());
  research.auditFacts.paidOfferCount = 21;

  assert.throws(
    () => validateResearchInput(research),
    /contagem determinística de ofertas pagas foi alterada/,
  );
});

test("aceita três oportunidades com doze ofertas pagas deduplicadas", () => {
  assert.doesNotThrow(() => validateResearchInput(researchFixture()));
});

test("bloqueia ciclo com menos de dez ofertas pagas", () => {
  const research = researchFixture();
  research.sources = research.sources.filter(
    (source) => !["B-offer-4", "C-offer-3", "C-offer-4"].includes(source.id),
  );
  research.candidates[1].sourceIds = research.candidates[1].sourceIds.filter(
    (id) => id !== "B-offer-4",
  );
  research.candidates[2].sourceIds = research.candidates[2].sourceIds.filter(
    (id) => !["C-offer-3", "C-offer-4"].includes(id),
  );
  assert.throws(
    () => validateResearchInput(research),
    /ao menos dez ofertas pagas/,
  );
});

test("bloqueia oportunidade com menos de três ofertas comparáveis", () => {
  const research = researchFixture();
  for (const source of research.sources) {
    if (source.id === "C-offer-3" || source.id === "C-offer-4") {
      source.sourceType = "COMMUNITY";
      source.paid = false;
      source.offerKey = "";
    }
  }
  assert.throws(
    () => validateResearchInput(research),
    /C precisa de ao menos três ofertas pagas/,
  );
});

test("Argos preserva fontes, nomes e contagem auditável", () => {
  const research = researchFixture();
  assert.doesNotThrow(() => validateFunctionalResult("argos", research, argosFixture()));
});

test("Argos não aprova vencedora sem duas vias independentes", () => {
  const research = researchFixture();
  research.sources.find((source) => source.id === "A-official").supports = ["MECHANISM"];
  assert.throws(
    () => validateFunctionalResult("argos", research, argosFixture()),
    /duas vias independentes para RECURRENCE/,
  );
});

test("Hermes preserva três jornadas e proíbe gasto", () => {
  const context = { research: researchFixture(), argos: argosFixture() };
  assert.doesNotThrow(() =>
    validateFunctionalResult("hermes", context, hermesFixture()),
  );
  const invalid = hermesFixture();
  invalid.journeyComparison[0].distributionRoutes[0].externalSpend = "FUTURE";
  assert.throws(
    () => validateFunctionalResult("hermes", context, invalid),
    /não pode autorizar gasto externo/,
  );
});

test("Hermes não aprova quando Argos mantém a pesquisa aberta", () => {
  const argos = argosFixture();
  argos.decision = "RESEARCH_MORE";

  assert.throws(
    () =>
      validateFunctionalResult(
        "hermes",
        { research: researchFixture(), argos },
        hermesFixture(),
      ),
    /pesquisa não aprovada por Argos/,
  );
});

test("Dédalo aceita a maior soma preservando formatos e benchmark", () => {
  const context = {
    research: researchFixture(),
    argos: argosFixture(),
    hermes: hermesFixture(),
  };
  assert.doesNotThrow(() =>
    validateFunctionalResult("dedalo", context, dedaloFixture()),
  );
});

test("Dédalo bloqueia score cuja soma não corresponde ao total", () => {
  const invalid = dedaloFixture();
  invalid.comparison[0].totalScore = 83;
  invalid.chosenOpportunity.benchmark.candidateScore = 83;
  invalid.chosenOpportunity.benchmark.result = "EXCEEDS";
  assert.throws(
    () =>
      validateFunctionalResult(
        "dedalo",
        { research: researchFixture(), argos: argosFixture(), hermes: hermesFixture() },
        invalid,
      ),
    /Score total inconsistente/,
  );
});

test("Dédalo não aprova uma vencedora abaixo de Rigel", () => {
  const invalid = dedaloFixture();
  invalid.comparison[0] = scoredAlternative("A", 81);
  invalid.chosenOpportunity.benchmark.candidateScore = 81;
  invalid.chosenOpportunity.benchmark.result = "BELOW";
  assert.throws(
    () =>
      validateFunctionalResult(
        "dedalo",
        { research: researchFixture(), argos: argosFixture(), hermes: hermesFixture() },
        invalid,
      ),
    /aprovou sem cumprir pesquisa, jornada e benchmark/,
  );
});

test("Psique avalia somente a vencedora e exige valor mínimo", () => {
  const context = { dedalo: dedaloFixture() };
  assert.doesNotThrow(() =>
    validateFunctionalResult("psique", context, psiqueFixture()),
  );
  const invalid = psiqueFixture();
  invalid.sourceAlternativeName = "B";
  assert.throws(
    () => validateFunctionalResult("psique", context, invalid),
    /oportunidade diferente/,
  );
});

test("gate final aprova somente consenso que alcança Rigel", () => {
  const context = fullContext();
  assert.deepEqual(buildFinalDecision(context), {
    decision: "APPROVE",
    chosenOpportunity: "A",
    workingProductName: "Produto A",
    totalScore: 82,
    benchmarkName: "Rigel",
    benchmarkScore: 82,
    benchmarkResult: "MEETS",
    agentDecisions: {
      argos: "APPROVE",
      hermes: "APPROVE",
      dedalo: "APPROVE",
      psique: "APPROVE",
    },
    reasons: [],
  });
});

test("gate final mantém pesquisa aberta quando um agente pede ajuste", () => {
  const context = fullContext();
  context.psique.decision = "RESEARCH_MORE";
  const decision = buildFinalDecision(context);
  assert.equal(decision.decision, "RESEARCH_MORE");
  assert.match(decision.reasons[0], /psique decidiu RESEARCH_MORE/);
});

test("gate final preserva rejeição de qualquer agente", () => {
  const context = fullContext();
  context.dedalo.decision = "REJECT";
  context.psique.decision = "RESEARCH_MORE";
  const decision = buildFinalDecision(context);
  assert.equal(decision.decision, "REJECT");
  assert.match(decision.reasons[0], /dedalo decidiu REJECT/);
});

function fullContext() {
  return {
    research: researchFixture(),
    argos: argosFixture(),
    hermes: hermesFixture(),
    dedalo: dedaloFixture(),
    psique: psiqueFixture(),
  };
}

function researchFixture() {
  const candidates = ["A", "B", "C"].map((name) => ({
    name,
    sourceIds: [
      `${name}-pain`,
      `${name}-official`,
      `${name}-offer-1`,
      `${name}-offer-2`,
      `${name}-offer-3`,
      `${name}-offer-4`,
    ],
  }));
  const sources = [];
  for (const name of ["A", "B", "C"]) {
    sources.push({
      id: `${name}-pain`,
      candidateName: name,
      sourceType: "COMMUNITY",
      pathway: "PUBLIC_PAIN",
      supports: ["RECURRENCE", "UNMETNESS"],
      paid: false,
      offerKey: "",
    });
    sources.push({
      id: `${name}-official`,
      candidateName: name,
      sourceType: "OFFICIAL_PLATFORM",
      pathway: "OFFICIAL_RULES",
      supports: ["RECURRENCE", "UNMETNESS", "MECHANISM"],
      paid: false,
      offerKey: "",
    });
    for (let index = 1; index <= 4; index += 1) {
      sources.push({
        id: `${name}-offer-${index}`,
        candidateName: name,
        sourceType: "COMMERCIAL_OFFER",
        pathway: index === 1 ? "COMMERCIAL_SOFTWARE" : "COMMERCIAL_SERVICE",
        supports: ["PURCHASE_INTENT", "UNMETNESS"],
        paid: true,
        offerKey: `${name}-paid-${index}`,
      });
    }
  }
  return {
    benchmark: { name: "Rigel", score: 82 },
    candidates,
    sources,
  };
}

function argosFixture() {
  return {
    decision: "APPROVE",
    evidenceSummary: {
      cycleOfferCount: 12,
      sourceDiversity: "três vias",
      strongestSignal: "dor e pagamento",
      principalLimitation: "sem vendas",
    },
    alternatives: ["A", "B", "C"].map((name) => ({
      name,
      evidenceSourceIds: [
        `${name}-pain`,
        `${name}-official`,
        `${name}-offer-1`,
        `${name}-offer-2`,
        `${name}-offer-3`,
        `${name}-offer-4`,
      ],
      paidOfferSourceIds: [
        `${name}-offer-1`,
        `${name}-offer-2`,
        `${name}-offer-3`,
        `${name}-offer-4`,
      ],
    })),
    recommendedOpportunity: "A",
  };
}

function hermesFixture() {
  return {
    decision: "APPROVE",
    journeyComparison: ["A", "B", "C"].map((candidateName) => ({
      candidateName,
      distributionRoutes: [1, 2, 3].map((number) => ({
        name: `Rota ${number}`,
        externalSpend: "NONE",
      })),
      chosenInitialRouteIndex: 0,
    })),
    recommendedOpportunity: "A",
  };
}

function dedaloFixture() {
  return {
    decision: "APPROVE",
    comparison: [
      scoredAlternative("A", 82),
      scoredAlternative("B", 76),
      scoredAlternative("C", 70),
    ],
    chosenOpportunity: {
      sourceAlternativeName: "A",
      workingProductName: "Produto A",
      chosenFormat: "GUIDED_WEBAPP",
      benchmark: {
        name: "Rigel",
        score: 82,
        candidateScore: 82,
        result: "MEETS",
      },
    },
  };
}

function scoredAlternative(name, totalScore) {
  return {
    name,
    formats: [
      { name: "STATIC_MATERIAL" },
      { name: "GUIDED_WEBAPP" },
      { name: "ASSISTED_EXECUTION" },
    ],
    chosenFormat: "GUIDED_WEBAPP",
    evidenceScore: totalScore - 63,
    purchaseIntentScore: 15,
    painValueScore: 12,
    pdeFitScore: 12,
    differentiationScore: 8,
    distributionScore: 8,
    economicsScalabilityScore: 4,
    riskSafetyScore: 4,
    totalScore,
  };
}

function psiqueFixture() {
  return {
    decision: "APPROVE",
    sourceAlternativeName: "A",
    workingProductName: "Produto A",
    valueScore: 80,
  };
}
