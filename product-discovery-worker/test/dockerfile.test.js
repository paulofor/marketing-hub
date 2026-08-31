import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const dockerfile = readFileSync(
  new URL("../Dockerfile", import.meta.url),
  "utf8",
);
const localCompose = readFileSync(
  new URL("../docker-compose.yml", import.meta.url),
  "utf8",
);
const deployCompose = readFileSync(
  new URL("../docker-compose.deploy.yml", import.meta.url),
  "utf8",
);
const workflow = readFileSync(
  new URL(
    "../../.github/workflows/product-discovery-worker-ci.yml",
    import.meta.url,
  ),
  "utf8",
);
const packageDefinition = JSON.parse(
  readFileSync(new URL("../package.json", import.meta.url), "utf8"),
);
const researchLibraryIgnore = readFileSync(
  new URL("../research-library/.gitignore", import.meta.url),
  "utf8",
);

test("instala certificados raiz antes do cliente Codex", () => {
  const certificates = dockerfile.indexOf("ca-certificates");
  const codex = dockerfile.indexOf("npm install -g @openai/codex");

  assert.ok(
    certificates >= 0,
    "[ARQUITETURA] O runtime de Argos deve instalar certificados raiz para autenticar no Codex.",
  );
  assert.ok(
    certificates < codex,
    "[ARQUITETURA] Os certificados raiz devem ser preparados antes da instalação do cliente Codex.",
  );
});

test("reporta a versão corrente de Argos nas duas topologias", () => {
  for (const compose of [localCompose, deployCompose]) {
    assert.match(
      compose,
      /AGENT_HEALTH_VERSION: \$\{ARGOS_AGENT_VERSION:-4\}/,
      "[ARQUITETURA] Argos deve reportar por padrão a versão 4 cadastrada no backend.",
    );
  }
});

test("empacota a biblioteca factual e mantém o modelo ativo no deploy", () => {
  assert.match(
    dockerfile,
    /COPY research-library \.\/research-library/,
    "[ARQUITETURA] A imagem de Argos deve conter o índice materializado dos artigos versionados.",
  );
  for (const compose of [localCompose, deployCompose]) {
    assert.match(
      compose,
      /ARGOS_CODEX_ENABLED: \$\{ARGOS_CODEX_ENABLED:-true\}/,
      "[ARQUITETURA] O planejamento e a síntese factual de Argos devem permanecer ativos por padrão.",
    );
    assert.match(
      compose,
      /PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS: \$\{PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS:-30\}/,
      "[ARQUITETURA] A coleta deve preservar a profundidade mínima homologada.",
    );
  }
  assert.match(workflow, /- "pesquisas\/\*\*"/);
  assert.match(workflow, /export ARGOS_CODEX_ENABLED=["']true["']/);
});

test("materializa a biblioteca vigente antes dos testes e da imagem", () => {
  const testJob = workflow.slice(
    workflow.indexOf("\n  test:"),
    workflow.indexOf("\n  docker:"),
  );
  const dockerJob = workflow.slice(
    workflow.indexOf("\n  docker:"),
    workflow.indexOf("\n  deploy:"),
  );

  assert.ok(
    testJob.indexOf("npm run build:research-library") <
      testJob.indexOf("npm test"),
    "[ARQUITETURA] O CI deve materializar a biblioteca atual antes de testar Argos.",
  );
  assert.equal(
    packageDefinition.scripts.pretest,
    "npm run build:research-library",
    "[ARQUITETURA] Testes locais devem materializar a biblioteca viva antes da validação.",
  );
  assert.match(
    researchLibraryIgnore,
    /^index\.json$/m,
    "[ARQUITETURA] O índice derivado não deve disputar merge com os Markdown que são a fonte de verdade.",
  );
  assert.doesNotMatch(
    workflow,
    /git diff --exit-code -- product-discovery-worker\/research-library\/index\.json/,
    "[ARQUITETURA] O CI não deve bloquear PR por drift de um artefato determinístico gerado no próprio job.",
  );
  assert.ok(
    dockerJob.indexOf("npm run build:research-library") <
      dockerJob.indexOf("docker/build-push-action@v5"),
    "[ARQUITETURA] A imagem deve regenerar o índice a partir do checkout vigente antes do build.",
  );
});

test("empacota Chromium como usuário sem privilégios e habilita a coleta limitada", () => {
  assert.match(dockerfile, /playwright-core install --with-deps chromium/);
  assert.match(dockerfile, /USER node/);
  assert.match(localCompose, /init: true/);
  assert.match(localCompose, /read_only: true/);
  assert.match(localCompose, /no-new-privileges:true/);
  for (const compose of [localCompose, deployCompose]) {
    assert.match(
      compose,
      /ARGOS_META_BROWSER_ENABLED: \$\{ARGOS_META_BROWSER_ENABLED:-true\}/,
      "[ARQUITETURA] Argos deve tentar a Biblioteca pública antes do fallback humano.",
    );
    assert.match(
      compose,
      /ARGOS_META_BROWSER_MAX_ADS: \$\{ARGOS_META_BROWSER_MAX_ADS:-12\}/,
    );
  }
});

test("valida o secret como usuário do runtime antes de substituir o worker", () => {
  const publishStep = workflow.slice(
    workflow.indexOf("- name: Publish service"),
  );
  const pullPosition = publishStep.indexOf(
    'docker compose "${compose_files[@]}" pull',
  );
  const preparePosition = publishStep.indexOf(
    "scripts/prepare-brave-runtime-secret.sh",
  );
  const prepareCodexPosition = publishStep.indexOf(
    "scripts/prepare-codex-runtime-home.sh",
  );
  const preflightPosition = publishStep.indexOf(
    "scripts/validate-runtime-search-config.mjs",
  );
  const codexPreflightPosition = publishStep.indexOf("codex login status");
  const publishPosition = publishStep.indexOf(
    'docker compose "${compose_files[@]}" up -d --force-recreate --remove-orphans',
  );

  assert.ok(pullPosition >= 0);
  assert.ok(pullPosition < prepareCodexPosition);
  assert.ok(prepareCodexPosition < preparePosition);
  assert.ok(pullPosition < preparePosition);
  assert.ok(preparePosition < preflightPosition);
  assert.ok(preflightPosition < codexPreflightPosition);
  assert.ok(codexPreflightPosition < publishPosition);
  assert.match(
    publishStep,
    /runtime_uid="\$\(docker run --rm --entrypoint id "\$product_discovery_worker_image" -u\)"/,
    "[ARQUITETURA] O deploy deve derivar o UID da própria imagem, sem tornar o worker root.",
  );
  assert.match(
    deployCompose,
    /BRAVE_SEARCH_API_KEY_HOST_FILE:-\/root\/infra\/argos\/secrets\/brave_search_api_key/,
    "[ARQUITETURA] O container deve montar apenas a cópia protegida preparada para seu usuário.",
  );
  assert.match(publishStep, /'"keyStatus":"CONFIGURED"'/);
});
