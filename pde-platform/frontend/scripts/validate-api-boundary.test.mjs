import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, mkdir, writeFile, copyFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

test("a fronteira cobre também componentes privados integrados à imagem", async (t) => {
  const root = await mkdtemp(join(tmpdir(), "vega-api-boundary-"));
  t.after(() => rm(root, { recursive: true, force: true }));
  for (const directory of ["src", "public", "vega-private", "scripts"])
    await mkdir(join(root, directory));
  for (const file of [
    "index.html",
    "vite.config.ts",
    "nginx.conf",
    "Dockerfile",
    "package.json",
    "package-lock.json",
  ])
    await writeFile(join(root, file), "");
  const script = join(root, "scripts", "validate-api-boundary.sh");
  await copyFile(
    new URL("./validate-api-boundary.sh", import.meta.url),
    script,
  );
  await writeFile(
    join(root, "vega-private", "component.tsx"),
    'fetch("/api/pde/vega/private/v1/session")',
  );
  assert.equal(spawnSync("bash", [script]).status, 0);
  await writeFile(
    join(root, "vega-private", "component.tsx"),
    'fetch("http://191.252.181.168/api/products")',
  );
  const failed = spawnSync("bash", [script], { encoding: "utf8" });
  assert.equal(failed.status, 1);
  assert.match(failed.stdout, /vega-private\/component.tsx/);
});
