import assert from 'node:assert/strict';
import test from 'node:test';
import { fixtureResult, completeVegaFixture, fixtureModel } from '../../main/resources/browser/vega-deterministic-fixture.mjs';

const job = { id: 91001, sessionId: 'local-session', context: { origin: 'AGENT_VALIDATION', cycleId: 91007, productId: 91004, experimentId: 91100, prototypeVersion: 'local-version', input: { occasion: 'Almoço', existingSelection: 'Camisa branca', optionalNote: '' } } };
const input = { cycleId: 91007, productId: 91004, sourceReference: 'experiment:91100', prototypeVersion: 'local-version' };

test('a fixture conserva contexto e registra consumo zero sem provedor', () => {
  const result = fixtureResult(job);
  assert.equal(result.status, 'COMPLETED');
  assert.match(result.card.application, /Almoço.*Camisa branca/);
  assert.equal(result.card.cardId, String(job.id));
  assert.equal(result.costUsd, 0);
  assert.equal(result.model, fixtureModel);
  assert.equal(result.rawResponse.synthetic, true);
  assert.deepEqual(fixtureResult(job), result);
});

test('compras e avaliações de saúde são bloqueadas sem cartão ou consumo', () => {
  for (const optionalNote of ['Quero comprar outra roupa', 'Faça um diagnóstico']) {
    const result = fixtureResult({ ...job, context: { ...job.context, input: { ...job.context.input, optionalNote } } });
    assert.equal(result.status, 'BLOCKED');
    assert.equal(result.card, null);
    assert.equal(result.costUsd, 0);
  }
  assert.throws(() => fixtureResult({ ...job, context: { ...job.context, origin: 'HUMAN' } }));
});

test('parte de pending e audita request antes de aplicar o callback correlacionado', async () => {
  const calls = [];
  const api = async (path, body) => {
    calls.push({ path, body });
    if (path.includes('/pending')) return [job];
    if (path.endsWith('/claim')) return { ...job, status: 'RUNNING' };
    if (path.endsWith('/request')) return null;
    return fixtureResult(job);
  };
  await completeVegaFixture(api, input, { id: job.sessionId }, job.id);
  assert.deepEqual(calls.map(c => c.path.split('/').at(-1)), ['pending?mode=FIXTURE', 'claim', 'request', 'result']);
  assert.equal(calls[2].body.request.provider, 'DETERMINISTIC_FIXTURE');
});

test('não consome tarefa de outro ciclo ou sessão', async () => {
  for (const foreignJob of [
    { ...job, sessionId: 'other-session' },
    { ...job, context: { ...job.context, cycleId: 91008 } },
    { ...job, context: { ...job.context, experimentId: 91101 } },
  ]) {
    const calls = [];
    const api = async path => { calls.push(path); return [foreignJob]; };
    await assert.rejects(completeVegaFixture(api, input, { id: job.sessionId }, job.id));
    assert.equal(calls.length, 1);
  }
});
