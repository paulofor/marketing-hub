import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { runRetention, runRetentionWithRetry } from '../src/worker.js';

describe('executor de retenção PDE', () => {
  it('autentica, correlaciona e preserva a resposta funcional auditável', async () => {
    const calls = [];
    const logs = [];
    const result = await runRetention({
      backendUrl: 'http://backend.test/',
      internalToken: 'segredo-local',
      correlationId: 'retention-job-1',
      logger: { info: (message) => logs.push(JSON.parse(message)) },
      fetchImpl: async (url, options) => {
        calls.push({ url, options });
        return new Response(JSON.stringify({ anonymizedAccesses: 3, executedAt: '2026-08-23T12:00:00Z' }), { status: 200 });
      },
    });

    assert.deepEqual(result, { anonymizedAccesses: 3, executedAt: '2026-08-23T12:00:00Z' });
    assert.equal(calls[0].url, 'http://backend.test/api/internal/pde/privacy/retention');
    assert.equal(calls[0].options.headers['X-PDE-Internal-Token'], 'segredo-local');
    assert.equal(calls[0].options.headers['X-Correlation-Id'], 'retention-job-1');
    assert.deepEqual(logs.map((entry) => entry.operation), ['retention-request', 'retention-response']);
    assert.equal(logs[1].rawResponse, '{"anonymizedAccesses":3,"executedAt":"2026-08-23T12:00:00Z"}');
  });

  it('não transforma HTTP ou contrato inválido em sucesso', async () => {
    await assert.rejects(
      runRetention({
        backendUrl: 'http://backend.test',
        internalToken: 'segredo-local',
        logger: { info() {} },
        fetchImpl: async () => new Response('{"error":"indisponível"}', { status: 503 }),
      }),
      /HTTP 503/,
    );
    await assert.rejects(
      runRetention({
        backendUrl: 'http://backend.test',
        internalToken: 'segredo-local',
        logger: { info() {} },
        fetchImpl: async () => new Response('{"status":"ok"}', { status: 200 }),
      }),
      /fora do contrato/,
    );
  });

  it('bloqueia execução sem segredo interno', async () => {
    await assert.rejects(
      runRetention({ backendUrl: 'http://backend.test', internalToken: '' }),
      /obrigatórios/,
    );
  });

  it('recupera falha transitória antes de aguardar o próximo ciclo diário', async () => {
    let attempts = 0;
    const waits = [];
    const warnings = [];
    const result = await runRetentionWithRetry({
      backendUrl: 'http://backend.test',
      internalToken: 'segredo-local',
      maxAttempts: 3,
      retryDelayMs: 50,
      sleepImpl: async (delayMs) => waits.push(delayMs),
      logger: { info() {}, warn: (message) => warnings.push(JSON.parse(message)) },
      fetchImpl: async () => {
        attempts += 1;
        if (attempts === 1) {
          throw new TypeError('connect ECONNREFUSED');
        }
        return new Response(JSON.stringify({ anonymizedAccesses: 0, executedAt: '2026-08-23T12:00:00Z' }), { status: 200 });
      },
    });

    assert.equal(attempts, 2);
    assert.deepEqual(waits, [50]);
    assert.equal(warnings[0].operation, 'retention-retry');
    assert.equal(result.anonymizedAccesses, 0);
  });
});
