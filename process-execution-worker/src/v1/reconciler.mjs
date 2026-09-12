const route = "/api/internal/business-processes/automation/v1/stage-executions";

/** Consulta pendências e solicita conciliação; somente o backend decide qual atividade executar. */
export class ProcessReconciler {
  constructor({
    baseUrl,
    token,
    concurrency = 4,
    batchSize = 20,
    timeoutMs = 45000,
    fetchImpl = fetch,
    log = console,
    heartbeat = async () => {},
  }) {
    const url = new URL(baseUrl);
    if (
      !["http:", "https:"].includes(url.protocol) ||
      url.username ||
      url.password
    )
      throw new Error("URL do backend inválida.");
    if (!token) throw new Error("PROCESS_EXECUTION_WORKER_TOKEN obrigatório.");
    for (const n of [concurrency, batchSize, timeoutMs])
      if (!Number.isSafeInteger(n) || n < 1)
        throw new Error("Configuração numérica inválida.");
    this.baseUrl = url.origin;
    this.token = token;
    this.concurrency = Math.min(concurrency, 16);
    this.batchSize = Math.min(batchSize, 100);
    this.timeoutMs = timeoutMs;
    this.fetch = fetchImpl;
    this.log = log;
    this.heartbeat = heartbeat;
  }

  /** Registra somente correlação e status, sem conteúdo de produto nem credenciais. */
  async request(path, method = "GET") {
    const response = await this.fetch(this.baseUrl + route + path, {
      method,
      headers: { "X-Process-Worker-Token": this.token },
      signal: AbortSignal.timeout(this.timeoutMs),
      redirect: "error",
    });
    if (!response.ok) {
      const error = new Error(`Backend respondeu HTTP ${response.status}`);
      error.httpStatus = response.status;
      throw error;
    }
    return response.json();
  }

  /** Processa lote limitado sem permitir que falha de um produto interrompa os demais. */
  async tick() {
    const ids = await this.request(`/pending?limit=${this.batchSize}`);
    if (
      !Array.isArray(ids) ||
      ids.length > this.batchSize ||
      ids.some((id) => !Number.isSafeInteger(id) || id < 1)
    )
      throw new Error("Contrato pending inválido.");
    // Confirma disponibilidade antes do lote, sem tratar trabalho demorado como processo parado.
    await this.heartbeat();
    const queue = [...new Set(ids)];
    const results = [];
    await Promise.all(
      Array.from(
        { length: Math.min(this.concurrency, queue.length) },
        async () => {
          while (queue.length) {
            const runId = queue.shift();
            try {
              const result = await this.request(`/${runId}/reconcile`, "POST");
              if (result?.id !== runId || typeof result.status !== "string")
                throw new Error("Contrato de conciliação inválido.");
              await this.heartbeat();
              this.log.info(
                JSON.stringify({
                  module: "process-execution-worker",
                  operation: "reconcile",
                  runId,
                  status: result.status,
                }),
              );
              results.push({ runId, ok: true });
            } catch (error) {
              this.log.error(
                JSON.stringify({
                  module: "process-execution-worker",
                  operation: "reconcile",
                  runId,
                  error: error.name,
                  httpStatus: error.httpStatus,
                }),
              );
              results.push({ runId, ok: false });
            }
          }
        },
      ),
    );
    return results;
  }
}
