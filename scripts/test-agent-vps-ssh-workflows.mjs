import assert from "node:assert/strict";
import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const workflows = path.join(root, ".github/workflows");
let checked = 0;
for (const file of readdirSync(workflows).filter((name) => name.endsWith(".yml"))) {
  const source = readFileSync(path.join(workflows, file), "utf8");
  const deploy = source.split(/^  deploy:\s*$/m)[1];
  if (!deploy?.includes("group: deploy-vps-163-245-202-80")) continue;
  checked += 1;
  const steps = deploy.split(/^      - (?:name:|uses:)/m);
  const preflight = steps.findIndex((step) => step.includes("bash scripts/configure-vps-ssh-fallback.sh"));
  assert.ok(preflight > 0, `Preflight SSH obrigatório: ${file}`);
  for (const secret of ["GROWTH_OPERATOR_VPS_SSH_KEY", "VPS_SSH_CHAVE", "VPS_SSH_KEY", "SSH_PRIVATE_KEY"]) {
    assert.ok(steps[preflight].includes(`secrets.${secret} }}`), `Fallback ${secret} ausente: ${file}`);
  }
  assert.doesNotMatch(deploy, /webfactory\/ssh-agent|StrictHostKeyChecking[= ]no/, `SSH divergente: ${file}`);
  let operations = 0;
  for (const [index, step] of steps.entries()) {
    const commands = step.replace(/\\\r?\n\s*/g, " ");
    for (const operation of commands.matchAll(/\b(ssh|scp|rsync)\s+([^\n]+)/g)) {
      const [, command, args] = operation;
      operations += 1;
      assert.ok(index > preflight, `Transporte antes da autenticação: ${file}: ${command}`);
      if (command === "rsync") {
        assert.ok(args.includes('-e "ssh ${SSH_COMMON_ARGS}"'), `rsync sem configuração validada: ${file}`);
      } else {
        assert.ok(args.startsWith("${SSH_COMMON_ARGS}"), `${command} sem configuração validada: ${file}`);
      }
    }
  }
  assert.ok(operations >= 3, `Transporte remoto não verificado: ${file}`);
  const restore = steps.find((step) => step.startsWith(" Restore free disk space"));
  assert.ok(restore, `Restauração final ausente: ${file}`);
  assert.match(restore, /if: always\(\)/, file);
  assert.match(restore, /if \[ "\$\{SSH_DEPLOY_READY:-false\}" != "true" \]; then\s+echo [^\n]+\s+exit 0\s+fi\s+ssh/, `Limpeza sem autenticação deve ser ignorada: ${file}`);
}
assert.equal(checked, 9, "Os nove publicadores do VPS devem manter o contrato SSH.");

const ci = readFileSync(path.join(workflows, "github-actions-contracts.yml"), "utf8");
for (const script of ["test-agent-vps-ssh-workflows.mjs", "test-configure-vps-ssh-fallback-e2e.sh"]) {
  assert.ok(ci.split(`scripts/${script}`).length >= 4, `CI deve observar push/PR e executar ${script}`);
}
assert.ok(ci.split("scripts/fixtures/vps-ssh/**").length >= 3, "CI deve acompanhar alterações na fixture SSH.");
console.log("Nove publicadores: preflight, fallbacks, SSH/SCP/rsync e limpeza sem autenticação protegidos.");
