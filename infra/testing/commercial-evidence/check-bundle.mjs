import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import path from "node:path";

const root = process.argv[2] ?? "/app/commercial-evidence";
const index = JSON.parse(
  await readFile(path.join(root, "commercial-review-bundle-index-v1.json")),
);
assert.equal(index.bundleVersion, "pde-commercial-review-evidence-v1");
const latestRigel =
  "pde-platform/contracts/kit-whatsapp-tasting-homologation-v7.json";
assert.ok(index.manifestPaths.includes(latestRigel));
for (const item of index.files) {
  const content = await readFile(path.join(root, item.path));
  assert.equal(
    createHash("sha256").update(content).digest("hex"),
    item.sha256,
    item.path,
  );
  assert.equal(content.length, item.sizeBytes, item.path);
}
const manifest = JSON.parse(await readFile(path.join(root, latestRigel)));
assert.equal(manifest.product.slug, "kit-whatsapp-pronto");
assert.ok(
  manifest.implementationEvidence.some((item) =>
    item.path.endsWith("homologation-v6.json"),
  ),
);
assert.ok(
  manifest.implementationEvidence.some((item) =>
    item.path.endsWith("homologation-v5.json"),
  ),
);
for (const item of [
  ...manifest.implementationEvidence,
  ...manifest.executableEvidence,
]) {
  const content = await readFile(path.join(root, item.path));
  assert.equal(
    createHash("sha256").update(content).digest("hex"),
    item.sha256,
    item.path,
  );
}
console.log(
  JSON.stringify({
    status: "PASS",
    files: index.files.length,
    manifests: index.manifestPaths.length,
    latestRigel,
  }),
);
