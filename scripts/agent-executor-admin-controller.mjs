import { spawn } from "node:child_process";

const backend = (process.env.BACKEND_URL || "http://191.252.181.168").replace(/\/$/, "");
const repository = process.env.MARKETING_HUB_REPOSITORY || "/opt/marketing-hub/repo";
const once = process.argv.includes("--once");
const allowed = new Map([
  ["customer-agent", "customer-agent-worker"],
  ["financial-agent", "financial-agent-worker"],
  ["growth-operator", "growth-operator-worker"],
  ["experiment-strategist", "experiment-strategist-worker"],
  ["meta-ad-approver", "meta-ad-approver-worker"],
  ["landing-generator", "landing-generator-agent-worker"],
]);

function execute(command, args, cwd) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { cwd, stdio: ["ignore", "pipe", "pipe"] });
    let detail = "";
    child.stdout.on("data", (chunk) => (detail += chunk));
    child.stderr.on("data", (chunk) => (detail += chunk));
    child.on("error", (error) => resolve({ success: false, detail: error.message }));
    child.on("close", (code) =>
      resolve({ success: code === 0, detail: detail.trim().slice(-450) || `docker exit ${code}` }),
    );
  });
}

async function poll() {
  const response = await fetch(`${backend}/api/internal/agents/executor-health/admin-operations/pending`);
  if (response.status === 204) return;
  if (!response.ok) throw new Error(`Backend respondeu ${response.status}.`);
  const operation = await response.json();
  const module = allowed.get(operation.agentKey);
  let result;
  if (!module) {
    result = { success: false, detail: "Executor fora da lista administrativa permitida." };
  } else {
    const args = ["compose", "up", "-d", "--no-deps"];
    if (operation.operationType === "UPDATE") args.push("--build");
    args.push("--force-recreate", `${module}`);
    result = await execute("docker", args, `${repository}/${module}`);
  }
  await fetch(
    `${backend}/api/internal/agents/executor-health/admin-operations/${operation.id}/completion`,
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(result),
    },
  );
}

do {
  try {
    await poll();
  } catch (error) {
    console.error(`Falha no controlador administrativo: ${error.message}`);
  }
  if (!once) await new Promise((resolve) => setTimeout(resolve, 5000));
} while (!once);
