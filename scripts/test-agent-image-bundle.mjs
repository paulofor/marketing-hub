import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const script = path.join(root, "scripts/agent-image-bundle.mjs");
const image = `marketing-hub/customer-agent-worker:${"a".repeat(40)}`;
const secondImage = `marketing-hub/iris-image-studio:${"a".repeat(40)}`;
const imageId = `sha256:${"b".repeat(64)}`;

function fixture(t) {
  const directory = mkdtempSync(path.join(os.tmpdir(), "agent-images-"));
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  const bin = path.join(directory, "bin");
  mkdirSync(bin);
  const double = `#!/usr/bin/env node
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const args = process.argv.slice(2);
const mode = process.env.IMAGE_TEST_MODE;
const dir = process.env.IMAGE_TEST_DIR;
fs.appendFileSync(path.join(dir, 'calls'), JSON.stringify({tool: path.basename(process.argv[1]), args}) + '\\n');
if (path.basename(process.argv[1]) === 'docker') {
  if (args[1] === 'inspect') {
    if (mode === 'missing-image') process.exit(1);
    console.log(mode === 'invalid-size' ? '${imageId} NaN' : '${imageId} 268435456');
  } else if (args[1] === 'save') {
    process.stdout.write('synthetic-image-archive');
    if (mode === 'export-failure') process.exitCode = 1;
  } else process.exit(91);
} else {
  const command = args.at(-1);
  if (mode === 'ssh-failure') process.exit(255);
  if (command.includes('bash -s')) {
    const source = fs.readFileSync(0, 'utf8');
    if (!source.includes('minimumInodes=10000')) process.exit(92);
    const counter = path.join(dir, 'gates');
    const calls = fs.existsSync(counter) ? Number(fs.readFileSync(counter)) + 1 : 1;
    fs.writeFileSync(counter, String(calls));
    console.log('gate ' + calls);
    if ((mode === 'disk-before' && calls === 1) || (mode === 'disk-after' && calls === 2)) process.exitCode = 1;
  } else if (command.includes('docker image load')) {
    if (zlib.gunzipSync(fs.readFileSync(0)).toString() !== 'synthetic-image-archive') process.exit(93);
    console.log('Loaded image');
    if (mode === 'load-failure') process.exitCode = 1;
  } else if (command.includes('docker image inspect')) {
    if (mode === 'inspect-failure') process.exit(1);
    console.log(mode === 'wrong-id' ? 'sha256:' + 'c'.repeat(64) : '${imageId}');
  } else process.exit(94);
}
`;
  for (const executable of ["docker", "ssh"]) writeFileSync(path.join(bin, executable), double, { mode: 0o700 });
  const env = {
    ...process.env, PATH: `${bin}:${process.env.PATH}`, IMAGE_TEST_DIR: directory,
    SSH_DEPLOY_READY: "true", SSH_COMMON_ARGS: `-F ${directory}/ssh-config`,
  };
  const bundle = path.join(directory, "bundle");
  const run = (operation, parameters = [image], changes = {}) => spawnSync(process.execPath,
    [script, operation, bundle, ...parameters], { env: { ...env, ...changes }, encoding: "utf8", timeout: 15_000 });
  const calls = () => readFileSync(path.join(directory, "calls"), "utf8").trim().split("\n").filter(Boolean).map(JSON.parse);
  return { directory, bundle, run, calls };
}

function success(result) {
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
}

test("pacote de duas imagens conserva identidade, ordem e reserva proporcional", (t) => {
  const f = fixture(t);
  success(f.run("pack", [image, secondImage]));
  success(f.run("verify", [image, secondImage]));
  const result = f.run("send", ["root@fixture.local", image, secondImage]);
  success(result);
  const ssh = f.calls().filter((call) => call.tool === "ssh");
  assert.equal(ssh.length, 5);
  assert.match(ssh[0].args.at(-1), /^AGENT_VPS_DISK_MIN_FREE_MB=5120 bash -s$/);
  assert.match(ssh[1].args.at(-1), /docker image load$/);
  assert.match(ssh[2].args.at(-1), new RegExp(image));
  assert.match(ssh[3].args.at(-1), new RegExp(secondImage));
  assert.equal(ssh[4].args.at(-1), "AGENT_VPS_DISK_MIN_FREE_MB=4096 bash -s");
  assert.match(result.stdout, /Imagens prontas/);
  assert.ok(ssh.every((call) => call.args[0] === "-F" && call.args[1].endsWith("/ssh-config")));
});

for (const mode of ["missing-image", "invalid-size", "export-failure"]) {
  test(`falha ao empacotar: ${mode}`, (t) => {
    const f = fixture(t);
    assert.notEqual(f.run("pack", [image], { IMAGE_TEST_MODE: mode }).status, 0);
    assert.notEqual(f.run("verify").status, 0);
  });
}

for (const [label, refs] of [["tag mutável", [image.replace(/a{40}$/, "latest")]],
  ["sem imagens", []], ["duplicada", [image, image]], ["shell", [`${image};touch /tmp/forbidden`]]]) {
  test(`entrada inválida: ${label}`, (t) => {
    const f = fixture(t);
    assert.notEqual(f.run("pack", refs).status, 0);
  });
}

for (const [label, mutate, refs] of [
  ["arquivo corrompido", (f) => writeFileSync(path.join(f.bundle, "images.tar.gz"), "truncated")],
  ["manifesto ausente", (f) => rmSync(path.join(f.bundle, "manifest.json"))],
  ["outra revisão", () => {}, [image.replace(/a{40}$/, "d".repeat(40))]],
  ["tamanho negativo", (f) => {
    const file = path.join(f.bundle, "manifest.json");
    const manifest = JSON.parse(readFileSync(file));
    manifest.images[0].sizeBytes = -1;
    writeFileSync(file, JSON.stringify(manifest));
  }],
]) {
  test(`pacote inválido bloqueia antes de SSH: ${label}`, (t) => {
    const f = fixture(t);
    success(f.run("pack"));
    mutate(f);
    assert.notEqual(f.run("send", ["root@fixture.local", ...(refs ?? [image])]).status, 0);
    assert.ok(f.calls().every((call) => call.tool !== "ssh"));
  });
}

for (const [mode, expectedCalls] of [["ssh-failure", 1], ["disk-before", 1], ["load-failure", 2],
  ["wrong-id", 3], ["inspect-failure", 3], ["disk-after", 4]]) {
  test(`interrompe na fase que falhou: ${mode}`, (t) => {
    const f = fixture(t);
    success(f.run("pack"));
    const result = f.run("send", ["root@fixture.local", image], { IMAGE_TEST_MODE: mode });
    assert.notEqual(result.status, 0);
    assert.doesNotMatch(result.stdout, /Imagens prontas/);
    assert.equal(f.calls().filter((call) => call.tool === "ssh").length, expectedCalls);
  });
}

for (const env of [{ SSH_DEPLOY_READY: "false" }, { SSH_COMMON_ARGS: "" },
  { SSH_COMMON_ARGS: "-o StrictHostKeyChecking=no" }]) {
  test(`sem preflight válido não conecta: ${JSON.stringify(env)}`, (t) => {
    const f = fixture(t);
    success(f.run("pack"));
    assert.notEqual(f.run("send", ["root@fixture.local", image], env).status, 0);
    assert.ok(f.calls().every((call) => call.tool !== "ssh"));
  });
}
