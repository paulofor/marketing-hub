import assert from "node:assert/strict";
import { ProcessReconciler } from "../../../process-execution-worker/src/v1/reconciler.mjs";
const base = process.env.PROCESS_TEST_URL || "http://127.0.0.1:18092";
assert(
  ["127.0.0.1", "localhost"].includes(new URL(base).hostname),
  "Somente backend local permitido",
);
const internal =
  "/api/internal/business-processes/automation/v1/stage-executions";
const headers = {
  "Content-Type": "application/json",
  "X-Process-Worker-Token": "process-fixture-only",
};
async function request(path, body, expected = 200) {
  const response = await fetch(base + path, {
    method: body === undefined ? "GET" : "POST",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json();
  assert.equal(response.status, expected, JSON.stringify(data));
  return data;
}
const root = (product, process = 92001) =>
  `/api/business-processes/${process}/products/${product}/automation/v1`;
const command = (product) => ({
  chainId: 92014,
  sourceReference: `experiment:${product}`,
  learningCycleId: product,
});
const start = (product, process = 92001) =>
  request(root(product, process), command(product));
const tick = (id) => request(`${internal}/${id}/reconcile`, {});
const state = (product, process = 92001) =>
  request(`${root(product, process)}?${new URLSearchParams(command(product))}`);
const callback = (
  product,
  process,
  activity,
  result = { status: "COMPLETED", achieved: true },
) => request(`/fixture/${product}/${process}/${activity}`, result);
const tasks = async (product, process = 92001) =>
  (await request("/fixture/tasks")).filter(
    (t) => t.product_id === product && t.process_id === process,
  );
const pause = (p, process, run) =>
  request(`${root(p, process)}/${run}/pause`, {});
const resume = (p, process, run) =>
  request(`${root(p, process)}/${run}/resume`, {});
let passed = 0;
async function scenario(name, run) {
  await run();
  passed++;
  console.log(`PASS ${name}`);
}

await scenario(
  "duplo clique e concorrência criam uma execução e um único disparo",
  async () => {
    const runs = await Promise.all(
      Array.from({ length: 12 }, () => start(92001)),
    );
    assert.equal(new Set(runs.map((x) => x.id)).size, 1);
    await Promise.all(Array.from({ length: 12 }, () => tick(runs[0].id)));
    const work = await tasks(92001);
    assert.equal(work.length, 1);
    assert.equal(work[0].activity_id, "a");
  },
);
await scenario(
  "worker real continua com tela fechada e preserva ordem e qualidade",
  async () => {
    const worker = new ProcessReconciler({
      baseUrl: base,
      token: "process-fixture-only",
      log: { info() {}, error() {} },
    });
    await callback(92001, 92001, "a");
    await worker.tick();
    assert.deepEqual(
      (await tasks(92001)).map((x) => x.activity_id),
      ["a", "b"],
    );
    await callback(92001, 92001, "b");
    await worker.tick();
    await worker.tick();
    const result = await state(92001);
    assert.equal(result.status, "COMPLETED");
    assert.equal(result.completedActivities, 3);
    assert.equal(result.remainingActivities, 0);
    assert.equal(result.costCoverage, "NOT_REPORTED");
  },
);
await scenario(
  "produto e ciclo divergentes não criam autorização",
  async () => {
    await request(
      root(92002),
      { ...command(92002), learningCycleId: 92001 },
      409,
    );
    await request(
      root(92002),
      { ...command(92002), sourceReference: "experiment:92001" },
      409,
    );
    await request(root(92002), { ...command(92002), chainId: 1 }, 404);
    await request(root(92002), {}, 400);
  },
);
await scenario(
  "autenticação do conciliador impede disparo externo sem credencial",
  async () => {
    const response = await fetch(base + internal + "/pending");
    assert.equal(response.status, 401);
  },
);
await scenario(
  "produtos diferentes avançam em paralelo e mesmo produto fica na fila",
  async () => {
    const a = await start(92002),
      b = await start(92003),
      queued = await start(92002, 92006);
    await Promise.all([tick(a.id), tick(b.id), tick(queued.id)]);
    assert.equal((await tasks(92002)).length, 1);
    assert.equal((await tasks(92003)).length, 1);
    assert.equal((await state(92002, 92006)).status, "QUEUED");
    assert.equal((await tasks(92002, 92006)).length, 0);
  },
);
await scenario(
  "pausa espera trabalho ativo antes de liberar outro processo",
  async () => {
    const a = await state(92002),
      queued = await state(92002, 92006);
    assert.equal((await pause(92002, 92001, a.id)).status, "PAUSING");
    await tick(a.id);
    await tick(queued.id);
    assert.equal((await tasks(92002, 92006)).length, 0);
    await callback(92002, 92001, "a");
    assert.equal((await tick(a.id)).status, "PAUSED");
    await tick(queued.id);
    assert.equal((await tasks(92002, 92006)).length, 1);
    await callback(92002, 92006, "a");
    await pause(92002, 92006, queued.id);
    await tick(queued.id);
    await resume(92002, 92001, a.id);
    await tick(a.id);
    assert.deepEqual(
      (await tasks(92002)).map((t) => t.activity_id),
      ["a", "b"],
    );
  },
);
await scenario(
  "falha sem progresso não gera repetição infinita e retomada é explícita",
  async () => {
    const a = await state(92003);
    await callback(92003, 92001, "a", {
      status: "BLOCKED",
      achieved: false,
      reason: "Falta prova real",
    });
    await tick(a.id);
    await tick(a.id);
    assert.equal((await tasks(92003)).length, 1);
    assert.equal((await state(92003)).status, "BLOCKED");
    await resume(92003, 92001, a.id);
    await tick(a.id);
    assert.equal((await tasks(92003)).length, 2);
  },
);
await scenario(
  "encerramento técnico sem objetivo não libera sucessora",
  async () => {
    const a = await start(92004);
    await tick(a.id);
    await callback(92004, 92001, "a", { status: "COMPLETED", achieved: false });
    await tick(a.id);
    assert.equal((await tasks(92004)).length, 1);
    assert.notEqual((await state(92004)).status, "COMPLETED");
  },
);
await scenario(
  "STOP interrompe novos disparos e PLAY preserva a continuação",
  async () => {
    const a = await start(92005);
    await tick(a.id);
    await callback(92005, 92001, "a");
    await request("/fixture/products/92005", { play: false });
    await tick(a.id);
    assert.equal((await tasks(92005)).length, 1);
    await request("/fixture/products/92005", { play: true });
    await tick(a.id);
    assert.equal((await tasks(92005)).length, 2);
  },
);
await scenario(
  "decisão humana conserva seu gate e não é confirmada pelo processo",
  async () => {
    const a = await start(92006, 92002);
    await tick(a.id);
    await callback(92006, 92002, "a");
    assert.equal((await tick(a.id)).status, "WAITING_HUMAN");
    await tick(a.id);
    assert.equal((await tasks(92006, 92002)).length, 1);
    await request("/fixture/92006/92002/approval", {});
    await tick(a.id);
    assert.equal((await tick(a.id)).status, "COMPLETED");
  },
);
await scenario(
  "recuperação declarada executa correção antes de revalidar",
  async () => {
    const a = await start(92007, 92003);
    await tick(a.id);
    await callback(92007, 92003, "a", {
      status: "BLOCKED",
      achieved: false,
      reason: "Ajuste funcional necessário",
    });
    await tick(a.id);
    assert.deepEqual(
      (await tasks(92007, 92003)).map((t) => t.activity_id),
      ["a", "fix"],
    );
    await callback(92007, 92003, "fix");
    await tick(a.id);
    assert.deepEqual(
      (await tasks(92007, 92003)).map((t) => t.activity_id),
      ["a", "fix", "a"],
    );
    await callback(92007, 92003, "a");
    await tick(a.id);
    await callback(92007, 92003, "b");
    await tick(a.id);
    await tick(a.id);
    assert.equal((await state(92007, 92003)).status, "COMPLETED");
  },
);
await scenario(
  "subprocesso herda contexto e pai só avança após conclusão confirmada",
  async () => {
    const a = await start(92008, 92004);
    const waiting = await tick(a.id);
    assert.equal(waiting.status, "WAITING_SUBPROCESS");
    assert(waiting.childRunId);
    await tick(waiting.childRunId);
    await callback(92008, 92005, "a");
    await tick(waiting.childRunId);
    await callback(92008, 92005, "b");
    await tick(waiting.childRunId);
    await tick(waiting.childRunId);
    await tick(a.id);
    assert.deepEqual(
      (await tasks(92008, 92004)).map((t) => t.activity_id),
      ["b"],
    );
  },
);
await scenario(
  "falha após escrita reverte tarefa e preserva diagnóstico separado",
  async () => {
    const a = await start(92009, 92030);
    await tick(a.id);
    await tick(a.id);
    assert.equal((await tick(a.id)).status, "ERROR");
    assert.equal((await tasks(92009, 92030)).length, 0);
    const events = await request(`${root(92009, 92030)}/${a.id}/events`);
    assert(events.some((e) => e.eventType === "RECONCILIATION_FAILED"));
  },
);
await scenario(
  "histórico e comando de outra identidade são recusados",
  async () => {
    const a = await state(92001);
    await request(`${root(92002)}/${a.id}/events`, undefined, 404);
    await request(`${root(92002)}/${a.id}/pause`, {}, 404);
    const events = await request(`${root(92001)}/${a.id}/events`);
    assert(events.some((e) => e.eventType === "COMPLETED"));
    assert(events.every((e) => typeof e.details === "object"));
    assert(
      events.length < 50,
      "Acompanhamento não deve poluir diário com ticks idênticos",
    );
  },
);
await scenario(
  "prova invalidada após conclusão permite revalidar sem apagar o histórico",
  async () => {
    const run = await start(92014);
    await tick(run.id);
    await callback(92014, 92001, "a");
    await tick(run.id);
    await callback(92014, 92001, "b");
    await tick(run.id);
    await tick(run.id);
    assert.equal((await state(92014)).status, "COMPLETED");
    await callback(92014, 92001, "b", { status: "BLOCKED", achieved: false });
    const invalidated = await state(92014);
    assert.equal(invalidated.status, "REVALIDATION_REQUIRED");
    assert.equal(invalidated.canResume, true);
    const count = (await tasks(92014)).length;
    await tick(run.id);
    assert.equal((await tasks(92014)).length, count);
    await resume(92014, 92001, run.id);
    await tick(run.id);
    assert.equal((await tasks(92014)).length, count + 1);
    await callback(92014, 92001, "b");
    await tick(run.id);
    assert.equal((await state(92014)).status, "COMPLETED");
    const events = await request(`${root(92014)}/${run.id}/events`);
    assert.equal(events.filter((e) => e.eventType === "COMPLETED").length, 2);
    assert(events.some((e) => e.eventType === "REVALIDATION_REQUESTED"));
  },
);
await scenario(
  "comando backend com workspace executa sem clique extra nem aprovação inventada",
  async () => {
    const run = await start(92015, 92010);
    await tick(run.id);
    await callback(92015, 92010, "a");
    await tick(run.id);
    await callback(92015, 92010, "b");
    const workspace = await tick(run.id);
    assert.equal(workspace.status, "WAITING_ACTIVITY");
    assert.equal(workspace.currentActivityId, "gate");
    assert.equal((await tick(run.id)).status, "COMPLETED");
    assert.deepEqual(
      (await tasks(92015, 92010)).map((t) => t.activity_id),
      ["a", "b", "gate"],
    );
  },
);
await scenario(
  "atividade do BPM sem contrato impede autorização e conclusão artificial",
  async () => {
    await request(root(92016, 92011), command(92016), 500);
    assert.equal((await tasks(92016, 92011)).length, 0);
    await request(
      root(92016, 92011) + "?" + new URLSearchParams(command(92016)),
      undefined,
      500,
    );
  },
);
console.log(
  JSON.stringify({
    passed,
    tests: passed,
    environment: "local-mysql57",
    productionWrites: 0,
  }),
);
