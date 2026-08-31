import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import {
  mkdtempSync,
  readFileSync,
  rmSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const moduleDirectory = dirname(fileURLToPath(import.meta.url));
const prepareScript = join(
  moduleDirectory,
  "../scripts/prepare-brave-runtime-secret.sh",
);
const validateScript = join(
  moduleDirectory,
  "../scripts/validate-runtime-search-config.mjs",
);

test("prepara uma cópia protegida da chave para o usuário do runtime", () => {
  const directory = mkdtempSync(join(tmpdir(), "argos-runtime-secret-"));
  const sourcePath = join(directory, "source-key");
  const targetPath = join(directory, "runtime", "brave-search-key");
  const secretValue = "brave-test-secret-never-log";
  writeFileSync(sourcePath, `${secretValue}\n`, { mode: 0o600 });

  try {
    const result = spawnSync(
      "bash",
      [
        prepareScript,
        sourcePath,
        targetPath,
        String(process.getuid()),
        String(process.getgid()),
      ],
      { encoding: "utf8" },
    );

    assert.equal(result.status, 0, result.stderr);
    assert.equal(readFileSync(targetPath, "utf8"), `${secretValue}\n`);
    assert.equal(statSync(targetPath).mode & 0o777, 0o400);
    assert.equal(result.stdout.includes(secretValue), false);
    assert.equal(result.stderr.includes(secretValue), false);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});

test("bloqueia a preparação quando a origem está ausente", () => {
  const directory = mkdtempSync(join(tmpdir(), "argos-runtime-secret-"));
  try {
    const result = spawnSync(
      "bash",
      [
        prepareScript,
        join(directory, "missing-key"),
        join(directory, "runtime", "brave-search-key"),
        String(process.getuid()),
        String(process.getgid()),
      ],
      { encoding: "utf8" },
    );

    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /ausente ou vazia/);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});

test("valida a credencial pelo mesmo contrato executado dentro da imagem", () => {
  const directory = mkdtempSync(join(tmpdir(), "argos-runtime-config-"));
  const keyPath = join(directory, "brave-search-key");
  const secretValue = "runtime-secret-never-log";
  writeFileSync(keyPath, secretValue, { mode: 0o400 });

  try {
    const success = spawnSync(process.execPath, [validateScript], {
      encoding: "utf8",
      env: runtimeEnvironment(keyPath),
    });
    assert.equal(success.status, 0, success.stderr);
    assert.match(success.stdout, /credential=CONFIGURED/);
    assert.equal(success.stdout.includes(secretValue), false);
    assert.equal(success.stderr.includes(secretValue), false);

    const failure = spawnSync(process.execPath, [validateScript], {
      encoding: "utf8",
      env: runtimeEnvironment(join(directory, "missing-key")),
    });
    assert.notEqual(failure.status, 0);
    assert.match(failure.stderr, /ausente, vazio ou ilegível/);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});

function runtimeEnvironment(keyPath) {
  const env = {
    ...process.env,
    PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
    BRAVE_SEARCH_API_KEY_FILE: keyPath,
  };
  delete env.BRAVE_SEARCH_API_KEY;
  delete env.BRAVE_API_KEY;
  return env;
}
