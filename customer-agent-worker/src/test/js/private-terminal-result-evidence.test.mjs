import test from 'node:test';
import assert from 'node:assert/strict';
import { completeTerminalMatrix, validTerminalResult, terminalStates, terminalDevices } from '../../main/resources/browser/private-terminal-result-evidence.mjs';

const matrix = id => terminalDevices.flatMap(deviceProfile => terminalStates.map(generationStatus => ({
  evidenceId: `${id}-${deviceProfile}-${generationStatus}`, deviceProfile, generationStatus,
  persistedGenerationStatus: generationStatus, sessionState: 'FINISHED', reloadedSessionState: 'FINISHED',
  resultPresent: generationStatus === 'COMPLETED', renderedResultPresent: generationStatus === 'COMPLETED',
  reloadedResultPresent: generationStatus === 'COMPLETED', savedResultConfirmed: generationStatus === 'COMPLETED' ? true : null,
  terminalMessage: `Encerramento observado: ${generationStatus}`, reloadedMessage: `Encerramento observado: ${generationStatus}`,
  messageMatchesResult: true,
})));

for (const id of ['vega-91004-cycle-91002', 'other-product-97004-cycle-97002']) {
  test(`${id}: aceita prova completa e recusa o falso positivo de sessão ainda ativa`, () => {
    assert.equal(completeTerminalMatrix(matrix(id)), true);
    const cases = matrix(id);
    cases[0].sessionState = 'ACTIVE';
    assert.equal(completeTerminalMatrix(cases), false);
  });
}
test('as cinco jornadas antigas não demonstram os quatro encerramentos nos três dispositivos', () => {
  assert.equal(completeTerminalMatrix(matrix('legacy').slice(0, 5)), false);
  const duplicate = matrix('duplicate');
  duplicate[11] = duplicate[0];
  assert.equal(completeTerminalMatrix(duplicate), false);
});
test('recusa resultado inventado, falha sem mensagem, salvamento e retomada não comprovados', () => {
  const original = matrix('original');
  for (const [index, patch] of [[1, {resultPresent: true}], [2, {terminalMessage: ''}], [0, {savedResultConfirmed: false}], [3, {reloadedMessage: 'Outro encerramento'}], [2, {persistedGenerationStatus: 'RUNNING'}]]) {
    assert.equal(validTerminalResult({...original[index], ...patch}), false);
  }
});
