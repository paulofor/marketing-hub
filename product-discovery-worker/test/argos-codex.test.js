import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import { readFile, writeFile } from "node:fs/promises";
import test from "node:test";
import {
  deterministicPlan,
  executeCodexWithInput,
  parseCodexUsage,
  planDirectedResearch,
  validatePlan,
} from "../src/argos-codex.js";

test("plano seguro direciona Hotmart e ClickBank sem credenciais", () => {
  const result = deterministicPlan({ theme: "leads no WhatsApp", targetAudience: "nail designers" });
  validatePlan(result.plan);
  assert.equal(result.plan.minimumComparableOffers, 10);
  assert.deepEqual(
    result.plan.marketplaceRequests.map((item) => item.marketplace),
    ["HOTMART", "CLICKBANK"],
  );
  assert.equal(result.plan.metaAdRequests[0].country, "BR");
  assert.doesNotMatch(result.rawResponse, /password|senha|token|cookie/i);
});

test("plano bloqueia marketplace e volume não autorizados", () => {
  const result = deterministicPlan({ theme: "agenda", targetAudience: "manicures" });
  result.plan.marketplaceRequests[0] = {
    marketplace: "OUTRO",
    query: "agenda",
    maxProducts: 100,
  };
  assert.throws(() => validatePlan(result.plan), /Marketplace não autorizado/);
});

test("plano B2C para Instagram pesquisa cena pessoal e microvalor mobile", () => {
  const result = deterministicPlan({
    theme: "preparação para entrevista de emprego",
    targetAudience: "pessoa física em busca de emprego",
    acquisitionChannel: "Instagram",
    commercialConstraints: "B2C, oferta simples e mobile",
  });

  assert.match(result.plan.questions[0], /cena pessoal/i);
  assert.ok(result.plan.publicQueries.some((query) => /Instagram Reel/.test(query)));
  assert.match(result.plan.metaAdRequests[0].query, /consumidor/);
  assert.ok(result.plan.stopConditions.some((condition) => /depende de empresa/i.test(condition)));
});

test("planejamento envia o contexto pela entrada padrão e lê a saída estruturada", async () => {
  let receivedInput;
  let receivedSchema;
  const expected = deterministicPlan({
    theme: "propostas comerciais",
    targetAudience: "prestadores locais",
  }).plan;
  const result = await planDirectedResearch(
    {
      cycleId: 33,
      theme: "propostas comerciais",
      targetAudience: "prestadores locais",
      objective: "priorizar uma oportunidade PDE",
    },
    {
      enabled: true,
      model: "modelo-teste",
      execute: async (_command, args, input) => {
        receivedInput = input;
        const schemaIndex = args.indexOf("--output-schema") + 1;
        receivedSchema = JSON.parse(await readFile(args[schemaIndex], "utf8"));
        const outputIndex = args.indexOf("--output-last-message") + 1;
        await writeFile(args[outputIndex], JSON.stringify(expected));
      },
    },
  );

  assert.match(receivedInput, /ciclo 33/);
  assert.match(receivedInput, /propostas comerciais/);
  assert.match(receivedInput, /B2B disfarçado/);
  assert.doesNotMatch(receivedInput, /{{[^}]+}}/);
  assert.equal(receivedSchema.additionalProperties, false);
  assert.ok(receivedSchema.required.includes("minimumComparableOffers"));
  assert.deepEqual(result.plan, expected);
  assert.equal(result.model, "modelo-teste");
});

test("executor fecha explicitamente a entrada padrão do Codex", async () => {
  const child = new EventEmitter();
  child.stdout = new EventEmitter();
  child.stderr = new EventEmitter();
  child.stdout.setEncoding = () => {};
  child.stderr.setEncoding = () => {};
  child.stdin = new EventEmitter();
  let receivedInput;
  child.stdin.end = (input) => {
    receivedInput = input;
    queueMicrotask(() => child.emit("close", 0, null));
  };
  child.kill = () => {};

  await executeCodexWithInput("codex", ["exec", "-"], "contexto completo", {
    timeoutMs: 100,
    spawnProcess: () => child,
  });

  assert.equal(receivedInput, "contexto completo");
});

test("executor preserva timeout como causa da falha", async () => {
  const child = new EventEmitter();
  child.stdout = new EventEmitter();
  child.stderr = new EventEmitter();
  child.stdout.setEncoding = () => {};
  child.stderr.setEncoding = () => {};
  child.stdin = new EventEmitter();
  child.stdin.end = () => {};
  child.kill = () => {};

  await assert.rejects(
    executeCodexWithInput("codex", ["exec", "-"], "contexto", {
      timeoutMs: 5,
      spawnProcess: () => child,
    }),
    /excedeu o timeout de 5 ms/,
  );
});

test("contabiliza os tokens emitidos pelo Codex", () => {
  const usage = parseCodexUsage(
    '{"type":"turn.completed","usage":{"input_tokens":20517,"cached_input_tokens":11008,"output_tokens":5}}\n',
  );

  assert.deepEqual(usage, {
    inputTokens: 20517,
    cachedInputTokens: 11008,
    outputTokens: 5,
  });
});
