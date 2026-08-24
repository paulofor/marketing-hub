import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

test("Dédalo usa âncoras e não transforma sinal comercial em venda", async () => {
  const prompt = await readFile(new URL("./prompts/dedalo.md", import.meta.url), "utf8");

  assert.match(prompt, /Quando a evidência ficar entre duas\s+faixas, use a menor/);
  assert.match(prompt, /temperatura, ranking e página publicada não são transação/);
  assert.match(prompt, /economia: no máximo 4 sem custo e margem observados/);
});

test("Psique separa curiosidade de valor percebido aprovável", async () => {
  const prompt = await readFile(new URL("./prompts/psique.md", import.meta.url), "utf8");

  assert.match(prompt, /65–74 para benefício concreto/);
  assert.match(prompt, /75–84 para\s+resultado imediato/);
  assert.match(prompt, /Na dúvida entre faixas, use a menor/);
});

test("Argos usa a contagem determinística sem promover relatos a ofertas", async () => {
  const prompt = await readFile(new URL("./prompts/argos.md", import.meta.url), "utf8");

  assert.match(prompt, /auditFacts\.paidOfferCount/);
  assert.match(prompt, /não são novas ofertas\s+pagas/);
  assert.match(prompt, /COMMERCIAL_OFFER/);
});

test("Hermes não supera uma decisão de evidência não aprovada", async () => {
  const prompt = await readFile(new URL("./prompts/hermes.md", import.meta.url), "utf8");

  assert.match(prompt, /argos\.decision/);
  assert.match(prompt, /nunca\s+`APPROVE`/);
});

test("executor limita a chamada externa e audita a URL do provedor", async () => {
  const runner = await readFile(new URL("./run-agent.mjs", import.meta.url), "utf8");

  assert.match(runner, /resolveOpportunityAgentTimeoutMs/);
  assert.match(runner, /AbortSignal\.timeout/);
  assert.match(runner, /persistAudit\("provider", \{ endpoint: providerEndpoint, timeoutMs \}\)/);
});
