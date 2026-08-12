import { spawnSync } from "node:child_process";

const backend = (process.env.BACKEND_URL || "").replace(/\/$/, "");
const auth = spawnSync("codex", ["login", "status"], { stdio: "ignore", timeout: 15000 });
const response = await fetch(`${backend}/api/internal/agents/executor-health`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({
    agentKey: process.env.AGENT_HEALTH_KEY,
    deployedVersion: Number(process.env.AGENT_HEALTH_VERSION),
    buildReference: process.env.AGENT_BUILD_REFERENCE || null,
    backendAccessible: true,
    codexAuthenticated: auth.status === 0,
    detail: auth.status === 0 ? "Executor pronto." : "Reconecte a sessão Codex compartilhada.",
  }),
});
if (!response.ok || auth.status !== 0) process.exit(1);
