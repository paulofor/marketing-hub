import assert from "node:assert/strict";
import { promises as fs } from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { resolveDeployTarget } from "./resolve-pde-frontend-deploy-target.mjs";

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

test("workflow usa o alvo resolvido sem fallback fixo para v7", async () => {
  const workflow = await fs.readFile(
    new URL(
      "../.github/workflows/pde-platform-metodo-musa-ci.yml",
      import.meta.url,
    ),
    "utf8",
  );
  assert.match(workflow, /Resolve frontend deployment scope/);
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
});
