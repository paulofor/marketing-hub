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
      assert.equal(
        surface.lifecycleStatus,
        "SUPPORTED",
        `[ARQUITETURA] Superfície sem ciclo de vida suportado: ${surface.deployTarget}`,
      );
      assert.ok(
        surface.experienceVersion,
        `[ARQUITETURA] Superfície sem experienceVersion: ${surface.deployTarget}`,
      );
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
    miraEntrypoint,
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
      source("pde-platform/frontend/docker-entrypoint-mira.d/10-runtime-config.sh"),
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
  assert.match(miraDockerfile, /source-fingerprint\.mjs/);
  assert.match(miraDockerfile, /frontend-source\.sha256/);
  assert.match(miraEntrypoint, /FRONTEND_SOURCE_SHA256/);
  assert.match(miraEntrypoint, /"frontendSourceSha256"/);
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
    "PDE_FRONTEND_VERSION_ID=mira-private-v3",
    "PDE_DEPLOY_FRONTEND_VERSION: ${{ needs.deployment_scope.outputs.frontend-version }}",
    "scripts/deploy-versioned-frontend.sh",
    "scripts/deploy-shared-component.sh",
    "frontend-contract-sha256",
    "PDE_DEPLOY_BACKEND",
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
  assert.doesNotMatch(workflow, /FRONTEND_SERVICES=/);
  assert.doesNotMatch(
    workflow,
    /docker rm -f pde-platform-backend pde-ai-worker pde-retention-worker/,
  );
  assert.match(proxy, /location = \/mira-private/);
  assert.match(proxy, /pde-platform-frontend-mira:80/);
  assert.match(proxy, /location \^~ \/mira-private-assets\//);
  assert.match(smoke, /validate_mira/);
  assert.match(smokeTest, /run_target mira/);
  assert.match(proxyValidation, /vega_v5_container_id_before/);
  assert.match(proxyValidation, /vega_v6_container_id_before/);
  assert.match(proxyValidation, /vega_container_id_before/);
  assert.match(proxyValidation, /force-recreate --no-deps --wait pde-platform-frontend-mira/);
});

test("Mira comercial possui imagem, rota e smoke próprios", async () => {
  const [workflow, proxy, smoke, smokeTest, reload] = await Promise.all([
    source(".github/workflows/pde-platform-metodo-musa-ci.yml"),
    source("lead-portal-payments-service/nginx.conf"),
    source("pde-platform/scripts/run-targeted-production-smokes.sh"),
    source("pde-platform/scripts/test-targeted-production-smokes.sh"),
    source("pde-platform/scripts/reload-published-frontend-proxies.sh"),
  ]);

  for (const marker of [
    "FRONTEND_MIRA_COMMERCIAL_IMAGE_NAME: pde-platform-frontend-mira-commercial",
    "PDE_PLATFORM_FRONTEND_MIRA_COMMERCIAL_PORT",
    "Dockerfile.mira-commercial",
    "mira-commercial-v1",
    "https://mira.digicomdigital.com.br",
  ]) {
    assert.ok(workflow.includes(marker), `[ARQUITETURA] Workflow sem ${marker}`);
  }
  assert.match(proxy, /mira\.digicomdigital\.com\.br/);
  assert.match(proxy, /pde-platform-frontend-mira-commercial:80/);
  assert.match(smoke, /validate_mira_commercial/);
  assert.match(smokeTest, /run_target mira-commercial/);
  assert.match(reload, /pde-platform-frontend-mira-commercial/);
});

test("Alcyone possui imagem, porta, proxy, fixtures e smoke exclusivos", async () => {
  const [workflow, proxy, smoke, smokeTest, entry, dockerfile, nginx, generator, localCompose] =
    await Promise.all([
      source(".github/workflows/pde-platform-metodo-musa-ci.yml"),
      source("lead-portal-payments-service/nginx.conf"),
      source("pde-platform/scripts/run-targeted-production-smokes.sh"),
      source("pde-platform/scripts/test-targeted-production-smokes.sh"),
      source("pde-platform/frontend/src/AlcyoneEntry.tsx"),
      source("pde-platform/frontend/Dockerfile.alcyone"),
      source("pde-platform/frontend/nginx.alcyone.conf"),
      source("pde-platform/frontend/scripts/generate-alcyone-static-fixtures.mjs"),
      source("pde-platform/docker-compose.yml"),
    ]);
  for (const marker of [
    "FRONTEND_ALCYONE_IMAGE_NAME: pde-platform-frontend-alcyone",
    "PDE_PLATFORM_FRONTEND_ALCYONE_PORT",
    "Dockerfile.alcyone",
    "alcyone-private-v3",
    "https://alcyone.digicomdigital.com.br",
    "test-alcyone-local-integration.sh",
  ]) {
    assert.ok(workflow.includes(marker), `[ARQUITETURA] Workflow sem ${marker}`);
  }
  assert.match(proxy, /alcyone\.digicomdigital\.com\.br pde-platform-frontend-alcyone:80/);
  assert.match(proxy, /live\/alcyone\.digicomdigital\.com\.br\/fullchain\.pem/);
  assert.match(smoke, /validate_alcyone/);
  assert.match(smokeTest, /run_target alcyone/);
  assert.match(entry, /alcyone-agent-validation-session/);
  assert.doesNotMatch(entry, /MiraPrivatePrototype|metodo-musa-7-dias/);
  assert.match(dockerfile, /build:alcyone/);
  assert.match(nginx, /agent-validation\/v1\/products\/pde-planejado-46/);
  assert.match(generator, /1024/);
  assert.match(generator, /PDE_STATIC_RESULT_FIXTURES_V1/);
  assert.match(
    localCompose,
    /pde-platform-frontend-alcyone:[\s\S]*?pde-platform-backend:[\s\S]*?condition: service_healthy[\s\S]*?healthcheck:[\s\S]*?version-diagnostics\.json/,
    "[ARQUITETURA] A superfície local de Alcyone deve aguardar a saúde do backend.",
  );
  assert.match(
    localCompose,
    /pde-platform-alcyone-e2e:[\s\S]*?pde-platform-frontend-alcyone:[\s\S]*?condition: service_healthy/,
    "[ARQUITETURA] O E2E de Alcyone deve aguardar a saúde do frontend e do proxy.",
  );
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
