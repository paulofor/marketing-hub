import { spawnSync } from "node:child_process";

export function createAgentHealthReporter({
  backendBaseUrl = process.env.BACKEND_BASE_URL || "http://191.252.181.168",
  agentKey = process.env.AGENT_HEALTH_KEY || "market-radar",
  deployedVersion = Number(process.env.AGENT_HEALTH_VERSION || "1"),
  buildReference = process.env.AGENT_BUILD_REFERENCE || null,
  fetchFn = fetch,
  spawnSyncFn = spawnSync,
  logger = console,
} = {}) {
  const backend = backendBaseUrl.replace(/\/$/, "");

  async function report() {
    const auth = spawnSyncFn("codex", ["login", "status"], {
      stdio: "ignore",
      timeout: 15000,
    });
    const codexAuthenticated = auth.status === 0;
    try {
      const response = await fetchFn(`${backend}/api/internal/agents/executor-health`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          agentKey,
          deployedVersion,
          buildReference,
          backendAccessible: true,
          codexAuthenticated,
          detail: codexAuthenticated
            ? "Executor Argos pronto."
            : "Reconecte a sessão Codex individual de Argos.",
        }),
      });
      if (!response.ok) throw new Error(`backend respondeu HTTP ${response.status}`);
      return await response.json();
    } catch (error) {
      logger.error("[product-discovery-worker] agent health report failed", error);
      return null;
    }
  }

  return { report };
}

export function startAgentHealthReporter(options = {}) {
  const reporter = createAgentHealthReporter(options);
  const intervalMs = Number(process.env.AGENT_HEALTH_INTERVAL_MS || "60000");
  void reporter.report();
  return setInterval(() => void reporter.report(), intervalMs);
}
