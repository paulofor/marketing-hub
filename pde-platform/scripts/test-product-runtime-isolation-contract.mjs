import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../../", import.meta.url);

async function source(path) {
  return readFile(new URL(path, root), "utf8");
}

async function contract() {
  return JSON.parse(await source("pde-platform/contracts/product-runtime-isolation-v1.json"));
}

function unique(values, label) {
  assert.equal(
    new Set(values).size,
    values.length,
    `[ARQUITETURA] ${label} deve ser exclusivo por superfície de produto.`,
  );
}

test("cada superfície PDE possui identidade operacional exclusiva", async () => {
  const definition = await contract();
  assert.equal(
    Object.values(definition.rules).every((required) => required === true),
    true,
    "[ARQUITETURA] Toda regra de isolamento do contrato deve permanecer obrigatória.",
  );
  const surfaces = definition.products.flatMap((product) =>
    product.surfaces.map((surface) => ({ ...surface, productId: product.productId })),
  );

  unique(surfaces.map(({ serviceName }) => serviceName), "serviceName");
  unique(surfaces.map(({ containerName }) => containerName), "containerName");
  unique(surfaces.map(({ imageName }) => imageName), "imageName");
  unique(surfaces.map(({ imageVariable }) => imageVariable), "imageVariable");
  unique(surfaces.map(({ portVariable }) => portVariable), "portVariable");
  unique(surfaces.map(({ hostPort }) => hostPort), "hostPort");
  unique(surfaces.map(({ publicUrl }) => publicUrl), "publicUrl");

  const mira = surfaces.filter(({ productId }) => productId === 10);
  const vega = surfaces.filter(({ productId }) => productId === 4);
  for (const field of ["serviceName", "containerName", "imageName", "hostPort"]) {
    assert.equal(
      mira.some((left) => vega.some((right) => left[field] === right[field])),
      false,
      `[ARQUITETURA] Mira e Vega não podem compartilhar ${field}.`,
    );
  }
});

test("o Compose materializa cada identidade declarada", async () => {
  const [definition, compose] = await Promise.all([
    contract(),
    source("pde-platform/docker-compose.deploy.yml"),
  ]);

  for (const product of definition.products) {
    for (const surface of product.surfaces) {
      assert.match(
        compose,
        new RegExp(`^  ${surface.serviceName}:$`, "m"),
        `[ARQUITETURA] Serviço ausente no Compose: ${surface.serviceName}`,
      );
      assert.ok(
        compose.includes(`image: \${${surface.imageVariable}:?`),
        `[ARQUITETURA] Imagem obrigatória ausente: ${surface.imageVariable}`,
      );
      assert.ok(
        compose.includes(`container_name: ${surface.containerName}`),
        `[ARQUITETURA] Container ausente: ${surface.containerName}`,
      );
      assert.ok(
        compose.includes(`\${${surface.portVariable}:-${surface.hostPort}}:80`),
        `[ARQUITETURA] Porta exclusiva ausente: ${surface.portVariable}`,
      );
    }
  }
});

test("Mira não volta ao bundle nem ao nginx do Vega", async () => {
  const [
    vegaEntry,
    vegaStyles,
    vegaNginx,
    miraEntry,
    miraStyles,
    miraNginx,
    miraDockerfile,
    miraVite,
    dockerIgnore,
  ] =
    await Promise.all([
      source("pde-platform/frontend/src/App.tsx"),
      source("pde-platform/frontend/src/styles.css"),
      source("pde-platform/frontend/nginx.conf"),
      source("pde-platform/frontend/src/MiraEntry.tsx"),
      source("pde-platform/frontend/src/mira.css"),
      source("pde-platform/frontend/nginx.mira.conf"),
      source("pde-platform/frontend/Dockerfile.mira"),
      source("pde-platform/frontend/vite.mira.config.ts"),
      source("pde-platform/frontend/.dockerignore"),
    ]);

  assert.doesNotMatch(vegaEntry, /MiraPrivatePrototype|mira-private/);
  assert.doesNotMatch(vegaStyles, /mira-private|mira-routine/);
  assert.doesNotMatch(vegaNginx, /proxy_pass[^;]*mira|try_files[^;]*mira/i);
  for (const path of [
    "/mira-private",
    "/mira-private/",
    "/mira-private-assets/",
    "/api/pde/mira/private/v1/",
  ]) {
    assert.match(
      vegaNginx,
      new RegExp(`location (?:=|\\^~) ${path.replaceAll("/", "\\/")}[\\s\\S]*?return 404;`),
      `[ARQUITETURA] O nginx de Vega deve negar explicitamente ${path}.`,
    );
  }
  assert.match(miraEntry, /MiraPrivatePrototype/);
  assert.match(miraEntry, /mira\.css/);
  assert.doesNotMatch(miraEntry, /styles\.css/);
  assert.match(miraStyles, /mira-private-shell/);
  assert.match(miraNginx, /api\/pde\/mira\/private\/v1/);
  assert.match(miraNginx, /mira-private-assets/);
  assert.match(miraDockerfile, /vite\.mira\.config\.ts|build:mira/);
  assert.match(miraDockerfile, /nginx\.mira\.conf/);
  assert.match(miraVite, /publicDir:\s*false/);
  assert.match(dockerIgnore, /^dist-mira$/m);
});

test("workflow, proxy e smoke publicam Mira sem operar Vega", async () => {
  const [workflow, proxy, smoke, smokeTest, proxyValidation] = await Promise.all([
    source(".github/workflows/pde-platform-metodo-musa-ci.yml"),
    source("lead-portal-payments-service/nginx.conf"),
    source("pde-platform/scripts/run-targeted-production-smokes.sh"),
    source("pde-platform/scripts/test-targeted-production-smokes.sh"),
    source("pde-platform/scripts/test-product-runtime-proxy-local.sh"),
  ]);

  for (const marker of [
    "FRONTEND_MIRA_IMAGE_NAME: pde-platform-frontend-mira",
    "PDE_PLATFORM_FRONTEND_MIRA_PORT",
    "Dockerfile.mira",
    "Build isolated Mira surface",
    "Validate product-exclusive build artifacts",
    "PDE_FRONTEND_VERSION_ID=mira-private-v1",
    "mira) FRONTEND_SERVICES='pde-platform-frontend-mira'",
    "TARGETED_FRONTEND_VERSION=v7",
    "bootstrap-legacy-route",
    "PDE_MIRA_PROXY_MODE",
    "bootstrap-legacy-route é permitido somente no primeiro deploy direcionado de Mira",
    "https://v7.clubemusa.com.br/mira-private/version-diagnostics.json",
    "https://v7.clubemusa.com.br/version-diagnostics.json",
    "Sua rotina, organizada com calma",
    "MIRA_PUBLIC_BASE_URL=\"http://${DEPLOY_HOST}:${PDE_PLATFORM_FRONTEND_MIRA_PORT}\"",
  ]) {
    assert.ok(workflow.includes(marker), `[ARQUITETURA] Workflow sem ${marker}`);
  }
  assert.match(proxy, /location = \/mira-private/);
  assert.match(proxy, /pde-platform-frontend-mira:80/);
  assert.match(proxy, /location \^~ \/mira-private-assets\//);
  assert.match(smoke, /validate_mira/);
  assert.match(smokeTest, /run_target mira/);
  assert.match(proxyValidation, /vega_container_id_before/);
  assert.match(proxyValidation, /force-recreate --no-deps --wait pde-platform-frontend-mira/);
});

test("a regra está na fonte canônica e na homologação técnica", async () => {
  const [platformCanon, chainCanon, validationCanon] = await Promise.all([
    source("docs/canonical/pde-platform-canon.v1.md"),
    source("docs/canonical/cadeia-produtos-pde-canon.v1.md"),
    source("docs/canonical/pde-validacao-multiagente-canon.v1.md"),
  ]);

  assert.match(platformCanon, /Isolamento obrigatório por produto/);
  assert.match(platformCanon, /Uma publicação de Mira[\s\S]+container do Vega\/Método\s+MUSA/);
  assert.match(chainCanon, /imagem, container, porta, proxy, diagnóstico e deploy/);
  assert.match(validationCanon, /imagem, container, porta, proxy e ciclo de deploy exclusivos/);
});
