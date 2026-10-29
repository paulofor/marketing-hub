import assert from 'node:assert/strict';

/** Confere conclusão, reabertura e repetição na mesma missão, separadas da geração da orientação. */
export function missionRecoveryEvidence(snapshots, expected, missionId, requestId, reflection) {
  const phases = ['beforeCompletion', 'afterCompletion', 'afterReopen', 'afterRepeat'];
  const baseline = snapshots.beforeCompletion;
  const completedBefore = baseline.workspace.completedMissionIds;
  assert.ok(Array.isArray(completedBefore) && !completedBefore.includes(missionId),
    'A prova precisa começar antes da conclusão da missão.');
  assert.notEqual(baseline.report.missions[missionId]?.completed, true);
  assert.equal(baseline.report.operations[requestId]?.response.status, 'COMPLETED');
  assert.equal(baseline.report.operations[requestId]?.missionId, missionId);
  const completedAfter = [...completedBefore, missionId].sort();
  const observations = {};
  for (const phase of phases) {
    const { report, workspace } = snapshots[phase];
    for (const [key, value] of Object.entries(expected))
      assert.equal(report[key], value, `A identidade ${key} mudou durante ${phase}.`);
    assert.equal(workspace.experienceVersion, expected.prototypeVersion);
    assert.equal(report.missions[missionId]?.latestRequestId, requestId);
    assert.deepEqual(report.operations, baseline.report.operations,
      'Concluir ou recuperar não pode gerar outra orientação nem alterar a operação original.');
    assert.deepEqual(report.events, baseline.report.events,
      'Conclusão não pode fabricar preferência, aplicação ou evento comercial.');
    if (phase !== 'beforeCompletion') {
      assert.equal(report.missions[missionId].completed, true,
        `A missão não permaneceu concluída em ${phase}.`);
      assert.equal(report.missions[missionId].answers.reflection, reflection);
      assert.deepEqual([...workspace.completedMissionIds].sort(), completedAfter,
        'A conclusão precisa aparecer uma única vez no acesso recuperado.');
      assert.equal(workspace.progressPercent, Math.floor(completedAfter.length * 100 / 7));
      assert.deepEqual(report.missions, snapshots.afterCompletion.report.missions,
        'Reabrir e repetir precisam preservar escolhas, comentário e conclusão.');
    }
    observations[phase] = {
      evidenceId: report.id, mission: report.missions[missionId],
      completedMissionIds: workspace.completedMissionIds,
      progressPercent: workspace.progressPercent,
      operationCount: Object.keys(report.operations).length,
    };
  }
  return {
    contractVersion: 'MISSION_COMPLETION_RECOVERY_V1', missionId, requestId,
    guidanceStatus: 'COMPLETED', missionCompleted: true,
    emptyReflection: reflection === '', preservedAfterReopen: true,
    repeatedWithoutDuplicate: true, observations,
  };
}
