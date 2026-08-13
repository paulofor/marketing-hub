import assert from "node:assert/strict";
import { chmod, mkdtemp, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";
import http from "node:http";

const root = path.resolve(import.meta.dirname, "..");
const reporters = [
  "growth-operator-worker/agent-health-report.mjs",
  "experiment-strategist-worker/agent-health-report.mjs",
  "customer-agent-worker/agent-health-report.mjs",
  "financial-agent-worker/agent-health-report.mjs",
  "meta-ad-approver-worker/agent-health-report.mjs",
  "landing-generator-agent-worker/agent-health-report.mjs",
];
const temporary = await mkdtemp(path.join(tmpdir(), "agent-version-gate-"));
const codex = path.join(temporary, "codex");
await writeFile(codex, "#!/usr/bin/env sh\nexit 0\n");
await chmod(codex, 0o755);

let response = { status: "READY", expectedVersion: 1, deployedVersion: 1, versionCurrent: true };
const server = http.createServer((request, reply) => {
  request.resume();
  reply.writeHead(200, { "content-type": "application/json" });
  reply.end(JSON.stringify(response));
});
await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const backend = `http://127.0.0.1:${server.address().port}`;

function run(reporter) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [path.join(root, reporter)], {
      env: {
        ...process.env,
        PATH: `${temporary}:${process.env.PATH}`,
        BACKEND_URL: backend,
        AGENT_HEALTH_KEY: "financial-agent",
        AGENT_HEALTH_VERSION: "1",
      },
      stdio: ["ignore", "pipe", "pipe"],
    });
    let error = "";
    child.stderr.on("data", (chunk) => (error += chunk));
    child.on("close", (code) => resolve({ code, error }));
  });
}

try {
  for (const reporter of reporters) assert.equal((await run(reporter)).code, 0, reporter);
  await writeFile(codex, "#!/usr/bin/env sh\nexit 1\n");
  response = { status: "BLOCKED", expectedVersion: 1, deployedVersion: 1, versionCurrent: true };
  for (const reporter of reporters) assert.equal((await run(reporter)).code, 0, reporter);
  response = { status: "BLOCKED", expectedVersion: 3, deployedVersion: 1, versionCurrent: false };
  for (const reporter of reporters) {
    const result = await run(reporter);
    assert.equal(result.code, 1, reporter);
    assert.match(result.error, /esperado=3 implantado=1 status=BLOCKED/);
  }
  console.log("Contrato de bloqueio por versão dos executores validado.");
} finally {
  server.close();
}
