import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { setTimeout as delay } from "node:timers/promises";

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
  fetch(address, {
    ...options,
    headers: { ...options.headers, connection: "close" },
    signal: options.signal ?? AbortSignal.timeout(10000),
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
    signal: AbortSignal.timeout(3000),
  });
  assert.equal(response.status, 200, `${port} ${resource}`);
  const body = await response.json();
  assert.equal(body.generation, expected);
  assert.equal(body.url, resource);
  return body;
}

const publicPath =
  "/api/pde/products/metodo-musa-7-dias?experienceVersion=v7%20teste&mh_test=1";
const miraPath = "/api/pde/mira/private/v1/contract?scenario=SAFETY&mh_test=1";
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
assert.equal(
  (await freshRequest(url(18280, "/materials/proof.txt"))).status,
  403,
);
assert.equal(
  (
    await freshRequest(url(18280, "/materials/proof.txt"), {
      headers: { "X-PDE-Access-Token": "local-evidence" },
    })
  ).status,
  200,
);
assert.equal(
  (await freshRequest(url(18280, "/api/pde/mira/private/v1/contract"))).status,
  404,
);
assert.equal(
  (await freshRequest(url(18281, "/api/pde/products/metodo-musa-7-dias")))
    .status,
  404,
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
  "PASS: troca real de IP; legado 502 recuperado; URI, query, POST, auth e isolamento preservados; mesmas imagens e containers.",
);
