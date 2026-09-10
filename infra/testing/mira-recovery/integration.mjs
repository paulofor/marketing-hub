import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import fs from "node:fs/promises";
import http from "node:http";
import path from "node:path";

// Executa a interface e o harness reais com atraso de rede antes da persistência do uso.
const root = path.resolve(import.meta.dirname, "../../..");
const outputDirectory = path.resolve(process.argv[2]);
const backend = process.env.MIRA_LOCAL_PDE_BACKEND || "http://127.0.0.1:18096";
assert.match(backend, /^http:\/\/(127\.0\.0\.1|localhost):\d+$/);
await fs.mkdir(outputDirectory, { recursive: true });
const calls = [];
const server = http.createServer(async (request, response) => {
  try {
    if (request.url.startsWith("/api/")) {
      const chunks = [];
      for await (const chunk of request) chunks.push(chunk);
      const body = Buffer.concat(chunks);
      const event = request.url.endsWith("/events") ? JSON.parse(body).eventType : null;
      const session = request.headers["x-mira-session"];
      if (event) calls.push({ event, phase: "requested", session });
      if (event === "READY_RESULT_USED") await new Promise(resolve => setTimeout(resolve, 500));
      const upstream = await fetch(new URL(request.url, backend), {
        method: request.method,
        headers: { "Content-Type": "application/json", "X-Mira-Session": session || "",
          "X-PDE-Internal-Token": request.headers["x-pde-internal-token"] || "" },
        body: body.length ? body : undefined,
      });
      const result = await upstream.text();
      if (event && upstream.ok) calls.push({ event, phase: "persisted", session });
      response.writeHead(upstream.status, { "content-type": "application/json" });
      response.end(result);
      return;
    }
    const file = request.url === "/mira-private" ? "mira.html" :
      request.url.replace(/^\/mira-private-assets\//, "");
    if (file.includes("..") || (!file.startsWith("assets/") && file !== "mira.html")) {
      response.writeHead(404); response.end(); return;
    }
    const mime = file.endsWith(".js") ? "text/javascript" : file.endsWith(".css") ? "text/css" : "text/html";
    response.writeHead(200, { "content-type": mime + ";charset=utf-8" });
    response.end(await fs.readFile(path.join(root, "pde-platform/frontend/dist-mira", file)));
  } catch {
    response.writeHead(502); response.end('{"error":"Falha no proxy local de teste"}');
  }
});
server.listen(0, "127.0.0.1");
await once(server, "listening");
try {
  const sourceUrl = `http://127.0.0.1:${server.address().port}/mira-private`;
  const input = { mode: "TECHNICAL", captureSessionId: randomUUID(), sourceUrl,
    sourceReference: "product:10@agent-validation-v1", productId: 10, productSlug: "pde-planejado-36" };
  const inputPath = path.join(outputDirectory, "input.json");
  const outputPath = path.join(outputDirectory, "output.json");
  await fs.writeFile(inputPath, JSON.stringify(input));
  const harness = process.env.PDE_HARNESS_TEST_SCRIPT || path.join(root,
    "customer-agent-worker/src/main/resources/browser/pde-agent-validation-harness.mjs");
  const child = spawn(process.execPath, [harness, inputPath, outputPath, path.join(outputDirectory, "evidence")],
    { env: { ...process.env, PDE_INTERNAL_API_TOKEN: "mira-local-internal" } });
  let log = "";
  child.stdout.on("data", chunk => { log += chunk; });
  child.stderr.on("data", chunk => { log += chunk; });
  const timer = setTimeout(() => child.kill("SIGKILL"), 90000);
  const [code] = await once(child, "close");
  clearTimeout(timer);
  assert.equal(code, 0, log);
  const result = JSON.parse(await fs.readFile(outputPath, "utf8"));
  assert.equal(result.decision, "APPROVED");
  assert.equal(result.prototypeVersion, "mira-private-v2");
  assert.equal(result.scenarios.length, 5);
  assert.equal(result.devices.length, 3);
  assert.equal(result.artifacts.length, 5);
  assert.equal(Object.values(result.checks).every(Boolean), true);
  for (const scenario of result.scenarios) {
    assert.equal(scenario.status, "PASS");
    assert.equal(scenario.trafficClass, "AGENT_VALIDATION");
    assert.equal(scenario.humanEvidenceClaimed, false);
    assert.equal(scenario.commercialEvidenceClaimed, false);
    assert.equal(scenario.sideEffects.mediaSpendBrl, 0);
    assert.equal(new Set(scenario.events).size, scenario.events.length);
  }
  const recovery = calls.find(call => call.event === "RECOVERY_COMPLETED");
  assert.deepEqual(calls.filter(call => call.session === recovery.session).map(call => `${call.event}:${call.phase}`), [
    "READY_RESULT_USED:requested", "READY_RESULT_USED:persisted",
    "RECOVERY_COMPLETED:requested", "RECOVERY_COMPLETED:persisted",
    "AGENT_SCENARIO_COMPLETED:requested", "AGENT_SCENARIO_COMPLETED:persisted",
  ]);
  console.log(JSON.stringify({ decision: result.decision, version: result.prototypeVersion,
    scenarios: result.scenarios.length, devices: result.devices.length, artifacts: result.artifacts.length,
    delayedUseConfirmed: true, humanEvidenceClaimed: false }));
} finally {
  server.closeAllConnections();
  await new Promise(resolve => server.close(resolve));
}
