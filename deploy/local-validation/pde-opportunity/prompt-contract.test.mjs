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
  assert.match(prompt, /executando Descoberta e priorização da\s+oportunidade PDE v5/);
  assert.match(prompt, /são inspirações, não\s+evidências de demanda/);
  assert.match(prompt, /Temperatura, score, ranking e presença na Hotmart não são vendas/);
  assert.match(prompt, /pesquisas\/momentos-de-compra-b2c/);
  assert.match(prompt, /duas leituras\s+consistentes/);
});

test("Dédalo usa padrões sem copiar nem pontuar inspiração", async () => {
  const prompt = await readFile(new URL("./prompts/dedalo.md", import.meta.url), "utf8");

  assert.match(prompt, /Não copie\s+produto, marca, promessa, texto, criativo ou estrutura proprietária/);
  assert.match(prompt, /não aumenta score nem substitui evidência independente/);
  assert.match(prompt, /score \*\*estritamente maior\*\* que o benchmark/);
  assert.match(prompt, /distribuição mínima 8\/10/);
  assert.match(prompt, /eligibleCandidateNames/);
});

test("agentes preservam o gate B2C e a atribuição Instagram", async () => {
  const argos = await readFile(new URL("./prompts/argos.md", import.meta.url), "utf8");
  const hermes = await readFile(new URL("./prompts/hermes.md", import.meta.url), "utf8");
  const psique = await readFile(new URL("./prompts/psique.md", import.meta.url), "utf8");

  assert.match(argos, /bloqueie\s+B2B disfarçado/);
  assert.match(argos, /mobileValueMomentMinutes/);
  assert.match(hermes, /IMPRESSION.*CHECKOUT_STARTED/s);
  assert.match(hermes, /rota escolhida.*INSTAGRAM/s);
  assert.match(psique, /canReachValueAlone/);
  assert.match(psique, /manipulationRisk.*LOW/s);
  assert.match(psique, /não substitui o comportamento observado/);
});

test("Hermes separa plano de validação de comportamento observado", async () => {
  const prompt = await readFile(new URL("./prompts/hermes.md", import.meta.url), "utf8");

  assert.match(prompt, /Aprovar o\s+plano não significa que a validação aconteceu/);
  assert.match(prompt, /início, microvalor, preferência.*checkout/s);
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
