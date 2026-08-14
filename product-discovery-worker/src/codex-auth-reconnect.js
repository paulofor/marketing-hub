import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const agentKey = "market-radar";
const defaultScript = join(
  dirname(fileURLToPath(import.meta.url)),
  "..",
  "scripts",
  "codex-app-server-device-login.mjs",
);

/** Inicia o consumo isolado dos pedidos de reconexão da sessão Codex de Argos. */
export function startCodexAuthReconnectConsumer(options = {}) {
  const intervalMs = Number(
    options.intervalMs ||
      process.env.ARGOS_CODEX_AUTH_POLL_INTERVAL_MS ||
      15000,
  );
  const coordinator = createCodexAuthReconnectCoordinator(options);
  coordinator.poll();
  const timer = setInterval(() => coordinator.poll(), intervalMs);
  timer.unref?.();
  return { ...coordinator, stop: () => clearInterval(timer) };
}

/** Cria um consumidor testável que reserva no máximo uma reconexão por vez. */
export function createCodexAuthReconnectCoordinator(options = {}) {
  const backendBaseUrl = String(
    options.backendBaseUrl ||
      process.env.BACKEND_BASE_URL ||
      "http://191.252.181.168",
  ).replace(/\/$/, "");
  const fetchFn = options.fetchFn || fetch;
  const spawnFn = options.spawnFn || spawn;
  const logger = options.logger || console;
  let active = false;

  async function complete(id, detail) {
    try {
      await postJson(
        fetchFn,
        `${backendBaseUrl}/api/internal/agents/executor-health/codex-auth/reconnections/${id}/completion`,
        {
          authenticated: false,
          detail,
        },
      );
    } catch (error) {
      logger.error(
        `[product-discovery-worker] failed to report Argos Codex reconnect id=${id}`,
        error,
      );
    }
  }

  async function execute(job) {
    const script = options.script || defaultScript;
    const child = spawnFn(process.execPath, [script], {
      env: {
        ...process.env,
        CODEX_AUTH_RECONNECT_ID: String(job.id),
        CODEX_AUTH_CALLBACK_BASE_URL: backendBaseUrl,
      },
      stdio: ["ignore", "inherit", "pipe"],
    });
    const result = await waitForChild(child, logger);
    if (result.error) {
      logger.error(
        `[product-discovery-worker] failed to start Argos Codex reconnect id=${job.id}`,
        result.error,
      );
      await complete(job.id, "Não foi possível iniciar o Codex App Server.");
    } else if (result.code !== 0) {
      await complete(
        job.id,
        result.detail ||
          "Codex App Server encerrou sem confirmar a sessão de Argos.",
      );
    }
  }

  async function poll() {
    if (active) return false;
    active = true;
    try {
      const response = await fetchFn(
        `${backendBaseUrl}/api/internal/agents/executor-health/${agentKey}/codex-auth/reconnections/pending`,
        { headers: { Accept: "application/json" } },
      );
      if (response.status === 204) return false;
      if (!response.ok)
        throw new Error(`Backend respondeu HTTP ${response.status}`);
      const job = await response.json();
      if (!job?.id) return false;
      await execute(job);
      return true;
    } catch (error) {
      logger.error(
        "[product-discovery-worker] Argos Codex reconnect poll failed",
        error,
      );
      return false;
    } finally {
      active = false;
    }
  }

  return { poll };
}

/** Aguarda o processo de device code sem interpretar ou transportar credenciais. */
function waitForChild(child, logger) {
  return new Promise((resolve) => {
    let detail = "";
    child.stderr?.on?.("data", (chunk) => {
      const line = String(chunk).trim();
      if (!line) return;
      logger.error(
        `[product-discovery-worker] Argos Codex App Server: ${line}`,
      );
      detail = sanitizeDetail(line);
    });
    child.once("error", (error) => resolve({ code: -1, error }));
    child.once("exit", (code) => resolve({ code: code ?? 1, detail }));
  });
}

/** Reduz a mensagem operacional sem permitir que credenciais cheguem ao backend. */
function sanitizeDetail(value) {
  return String(value)
    .replace(/(?:sk-|sess-|eyJ)[A-Za-z0-9._-]+/g, "[SEGREDO_REMOVIDO]")
    .slice(-1000);
}

/** Envia somente estado operacional ao contrato canônico do backend. */
async function postJson(fetchFn, url, payload) {
  const response = await fetchFn(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!response.ok)
    throw new Error(`Backend respondeu HTTP ${response.status}`);
}
