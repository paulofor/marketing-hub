import { setTimeout as delay } from "node:timers/promises";
import { writeFile, readFile } from "node:fs/promises";
import { ProcessReconciler } from "./v1/reconciler.mjs";

const worker = new ProcessReconciler({
  baseUrl: process.env.BACKEND_BASE_URL,
  token:
    process.env.PROCESS_EXECUTION_WORKER_TOKEN ||
    (process.env.PROCESS_EXECUTION_WORKER_TOKEN_FILE
      ? (
          await readFile(
            process.env.PROCESS_EXECUTION_WORKER_TOKEN_FILE,
            "utf8",
          )
        ).trim()
      : undefined),
  concurrency: Number(process.env.PROCESS_EXECUTION_CONCURRENCY || 4),
  batchSize: Number(process.env.PROCESS_EXECUTION_BATCH_SIZE || 20),
  heartbeat: () =>
    writeFile("/tmp/process-execution-heartbeat", String(Date.now())),
});
const intervalMs = Number(process.env.PROCESS_EXECUTION_POLL_MS || 5000);
if (!Number.isSafeInteger(intervalMs) || intervalMs < 100)
  throw new Error("Intervalo de conciliação inválido.");
let stopped = false;
process.on("SIGTERM", () => {
  stopped = true;
});
process.on("SIGINT", () => {
  stopped = true;
});

// A parada preserva requisições em curso; nenhum sinal local cancela tarefa do produto.
while (!stopped) {
  try {
    await worker.tick();
  } catch (error) {
    console.error(
      JSON.stringify({
        module: "process-execution-worker",
        operation: "pending",
        error: error.name,
        httpStatus: error.httpStatus,
      }),
    );
  }
  if (!stopped) await delay(intervalMs);
}
