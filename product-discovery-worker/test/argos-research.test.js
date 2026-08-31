import assert from "node:assert/strict";
import { readFile, writeFile } from "node:fs/promises";
import test from "node:test";
import {
  deterministicSynthesis,
  synthesizeMarketCandidates,
  validateSynthesis,
} from "../src/argos-research.js";

test("síntese usa somente evidências coletadas e preserva o schema estrito", async () => {
  const context = researchContext();
  const expected = validSynthesis();
  let prompt;
  let schema;
  const result = await synthesizeMarketCandidates(context, {
    enabled: true,
    model: "modelo-teste",
    execute: async (_command, args, input) => {
      prompt = input;
      schema = JSON.parse(
        await readFile(args[args.indexOf("--output-schema") + 1], "utf8"),
      );
      await writeFile(
        args[args.indexOf("--output-last-message") + 1],
        JSON.stringify(expected),
      );
      return {
        stdout:
          '{"type":"turn.completed","usage":{"input_tokens":300,"cached_input_tokens":100,"output_tokens":80}}\n',
      };
    },
  });

  assert.equal(schema.additionalProperties, false);
  assert.equal(schema.properties.candidates.items.additionalProperties, false);
  assert.match(prompt, /DISCOVER_MARKETS/);
  assert.match(prompt, /P1/);
  assert.match(prompt, /R1/);
  assert.doesNotMatch(prompt, /{{[^}]+}}/);
  assert.deepEqual(result.synthesis, expected);
  assert.deepEqual(result.usage, {
    inputTokens: 300,
    cachedInputTokens: 100,
    outputTokens: 80,
  });
  assert.equal(result.accessedUrls.length, 3);
  assert.deepEqual(
    [...new Set(result.accessedUrls.map((item) => item.accessMethod))],
    ["WEB_SEARCH"],
  );
});

test("auditoria de busca pública usa método aceito pelo contrato do backend", async () => {
  const method =
    deterministicSynthesis(researchContext()).accessedUrls[0].accessMethod;
  const swagger = await readFile(
    new URL("../../docs/swagger/agent-tasks-v1-swagger.yaml", import.meta.url),
    "utf8",
  );
  const accessedUrlContract = swagger.match(
    /    AccessedUrl:\n[\s\S]*?(?=\n    AuditLink:)/,
  )?.[0];

  assert.ok(
    accessedUrlContract,
    "contrato AccessedUrl não encontrado no Swagger",
  );
  assert.match(accessedUrlContract, new RegExp(`\\b${method}\\b`));
  assert.doesNotMatch(accessedUrlContract, /PUBLIC_SEARCH/);
});

test("síntese bloqueia referência inventada em vez de corrigir silenciosamente", () => {
  const synthesis = validSynthesis();
  synthesis.candidates[0].evidenceIds = ["P1", "P999"];

  assert.throws(
    () => validateSynthesis(synthesis, researchContext()),
    /evidência ausente ou insuficiente/,
  );
});

test("síntese bloqueia evidência repetida sem usar keyword incompatível no schema", async () => {
  const context = researchContext();
  const duplicate = validSynthesis();
  duplicate.candidates[0].evidenceIds = ["P1", "P1"];

  assert.throws(
    () => validateSynthesis(duplicate, context),
    /evidência ausente ou insuficiente/,
  );
  const schema = await readFile(
    new URL(
      "../prompts/productdiscovery.v1/research/response-schema.json",
      import.meta.url,
    ),
    "utf8",
  );
  assert.doesNotMatch(schema, /"uniqueItems"/);
});

test("modo degradado não fabrica as três sugestões genéricas antigas", () => {
  const result = deterministicSynthesis(researchContext());

  assert.equal(result.mode, "DETERMINISTIC");
  assert.deepEqual(result.synthesis.candidates, []);
  assert.match(result.synthesis.decisionSummary, /modelo.*desabilitado/i);
  assert.doesNotMatch(result.rawResponse, /Diagnóstico|Plano de primeira ação|Simulador/);
});

function researchContext() {
  return {
    job: {
      cycleId: 44,
      researchMode: "DISCOVER_MARKETS",
      marketType: "B2C",
      theme: "mulheres interessadas em estilo e bem-estar",
      targetAudience: "mulheres 40+",
      acquisitionChannel: "Instagram",
    },
    plan: { questions: ["Qual situação é urgente?"] },
    publicEvidence: [
      {
        evidenceId: "P1",
        title: "Relato de dificuldade",
        url: "https://forum.example/relato",
        snippet: "Não consigo resolver e já tentei alternativas.",
      },
      {
        evidenceId: "P2",
        title: "Comparação de alternativas",
        url: "https://reviews.example/comparacao",
        snippet: "Preço, review e esforço manual.",
      },
    ],
    repositoryEvidence: [
      {
        evidenceId: "R1",
        path: "pesquisas/ia-aplicada/exemplo.md",
        title: "Tendências",
        excerpt: "IA deve reduzir esforço sem aparecer como produto bruto.",
      },
    ],
    repositoryCoverage: [],
    marketplaceOffers: [
      {
        evidenceId: "O1",
        title: "Alternativa paga",
        url: "https://oferta.example/produto",
      },
    ],
    metaAdEvidence: [],
    metaCoverage: [{ sourceStatus: "AWAITING_SUPERVISED_OBSERVATION" }],
  };
}

function validSynthesis() {
  return {
    decisionSummary: "Há uma situação pesquisável, ainda sem priorização estratégica.",
    candidates: [
      {
        name: "Decisão de roupa para evento próximo",
        primaryAudience: "Mulheres 40+ com evento marcado",
        purchaseSituation: "Evento próximo e receio de comprar a peça errada.",
        rootPain: "Dificuldade de decidir com o que já possui e o que precisa comprar.",
        practicalPain: "Comparação fragmentada entre peças, clima e ocasião.",
        emotionalPain: "Insegurança de se sentir inadequada no evento.",
        observedLanguage: ["não sei o que vestir", "vale a pena comprar"],
        currentAlternatives: ["vídeos gratuitos", "consultoria de estilo"],
        residualEffort: "A pessoa ainda precisa juntar sugestões e montar o resultado.",
        scaleEvidence: "A dor aparece em duas fontes públicas independentes.",
        unmetnessEvidence: "Alternativas exigem comparação e montagem manual.",
        pdeValueBoundary: "Reduzir comparação e montagem, sem definir o produto.",
        instagramFitEvidence: "A cena permite contraste visual entre alternativas.",
        commercialRisk: "Cobertura Meta ainda não observada.",
        evidenceIds: ["P1", "P2", "O1", "R1"],
        maturity: "RESEARCHABLE",
      },
    ],
  };
}
