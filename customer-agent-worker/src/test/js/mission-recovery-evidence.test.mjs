import assert from 'node:assert/strict';
import test from 'node:test';
import { missionRecoveryEvidence } from '../../main/resources/browser/mission-recovery-evidence.mjs';

/** Monta estados separados como os recibos de orientação, missão e acesso persistidos. */
function fixture(productId = 4, version = 'musa-pde-entry-v17-continuidade-clara') {
  const expected = { id: `session-${productId}`, productId, cycleId: productId + 14,
    experimentId: productId + 107, prototypeVersion: version };
  const requestId = `operation-${productId}`, missionId = 'estilo-dia-1';
  const reflection = version.includes('v17-') ? '' : 'Registro sintético.';
  const report = { ...expected, operations: { [requestId]: { missionId, providerCalls: 0,
    response: { status: 'COMPLETED', requestId } } },
    missions: { [missionId]: { latestRequestId: requestId, answers: { reflection } } },
    events: { EXPERIENCE_STARTED: { requestId: '' } } };
  const workspace = { experienceVersion: version, completedMissionIds: [], progressPercent: 0 };
  const beforeCompletion = { report, workspace };
  const afterCompletion = structuredClone(beforeCompletion);
  afterCompletion.report.missions[missionId].completed = true;
  afterCompletion.workspace.completedMissionIds = [missionId];
  afterCompletion.workspace.progressPercent = 14;
  return { expected, requestId, missionId, reflection, snapshots: {
    beforeCompletion, afterCompletion,
    afterReopen: structuredClone(afterCompletion), afterRepeat: structuredClone(afterCompletion),
  } };
}

function evaluate(f) {
  return missionRecoveryEvidence(f.snapshots, f.expected, f.missionId, f.requestId, f.reflection);
}

for (const productId of [4, 91007]) {
  test(`preserva conclusão vazia, identidade e operação do produto ${productId}`, () => {
    const f = fixture(productId), evidence = evaluate(f);
    assert.equal(evidence.missionCompleted, true);
    assert.equal(evidence.emptyReflection, true);
    assert.equal(evidence.observations.beforeCompletion.progressPercent, 0);
    assert.equal(evidence.observations.afterReopen.mission.answers.reflection, '');
    assert.equal(evidence.observations.afterRepeat.operationCount, 1);
    assert.deepEqual(evidence.observations.afterRepeat.completedMissionIds, [f.missionId]);
  });
}

test('preserva a conclusão com comentário da versão histórica', () => {
  const evidence = evaluate(fixture(91008, 'musa-pde-entry-v16-modelagem-internacional'));
  assert.equal(evidence.emptyReflection, false);
  assert.equal(evidence.observations.afterRepeat.mission.answers.reflection, 'Registro sintético.');
});

for (const [name, mutate] of [
  ['orientação COMPLETED sem conclusão da missão, como no recibo 861', f => {
    for (const phase of ['afterCompletion', 'afterReopen', 'afterRepeat'])
      f.snapshots[phase] = structuredClone(f.snapshots.beforeCompletion);
  }],
  ['conclusão perdida ao reabrir', f => { f.snapshots.afterReopen.report.missions[f.missionId].completed = false; }],
  ['progresso duplicado no reenvio', f => { f.snapshots.afterRepeat.workspace.completedMissionIds.push(f.missionId); }],
  ['progresso divergente da missão', f => { f.snapshots.afterRepeat.workspace.progressPercent = 0; }],
  ['nova orientação na recuperação', f => { f.snapshots.afterRepeat.report.operations.extra = { providerCalls: 0 }; }],
  ['outra sessão', f => { f.snapshots.afterReopen.report.id = 'foreign-session'; }],
  ['outro produto', f => { f.snapshots.afterReopen.report.productId++; }],
  ['outra versão', f => { f.snapshots.afterReopen.report.prototypeVersion = 'foreign-version'; }],
  ['outra operação', f => { f.snapshots.afterReopen.report.missions[f.missionId].latestRequestId = 'foreign-operation'; }],
  ['comentário fabricado', f => { f.snapshots.afterReopen.report.missions[f.missionId].answers.reflection = 'Inventado'; }],
  ['evento de aplicação fabricado', f => { f.snapshots.afterRepeat.report.events.READY_RESULT_USED = {}; }],
]) {
  test(`recusa ${name}`, () => {
    const f = fixture(); mutate(f);
    assert.throws(() => evaluate(f));
  });
}
