import assert from "node:assert/strict";
import { promises as fs } from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import {
  parseChangedPathEntries,
  resolveDeploymentPlan,
  resolveDeployTarget,
  validateImmutableVersionedManifests,
} from "./resolve-pde-frontend-deploy-target.mjs";

async function fixture(t, contracts) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), "pde-deploy-target-"));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  await fs.mkdir(path.join(root, "pde-platform/contracts"), { recursive: true });
  for (const [name, contract] of Object.entries(contracts)) {
    await fs.writeFile(
      path.join(root, "pde-platform/contracts", name),
      JSON.stringify(contract),
    );
  }
  return root;
}

function contract(target = "v8", hash = "a".repeat(64)) {
  return {
    status: "READY_FOR_INDEPENDENT_REVIEW",
    publicationContract: {
      automaticDeployOnMerge: true,
      frontendVersion: target,
      publicUrl: `https://${target}.example.com/`,
      requiredFrontendSourceSha256: hash,
    },
    liveVisualContract: {
      runtimeIdentity: { frontendSourceSha256: hash },
    },
  };
}

function versionedContract(revision, target = "v8") {
  return {
    ...contract(target),
    contractVersion: `commercial-homologation.v${revision}`,
  };
}

test("seleciona v8 quando a atestação pronta solicita essa superfície", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  assert.equal(
    await resolveDeployTarget(
      root,
      ["pde-platform/contracts/vega-v5.json", "docs/registro.md"],
      async () => "a".repeat(64),
    ),
    "v8",
  );
});

test("não publica frontend quando o diff não contém candidata explícita", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  assert.equal(await resolveDeployTarget(root, ["pde-platform/frontend/README.md"]), "none");
});

test("separa inclusões de alterações na saída name-status do Git", () => {
  assert.deepEqual(
    parseChangedPathEntries(
      "A\tpde-platform/contracts/vega-v11.json\n" +
        "M\tpde-platform/contracts/vega-v10.json\n" +
        "R100\tantigo.json\tnovo.json\n",
    ),
    {
      changedPaths: [
        "pde-platform/contracts/vega-v11.json",
        "pde-platform/contracts/vega-v10.json",
        "novo.json",
      ],
      addedPaths: ["pde-platform/contracts/vega-v11.json"],
    },
  );
});

test("recusa reescrever manifesto versionado de evidência", async (t) => {
  const root = await fixture(t, {
    "vega-v10.json": {
      ...versionedContract(10),
      implementationEvidence: [
        { path: "codigo.java", sha256: "a".repeat(64) },
      ],
    },
  });
  await assert.rejects(
    validateImmutableVersionedManifests(
      root,
      ["pde-platform/contracts/vega-v10.json"],
      [],
    ),
    /Manifesto versionado imutável.*Crie uma nova revisão/,
  );
});

test("recusa remover contrato histórico", async (t) => {
  const root = await fixture(t, {});
  await assert.rejects(
    validateImmutableVersionedManifests(
      root,
      ["pde-platform/contracts/vega-v10.json"],
      [],
    ),
    /Contrato PDE histórico foi removido.*preserve o arquivo anterior/,
  );
});

test("permite atualizar inventário não versionado como atestação", async (t) => {
  const root = await fixture(t, {
    "product-runtime-isolation-v1.json": { products: [] },
  });
  await validateImmutableVersionedManifests(
    root,
    ["pde-platform/contracts/product-runtime-isolation-v1.json"],
    [],
  );
});

test("nova atestação sem autorização de publicação não seleciona frontend", async (t) => {
  const evidenceOnly = versionedContract(11);
  evidenceOnly.status = "EVIDENCE_COMPATIBILITY_ONLY";
  evidenceOnly.publicationContract.automaticDeployOnMerge = false;
  evidenceOnly.implementationEvidence = [
    { path: "codigo.java", sha256: "a".repeat(64) },
  ];
  const root = await fixture(t, { "vega-v11.json": evidenceOnly });
  await validateImmutableVersionedManifests(
    root,
    ["pde-platform/contracts/vega-v11.json"],
    ["pde-platform/contracts/vega-v11.json"],
  );
  const plan = await resolveDeploymentPlan(
    root,
    ["pde-platform/contracts/vega-v11.json"],
    {
      addedPaths: ["pde-platform/contracts/vega-v11.json"],
      fingerprintResolver: async () => "a".repeat(64),
    },
  );
  assert.equal(plan.frontend.target, "none");
  assert.equal(plan.hasDeployment, false);
});

test("recusa fingerprint diferente entre publicação e revisão visual", async (t) => {
  const value = contract();
  value.liveVisualContract.runtimeIdentity.frontendSourceSha256 = "b".repeat(64);
  const root = await fixture(t, { "vega-v5.json": value });
  await assert.rejects(
    resolveDeployTarget(
      root,
      ["pde-platform/contracts/vega-v5.json"],
      async () => "a".repeat(64),
    ),
    /Fingerprint público divergente/,
  );
});

test("recusa duas publicações automáticas no mesmo merge", async (t) => {
  const root = await fixture(t, {
    "vega-v5.json": contract("v8"),
    "outro-v2.json": contract("v7"),
  });
  await assert.rejects(
    resolveDeployTarget(
      root,
      [
        "pde-platform/contracts/vega-v5.json",
        "pde-platform/contracts/outro-v2.json",
      ],
      async () => "a".repeat(64),
    ),
    /Mais de uma superfície PDE/,
  );
});

test("recusa manifesto que não representa a fonte frontend atual", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  await assert.rejects(
    resolveDeployTarget(
      root,
      ["pde-platform/contracts/vega-v5.json"],
      async () => "c".repeat(64),
    ),
    /não corresponde à fonte frontend/,
  );
});

test("separa backend e workers do deploy de frontend", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  const plan = await resolveDeploymentPlan(
    root,
    [
      "pde-platform/contracts/vega-v5.json",
      "pde-platform/backend/src/main/java/Backend.java",
      "pde-platform/pde-ai-worker/src/worker.js",
    ],
    { fingerprintResolver: async () => "a".repeat(64) },
  );
  assert.equal(plan.frontend.target, "v8");
  assert.equal(plan.backend, true);
  assert.equal(plan.aiWorker, true);
  assert.equal(plan.retentionWorker, false);
  assert.equal(plan.fullCompatibility, true);
  assert.equal(plan.hasDeployment, true);
});

test("mudança operacional não reinicia nenhum runtime por efeito colateral", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  const plan = await resolveDeploymentPlan(root, [
    ".github/workflows/pde-platform-metodo-musa-ci.yml",
    "pde-platform/scripts/deploy-versioned-frontend.sh",
  ]);
  assert.equal(plan.frontend.target, "none");
  assert.equal(plan.backend, false);
  assert.equal(plan.aiWorker, false);
  assert.equal(plan.retentionWorker, false);
  assert.equal(plan.hasDeployment, false);
});

test("dispatch de frontend exige manifesto imutável da superfície", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  await assert.rejects(
    resolveDeploymentPlan(root, [], {
      manualFrontend: "v7",
      fingerprintResolver: async () => "a".repeat(64),
    }),
    /não possui manifesto imutável/,
  );
  const plan = await resolveDeploymentPlan(root, [], {
    manualFrontend: "v8",
    fingerprintResolver: async () => "a".repeat(64),
  });
  assert.equal(plan.frontend.target, "v8");
  assert.equal(plan.frontend.contractSha256.length, 64);
});

test("dispatch seleciona a maior revisão numérica, inclusive depois da v9", async (t) => {
  const root = await fixture(t, {
    "vega-v9.json": versionedContract(9),
    "vega-v10.json": versionedContract(10),
  });
  const plan = await resolveDeploymentPlan(root, [], {
    manualFrontend: "v8",
    fingerprintResolver: async () => "a".repeat(64),
  });
  assert.equal(plan.frontend.relativePath, "pde-platform/contracts/vega-v10.json");
});

test("dispatch recusa duas atestações vigentes com a mesma revisão", async (t) => {
  const root = await fixture(t, {
    "vega-a-v10.json": versionedContract(10),
    "vega-b-v10.json": versionedContract(10),
  });
  await assert.rejects(
    resolveDeploymentPlan(root, [], {
      manualFrontend: "v8",
      fingerprintResolver: async () => "a".repeat(64),
    }),
    /mais de um manifesto vigente/,
  );
});

test("dispatch manual preserva superfícies não MUSA pelo inventário suportado", async (t) => {
  const root = await fixture(t, {
    "product-runtime-isolation-v1.json": {
      products: [
        {
          productId: 10,
          productSlug: "pde-planejado-36",
          surfaces: [{ deployTarget: "mira", lifecycleStatus: "SUPPORTED" }],
        },
      ],
    },
  });
  const plan = await resolveDeploymentPlan(root, [], {
    manualFrontend: "mira",
    fingerprintResolver: async () => "f".repeat(64),
  });
  assert.equal(plan.frontend.target, "mira");
  assert.equal(
    plan.frontend.relativePath,
    "pde-platform/contracts/product-runtime-isolation-v1.json",
  );
  assert.equal(plan.frontend.sourceSha256, "f".repeat(64));
});

test("dispatch de componente compartilhado nunca seleciona frontend", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  const plan = await resolveDeploymentPlan(root, [], {
    manualFrontend: "none",
    manualSharedComponent: "backend",
  });
  assert.equal(plan.frontend.target, "none");
  assert.equal(plan.backend, true);
  assert.equal(plan.aiWorker, false);
  assert.equal(plan.retentionWorker, false);
});

test("dispatch recusa publicar todos os componentes compartilhados em lote", async (t) => {
  const root = await fixture(t, { "vega-v5.json": contract() });
  await assert.rejects(
    resolveDeploymentPlan(root, [], {
      manualFrontend: "none",
      manualSharedComponent: "all",
    }),
    /Componente compartilhado PDE inválido/,
  );
});

test("workflow usa o alvo resolvido sem fallback fixo para v7", async () => {
  const workflow = await fs.readFile(
    new URL(
      "../.github/workflows/pde-platform-metodo-musa-ci.yml",
      import.meta.url,
    ),
    "utf8",
  );
  assert.match(workflow, /Resolve frontend deployment scope/);
  assert.match(workflow, /git diff .*--name-status --diff-filter=ACMRD/);
  assert.doesNotMatch(workflow, /git diff .*--name-only --diff-filter=ACMR/);
  assert.match(
    workflow,
    /PDE_DEPLOY_FRONTEND_VERSION: \$\{\{ needs\.deployment_scope\.outputs\.frontend-version \}\}/,
  );
  assert.doesNotMatch(workflow, /TARGETED_FRONTEND_VERSION=v7/);
  assert.equal(
    workflow.split(
      '      - "scripts/resolve-pde-frontend-deploy-target.mjs"',
    ).length - 1,
    2,
  );
  assert.match(workflow, /PDE_DEPLOY_BACKEND:/);
  assert.match(workflow, /shared_component/);
  assert.match(
    workflow,
    /REMOTE_FRONTEND_CONTRACT_PATH="\$\{PDE_FRONTEND_CONTRACT_PATH#pde-platform\/\}"/,
  );
  assert.match(workflow, /PDE_RUNTIME_INVENTORY=/);
  assert.match(workflow, /PDE_DEPLOY_COMPOSE_FILE_PATH=/);
});
