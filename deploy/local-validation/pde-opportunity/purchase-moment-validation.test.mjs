import assert from "node:assert/strict";
import test from "node:test";
import {
  assertPurchaseMomentEligible,
  buildPurchaseMomentGate,
} from "./purchase-moment-validation.mjs";

test("libera priorização somente após duas leituras consistentes", () => {
  const research = fixture();

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "PASS");
  assert.equal(gate.sourceQualityPassed, true);
  assert.equal(gate.finalPrioritizationEligible, true);
  assert.equal(gate.minimumIndependentReadings, 2);
  assert.deepEqual(gate.eligibleCandidateNames, ["Candidata A"]);
  assert.equal(gate.candidates[0].readings[0].valueMomentRate, 0.75);
  assert.equal(gate.candidates[0].readings[0].readyResultUseRate, 0.75);
  assert.equal(gate.candidates[0].readings[0].eventSource, "FIRST_PARTY_EVENTS");
  assert.deepEqual(gate.candidates[0].humanValueDelivery.territories, [
    "RECOGNITION",
    "EFFORT_RELIEF",
  ]);
  assert.equal(gate.candidates[0].prototype.private, true);
  assert.doesNotThrow(() => assertPurchaseMomentEligible(gate, "Candidata A"));
});

test("mantém a candidata aguardando quando existe somente uma leitura", () => {
  const research = fixture();
  research.purchaseMomentValidation.candidates[0].readings.pop();

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "WAITING_VALIDATION");
  assert.equal(gate.candidates[0].status, "WAITING_VALIDATION");
  assert.throws(
    () => assertPurchaseMomentEligible(gate, "Candidata A"),
    /não possui Validação do Momento de Compra aprovada/,
  );
});

test("manda ajustar quando uma das leituras não alcança o critério predeclarado", () => {
  const research = fixture();
  research.purchaseMomentValidation.candidates[0].readings[1].checkoutStarted = 0;

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "ADJUST");
  assert.equal(gate.candidates[0].status, "ADJUST");
  assert.equal(gate.finalPrioritizationEligible, false);
  assert.equal(gate.eligibleCandidateNames.length, 0);
  assert.match(
    gate.candidates[0].reasons.join(" "),
    /mínimo predeclarado de checkout iniciado/,
  );
});

test("manda ajustar quando o resultado gerado ainda exige montagem externa", () => {
  const research = fixture();
  research.purchaseMomentValidation.candidates[0].readings[1]
    .readyResultsUsedWithoutAssembly = 1;

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "ADJUST");
  assert.match(
    gate.candidates[0].reasons.join(" "),
    /uso do resultado pronto sem montagem/,
  );
});

test("bloqueia critério nulo que permitiria aprovar sem uso do resultado pronto", () => {
  const research = fixture();
  research.purchaseMomentValidation.successCriteria.minimumReadyResultUseRate = 0;
  for (const reading of research.purchaseMomentValidation.candidates[0].readings) {
    reading.readyResultsUsedWithoutAssembly = 0;
  }

  const gate = buildPurchaseMomentGate(research);

  assert.notEqual(gate.status, "PASS");
  assert.equal(gate.finalPrioritizationEligible, false);
  assert.deepEqual(gate.eligibleCandidateNames, []);
  assert.match(gate.reasons.join(" "), /deve ser maior que zero/);
  assert.throws(
    () => assertPurchaseMomentEligible(gate, "Candidata A"),
    /não possui Validação do Momento de Compra aprovada/,
  );
});

test("bloqueia candidata que transfere prompting ou montagem ao consumidor", () => {
  const research = fixture();
  research.purchaseMomentValidation.candidates[0].humanValueDelivery
    .requiresPromptEngineering = true;

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "ADJUST");
  assert.match(
    gate.candidates[0].reasons.join(" "),
    /transfere prompting, montagem ou conhecimento de IA/,
  );
});

test("manda parar quando a alternativa gratuita vence nas duas leituras", () => {
  const research = fixture();
  for (const reading of research.purchaseMomentValidation.candidates[0].readings) {
    reading.prototypePreferredOverFree = 2;
  }

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "STOP");
  assert.equal(gate.candidates[0].status, "STOP");
  assert.match(
    gate.candidates[0].reasons.join(" "),
    /mínimo predeclarado de preferência sobre o gratuito/,
  );
});

test("registra a causa quando Psique bloqueia uma leitura", () => {
  const research = fixture();
  research.purchaseMomentValidation.candidates[0].readings[1].psiqueDecision = "BLOCK";

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.status, "STOP");
  assert.match(gate.candidates[0].reasons.join(" "), /bloqueada por Psique/);
});

test("bloqueia snapshot Hotmart vencido e coleção diária vazia", () => {
  const research = fixture();
  research.inspirations.collections.find(
    (collection) => collection.code === "MOMENTOS_COMPRA_B2C",
  ).status = "EMPTY";
  research.inspirations.hotmartProducts[0].collectedAt = "2026-06-01T00:00:00Z";

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.sourceQuality.passed, false);
  assert.match(gate.sourceQuality.reasons.join(" "), /MOMENTOS_COMPRA_B2C está EMPTY/);
  assert.match(gate.sourceQuality.reasons.join(" "), /fora da validade/);
  assert.equal(gate.candidates[0].status, "WAITING_SOURCE_QUALITY");
});

test("bloqueia produto Hotmart sem preço nem sinal de tração", () => {
  const research = fixture();
  delete research.inspirations.hotmartProducts[0].price;

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.sourceQuality.passed, false);
  assert.match(gate.sourceQuality.reasons.join(" "), /preço nem sinal de tração/);
});

test("bloqueia contagens impossíveis e critérios definidos depois do uso", () => {
  const research = fixture();
  research.purchaseMomentValidation.successCriteria.declaredAt = "2026-08-27T00:00:00Z";
  research.purchaseMomentValidation.candidates[0].readings[0].valueMoments = 9;

  const gate = buildPurchaseMomentGate(research);

  assert.equal(gate.candidates[0].status, "ADJUST");
  assert.match(gate.candidates[0].reasons.join(" "), /antes da declaração/);
  assert.match(gate.candidates[0].reasons.join(" "), /numerador maior/);
});

function fixture() {
  return {
    processVersion: 5,
    commercialFocus: {
      audienceModel: "B2C",
      acquisitionChannel: "INSTAGRAM",
    },
    candidates: [
      {
        name: "Candidata A",
        humanValueTerritories: ["RECOGNITION", "EFFORT_RELIEF"],
        readyMadeDeliverable: "Resposta falada revisada e pronta para novo ensaio",
      },
    ],
    sources: [
      {
        id: "human-value-study",
        candidateName: "Candidata A",
        pathway: "RECOGNITION_STUDY",
      },
      {
        id: "human-value-community",
        candidateName: "Candidata A",
        pathway: "EFFORT_COMMUNITY",
      },
    ],
    inspirations: {
      collections: [
        { code: "GARTNER", status: "CURRENT" },
        { code: "IA_APLICADA", status: "CURRENT" },
        { code: "MOMENTOS_COMPRA_B2C", status: "CURRENT" },
      ],
      hotmartContract: { status: "CURRENT" },
      hotmartProducts: [
        {
          id: "hotmart:1",
          title: "Treino profissional",
          url: "https://hotmart.com/treino-profissional",
          price: "R$ 29,00",
          collectedAt: "2026-08-25T10:00:00Z",
        },
      ],
    },
    purchaseMomentValidation: {
      sourceQuality: {
        evaluatedAt: "2026-08-26T10:00:00Z",
        maxAgeDays: 30,
      },
      successCriteria: {
        declaredAt: "2026-08-25T08:00:00Z",
        minimumEligibleParticipantsPerReading: 5,
        minimumExperienceStartRate: 0.7,
        minimumValueMomentRate: 0.6,
        minimumReadyResultUseRate: 0.6,
        minimumPrototypePreferenceRate: 0.6,
        minimumCheckoutStartRate: 0.2,
      },
      candidates: [
        {
          candidateName: "Candidata A",
          scene: {
            trigger: "Entrevista marcada",
            deadline: "Sete dias",
            costOfError: "Perder uma oportunidade de renda",
            budgetEvidence: "Já compara simuladores pagos entre R$ 20 e R$ 50",
            failedAttempt: "Ensaio sem feedback",
            currentPaidBehavior: "Assinatura ou compra avulsa de simulação",
          },
          freeAlternative: {
            name: "Ensaio sozinho com ChatGPT",
            prototypeAdvantage: "Compara duas respostas faladas usando a própria evidência",
          },
          humanValueDelivery: {
            territories: ["RECOGNITION", "EFFORT_RELIEF"],
            desiredTransformation:
              "Sentir que consegue demonstrar a própria capacidade com menos esforço",
            evidenceSourceIds: ["human-value-study", "human-value-community"],
            readyMadeOutcome: "Resposta falada revisada e pronta para novo ensaio",
            minimumCustomerInput: "Vaga, pergunta e uma gravação curta",
            requiresPromptEngineering: false,
            requiresManualAssembly: false,
            usableWithoutAiKnowledge: true,
            customerStepsToValue: 3,
            timeToUsableResultMinutes: 8,
            automationBoundary:
              "A pessoa aprova a versão final e nenhuma experiência profissional é inventada",
          },
          prototype: {
            prototypeId: "PRIVATE-A-1",
            private: true,
            published: false,
            paymentEnabled: false,
            mediaSpend: 0,
            testMarker: "PRIVATE_PROTOTYPE",
          },
          readings: [
            reading("R1", "2026-08-25T12:00:00Z", 5, 4, 3, 3, 4, 1),
            reading("R2", "2026-08-26T12:00:00Z", 5, 5, 4, 4, 4, 2),
          ],
        },
      ],
    },
  };
}

function reading(
  readingId,
  observedAt,
  eligibleParticipants,
  experienceStarted,
  valueMoments,
  readyResultsUsedWithoutAssembly,
  prototypePreferredOverFree,
  checkoutStarted,
) {
  return {
    readingId,
    observedAt,
    eligibleParticipants,
    experienceStarted,
    valueMoments,
    readyResultsUsedWithoutAssembly,
    prototypePreferredOverFree,
    checkoutStarted,
    psiqueDecision: "APPROVE",
    temisDecision: "APPROVE",
    eventSource: "FIRST_PARTY_EVENTS",
    testMarker: "PRIVATE_PROTOTYPE",
  };
}
