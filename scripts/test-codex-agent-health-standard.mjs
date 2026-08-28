import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const contractPath = path.join(root, "config/agents/codex-agent-health-compliance.json");
const contract = JSON.parse(await readFile(contractPath, "utf8"));

assert.ok(contract.maximumHeartbeatSeconds > 0 && contract.maximumHeartbeatSeconds <= 60);
assert.ok(contract.agents.length >= 8, "todos os agentes Codex atuais devem estar cadastrados");
assert.equal(new Set(contract.agents.map(({ key }) => key)).size, contract.agents.length);

for (const agent of contract.agents) {
  const module = path.join(root, agent.module);
  const reporter = await readFile(path.join(module, agent.reporter), "utf8");
  const activation = await readFile(path.join(module, agent.activation), "utf8");

  assert.ok(
    Number.isInteger(agent.expectedVersion) && agent.expectedVersion > 0,
    `${agent.key}: versão esperada ausente ou inválida`,
  );
  assert.ok(
    Array.isArray(agent.versionSources) && agent.versionSources.length > 0,
    `${agent.key}: fonte da versão implantada ausente`,
  );
  for (const versionSource of agent.versionSources) {
    const versionConfiguration = await readFile(path.join(root, versionSource), "utf8");
    const version = agent.expectedVersion;
    const declaresExpectedVersion = [
      new RegExp(`AGENT_HEALTH_VERSION:\\s*["']${version}["']`),
      new RegExp(`AGENT_HEALTH_VERSION[^\\n]*:-${version}\\}`),
      new RegExp(`deployed-version:\\s*\\$\\{AGENT_HEALTH_VERSION:${version}\\}`),
    ].some((pattern) => pattern.test(versionConfiguration));
    assert.ok(
      declaresExpectedVersion,
      `${agent.key}: ${versionSource} não declara a versão ${version} do contrato canônico`,
    );
  }

  const checksCodexAuthentication =
    /["']codex["']\s*,\s*\[\s*["']login["']\s*,\s*["']status["']/.test(reporter) ||
    /ProcessBuilder\(\s*"codex"\s*,\s*"login"\s*,\s*"status"\s*\)/.test(reporter) ||
    /codex\s+login\s+status/i.test(reporter);
  assert.ok(checksCodexAuthentication, `${agent.key}: autenticação Codex não é comprovada`);
  assert.match(reporter, /\/api\/internal\/agents\/executor-health/, `${agent.key}: endpoint canônico ausente`);
  assert.match(reporter, /codexAuthenticated/, `${agent.key}: estado da sessão não é reportado`);

  if (agent.reporter.endsWith(".mjs")) {
    assert.match(activation, /agent-health-report\.mjs/, `${agent.key}: reporter não ativado`);
    assert.match(activation, /interval:\s*(?:[1-5]?[0-9]s|1m)/, `${agent.key}: heartbeat deve ocorrer em até 60 segundos`);
  } else if (agent.reporter.endsWith(".java")) {
    assert.match(reporter, /@Scheduled\(cron\s*=\s*"[^"]+"\)/, `${agent.key}: agendamento periódico ausente`);
  } else {
    assert.match(activation, /startAgentHealthReporter/, `${agent.key}: reporter não iniciado pelo worker`);
    assert.match(reporter, /AGENT_HEALTH_INTERVAL_MS[^\n]+60000/, `${agent.key}: intervalo padrão deve ser no máximo 60 segundos`);
  }
}

console.log(`[ARQUITETURA] Health-check periódico validado para ${contract.agents.length} agentes Codex.`);
