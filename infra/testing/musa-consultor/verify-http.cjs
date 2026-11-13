const fs = require("fs");
const assert = require("node:assert/strict");
const base = "http://127.0.0.1:18086/api/pde/vega/consultor/v1";
const phase = process.argv[2];
const state = process.argv[3];
if (!state?.startsWith("/tmp/"))
  throw new Error(
    "Estado com credencial sintética exige arquivo temporário fora da entrega.",
  );
async function request(path, data, token, operator = false) {
  const response = await fetch(base + path, {
    method: data === undefined ? "GET" : "POST",
    headers: {
      "Content-Type": "application/json",
      ...(operator
        ? { "X-PDE-Internal-Token": "vega-local-internal-only" }
        : {}),
      ...(token ? { "X-Musa-Session": token } : {}),
    },
    ...(data !== undefined ? { body: JSON.stringify(data) } : {}),
  });
  return { status: response.status, data: await response.json() };
}
(async () => {
  if (phase === "before-restart") {
    const issued = await request(
      "/internal/cycles/91023/access",
      {},
      null,
      true,
    );
    assert.equal(issued.status, 200);
    const login = await request("/login", { accessKey: issued.data.accessKey });
    assert.equal(login.status, 200);
    // O cookie HttpOnly é lido apenas pelo cliente técnico de testes, nunca pelo aplicativo.
    const exchange = await fetch(base + "/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ accessKey: issued.data.accessKey }),
    });
    const token = /musa-consultor-session=([^;]+)/.exec(
      exchange.headers.get("set-cookie"),
    )[1];
    const cmd = (action, payload) =>
      request("/commands", { action, payload }, token);
    await cmd("consent", { accepted: true });
    const payload = {
      operationKey: "concurrent-operation-0001",
      revision: 0,
      answers: {
        occasion: "almoço",
        existingSelection:
          "Camisa, lenço e bolsa. Posso usar o lenço no pescoço ou na alça.",
        stylePreference: "Cor e criatividade",
      },
    };
    const results = await Promise.all(
      Array.from({ length: 6 }, () => cmd("generate", payload)),
    );
    assert(results.every((r) => r.status === 200));
    assert(
      results.every(
        (r) => JSON.stringify(r.data) === JSON.stringify(results[0].data),
      ),
    );
    const saved = await Promise.all(
      Array.from({ length: 6 }, () =>
        cmd("save", { operationId: payload.operationKey, revision: 1 }),
      ),
    );
    assert(saved.every((r) => r.status === 200));
    const report = await request("/internal/report", undefined, token, true);
    assert.equal(Object.keys(report.data.execution.operations).length, 1);
    assert.equal(Object.keys(report.data.execution.choices).length, 1);
    assert.equal(report.data.providerCalls, 0);
    fs.writeFileSync(
      state,
      JSON.stringify({
        token,
        cycleId: 91023,
        sessionId: issued.data.sessionId,
        expiresAt: issued.data.expiresAt,
        result: results[0].data,
      }),
      { mode: 0o600 },
    );
    console.log(
      JSON.stringify({
        phase,
        concurrentGenerations: 6,
        concurrentSaves: 6,
        persistedOperations: 1,
        persistedChoices: 1,
        status: "PASS",
      }),
    );
  } else if (phase === "after-restart") {
    const prior = JSON.parse(fs.readFileSync(state));
    const recovered = await request(
      "/operations/concurrent-operation-0001",
      undefined,
      prior.token,
    );
    assert.equal(recovered.status, 200);
    assert.deepEqual(recovered.data, prior.result);
    const renewed = await request(
      `/internal/access/${prior.sessionId}/renew`,
      {},
      null,
      true,
    );
    assert.equal(renewed.status, 200);
    assert.equal(renewed.data.sessionId, prior.sessionId);
    const close = await request("/internal/cycles/91023/close", {}, null, true);
    assert.equal(close.status, 200);
    assert.equal(
      (await request("/workspace", undefined, prior.token)).status,
      401,
    );
    assert.notEqual(
      (
        await request(
          `/internal/access/${prior.sessionId}/renew`,
          {},
          null,
          true,
        )
      ).status,
      200,
    );
    assert.equal(
      (await request("/internal/cycles/91023/access", {}, null, true)).status,
      409,
    );
    console.log(
      JSON.stringify({
        phase,
        preservedAcrossRestart: true,
        renewalPreservesOwner: true,
        closureRevokes: true,
        reissueBlocked: true,
        status: "PASS",
      }),
    );
    fs.unlinkSync(state);
  } else throw new Error("Fase inválida.");
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
