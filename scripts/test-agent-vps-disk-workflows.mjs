import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { spawn } from "node:child_process";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const modules = [
  "agent-executor-admin-controller", "communication-agent-worker", "customer-agent-worker",
  "experiment-strategist-worker", "financial-agent-worker", "growth-operator-worker",
  "landing-generator-agent-worker", "meta-ad-approver-worker", "product-discovery-worker",
];

for (const module of modules) {
  const workflow = readFileSync(path.join(root, `.github/workflows/${module}-ci.yml`), "utf8");
  const push = workflow.split(/^  push:\s*$/m)[1]?.split(/^  [\w-]+:/m)[0];
  assert.ok(push, `Gatilho push ausente: ${module}`);
  assert.ok(!push.includes(`.github/workflows/${module}-ci.yml`), `Alteração isolada do workflow não deve ocupar o disco com novo deploy: ${module}`);
  const deploy = workflow.split(/^  deploy:\s*$/m)[1];
  assert.ok(deploy, `Deploy ausente: ${module}`);
  assert.match(deploy, /group: deploy-vps-163-245-202-80\s+queue: max\s+cancel-in-progress: false/, module);
  const gates = [...deploy.matchAll(/< scripts\/ensure-agent-vps-disk-space\.sh/g)].map(({ index }) => index);
  assert.equal(gates.length, 2, `Sondas antes e depois do deploy são obrigatórias: ${module}`);
  const [gate, restore] = gates;
  const firstWrite = deploy.search(/\brsync\b|\bscp\b|docker compose (?:up|pull|build)|name: Provision OpenAI credential/);
  assert.ok(firstWrite > gate, `Gate deve anteceder sincronização e Docker: ${module}`);
  assert.match(deploy.slice(0, gate), /ssh[^\n]+['"]bash -s['"]\s*$/, module);
  assert.ok(restore > deploy.lastIndexOf("docker compose"), `Reserva final deve ocorrer após o deploy: ${module}`);
  assert.match(deploy.slice(Math.max(0, restore - 220), restore), /name: Restore free disk space[\s\S]+if: always\(\)/, module);
}

const customer = readFileSync(path.join(root, ".github/workflows/customer-agent-worker-ci.yml"), "utf8");
assert.match(customer, /curl --fail-with-body .*--max-time 10/, "Health 503 deve preservar diagnóstico e limitar duração.");
const ci = readFileSync(path.join(root, ".github/workflows/github-actions-contracts.yml"), "utf8");
for (const file of ["ensure-agent-vps-disk-space.sh", "test-agent-vps-disk-space.sh", "test-agent-vps-disk-space-e2e.sh", "test-agent-vps-disk-workflows.mjs"]) {
  assert.ok(ci.split(`scripts/${file}`).length >= 4, `CI deve observar push/PR e executar/validar ${file}`);
}
console.log("Nove deploys protegidos por capacidade de disco antes e depois de cada publicação.");

// Executa os argumentos reais da sonda para provar que HTTP 503 mantém o diagnóstico.
const curlOptions = customer.match(/curl (.+?) http:\/\/127\.0\.0\.1:8099\//)[1].split(" ");
let statusCode = 503;
const server = http.createServer((_request, reply) => {
  reply.writeHead(statusCode, { "content-type": "application/json" });
  reply.end(JSON.stringify({ status: statusCode === 200 ? "UP" : "DOWN" }));
});
await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
try {
  for (const expected of [{ status: 503, exit: 22, body: "DOWN" }, { status: 200, exit: 0, body: "UP" }]) {
    statusCode = expected.status;
    const result = await new Promise((resolve, reject) => {
      const child = spawn("curl", [...curlOptions, `http://127.0.0.1:${server.address().port}/health`]);
      let output = "";
      child.stdout.on("data", (chunk) => { output += chunk; });
      child.stderr.resume();
      child.on("error", reject);
      child.on("close", (code) => resolve({ code, output }));
    });
    assert.equal(result.code, expected.exit);
    assert.equal(JSON.parse(result.output).status, expected.body);
  }
} finally {
  await new Promise((resolve) => server.close(resolve));
}
console.log("Health real local: HTTP 503/DOWN e 200/UP preservam corpo e código de saída.");
