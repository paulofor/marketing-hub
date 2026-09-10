import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { promises as fs } from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import {
  buildBundle,
  FIXED_REVIEW_PATHS,
} from "./build-commercial-review-evidence.mjs";

const proofPath = "pde-platform/backend/catalogo-compartilhado.java";
const currentContent = "catálogo com Mira v3 e contrato do Rigel preservado";
const hash = (content) => createHash("sha256").update(content).digest("hex");

async function fixture(t) {
  const root = await fs.mkdtemp(
    path.join(os.tmpdir(), "commercial-evidence-test-"),
  );
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  for (const relative of [...FIXED_REVIEW_PATHS, proofPath]) {
    const file = path.join(root, relative);
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(
      file,
      relative.endsWith(".json") ? "{}" : currentContent,
    );
  }
  return { root, destination: path.join(root, "bundle") };
}

async function manifest(
  root,
  revision,
  overrides = {},
  filename = `rigel-v${revision}.json`,
) {
  const relativePath = `pde-platform/contracts/${filename}`;
  const value = {
    evidenceVersion: `rigel-homologation-v${revision}`,
    product: { id: 9, slug: "rigel" },
    implementationEvidence: [{ path: proofPath, sha256: hash(currentContent) }],
    ...overrides,
  };
  await fs.writeFile(path.join(root, relativePath), JSON.stringify(value));
  return relativePath;
}

test("empacota revisão vigente íntegra e preserva a prova histórica sem reescrever hashes", async (t) => {
  const { root, destination } = await fixture(t);
  const historical = await manifest(root, 6, {
    implementationEvidence: [{ path: proofPath, sha256: "0".repeat(64) }],
  });
  const original = await fs.readFile(path.join(root, historical), "utf8");
  const current = await manifest(root, 7);
  const index = await buildBundle(root, destination);
  assert.deepEqual(index.manifestPaths, [historical, current]);
  assert.equal(
    await fs.readFile(path.join(destination, historical), "utf8"),
    original,
  );
  assert.equal(
    index.files.find((item) => item.path === proofPath).sha256,
    hash(currentContent),
  );
  assert.deepEqual(
    JSON.parse(
      await fs.readFile(
        path.join(destination, "commercial-review-bundle-index-v1.json"),
      ),
    ),
    index,
  );
});

for (const collection of ["implementationEvidence", "executableEvidence"]) {
  test(`recusa ${collection} vigente alterada e mantém pacote anterior`, async (t) => {
    const { root, destination } = await fixture(t);
    await manifest(root, 7);
    await buildBundle(root, destination);
    const originalIndex = await fs.readFile(
      path.join(destination, "commercial-review-bundle-index-v1.json"),
    );
    await manifest(root, 7, {
      [collection]: [{ path: proofPath, sha256: "0".repeat(64) }],
    });
    await assert.rejects(buildBundle(root, destination), (error) => {
      assert.match(error.message, /SHA-256 divergente/);
      assert.match(error.message, /produto=rigel/);
      assert.match(
        error.message,
        /manifesto=pde-platform\/contracts\/rigel-v7.json/,
      );
      assert.ok(error.message.includes(proofPath));
      return true;
    });
    assert.deepEqual(
      await fs.readFile(
        path.join(destination, "commercial-review-bundle-index-v1.json"),
      ),
      originalIndex,
    );
  });
}

test("seleciona revisão numérica por produto e verifica todos os produtos", async (t) => {
  const { root, destination } = await fixture(t);
  await manifest(root, 9, {
    implementationEvidence: [{ path: proofPath, sha256: "0".repeat(64) }],
  });
  await manifest(root, 10);
  await manifest(root, 1, { product: { id: 4, slug: "vega" } }, "vega-v1.json");
  await buildBundle(root, destination);
  await manifest(
    root,
    1,
    {
      product: { id: 4, slug: "vega" },
      implementationEvidence: [{ path: proofPath, sha256: "0".repeat(64) }],
    },
    "vega-v1.json",
  );
  await assert.rejects(buildBundle(root, destination), /produto=vega/);
});

test("recusa dois manifestos vigentes da mesma revisão e produto", async (t) => {
  const { root, destination } = await fixture(t);
  await manifest(root, 7);
  await manifest(root, 7, {}, "rigel-duplicado-v7.json");
  await assert.rejects(
    buildBundle(root, destination),
    /Mais de um manifesto vigente.*rigel/,
  );
});

test("respeita contractVersion antes de evidenceVersion como o carregador Java", async (t) => {
  const { root, destination } = await fixture(t);
  await manifest(root, 1, { contractVersion: "rigel.v10" });
  await manifest(root, 9, {
    implementationEvidence: [{ path: proofPath, sha256: "0".repeat(64) }],
  });
  await buildBundle(root, destination);
});

test("preserva candidata homologationEvidence para revisão independente sem aprová-la", async (t) => {
  const { root, destination } = await fixture(t);
  await manifest(root, 7, {
    implementationEvidence: [],
    homologationEvidence: [{ path: proofPath, sha256: "0".repeat(64) }],
  });
  const index = await buildBundle(root, destination);
  assert.equal(
    index.files.find((item) => item.path === proofPath).sha256,
    hash(currentContent),
  );
  const value = JSON.parse(
    await fs.readFile(path.join(destination, index.manifestPaths[0])),
  );
  assert.equal(value.homologationEvidence[0].sha256, "0".repeat(64));
  assert.equal(value.status, undefined);
});

test("bloqueia atestação sem produto ou sem identidade auditável", async (t) => {
  const { root, destination } = await fixture(t);
  await manifest(root, 7, { product: { slug: "rigel" } });
  await assert.rejects(
    buildBundle(root, destination),
    /sem identidade de produto/,
  );
});

test("bloqueia arquivo ausente, traversal e link simbólico", async (t) => {
  const { root, destination } = await fixture(t);
  for (const invalidPath of [
    "inexistente.java",
    "../fora.java",
    "/tmp/fora.java",
  ]) {
    await manifest(root, 7, {
      implementationEvidence: [
        { path: invalidPath, sha256: hash(currentContent) },
      ],
    });
    await assert.rejects(buildBundle(root, destination));
  }
  await fs.symlink(path.join(root, proofPath), path.join(root, "atalho.java"));
  await manifest(root, 7, {
    implementationEvidence: [
      { path: "atalho.java", sha256: hash(currentContent) },
    ],
  });
  await assert.rejects(buildBundle(root, destination), /não regular/);
});

test("referência resumida mantém obrigatoriedade de hash e justificativa", async (t) => {
  const { root, destination } = await fixture(t);
  await manifest(root, 7, {
    implementationEvidence: [
      {
        path: proofPath,
        sha256: hash(currentContent),
        promptMode: "ATTESTED_REFERENCE",
      },
    ],
  });
  await assert.rejects(
    buildBundle(root, destination),
    /sem resumo verificável/,
  );
  await manifest(root, 7, {
    implementationEvidence: [
      {
        path: proofPath,
        sha256: "0".repeat(64),
        promptMode: "ATTESTED_REFERENCE",
        reviewSummary: "Resumo não substitui atestação",
      },
    ],
  });
  await assert.rejects(buildBundle(root, destination), /SHA-256 divergente/);
});

test("workflows e matriz Mira executam a validação compartilhada", async () => {
  for (const worker of ["customer-agent-worker", "meta-ad-approver-worker"]) {
    const workflow = await fs.readFile(
      new URL(`../.github/workflows/${worker}-ci.yml`, import.meta.url),
      "utf8",
    );
    assert.equal(
      workflow.split("- scripts/build-commercial-review-evidence.test.mjs")
        .length - 1,
      2,
    );
    assert.ok(
      workflow.includes(
        "node --test scripts/build-commercial-review-evidence.test.mjs",
      ),
    );
    assert.ok(
      workflow.includes(
        `node scripts/build-commercial-review-evidence.mjs . ${worker}/review-evidence`,
      ),
    );
  }
  const matrix = await fs.readFile(
    new URL(
      "../infra/testing/mira-recovery/run-safety-round.sh",
      import.meta.url,
    ),
    "utf8",
  );
  assert.ok(
    matrix.includes(
      "node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence",
    ),
  );
  assert.ok(matrix.includes("-f meta-ad-approver-worker/pom.xml test"));
});
