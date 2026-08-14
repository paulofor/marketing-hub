import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import test from "node:test";
import { createCodexAuthReconnectCoordinator } from "../src/codex-auth-reconnect.js";

test("consome somente a fila market-radar e executa o cliente de device code", async () => {
  const requests = [];
  const children = [];
  const coordinator = createCodexAuthReconnectCoordinator({
    backendBaseUrl: "http://backend",
    fetchFn: async (url) => {
      requests.push(url);
      return { ok: true, status: 200, json: async () => ({ id: 42 }) };
    },
    spawnFn: (command, args, options) => {
      const child = new EventEmitter();
      child.stderr = new EventEmitter();
      children.push({ command, args, options });
      queueMicrotask(() => child.emit("exit", 0));
      return child;
    },
    logger: { error() {} },
  });

  assert.equal(await coordinator.poll(), true);
  assert.match(
    requests[0],
    /market-radar\/codex-auth\/reconnections\/pending$/,
  );
  assert.equal(children[0].options.env.CODEX_AUTH_RECONNECT_ID, "42");
  assert.equal(
    children[0].options.env.CODEX_AUTH_CALLBACK_BASE_URL,
    "http://backend",
  );
});

test("persiste a causa real quando o App Server encerra antes do device code", async () => {
  const payloads = [];
  const coordinator = createCodexAuthReconnectCoordinator({
    backendBaseUrl: "http://backend",
    fetchFn: async (url, options = {}) => {
      if (options.method === "POST") {
        payloads.push(JSON.parse(options.body));
        return { ok: true, status: 200 };
      }
      return { ok: true, status: 200, json: async () => ({ id: 43 }) };
    },
    spawnFn: () => {
      const child = new EventEmitter();
      child.stderr = new EventEmitter();
      queueMicrotask(() => {
        child.stderr.emit("data", "CODEX_HOME não possui permissão de escrita");
        child.emit("exit", 1);
      });
      return child;
    },
    logger: { error() {} },
  });

  assert.equal(await coordinator.poll(), true);
  assert.equal(
    payloads[0].detail,
    "CODEX_HOME não possui permissão de escrita",
  );
});

test("não inicia processo quando não existe reconexão pendente", async () => {
  let spawned = false;
  const coordinator = createCodexAuthReconnectCoordinator({
    fetchFn: async () => ({ ok: true, status: 204 }),
    spawnFn: () => {
      spawned = true;
    },
    logger: { error() {} },
  });

  assert.equal(await coordinator.poll(), false);
  assert.equal(spawned, false);
});
