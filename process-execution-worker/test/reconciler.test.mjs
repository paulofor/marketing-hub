import test from "node:test";
import assert from "node:assert/strict";
import { ProcessReconciler } from "../src/v1/reconciler.mjs";
const quiet = { info() {}, error() {} };

test("saúde é confirmada antes de aguardar um lote demorado", async () => {
  let beats = 0;
  let finish;
  const completion = new Promise((resolve) => {
    finish = resolve;
  });
  const worker = new ProcessReconciler({
    baseUrl: "http://localhost",
    token: "local",
    log: quiet,
    heartbeat: async () => {
      beats++;
    },
    fetchImpl: async (url) => {
      if (url.includes("pending")) return Response.json([1]);
      assert.equal(beats, 1);
      await completion;
      return Response.json({ id: 1, status: "WAITING_ACTIVITY" });
    },
  });
  const batch = worker.tick();
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(beats, 1);
  finish();
  await batch;
  assert.equal(beats, 2);
});

test("consome pending e concilia IDs sem escolher atividades", async () => {
  const calls = [];
  const worker = new ProcessReconciler({
    baseUrl: "http://127.0.0.1:8080",
    token: "local-only",
    log: quiet,
    fetchImpl: async (url, options) => {
      calls.push({ url, options });
      return Response.json(
        url.includes("/pending")
          ? [1, 2, 2]
          : { id: Number(url.split("/").at(-2)), status: "WAITING_ACTIVITY" },
      );
    },
  });
  assert.deepEqual((await worker.tick()).map((x) => x.runId).sort(), [1, 2]);
  assert.equal(calls[0].options.method, "GET");
  assert.equal(calls.length, 3);
  assert(
    calls.slice(1).every((c) => c.options.method === "POST" && !c.options.body),
  );
  assert(calls.every((c) => c.options.redirect === "error"));
});

test("falha de um produto não interrompe os outros e limita concorrência", async () => {
  const failures = [];
  let active = 0,
    max = 0;
  const worker = new ProcessReconciler({
    baseUrl: "http://localhost",
    token: "local",
    concurrency: 2,
    log: {
      info() {},
      error(value) {
        failures.push(JSON.parse(value));
      },
    },
    fetchImpl: async (url) => {
      if (url.includes("pending")) return Response.json([1, 2, 3, 4, 5]);
      const id = Number(url.split("/").at(-2));
      active++;
      max = Math.max(max, active);
      await new Promise((r) => setTimeout(r, 10));
      active--;
      return id === 1
        ? new Response("", { status: 500 })
        : Response.json({ id, status: "COMPLETED" });
    },
  });
  const results = await worker.tick();
  assert.equal(results.filter((x) => x.ok).length, 4);
  assert.equal(max, 2);
  assert.equal(failures[0].httpStatus, 500);
  assert.equal(failures[0].runId, 1);
});

test("recusa contrato inválido e nunca interpreta callback de outra execução", async () => {
  let calls = 0;
  const worker = new ProcessReconciler({
    baseUrl: "http://localhost",
    token: "local",
    log: quiet,
    fetchImpl: async () => {
      calls++;
      return Response.json([1, "2"]);
    },
  });
  await assert.rejects(worker.tick(), /pending inválido/);
  assert.equal(calls, 1);
  worker.fetch = async (url) =>
    Response.json(
      url.includes("pending") ? [1] : { id: 2, status: "COMPLETED" },
    );
  assert.equal((await worker.tick())[0].ok, false);
});

test("credencial ausente e configuração numérica inválida impedem inicialização", () => {
  assert.throws(
    () => new ProcessReconciler({ baseUrl: "http://localhost" }),
    /TOKEN obrigatório/,
  );
  assert.throws(
    () =>
      new ProcessReconciler({
        baseUrl: "http://localhost",
        token: "local",
        concurrency: 0,
      }),
    /numérica/,
  );
});

test("erros não expõem token nem payloads em logs", async () => {
  const logs = [];
  const worker = new ProcessReconciler({
    baseUrl: "http://localhost",
    token: "local-sensitive",
    log: {
      info() {},
      error(x) {
        logs.push(x);
      },
    },
    fetchImpl: async (url) => {
      if (url.includes("pending")) return Response.json([1]);
      throw new Error("local-sensitive");
    },
  });
  await worker.tick();
  assert.equal(logs.length, 1);
  assert(!logs[0].includes("local-sensitive"));
});
