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
