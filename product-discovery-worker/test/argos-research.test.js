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
  context.job.researchIntelligence = {
    routes: [
      {
        agentKey: "market-radar",
        cards: [
          {
            cardId: "RI1-3B283DA81459",
            sourceSha256: "a".repeat(64),
            finding: "CURADORIA-ARGOS-CENA-PAGA",
          },
        ],
      },
    ],
  };
  const expected = validSynthesis();
  let prompt;
  let schema;
  let executionOptions;
  const result = await synthesizeMarketCandidates(context, {
    enabled: true,
    model: "modelo-teste",
    execute: async (_command, args, input, options) => {
      prompt = input;
      executionOptions = options;
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
  assert.equal(schema.properties.decisionSummary.maxLength, 1500);
  assert.equal(
    schema.properties.candidates.items.properties.observedLanguage.maxItems,
    8,
  );
  assert.match(prompt, /DISCOVER_MARKETS/);
  assert.match(prompt, /RI1-3B283DA81459/);
  assert.equal(prompt.split("CURADORIA-ARGOS-CENA-PAGA").length - 1, 1);
  assert.match(prompt, /P1/);
  assert.match(prompt, /R1/);
  assert.doesNotMatch(prompt, /{{[^}]+}}/);
  assert.deepEqual(result.synthesis, expected);
  assert.equal(result.reasoningEffort, "medium");
  assert.equal(executionOptions.phaseName, "síntese factual");
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

test("síntese não duplica biblioteca e limita trechos extensos no prompt", async () => {
  const context = researchContext();
  context.job.researchLibraryContext = {
    documents: [{ content: `BIBLIOTECA-DUPLICADA ${"z".repeat(60000)}` }],
  };
  context.repositoryEvidence[0].excerpt = `RECORTE-PRESERVADO ${"x".repeat(12000)} RECORTE-CORTADO`;
  let prompt;

  await synthesizeMarketCandidates(context, {
    enabled: true,
    execute: async (_command, args, input) => {
      prompt = input;
      await writeFile(
        args[args.indexOf("--output-last-message") + 1],
        JSON.stringify(validSynthesis()),
      );
    },
  });

  assert.match(prompt, /RECORTE-PRESERVADO/);
  assert.doesNotMatch(prompt, /RECORTE-CORTADO/);
  assert.doesNotMatch(prompt, /BIBLIOTECA-DUPLICADA/);
  assert.ok(prompt.length < 30000);
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

test("aprofundamento preserva todas as candidatas e vincula entrevista da própria situação", () => {
  const context = researchContext();
  const first = validSynthesis().candidates[0];
  first.evidenceIds = [...first.evidenceIds, "I1"];
  const second = structuredClone(first);
  second.name = "Imagem para encontro importante";
  second.evidenceIds = ["P1", "P2", "O1", "R1", "I2"];
  context.job.stageCode = "candidate-gap-deepening";
  context.job.previousCandidates = [
    { name: first.name },
    { name: second.name },
  ];
  context.job.customerInterviews = [
    {
      opportunityName: first.name,
      outcome: "PURCHASED",
      purchaseSituation: "Evento marcado.",
    },
    {
      opportunityName: second.name,
      outcome: "ABANDONED",
      purchaseSituation: "Encontro marcado.",
    },
  ];
  const synthesis = {
    decisionSummary:
      "As duas lacunas foram reavaliadas sem fabricar aprovação.",
    candidates: [first, second],
  };

  validateSynthesis(synthesis, context);
  synthesis.candidates[1].name = "Candidata inventada";

  assert.throws(
    () => validateSynthesis(synthesis, context),
    /alterou a identidade da candidata/,
  );
});

test("reanálise Meta supervisionada preserva todas as identidades sem exigir entrevistas", () => {
  const context = researchContext();
  const first = validSynthesis().candidates[0];
  const second = structuredClone(first);
  second.name = "Imagem para ocasião marcada";
  context.job.supervisedMetaReanalysis = {
    investigationId: 43,
    query: "consultoria imagem encontro",
    country: "BR",
    publisherPlatform: "INSTAGRAM",
  };
  context.job.previousCandidates = [
    { name: first.name },
    { name: second.name },
  ];
  const synthesis = {
    decisionSummary: "A sessão Meta foi confrontada com as candidatas preservadas.",
    candidates: [first, second],
  };

  validateSynthesis(synthesis, context);
  synthesis.candidates.pop();

  assert.throws(
    () => validateSynthesis(synthesis, context),
    /preservar todas as candidatas iniciais/,
  );
});

test("modo degradado não fabrica as três sugestões genéricas antigas", () => {
  const result = deterministicSynthesis(researchContext());

  assert.equal(result.mode, "DETERMINISTIC");
  assert.deepEqual(result.synthesis.candidates, []);
  assert.match(result.synthesis.decisionSummary, /modelo.*desabilitado/i);
  assert.doesNotMatch(
    result.rawResponse,
    /Diagnóstico|Plano de primeira ação|Simulador/,
  );
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
    decisionSummary:
      "Há uma situação pesquisável, ainda sem priorização estratégica.",
    candidates: [
      {
        name: "Decisão de roupa para evento próximo",
        primaryAudience: "Mulheres 40+ com evento marcado",
        purchaseSituation: "Evento próximo e receio de comprar a peça errada.",
        rootPain:
          "Dificuldade de decidir com o que já possui e o que precisa comprar.",
        practicalPain: "Comparação fragmentada entre peças, clima e ocasião.",
        emotionalPain: "Insegurança de se sentir inadequada no evento.",
        observedLanguage: ["não sei o que vestir", "vale a pena comprar"],
        currentAlternatives: ["vídeos gratuitos", "consultoria de estilo"],
        residualEffort:
          "A pessoa ainda precisa juntar sugestões e montar o resultado.",
        scaleEvidence: "A dor aparece em duas fontes públicas independentes.",
        unmetnessEvidence: "Alternativas exigem comparação e montagem manual.",
        pdeValueBoundary:
          "Reduzir comparação e montagem, sem definir o produto.",
        pdeDeliveryFit: {
          deliveryMode: "AI_DIGITAL_EXPERIENCE",
          minimumInput: "Foto da roupa e ocasião informada em uma escolha.",
          aiBackstageWork: "Comparar contexto, peças e sinais visuais.",
          readyDigitalOutcome: "Orientação visual individual pronta para usar.",
          physicalDependency: "NONE",
        },
        instagramFitEvidence:
          "A cena permite contraste visual entre alternativas.",
        commercialRisk: "Cobertura Meta ainda não observada.",
        evidenceIds: ["P1", "P2", "O1", "R1"],
        maturity: "RESEARCHABLE",
      },
    ],
  };
}

test("síntese rejeita produto físico como candidata PDE", () => {
  const synthesis = validSynthesis();
  synthesis.candidates[0].pdeDeliveryFit = {
    deliveryMode: "AI_DIGITAL_EXPERIENCE",
    minimumInput: "Preferências da cliente.",
    aiBackstageWork: "Escolher os itens.",
    readyDigitalOutcome: "Caixa de cosméticos enviada para a casa.",
    physicalDependency: "SHIPPING",
  };

  assert.throws(
    () => validateSynthesis(synthesis, researchContext()),
    /experiência digital com IA/,
  );
});

test("síntese não aceita caixa física mesmo com marcador PDE inconsistente", () => {
  const synthesis = validSynthesis();
  synthesis.candidates[0].name =
    "Assinaturas de caixas de beleza e autocuidado";
  synthesis.candidates[0].pdeDeliveryFit.readyDigitalOutcome =
    "Caixa de cosméticos enviada mensalmente para a cliente.";

  assert.throws(
    () => validateSynthesis(synthesis, researchContext()),
    /entrega física/,
  );
});

test("rota pública usa schema próprio e preserva candidatas sem exigir entrevistas", async () => {
  const context = researchContext();
  context.job.evidencePolicy = "PUBLIC_SOURCES_V1";
  context.job.stageCode = "candidate-gap-deepening";
  const expected = validSynthesis();
  expected.candidates.forEach((candidate) => {
    candidate.publicObservations = [];
    candidate.maturity = "RESEARCHABLE";
  });
  context.job.previousCandidates = expected.candidates.map((candidate) => ({
    name: candidate.name,
  }));
  const result = await synthesizeMarketCandidates(context, {
    enabled: true,
    execute: async (_cmd, args, prompt) => {
      const schema = JSON.parse(
        await readFile(args[args.indexOf("--output-schema") + 1], "utf8"),
      );
      assert.ok(
        schema.properties.candidates.items.required.includes(
          "publicObservations",
        ),
      );
      assert.match(prompt, /PUBLIC_SOURCES_V1/);
      await writeFile(
        args[args.indexOf("--output-last-message") + 1],
        JSON.stringify(expected),
      );
    },
  });
  assert.equal(result.synthesis.candidates.length, expected.candidates.length);
  assert.equal(result.synthesis.candidates[0].publicObservations.length, 0);
});

test("contrato público separa relatos P de ofertas O e anúncios M antes da chamada", async () => {
  const schema = JSON.parse(
    await readFile(
      new URL(
        "../prompts/productdiscovery.v1/research/public-response-schema.json",
        import.meta.url,
      ),
      "utf8",
    ),
  );
  const pattern = new RegExp(
    schema.properties.candidates.items.properties.publicObservations.items.properties.evidenceId.pattern,
  );
  assert.equal(pattern.test("P76"), true);
  for (const id of ["O15", "M1", "R2", "I9", "P0", "P1O15"])
    assert.equal(pattern.test(id), false);
  const context = researchContext();
  context.job.evidencePolicy = "PUBLIC_SOURCES_V1";
  const synthesis = validSynthesis();
  synthesis.candidates[0].publicObservations = [
    {
      evidenceId: "O1",
      sourceRole: "SELLER_CLAIM",
      reportedAction: "UNKNOWN",
      supportingExcerpt: "Texto legítimo da oferta comercial",
      limitation: "Oferta não é relato",
    },
  ];
  assert.throws(
    () => validateSynthesis(synthesis, context),
    /O1.*publicEvidence/,
  );
});

test("resposta recusada preserva auditoria e tokens sem repetir o modelo", async () => {
  const context = researchContext();
  const invalid = validSynthesis();
  invalid.candidates[0].evidenceIds = ["P1", "P999"];
  let calls = 0;
  await assert.rejects(
    synthesizeMarketCandidates(context, {
      enabled: true,
      model: "modelo-auditado",
      execute: async (_command, args) => {
        calls += 1;
        await writeFile(
          args[args.indexOf("--output-last-message") + 1],
          JSON.stringify(invalid),
        );
        return {
          stdout:
            '{"type":"turn.completed","usage":{"input_tokens":130,"cached_input_tokens":20,"output_tokens":45}}\n',
        };
      },
    }),
    (error) => {
      assert.equal(
        JSON.parse(error.analysisAudit.rawResponse).rawResponse,
        JSON.stringify(invalid),
      );
      assert.equal(error.analysisAudit.inputTokens, 130);
      assert.equal(error.analysisAudit.outputTokens, 45);
      assert.equal(error.analysisAudit.model, "modelo-auditado");
      return true;
    },
  );
  assert.equal(calls, 1);
});

test("dossiê anterior preserva conclusão sem repetir corpus global no prompt", async () => {
  const context = researchContext();
  context.job.previousCandidates = [
    {
      name: "Caso anterior",
      evidenceJson: JSON.stringify({
        candidateEvidence: { rootPain: "CAUSA-PRESERVADA" },
        candidateReadiness: { decision: "RESEARCH_MORE" },
        publicEvidence: [{ snippet: "CORPUS-REPETIDO".repeat(1000) }],
      }),
    },
  ];
  await synthesizeMarketCandidates(context, {
    enabled: true,
    execute: async (_command, args, input) => {
      assert.match(input, /CAUSA-PRESERVADA/);
      assert.match(input, /RESEARCH_MORE/);
      assert.doesNotMatch(input, /CORPUS-REPETIDO/);
      await writeFile(
        args[args.indexOf("--output-last-message") + 1],
        JSON.stringify(validSynthesis()),
      );
    },
  });
});

test("JSON malformado fica no envelope de falha sem derrubar o callback de auditoria", async () => {
  await assert.rejects(
    synthesizeMarketCandidates(researchContext(), {
      enabled: true,
      execute: async (_cmd, args) => {
        await writeFile(
          args[args.indexOf("--output-last-message") + 1],
          "resposta incompleta {",
        );
        return { stdout: "" };
      },
    }),
    (error) => {
      assert.deepEqual(JSON.parse(error.analysisAudit.rawResponse), {
        status: "REJECTED",
        rawResponse: "resposta incompleta {",
      });
      return true;
    },
  );
});
