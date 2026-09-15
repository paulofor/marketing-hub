import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { createServer } from "node:http";
import test from "node:test";
import { httpProbe } from "./http-probe.mjs";

// Usa HTTP real em loopback; cada teste encerra também conexões incompletas.
async function serverFor(t, handler) {
  const server = createServer(handler).listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => {
    server.closeAllConnections();
    server.close();
  });
  return `http://127.0.0.1:${server.address().port}`;
}

test("preserva URI, query, POST, cabeçalhos e corpo completo", async (t) => {
  const address = await serverFor(t, async (req, res) => {
    let body = "";
    for await (const chunk of req) body += chunk;
    res.writeHead(201, { "cache-control": "private, no-store" });
    res.end(JSON.stringify({ url: req.url, method: req.method, body }));
  });
  const response = await httpProbe(`${address}/api?mh_test=1`, {
    method: "POST",
    body: "QA_INTERNAL",
  });
  assert.equal(response.status, 201);
  assert.equal(response.headers["cache-control"], "private, no-store");
  assert.deepEqual(JSON.parse(response.body), {
    url: "/api?mh_test=1",
    method: "POST",
    body: "QA_INTERNAL",
  });
});

for (const status of [403, 404, 500, 502, 503, 504]) {
  test(`preserva HTTP ${status} para o critério funcional do cenário`, async (t) => {
    const address = await serverFor(t, (_req, res) =>
      res.writeHead(status).end(),
    );
    assert.equal((await httpProbe(address)).status, status);
  });
}

test("recusa corpo truncado antes de tratar a resposta como sucesso", async (t) => {
  const address = await serverFor(t, (_req, res) => {
    res.writeHead(200, { "content-length": "100" });
    res.write("incompleto");
    setTimeout(() => res.destroy(), 10);
  });
  await assert.rejects(
    httpProbe(address),
    /Resposta HTTP (abortada|interrompida|incompleta)/,
  );
});

test("limita a espera pelo corpo mesmo depois de receber os cabeçalhos", async (t) => {
  const address = await serverFor(t, (_req, res) => {
    res.writeHead(200);
    res.flushHeaders();
  });
  await assert.rejects(
    httpProbe(address, { timeoutMs: 50 }),
    /Timeout HTTP após 50 ms/,
  );
});

test("conexão recusada produz erro com contexto", async (t) => {
  const server = createServer().listen(0, "127.0.0.1");
  await once(server, "listening");
  const address = `http://127.0.0.1:${server.address().port}/contract`;
  await new Promise((resolve) => server.close(resolve));
  await assert.rejects(httpProbe(address), /Falha HTTP: GET \/contract/);
});

test("socket reutilizado sem referência encerra por timeout explícito, sem exit 13", async (t) => {
  const address = await serverFor(t, (req, res) => {
    if (req.url === "/first") res.end("ok");
  });
  const child = spawn(
    process.execPath,
    [
      "--input-type=module",
      "-e",
      `
    import { Agent } from 'node:http';
    import { httpProbe } from ${JSON.stringify(new URL("./http-probe.mjs", import.meta.url).href)};
    class IdleAgent extends Agent {
      reuseSocket(socket, request) { super.reuseSocket(socket, request); socket.unref(); }
    }
    const agent = new IdleAgent({ keepAlive: true, maxSockets: 1 });
    try {
      await httpProbe(${JSON.stringify(address + "/first")}, { agent });
      await httpProbe(${JSON.stringify(address + "/pending")}, { agent, timeoutMs: 100 });
      process.exitCode = 2;
    } catch (error) {
      console.error(error.message);
      process.exitCode = 1;
    } finally { agent.destroy(); }
  `,
    ],
    { stdio: ["ignore", "pipe", "pipe"], timeout: 5000 },
  );
  let stderr = "";
  child.stderr.on("data", (chunk) => {
    stderr += chunk;
  });
  const [code, signal] = await once(child, "close");
  assert.equal(signal, null);
  assert.equal(code, 1, stderr);
  assert.match(stderr, /Timeout HTTP após 100 ms: GET \/pending/);
});
