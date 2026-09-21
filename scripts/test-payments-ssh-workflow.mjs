import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const source = readFileSync(new URL("../.github/workflows/lead-portal-payments-ci.yml", import.meta.url), "utf8");
const deploy = source.split(/^  deploy:\s*$/m)[1];
const steps = deploy.split(/^      - name:/m);
const preflight = steps.findIndex(step => step.includes("bash scripts/configure-vps-ssh-fallback.sh"));
assert.ok(preflight > 0, "Pagamentos deve autenticar antes de operações remotas");
for (const name of ["PRIMARY", "FALLBACK_1", "FALLBACK_2", "FALLBACK_3", "FALLBACK_4"]) {
  assert.ok(steps[preflight].includes(`VPS_SSH_KEY_${name}:`), `Credencial ${name} ausente`);
}
assert.doesNotMatch(deploy, /StrictHostKeyChecking[= ]no|webfactory\/ssh-agent/);
let operations = 0;
for (const [index, step] of steps.entries()) {
  for (const [, command, args] of step.replace(/\\\r?\n\s*/g, " ").matchAll(/\b(ssh|scp|rsync)\s+([^\n]+)/g)) {
    operations++;
    assert.ok(index > preflight, `Operação ${command} antes da autenticação`);
    assert.ok(command === "rsync" ? args.includes('-e "ssh ${SSH_COMMON_ARGS}"') : args.startsWith("${SSH_COMMON_ARGS}"), `Transporte ${command} sem configuração validada`);
  }
}
assert.ok(operations >= 5);
assert.ok(source.includes("node ../scripts/test-payments-ssh-workflow.mjs"));
assert.equal(source.split('"scripts/test-payments-ssh-workflow.mjs"').length, 3, "Push e PR devem executar o contrato");
console.log("Pagamentos: fallback, autenticação prévia e todos os transportes protegidos.");

// O alvo público não pode depender de solicitar um certificado institucional.
assert.match(source, /deployment_target:\s+description:[\s\S]*?public_payments/);
assert.ok(deploy.includes("inputs.deployment_target == 'public_payments' || inputs.issue_digicomdigital_certificate == 'true'"));
assert.ok(source.includes('run: echo "value=${IMAGE_SHA_TAG}" >> "$GITHUB_OUTPUT"'), "Deploy deve usar imagem imutável");
const artifactCheck = steps.find(step => step.includes("Verify public payment artifacts"));
assert.ok(artifactCheck?.includes("if: inputs.deployment_target == 'public_payments'"));
for (const required of ["https://pagamentopalf.site/agenda-cheia/", '--expected-address "$DEPLOY_HOST"', "obrigado.html obrigado.js"]) {
  assert.ok(artifactCheck.includes(required), `Prova pública ausente: ${required}`);
}
assert.ok(deploy.indexOf("Verify public payment artifacts") > deploy.indexOf("Publish service"));
assert.equal(source.split('"scripts/verify-public-artifacts.py"').length, 3);
console.log("Destino público explícito, imagem imutável e hashes da experiência validados.");
const { spawnSync } = await import('node:child_process');
const targetStep = steps.find(step => step.includes('Validate deployment target'));
const targetScript = targetStep.split('run: |')[1].split('\n').map(line => line.replace(/^          /, '')).join('\n');
for (const [program, args] of [['bash', ['-n']], ['shellcheck', ['-s', 'bash', '-']]]) {
  const check = spawnSync(program, args, { input: targetScript, encoding: 'utf8' });
  assert.equal(check.status, 0, check.stdout + check.stderr);
}
for (const [target, club, kit, success] of [
  ['pde', 'false', 'false', true], ['public_payments', 'false', 'false', true],
  ['public_payments', 'true', 'false', false], ['public_payments', 'false', 'true', false],
  ['unknown', 'false', 'false', false],
]) {
  const result = spawnSync('bash', ['-e'], {input: targetScript, encoding: 'utf8', env: {...process.env,
    DEPLOYMENT_TARGET: target, ISSUE_CLUBEMUSA: club, ISSUE_KIT_WHATSAPP: kit}});
  assert.equal(result.status === 0, success, `${target}/${club}/${kit}: ${result.stderr}`);
}
console.log('Seleção de destino executada localmente: 5 casos, bash -n e ShellCheck.');
