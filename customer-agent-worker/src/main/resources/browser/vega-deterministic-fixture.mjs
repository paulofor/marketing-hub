import assert from 'node:assert/strict';

export const fixtureModel = 'vega-deterministic-fixture-v1';

// Materializa somente a resposta sintética do próprio cenário, sem inferência ou efeito comercial.
export function fixtureResult(job) {
  const {input, origin} = job.context;
  assert.ok(['AGENT_VALIDATION', 'QA_INTERNAL'].includes(origin));
  const unsafe = /\b(comprar|compre|adquirir|emagrecer|emagreça|diagn[oó]stico)\b/iu.test(Object.values(input).join(' '));
  const error = unsafe ? 'Este ajuste usa apenas a roupa que você já possui. Não recomenda compras, mudanças no corpo ou avaliações de saúde. Você pode reformular com a ocasião e a combinação disponível.' : null;
  const card = unsafe ? null : {
    cardId: String(job.id),
    action: 'Alinhe o caimento da peça que já está usando, sem apertar.',
    application: `Para ${input.occasion}, use ${input.existingSelection}. Diante do espelho, alinhe o tecido na cintura e confira se ombros e braços se movem com conforto. Mantenha as mesmas peças.`,
    occasion: input.occasion,
    selfAssessmentPrompt: 'Ao se mover, o tecido ficou confortável e o acabamento corresponde à ocasião escolhida?',
    usesOnlyAvailableItems: true,
  };
  return {status: unsafe ? 'BLOCKED' : 'COMPLETED', card, rawResponse: {fixtureVersion: fixtureModel, synthetic: true, card, boundary: error}, model: fixtureModel, inputTokens: 0, outputTokens: 0, costUsd: 0, error};
}

// Inicia pela fila oficial e conclui apenas a execução correlacionada ao cenário desta sessão.
export async function completeVegaFixture(api, input, session, executionId) {
  const path = '/internal/adjustment/stage-executions';
  const pending = await api(path + '/pending?mode=FIXTURE');
  const queued = pending.find(job => job.id === executionId && job.sessionId === session.id);
  assert.ok(queued, 'A fixture precisa partir da pendência oficial da própria sessão');
  assert.equal(queued.context.cycleId, input.cycleId);
  assert.equal(queued.context.productId, input.productId);
  assert.equal(queued.context.experimentId, Number(input.sourceReference.split(':')[1]));
  assert.equal(queued.context.prototypeVersion, input.prototypeVersion);
  const job = await api(`${path}/${executionId}/claim`, {});
  assert.equal(job.status, 'RUNNING');
  const request = {provider: 'DETERMINISTIC_FIXTURE', fixtureVersion: fixtureModel, context: job.context};
  await api(`${path}/${executionId}/request`, {request, model: fixtureModel});
  const result = fixtureResult(job);
  const completed = await api(`${path}/${executionId}/result`, result);
  assert.equal(completed.status, result.status);
  assert.equal(completed.costUsd, 0);
  return completed;
}
