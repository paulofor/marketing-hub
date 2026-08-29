import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import {
  executionTimeout,
  executionAudit,
  parseUsage,
  refreshedProcessContext,
  resolveContract,
  runAuditedModel,
  taskFromPayload,
  validateResult,
} from "./run-commercial-plan-v4-agent.mjs";

test("protege o executor contra timeout ausente ou inválido", () => {
  assert.equal(executionTimeout(undefined), 600000);
  assert.equal(executionTimeout("120000"), 120000);
  assert.throws(() => executionTimeout("inválido"), /Timeout do agente/);
});

test("segrega atividades por agente", () => {
  assert.equal(
    resolveContract("experiment-strategist", "marketStrategy").activities
      .length,
    1,
  );
  assert.equal(
    resolveContract("landing-generator", "productArchitecture").activities
      .length,
    1,
  );
  assert.throws(
    () => resolveContract("financial-agent", "review"),
    /não suportado/,
  );
  assert.throws(
    () => resolveContract("meta-ad-approver", "marketStrategy"),
    /não suportado/,
  );
  assert.throws(
    () => resolveContract("growth-operator", "marketStrategy"),
    /não suportado/,
  );
});

test("preserva a última telemetria cumulativa", () => {
  const usage = parseUsage(
    [
      '{"usage":{"input_tokens":100,"cached_input_tokens":20,"output_tokens":30}}',
      '{"response":{"service_tier":"flex","usage":{"input_tokens":180,"input_tokens_details":{"cached_tokens":45},"output_tokens":60}}}',
    ].join("\n"),
  );
  assert.deepEqual(usage, {
    informed: true,
    inputTokens: 180,
    cachedInputTokens: 45,
    outputTokens: 60,
    serviceTier: "flex",
  });
});

test("registra o modelo, o esforço e o prompt final enviados ao Codex", () => {
  assert.deepEqual(executionAudit("Prompt final", "high"), {
    executionMode: "MODEL",
    modelCode: "gpt-5.6-sol",
    reasoningEffort: "high",
    promptSent: "Prompt final",
    accessedUrls: [],
  });
});

test("bloqueia resposta inválida preservando prompt e raciocínio", async () => {
  const audit = executionAudit("Prompt final integral", "high");
  let reported;

  await assert.rejects(
    runAuditedModel({
      run: async () => '{"type":"turn.completed"}',
      readResult: async () =>
        '{"activity":"economics","decision":"APPROVE","scenarios":[]}',
      activityId: "economics",
      reportFailure: async (error, receivedAudit) => {
        reported = { error, receivedAudit };
      },
      executionAuditPayload: audit,
    }),
    /exatamente três/,
  );

  assert.equal(reported.receivedAudit, audit);
  assert.equal(reported.error.codexLog, '{"type":"turn.completed"}');
});

test("rejeita resposta sem três alternativas", () => {
  assert.throws(
    () =>
      validateResult(
        { activity: "economics", decision: "APPROVE", scenarios: [1, 2] },
        "economics",
      ),
    /exatamente três/,
  );
});

test("recupera tarefa original de uma execução bloqueada", () => {
  assert.equal(taskFromPayload({ task: { taskId: 202 } }).taskId, 202);
  assert.throws(() => taskFromPayload({ result: {} }), /sem contexto original/);
});

test("atualiza contexto somente com predecessores concluídos da mesma instância", () => {
  const context = JSON.parse(
    refreshedProcessContext(
      [
        [
          {
            id: 2,
            assignedAgentKey: "landing-generator",
            sourceReference: "commercial-plan:5@v2",
            processCode: "pde-commercial-plan-offer",
            status: "COMPLETED",
            processActivityId: "productArchitecture",
            resultJson: "{}",
          },
          {
            id: 3,
            sourceReference: "commercial-plan:5@v2",
            processCode: "pde-commercial-plan-offer",
            status: "BLOCKED",
            processActivityId: "review",
            resultJson: "{}",
          },
          {
            id: 4,
            sourceReference: "commercial-plan:4@v2",
            processCode: "pde-commercial-plan-offer",
            status: "COMPLETED",
            processActivityId: "productArchitecture",
            resultJson: "{}",
          },
          {
            id: 5,
            assignedAgentKey: "landing-generator",
            sourceReference: "commercial-plan:5@v2",
            processCode: "pde-commercial-plan-offer",
            status: "COMPLETED",
            processActivityId: "productArchitecture",
            resultJson: '{"latest":true}',
          },
        ],
      ],
      "commercial-plan:5@v2",
    ),
  );
  assert.deepEqual(
    context.completedActivities.map((item) => item.taskId),
    [5],
  );
});

test("atribui estratégia somente a Atena e arquitetura somente a Dédalo", async () => {
  const strategyPrompt = await readFile(
    new URL(
      "../experiment-strategist-worker/src/main/resources/prompts/pde-commercial-plan/v5/market-strategy.md",
      import.meta.url,
    ),
    "utf8",
  );
  const architecturePrompt = await readFile(
    new URL(
      "../landing-generator-agent-worker/src/main/resources/prompts/pde-commercial-plan/v5/product-architecture.md",
      import.meta.url,
    ),
    "utf8",
  );
  assert.match(strategyPrompt, /única autora da estratégia/);
  assert.match(strategyPrompt, /Não calcule preço/);
  assert.match(
    architecturePrompt,
    /não redefina público, desejo, posicionamento/,
  );
  assert.match(architecturePrompt, /Audiovisual pertence a Apolo/);
});

test("permite aprovar economia hipotética com envelope e travas futuras", async () => {
  const prompt = await readFile(
    new URL(
      "../financial-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/economics.md",
      import.meta.url,
    ),
    "utf8",
  );
  assert.match(prompt, /três cenários e por envelopes máximos explícitos/);
  assert.match(
    prompt,
    /Não retorne `ADJUST` apenas porque esses controles ainda não foram/,
  );
  assert.match(
    prompt,
    /nunca orçamento, campanha,\s+contato, publicação ou gasto/,
  );
});
