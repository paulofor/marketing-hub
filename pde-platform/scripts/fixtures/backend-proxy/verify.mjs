import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { Agent } from "node:http";
import { setTimeout as delay } from "node:timers/promises";
import { httpProbe } from "./http-probe.mjs";

const project = process.env.EVIDENCE_COMPOSE_PROJECT;
assert.match(project, /^aihub-[a-z0-9-]+$/);
const composeFile = "pde-platform/scripts/fixtures/backend-proxy/compose.yml";
const docker = (...args) =>
  execFileSync("docker", args, { encoding: "utf8" }).trim();
const compose = (...args) =>
  docker("compose", "-p", project, "-f", composeFile, ...args);
const inspect = (id) => JSON.parse(docker("inspect", id))[0];
const host = process.env.DOCKER_HOST?.startsWith("tcp:")
  ? new URL(process.env.DOCKER_HOST).hostname
  : "127.0.0.1";
const url = (port, resource) => `http://${host}:${port}${resource}`;

// Usa conexão nova para medir o upstream após reload, sem reutilizar socket HTTP já encerrado.
const freshRequest = (address, options = {}) =>
  httpProbe(address, {
    ...options,
    headers: { ...options.headers, connection: "close" },
  });

async function eventually(operation) {
  let failure;
  for (let attempt = 0; attempt < 25; attempt++) {
    try {
      return await operation();
    } catch (error) {
      failure = error;
    }
    await delay(1000);
  }
  throw failure;
}

async function version(port, resource, expected, options = {}) {
  const response = await freshRequest(url(port, resource), {
    ...options,
    timeoutMs: 3000,
  });
  assert.equal(response.status, 200, `${port} ${resource}`);
  const body = JSON.parse(response.body);
  assert.equal(body.generation, expected);
  assert.equal(body.url, resource);
  return body;
}

// Mantém uma conexão no worker que resolveu o IP antigo, expondo seu cache DNS independente.
const authAgent = new Agent({ keepAlive: true, maxSockets: 1 });
process.on("exit", () => authAgent.destroy());
const cachedRequest = (resource, headers = {}) =>
  httpProbe(url(18280, resource), {
    agent: authAgent,
    headers,
    timeoutMs: 3000,
  });

// Aguarda apenas erros transitórios; acesso indevido e falhas permanentes nunca viram sucesso.
async function materialAccess(recovering = false) {
  const checks = [
    { headers: {}, expected: 403 },
    { headers: { "X-PDE-Access-Token": "invalid-local-token" }, expected: 403 },
    { headers: { "X-PDE-Access-Token": "local-evidence" }, expected: 200 },
  ];
  for (let attempt = 0; attempt < (recovering ? 25 : 1); attempt++) {
    const statuses = [];
    for (const check of checks) {
      const response = await cachedRequest(
        "/materials/proof.txt",
        check.headers,
      );
      statuses.push(response.status);
      if (response.status === check.expected) {
        assert.match(response.headers["cache-control"], /private, no-store/);
        if (check.expected === 200)
          assert.equal(response.body, "material local autorizado");
      } else {
        assert.ok(
          recovering && [500, 502, 503, 504].includes(response.status),
          `Autorização incorreta: esperado=${check.expected}, recebido=${response.status}`,
        );
      }
    }
    if (statuses.every((status, index) => status === checks[index].expected))
      return;
    console.log(
      `Autorização em recuperação DNS: tentativa=${attempt + 1}, status=${statuses.join(",")}`,
    );
    await delay(1000);
  }
  assert.fail(
    "Autorização de materiais não recuperou após a troca de IP do backend",
  );
}

const publicPath =
  "/api/pde/products/metodo-musa-7-dias?experienceVersion=v7%20teste&mh_test=1";
const miraPath = "/api/pde/mira/private/v1/contract?scenario=SAFETY&mh_test=1";
console.log("Cenário: conferir API e autorização antes da troca de IP.");
await eventually(async () => {
  for (const port of [18280, 18282, 18283])
    await version(port, publicPath, "old");
  await version(18281, miraPath, "old");
});
const frontendIds = [
  "pde-platform-frontend-v7",
  "pde-platform-frontend-mira",
  "pde-platform-frontend-v5",
].map((service) => compose("ps", "-q", service));
const before = frontendIds
  .map((id) => inspect(id))
  .map((state) => [state.Id, state.Image, state.State.StartedAt]);
const newId = compose("ps", "-q", "new-backend");
const oldId = compose("ps", "-q", "old-backend");
const network = `${project}_default`;
const newIp = inspect(newId).NetworkSettings.Networks[network].IPAddress;
const oldIp = inspect(oldId).NetworkSettings.Networks[network].IPAddress;
assert.notEqual(newIp, oldIp);
await materialAccess();
assert.equal((await cachedRequest(publicPath)).status, 200);
console.log(
  "Cenário: trocar o IP e conferir recuperação, isolamento e autorização.",
);
docker("kill", "--signal", "USR2", oldId);
docker("network", "disconnect", network, oldId);
docker("network", "connect", "--ip", oldIp, network, oldId);
docker("network", "disconnect", network, newId);
docker(
  "network",
  "connect",
  "--ip",
  newIp,
  "--alias",
  "pde-backend-current",
  network,
  newId,
);
await eventually(async () => {
  await version(18280, publicPath, "new");
  await version(18281, miraPath, "new");
});
assert.equal(
  (await freshRequest(url(18282, publicPath))).status,
  502,
  "controle legado deve reproduzir o Action",
);

const posted = await version(18281, miraPath, "new", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: '{"trafficClass":"QA_INTERNAL"}',
});
assert.equal(posted.method, "POST");
assert.equal(posted.body, '{"trafficClass":"QA_INTERNAL"}');
await materialAccess(true);
authAgent.destroy();
assert.equal(
  (await freshRequest(url(18280, "/api/pde/mira/private/v1/contract"))).status,
  404,
);
assert.equal(
  (await freshRequest(url(18281, "/api/pde/products/metodo-musa-7-dias")))
    .status,
  404,
);

console.log(
  "Cenário: recuperar proxy legado preservando imagens e containers.",
);
execFileSync(
  "bash",
  ["pde-platform/scripts/reload-published-frontend-proxies.sh"],
  { env: { ...process.env, PDE_PLATFORM_NETWORK: network }, stdio: "inherit" },
);
await version(18282, publicPath, "new");
assert.equal(
  (await freshRequest(url(18283, publicPath))).status,
  502,
  "não recarregar proxy de outro contexto",
);
assert.deepEqual(
  frontendIds
    .map((id) => inspect(id))
    .map((state) => [state.Id, state.Image, state.State.StartedAt]),
  before,
);

const workflow = await readFile(
  ".github/workflows/pde-platform-metodo-musa-ci.yml",
  "utf8",
);
assert.ok(
  workflow.includes(
    "< pde-platform/scripts/reload-published-frontend-proxies.sh",
  ),
);
assert.ok(
  workflow.indexOf("Reconnect published frontend proxies") <
    workflow.indexOf("Validate targeted production public contracts"),
);
assert.ok(
  workflow.includes("bash pde-platform/scripts/test-backend-proxy-recovery.sh"),
);
console.log(
  "PASS: troca real de IP; legado 502 recuperado; URI, query, POST e autorização antes/depois do DNS preservados; mesmas imagens e containers e isolamento entre produtos.",
);
