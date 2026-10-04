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
const publication = (process, status) =>
  request(`/fixture/processes/${process}`, { status });
async function withRetirement(process, run) {
  try {
    await run(() => publication(process, "RETIRED"));
  } finally {
    await publication(process, "PUBLISHED");
  }
}
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
  if (
    process.env.PROCESS_TEST_SCENARIO &&
    !name.includes(process.env.PROCESS_TEST_SCENARIO)
  )
    return;
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
    assert.equal((await tick(child.id)).status, "COMPLETED");
    await tick(parent.id);
    // O pai persiste a prova do filho antes de despachar sua próxima atividade.
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

await scenario(
  "destino aprovado encerra filho indevido e preserva sua falha",
  async () => {
    const product = 92027;
    const parent = await start(product, 92004);
    const childId = (await tick(parent.id)).childRunId;
    await tick(childId);
    await request(`/fixture/${product}/92005/a`, {
      status: "BLOCKED",
      achieved: false,
    });
    await request(root(product, 92004) + `/${parent.id}/pause`, {});
    assert.equal((await tick(parent.id)).status, "PAUSED");
    await request(`/fixture/products/${product}`, {
      destination: "PRIVATE_PDE",
    });
    await request(root(product, 92004) + `/${parent.id}/resume`, {});
    assert.equal((await tick(parent.id)).status, "WAITING_ACTIVITY");
    await tick(parent.id);
    const child = await request(
      root(product, 92005) +
        `?chainId=92014&learningCycleId=${product}&sourceReference=experiment:${product}`,
    );
    assert.equal(child.status, "CLOSED");
    assert.equal(child.completedActivities, 0);
    assert.equal(child.remainingActivities, 3);
    assert.match(child.reason, /não necessário/);
    assert(
      child.parentProcesses.some(
        (relation) => relation.processDefinitionId === 92004,
      ),
    );
    await callback(product, "b", 92004);
    await tick(parent.id);
    assert.equal((await tick(parent.id)).status, "COMPLETED");
    assert.equal(
      (await tasks(product)).filter((task) => task.process_id === 92005).length,
      1,
    );
    assert.equal(
      (await tasks(product)).find((task) => task.process_id === 92005).status,
      "BLOCKED",
    );
  },
);

await scenario(
  "mudança de destino aguarda tarefa já iniciada sem cancelar transação",
  async () => {
    const product = 92028;
    const parent = await start(product, 92004);
    const childId = (await tick(parent.id)).childRunId;
    await tick(childId);
    await request(`/fixture/products/${product}`, {
      destination: "PRIVATE_PDE",
    });
    await tick(parent.id);
    assert.equal((await tick(parent.id)).status, "WAITING_SUBPROCESS");
    assert.equal(
      (await tasks(product)).find((task) => task.process_id === 92005).status,
      "PENDING",
    );
    await request(`/fixture/${product}/92005/a`, {
      status: "BLOCKED",
      achieved: false,
    });
    await tick(parent.id);
    await callback(product, "b", 92004);
    await tick(parent.id);
    assert.equal((await tick(parent.id)).status, "COMPLETED");
    assert.equal((await tick(childId)).status, "CLOSED");
  },
);

await scenario(
  "ciclo privado novo conclui pelo destino aprovado sem abrir landing",
  async () => {
    const product = 92029;
    await request(`/fixture/products/${product}`, {
      destination: "PRIVATE_PDE",
    });
    const parent = await start(product, 92004);
    assert.equal((await tick(parent.id)).childRunId, null);
    await tick(parent.id);
    await callback(product, "b", 92004);
    await tick(parent.id);
    const completed = await tick(parent.id);
    assert.equal(completed.status, "COMPLETED");
    assert.equal(completed.completedActivities, 3);
    assert.equal(
      (await tasks(product)).some((task) => task.process_id === 92005),
      false,
    );
  },
);

await scenario(
  "versão retirada em espera humana libera a fila e preserva diário sem aprovar objetivos",
  () =>
    withRetirement(92002, async (retire) => {
      const product = 92031;
      const old = await start(product, 92002);
      await tick(old.id);
      await callback(product, "a", 92002);
      assert.equal((await tick(old.id)).status, "WAITING_HUMAN");
      const next = await start(product, 92006);
      assert.equal((await tick(next.id)).status, "QUEUED");
      await retire();
      const closed = await tick(old.id);
      assert.equal(closed.status, "CLOSED");
      assert.equal(closed.completedActivities, 1);
      assert.equal(closed.remainingActivities, 2);
      assert.equal(closed.canResume, false);
      assert.match(closed.reason, /não está publicada nem fixada/);
      assert.equal((await tick(next.id)).status, "WAITING_ACTIVITY");
      await tick(old.id);
      const history = await request(root(product, 92002) + `/${old.id}/events`);
      assert.equal(
        history.filter((e) => e.eventType === "CONTEXT_CLOSED").length,
        1,
      );
      assert.equal(
        history.filter((e) => e.eventType === "ACTIVITY_REQUESTED").length,
        1,
      );
      await request(root(product, 92002) + `/${old.id}/resume`, {}, 409);
      const pending = await request(
        "/api/internal/business-processes/automation/v1/stage-executions/pending?limit=100",
      );
      assert(!pending.includes(old.id));
      assert.equal(
        (await tasks(product)).filter((t) => t.process_id === 92002).length,
        1,
      );
    }),
);

await scenario(
  "retirada aguarda callback da tarefa real antes de encerrar e liberar o produto",
  () =>
    withRetirement(92001, async (retire) => {
      const product = 92032;
      const old = await start(product);
      await tick(old.id);
      const next = await start(product, 92006);
      await retire();
      assert.equal((await tick(old.id)).status, "WAITING_ACTIVITY");
      assert.equal((await tick(next.id)).status, "QUEUED");
      assert.equal((await tasks(product)).length, 1);
      await callback(product, "a");
      const closed = await tick(old.id);
      assert.equal(closed.status, "CLOSED");
      assert.equal(closed.completedActivities, 1);
      assert.equal((await tick(next.id)).status, "WAITING_ACTIVITY");
      assert.equal(
        (await tasks(product)).filter((t) => t.process_id === 92001).length,
        1,
      );
    }),
);

await scenario(
  "origem retirada drena o filho sem despachar sua próxima atividade",
  () =>
    withRetirement(92004, async (retire) => {
      const product = 92033;
      const parent = await start(product, 92004);
      const childId = (await tick(parent.id)).childRunId;
      await tick(childId);
      const next = await start(product, 92006);
      await retire();
      assert.equal((await tick(parent.id)).status, "WAITING_SUBPROCESS");
      assert.equal((await tick(next.id)).status, "QUEUED");
      assert.equal((await tick(childId)).status, "WAITING_ACTIVITY");
      await callback(product, "a", 92005);
      assert.equal((await tick(childId)).status, "CLOSED");
      assert.equal((await tick(parent.id)).status, "CLOSED");
      assert.equal((await tick(next.id)).status, "WAITING_ACTIVITY");
      const childTasks = (await tasks(product)).filter(
        (t) => t.process_id === 92005,
      );
      assert.equal(childTasks.length, 1);
      assert.equal(childTasks[0].status, "COMPLETED");
      assert.equal(childTasks[0].activity_id, "a");
    }),
);

await scenario(
  "filho ainda não iniciado encerra quando a origem perde autorização",
  () =>
    withRetirement(92004, async (retire) => {
      const product = 92034;
      const parent = await start(product, 92004);
      const childId = (await tick(parent.id)).childRunId;
      await retire();
      assert.equal((await tick(childId)).status, "CLOSED");
      assert.equal((await tick(parent.id)).status, "CLOSED");
      assert.equal((await tasks(product)).length, 0);
    }),
);

await scenario(
  "retirada preserva conclusão já comprovada em vez de converter sucesso em CLOSED",
  () =>
    withRetirement(92001, async (retire) => {
      const product = 92035;
      const run = await start(product);
      await prove(run, product);
      await retire();
      const result = await tick(run.id);
      assert.equal(result.status, "COMPLETED");
      assert.equal(result.completedActivities, 3);
      assert.equal((await tasks(product)).length, 3);
    }),
);

await scenario(
  "ficha da mesma referência permite continuar a versão retirada até concluir",
  () =>
    withRetirement(92001, async (retire) => {
      const product = 92036;
      const run = await start(product);
      await tick(run.id);
      await request(`/fixture/products/${product}`, { pinnedProcessId: 92001 });
      await retire();
      await callback(product, "a");
      assert.equal((await tick(run.id)).status, "WAITING_ACTIVITY");
      await callback(product, "b");
      await tick(run.id);
      assert.equal((await tick(run.id)).status, "COMPLETED");
      assert.equal((await tasks(product)).length, 3);
    }),
);

await scenario(
  "espera de versão legada sem ciclo ou ficha encerra e libera a fila",
  () =>
    withRetirement(92002, async (retire) => {
      const product = 92037;
      const old = await request(root(product, 92002), {
        chainId: 92014,
        sourceReference: `experiment:${product}`,
      });
      await tick(old.id);
      await callback(product, "a", 92002);
      assert.equal((await tick(old.id)).status, "WAITING_HUMAN");
      const next = await request(root(product, 92006), {
        chainId: 92014,
        sourceReference: `experiment:${product}`,
      });
      assert.equal((await tick(next.id)).status, "QUEUED");
      await retire();
      const closed = await tick(old.id);
      assert.equal(closed.status, "CLOSED");
      assert.equal(closed.learningCycleId, null);
      assert.equal(closed.sourceReference, `experiment:${product}`);
      assert.equal((await tick(next.id)).status, "WAITING_ACTIVITY");
      assert.equal(
        (await tasks(product)).filter((t) => t.process_id === 92002).length,
        1,
      );
    }),
);

await scenario(
  "fila identifica a reserva exata e libera somente após pausa persistida",
  async () => {
    const product = 92038;
    const waiting = await start(product, 92002);
    await tick(waiting.id);
    await callback(product, "a", 92002);
    assert.equal((await tick(waiting.id)).status, "WAITING_HUMAN");
    const nextCommand = {
      chainId: 92014,
      sourceReference: `experiment:${product}`,
    };
    const next = await request(root(product), nextCommand);
    assert.equal((await tick(next.id)).status, "QUEUED");
    const query = root(product) + "?" + new URLSearchParams(nextCommand);
    const before = await request(
      root(product, 92002) + "?" + new URLSearchParams(command(product)),
    );
    const historyBefore = await request(
      root(product, 92002) + `/${waiting.id}/events`,
    );
    const queued = await request(query);
    assert.equal(queued.queueBlocker.runId, waiting.id);
    assert.equal(queued.queueBlocker.sourceReference, `experiment:${product}`);
    assert.equal(queued.sourceReference, `experiment:${product}`);
    assert.equal(queued.queueBlocker.status, "WAITING_HUMAN");
    assert(
      queued.queueBlocker.navigationUrl.includes(`learningCycleId=${product}`),
    );
    assert(
      queued.queueBlocker.navigationUrl.includes(
        `sourceReference=experiment%3A${product}`,
      ),
    );
    const after = await request(
      root(product, 92002) + "?" + new URLSearchParams(command(product)),
    );
    assert.equal(after.revision, before.revision);
    assert.equal(
      (await request(root(product, 92002) + `/${waiting.id}/events`)).length,
      historyBefore.length,
    );
    assert.equal(
      (await tasks(product)).filter((t) => t.process_id === 92001).length,
      0,
    );
    await request(root(product, 92002) + `/${waiting.id}/pause`, {});
    assert.equal((await tick(waiting.id)).status, "PAUSED");
    const released = await request(query);
    assert.equal(released.status, "QUEUED");
    assert.equal(released.queueBlocker, null);
    assert.equal((await tick(next.id)).status, "WAITING_ACTIVITY");
    assert.equal(
      (await tasks(product)).filter((t) => t.process_id === 92001).length,
      1,
    );
    console.log(
      JSON.stringify({
        queueReservation: {
          waitingId: waiting.id,
          nextId: next.id,
          readOnly: true,
        },
      }),
    );
  },
);

for (const [product, state] of [
  [92039, "IN_PROGRESS"],
  [92041, "PENDING"],
]) {
  await scenario(
    `medição projetada ${state} sem tarefa encerra reserva retirada e preserva gate seguinte`,
    () =>
      withRetirement(92001, async (retire) => {
        await request(`/fixture/products/${product}`, {
          projectedProcessId: 92001,
          projectedState: state,
          blockedProcessId: 92006,
        });
        const old = await start(product);
        const next = await start(product, 92006);
        assert.equal((await tick(next.id)).status, "QUEUED");
        await retire();
        const closed = await tick(old.id);
        assert.equal(closed.status, "CLOSED");
        assert.equal(closed.completedActivities, 0);
        assert.equal(closed.remainingActivities, 3);
        assert.equal(closed.childRunId, null);
        const released = await tick(next.id);
        assert.equal(released.status, "WAITING_INPUT");
        assert.match(released.reason, /Estratégia vigente pendente/);
        assert.equal(released.queueBlocker, null);
        await tick(old.id);
        const history = await request(root(product) + `/${old.id}/events`);
        assert.equal(
          history.filter((e) => e.eventType === "CONTEXT_CLOSED").length,
          1,
        );
        assert.equal((await tasks(product)).length, 0);
        console.log(
          JSON.stringify({
            projectedReservation: {
              product,
              oldId: old.id,
              nextId: next.id,
              state,
            },
          }),
        );
      }),
  );
}

await scenario(
  "subprocesso com projeção de medição reutiliza filho e retorna prova ao pai",
  async () => {
    const product = 92040;
    await request(`/fixture/products/${product}`, {
      projectedProcessId: 92004,
      projectedState: "IN_PROGRESS",
    });
    const parent = await start(product, 92004);
    const child = await start(product, 92005);
    assert.equal((await tick(child.id)).status, "QUEUED");
    const delegated = await tick(parent.id);
    assert.equal(delegated.status, "WAITING_SUBPROCESS");
    assert.equal(delegated.childRunId, child.id);
    await prove(child, product, 92005);
    assert.equal((await tick(child.id)).status, "COMPLETED");
    assert.equal((await tick(parent.id)).status, "RUNNING");
    await tick(parent.id);
    await callback(product, "b", 92004);
    await tick(parent.id);
    assert.equal((await tick(parent.id)).status, "COMPLETED");
    const history = await request(root(product, 92005) + `/${child.id}/events`);
    assert.equal(
      history.filter((e) => e.eventType === "DELEGATION_LINKED").length,
      1,
    );
    assert.equal(
      (await tasks(product)).filter((t) => t.process_id === 92005).length,
      3,
    );
  },
);

await scenario(
  "pausa de medição projetada não espera callback inexistente",
  async () => {
    const product = 92042;
    await request(`/fixture/products/${product}`, {
      projectedProcessId: 92001,
      projectedState: "IN_PROGRESS",
    });
    const run = await start(product);
    await request(root(product) + `/${run.id}/pause`, {});
    assert.equal((await tick(run.id)).status, "PAUSED");
    assert.equal((await tasks(product)).length, 0);
  },
);

await scenario(
  "ciclo encerrado com medição projetada libera reserva sem fabricar conclusão",
  async () => {
    const product = 92043;
    await request(`/fixture/products/${product}`, {
      projectedProcessId: 92001,
      projectedState: "PENDING",
    });
    const run = await start(product);
    await request(`/fixture/products/${product}`, { cycleStatus: "CLOSED" });
    assert.equal((await tick(run.id)).status, "CLOSED");
    assert.equal((await tasks(product)).length, 0);
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
assert(
  passed > 0,
  "O filtro da matriz precisa selecionar pelo menos um cenário.",
);
