import assert from "node:assert/strict";
import { readdirSync, readFileSync, mkdtempSync, writeFileSync, rmSync, statSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const workflows = path.join(root, ".github/workflows");

// Aceita a forma legada e o array seguro somente quando deriva do preflight sem reatribuição.
function validatedSshArguments(args, before) {
  if (args.startsWith("${SSH_COMMON_ARGS}")) return true;
  if (!args.startsWith('"${ssh_args[@]}"')) return false;
  const declaration = 'read -r -a ssh_args <<< "$SSH_COMMON_ARGS"';
  const at = before.lastIndexOf(declaration);
  if (at < 0) return false;
  return !/\b(?:ssh_args(?:\[[^\]]*\])?\s*=|unset\s+ssh_args)/.test(before.slice(at + declaration.length));
}

assert.ok(validatedSshArguments('"${ssh_args[@]}" host', 'read -r -a ssh_args <<< "$SSH_COMMON_ARGS"\n'));
assert.ok(!validatedSshArguments('"${ssh_args[@]}" host', 'read -r -a ssh_args <<< "$UNTRUSTED"\n'));
assert.ok(!validatedSshArguments('"${ssh_args[@]}" host', 'read -r -a ssh_args <<< "$SSH_COMMON_ARGS"\nssh_args=(-o StrictHostKeyChecking=no)\n'));
assert.ok(!validatedSshArguments('"${ssh_args[@]}" host', 'read -r -a ssh_args <<< "$SSH_COMMON_ARGS"\nunset ssh_args\n'));
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
        assert.ok(validatedSshArguments(args, commands.slice(0, operation.index)), `${command} sem configuração validada: ${file}`);
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

// Exercita o comando real de cópia e a interferência de outro publicador com chaves sintéticas.
const psique = readFileSync(path.join(workflows, "customer-agent-worker-ci.yml"), "utf8");
const iris = readFileSync(path.join(workflows, "meta-ad-approver-worker-ci.yml"), "utf8");
const sourceKey = "/root/infra/psique-openai-token/openai_api_key";
const runtimeKey = "/opt/growth-operator/agents/customer-agent/secrets/openai_api_key";
assert.ok(psique.includes(`test -f ${sourceKey}`), "Psique exige sua fonte própria antes da cópia.");
assert.ok(!iris.includes(sourceKey), "A publicação de Íris não pode reescrever a fonte de Psique.");
const copyMatch = psique.match(/&& test -f [^\n]+\\\n\s*&& test -s [^\n]+\\\n\s*&& install [^\n]+/);
assert.ok(copyMatch, "O pipeline deve validar arquivo e conteúdo antes de instalar a chave de Psique.");
const fixture = mkdtempSync(path.join(tmpdir(), "psique-credential-"));
try {
  const own = path.join(fixture, "psique-source");
  const shared = path.join(fixture, "iris-source");
  const installed = path.join(fixture, "runtime-key");
  const synthetic = "sk-local-only-psique-fixture";
  writeFileSync(own, synthetic);
  writeFileSync(shared, "sk-local-only-old-iris-fixture");
  const quote = (value) => `'${value.replaceAll("'", "'\\''")}'`;
  // O runner não é root; a propriedade permanece validada no contrato, sem chown na fixture.
  assert.ok(copyMatch[0].includes("-o 10001 -g 10001 -m 0400"));
  const command = copyMatch[0].replace(/^&& /, "").replace(/\\\n\s*/g, " ")
    .replace(/\s*\\$/, "").replace("-o 10001 -g 10001 ", "")
    .replaceAll(sourceKey, quote(own)).replaceAll(runtimeKey, quote(installed));
  const runCopy = () => spawnSync("bash", ["-euc", command], { encoding: "utf8" });
  assert.equal(runCopy().status, 0);
  assert.equal(readFileSync(installed, "utf8"), synthetic);
  assert.equal(statSync(installed).mode & 0o777, 0o400);
  writeFileSync(shared, "sk-local-only-rotated-iris-fixture");
  assert.equal(runCopy().status, 0);
  assert.equal(readFileSync(installed, "utf8"), synthetic, "Outro agente não muda a credencial de Psique.");
  writeFileSync(own, "");
  assert.notEqual(runCopy().status, 0);
  assert.equal(readFileSync(installed, "utf8"), synthetic, "Fonte vazia preserva a chave instalada.");
  rmSync(own);
  assert.notEqual(runCopy().status, 0);
  assert.equal(readFileSync(installed, "utf8"), synthetic, "Fonte ausente não recebe fallback de outro agente.");
} finally {
  rmSync(fixture, { recursive: true, force: true });
}
console.log("Nove publicadores: preflight, fallbacks, SSH/SCP/rsync e limpeza sem autenticação protegidos.");
console.log("Psique: fonte isolada, rotação de outro agente e preservação da chave em falhas validadas.");
