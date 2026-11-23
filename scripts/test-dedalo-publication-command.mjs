import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from "node:fs";
import { spawnSync } from "node:child_process";
import os from "node:os";
import path from "node:path";
import test from "node:test";

const root = path.resolve(import.meta.dirname, "..");
const source = readFileSync(
  path.join(root, ".github/workflows/landing-generator-agent-worker-ci.yml"),
  "utf8",
);
const step = source
  .split(
    "      - name: Subir executor com sessão individual\n        run: |\n",
  )[1]
  .split("      - name: Restore free disk space")[0]
  .split("\n")
  .map((line) => line.slice(10))
  .join("\n");
function bash(script, env = {}) {
  const result = spawnSync("bash", ["-c", script], {
    cwd: root,
    env: { ...process.env, ...env },
    encoding: "utf8",
  });
  assert.equal(result.status, 0, result.stderr);
  return result;
}
test("Dédalo publica pelo lock existente e o comando remoto real preserva aspas e saúde", () => {
  const dir = mkdtempSync(path.join(os.tmpdir(), "dedalo-publication-"));
  try {
    const local = path.join(dir, "local.sh");
    writeFileSync(local, step);
    for (const [command, args] of [
      ["bash", ["-n", local]],
      ["shellcheck", ["-s", "bash", local]],
    ]) {
      const result = spawnSync(command, args, { encoding: "utf8" });
      assert.equal(result.status, 0, result.stdout + result.stderr);
    }
    const capture = path.join(dir, "remote.sh");
    bash('ssh(){ printf "%s" "${@: -1}" > "$CAPTURE"; };\n' + step, {
      SSH_COMMON_ARGS: "-o BatchMode=yes",
      VPS_USER: "synthetic",
      VPS_IP: "127.0.0.1",
      REPOSITORY_DIR: root,
      DEPLOY_SOURCE_SHA: "a".repeat(40),
      CAPTURE: capture,
    });
    const remote = readFileSync(capture, "utf8");
    assert.ok(remote.includes("with-agent-consumer-lock.py"));
    assert.ok(remote.includes("--state-directory"));
    const inner = path.join(dir, "inner.sh");
    // Intercepta instalação e coordenador; nenhum Docker, SSH ou diretório operacional é tocado.
    bash(
      'install(){ :; }; python3(){ printf "%s" "${@: -1}" > "$INNER"; };\n' +
        remote,
      { INNER: inner },
    );
    const command = readFileSync(inner, "utf8");
    assert.ok(
      command.includes(
        "docker compose up -d --no-build --pull never --remove-orphans",
      ),
    );
    assert.equal(spawnSync("bash", ["-n", inner]).status, 0);
    bash(
      'docker(){ :; }; curl(){ printf \'{"status":"UP"}\'; }; export -f docker curl; bash "$INNER"',
      { INNER: inner },
    );
    const failed = spawnSync(
      "bash",
      [
        "-c",
        'docker(){ :; }; curl(){ printf \'{"status":"DOWN"}\'; }; sleep(){ :; }; export -f docker curl sleep; bash "$INNER"',
      ],
      { cwd: root, env: { ...process.env, INNER: inner }, encoding: "utf8" },
    );
    assert.equal(failed.status, 1);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
