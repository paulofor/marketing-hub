import assert from "node:assert/strict";
import { promises as fs } from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { sourceFingerprint } from "./source-fingerprint.mjs";

async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), "pde-source-fingerprint-"));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  for (const directory of ["docker-entrypoint.d", "public", "scripts", "src"]) {
    await fs.mkdir(path.join(root, directory), { recursive: true });
  }
  for (const file of [
    ".npmrc",
    "Dockerfile",
    "index.html",
    "mira.html",
    "nginx.conf",
    "nginx.mira.conf",
    "package-lock.json",
    "package.json",
    "tsconfig.json",
    "tsconfig.vega.json",
    "vite.config.ts",
    "vite.mira.config.ts",
    "vite.vega.config.ts",
  ]) {
    await fs.writeFile(path.join(root, file), `${file}\n`);
  }
  await fs.writeFile(path.join(root, "src", "App.tsx"), "candidata");
  await fs.writeFile(path.join(root, "public", "contract.json"), "{}\n");
  return root;
}

test("produz a mesma identidade para a mesma fonte", async (t) => {
  const root = await fixture(t);
  assert.equal(await sourceFingerprint(root), await sourceFingerprint(root));
  assert.match(await sourceFingerprint(root), /^[a-f0-9]{64}$/);
});

test("muda a identidade quando pixels ou contrato público mudam", async (t) => {
  const root = await fixture(t);
  const original = await sourceFingerprint(root);
  await fs.writeFile(path.join(root, "src", "App.tsx"), "candidata corrigida");
  assert.notEqual(await sourceFingerprint(root), original);
  const afterSource = await sourceFingerprint(root);
  await fs.writeFile(path.join(root, "public", "contract.json"), '{"version":2}\n');
  assert.notEqual(await sourceFingerprint(root), afterSource);
});

test("recusa links na árvore de fontes", async (t) => {
  const root = await fixture(t);
  await fs.symlink(path.join(root, "src", "App.tsx"), path.join(root, "src", "alias.tsx"));
  await assert.rejects(sourceFingerprint(root), /não regular/);
});
