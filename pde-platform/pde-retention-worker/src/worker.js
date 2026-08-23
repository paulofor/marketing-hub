import { randomUUID } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const DEFAULT_INTERVAL_MS = 24 * 60 * 60 * 1000;
const DEFAULT_RETRY_ATTEMPTS = 12;
const DEFAULT_RETRY_DELAY_MS = 5_000;

/** Aciona a política canônica e valida a resposta auditável do backend. */
export async function runRetention({ backendUrl, internalToken, fetchImpl = fetch, logger = console, correlationId = randomUUID() }) {
  const baseUrl = String(backendUrl ?? '').replace(/\/+$/, '');
  const token = String(internalToken ?? '').trim();
  if (!baseUrl || !token) {
    throw new Error('PDE_BACKEND_URL e PDE_INTERNAL_API_TOKEN são obrigatórios no executor de retenção');
  }
  const url = `${baseUrl}/api/internal/pde/privacy/retention`;
  logger.info(JSON.stringify({ module: 'pde-retention-worker', operation: 'retention-request', correlationId, url }));
  const response = await fetchImpl(url, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'X-PDE-Internal-Token': token,
      'X-Correlation-Id': correlationId,
    },
  });
  const rawResponse = await response.text();
  logger.info(JSON.stringify({
    module: 'pde-retention-worker',
    operation: 'retention-response',
    correlationId,
    url,
    status: response.status,
    rawResponse,
  }));
  if (!response.ok) {
    throw new Error(`Retenção PDE recusada com HTTP ${response.status}`);
  }
  const result = JSON.parse(rawResponse);
  if (!Number.isInteger(result.anonymizedAccesses) || !result.executedAt) {
    throw new Error('Resposta de retenção PDE fora do contrato auditável');
  }
  return result;
}

/** Repete falhas transitórias sem adiar a política de retenção para o dia seguinte. */
export async function runRetentionWithRetry(config) {
  const maxAttempts = Number(config.maxAttempts ?? DEFAULT_RETRY_ATTEMPTS);
  const retryDelayMs = Number(config.retryDelayMs ?? DEFAULT_RETRY_DELAY_MS);
  if (!Number.isInteger(maxAttempts) || maxAttempts < 1 || !Number.isFinite(retryDelayMs) || retryDelayMs < 0) {
    throw new Error('Configuração de retry da retenção PDE inválida');
  }
  const sleepImpl = config.sleepImpl ?? ((delayMs) => new Promise((resolve) => setTimeout(resolve, delayMs)));
  let lastError;
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await runRetention(config);
    } catch (error) {
      lastError = error;
      config.logger?.warn?.(JSON.stringify({
        module: 'pde-retention-worker',
        operation: 'retention-retry',
        attempt,
        maxAttempts,
        error: error instanceof Error ? error.message : String(error),
      }));
      if (attempt < maxAttempts) {
        await sleepImpl(retryDelayMs);
      }
    }
  }
  throw lastError;
}

/** Mantém a rotina no módulo executor e repete o ciclo somente após concluir a execução anterior. */
export async function startRetentionLoop(config) {
  const intervalMs = Number(config.intervalMs ?? DEFAULT_INTERVAL_MS);
  if (!Number.isFinite(intervalMs) || intervalMs < 60_000) {
    throw new Error('PDE_RETENTION_INTERVAL_MS deve ser de pelo menos 60000 ms');
  }
  while (true) {
    try {
      await runRetentionWithRetry(config);
    } catch (error) {
      config.logger?.error?.('Falha no ciclo de retenção PDE', error);
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }
}

/** Inicializa o executor com as configurações operacionais do ambiente. */
async function main() {
  const config = {
    backendUrl: process.env.PDE_BACKEND_URL ?? 'http://pde-platform-backend:8096',
    internalToken: process.env.PDE_INTERNAL_API_TOKEN ?? '',
    intervalMs: process.env.PDE_RETENTION_INTERVAL_MS ?? DEFAULT_INTERVAL_MS,
    maxAttempts: process.env.PDE_RETENTION_RETRY_ATTEMPTS ?? DEFAULT_RETRY_ATTEMPTS,
    retryDelayMs: process.env.PDE_RETENTION_RETRY_DELAY_MS ?? DEFAULT_RETRY_DELAY_MS,
    logger: console,
  };
  if (process.env.PDE_RETENTION_RUN_ONCE === 'true') {
    await runRetentionWithRetry(config);
    return;
  }
  await startRetentionLoop(config);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error('Executor de retenção PDE encerrado por falha de configuração', error);
    process.exitCode = 1;
  });
}
