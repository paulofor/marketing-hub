import assert from "node:assert/strict";

const base = "http://127.0.0.1:18092";
const root = (product, process = 92001) =>
  `/api/business-processes/${process}/products/${product}/automation/v1`;
const command = (product) => ({
  chainId: 92014,
  learningCycleId: product,
  sourceReference: `experiment:${product}`,
});
async function request(path, body, expected = 200) {
  const response = await fetch(base + path, {
    method: body === undefined ? "GET" : "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Process-Worker-Token": "process-fixture-only",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json();
  assert.equal(response.status, expected, JSON.stringify(data));
  return data;
}
const start = (product, process = 92001) =>
  request(root(product, process), command(product));
const tick = (id) =>
  request(
    `/api/internal/business-processes/automation/v1/stage-executions/${id}/reconcile`,
    {},
  );
const callback = (product, activity, process = 92001) =>
  request(`/fixture/${product}/${process}/${activity}`, {
    status: "COMPLETED",
    achieved: true,
  });
const tasks = async (product) =>
  (await request("/fixture/tasks")).filter((t) => t.product_id === product);
async function prove(run, product, process = 92001) {
  await tick(run.id);
  await callback(product, "a", process);
  await tick(run.id);
  await callback(product, "b", process);
  await tick(run.id);
}
let passed = 0;
const failures = [];
async function scenario(name, run) {
  try {
    await run();
    passed++;
    console.log(`PASS ${name}`);
  } catch (error) {
    failures.push({ name, message: error.message });
    console.error(`FAIL ${name}: ${error.message}`);
  }
}

await scenario(
  "ciclo encerrado permite registrar objetivos já comprovados",
  async () => {
    const run = await start(92017);
    await prove(run, 92017);
    await request("/fixture/products/92017", { cycleStatus: "CLOSED" });
    const result = await tick(run.id);
    assert.equal(result.status, "COMPLETED");
    assert.equal(result.completedActivities, 3);
    assert.equal((await tasks(92017)).length, 3);
  },
);

await scenario(
  "ciclo encerrado preserva resultado parcial sem autorizar outra atividade",
  async () => {
    const run = await start(92018);
    await tick(run.id);
    await callback(92018, "a");
    await request("/fixture/products/92018", { cycleStatus: "ADJUSTED" });
    const result = await tick(run.id);
    assert.equal(result.status, "CLOSED");
    assert.equal(result.currentActivityId, null);
    assert.equal(result.canResume, false);
    assert.equal(result.completedActivities, 1);
    assert.equal(result.remainingActivities, 2);
    assert.equal((await tasks(92018)).length, 1);
    await request(root(92018) + `/${run.id}/resume`, {}, 409);
    const pending = await request(
      "/api/internal/business-processes/automation/v1/stage-executions/pending?limit=100",
    );
    assert(!pending.includes(run.id));
    const next = await request(root(92018, 92002), {
      chainId: 92014,
      sourceReference: "experiment:92018",
    });
    assert.equal((await tick(next.id)).status, "WAITING_ACTIVITY");
    assert.equal(
      (await tasks(92018)).filter((t) => t.process_id === 92002).length,
      1,
    );
  },
);

await scenario(
  "subprocesso registra conclusão mesmo após encerramento comprovado do pai",
  async () => {
    const parent = await start(92019, 92004);
    const waiting = await tick(parent.id);
    const child = { id: waiting.childRunId };
    assert(child.id);
    await prove(child, 92019, 92005);
    await tick(parent.id);
    await callback(92019, "b", 92004);
    await tick(parent.id);
    assert.equal((await tick(parent.id)).status, "COMPLETED");
    assert.equal((await tick(child.id)).status, "COMPLETED");
  },
);

await scenario(
  "STOP preserva conclusão comprovada sem iniciar trabalho",
  async () => {
    const run = await start(92020);
    await prove(run, 92020);
    await request("/fixture/products/92020", { play: false });
    assert.equal((await tick(run.id)).status, "COMPLETED");
    assert.equal((await tasks(92020)).length, 3);
  },
);

await scenario(
  "retomada apenas registra conclusão quando não resta trabalho no ciclo fechado",
  async () => {
    const run = await start(92024);
    await prove(run, 92024);
    await request(root(92024) + `/${run.id}/pause`, {});
    await request("/fixture/products/92024", {
      cycleStatus: "CLOSED",
      play: false,
    });
    assert.equal(
      (await request(root(92024) + `/${run.id}/resume`, {})).status,
      "COMPLETED",
    );
    assert.equal((await tasks(92024)).length, 3);
  },
);

await scenario(
  "pausa reconhece todos os objetivos comprovados antes do encerramento",
  async () => {
    const run = await start(92025);
    await prove(run, 92025);
    await request(root(92025) + `/${run.id}/pause`, {});
    await request("/fixture/products/92025", {
      cycleStatus: "CLOSED",
      play: false,
    });
    assert.equal((await tick(run.id)).status, "COMPLETED");
    assert.equal((await tasks(92025)).length, 3);
  },
);

console.log(
  JSON.stringify({
    passed,
    tests: passed + failures.length,
    failures,
    environment: "local-mysql57",
  }),
);
assert.equal(
  failures.length,
  0,
  "A leitura do resultado não pode depender de autorização para disparar novo trabalho.",
);
