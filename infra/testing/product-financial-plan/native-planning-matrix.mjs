import assert from "node:assert/strict";

const base = "http://127.0.0.1:18095";
const queue =
  "/api/internal/agent-tasks/experiment-strategist/stage-executions";
let checks = 0;
async function request(path, method = "GET", body) {
  const response = await fetch(base + path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const raw = await response.text();
  assert.equal(response.status, 200, `${path}: ${raw}`);
  checks++;
  return raw ? JSON.parse(raw) : null;
}

for (const id of [95121, 95122]) {
  const revisions = await request(`/api/financial-plans/v1/products/${id}`);
  const current = revisions[0];
  assert.equal(current.stale, false);
  for (let revision = 0; revision < 2; revision++) {
    await request(
      `/api/financial-plans/v1/products/${id}?environment=TEST`,
      "POST",
      {
        name: "Revisão TEST mais recente",
        createdBy: "Fixture local",
        expectedRevision: revision,
        commercialPlanId: id,
        assumptions: current.assumptions,
      },
    );
  }
  await request(`/fixture/native-planning/${id}`, "POST");
  const pending = await request(
    `${queue}/pending?processCode=pde-commercial-plan-offer&activityId=marketStrategy`,
  );
  assert.equal(pending.length, 1);
  const task = pending[0];
  assert.equal(task.sourceReference, `experiment:${id}`);
  assert.equal(task.taskTarget.productId, id);
  const context = task.taskTarget.pdeContext;
  assert.equal(context.currentFinancialPlan.id, current.id);
  assert.equal(context.currentFinancialPlan.scopeId, id);
  assert.equal(context.currentFinancialPlan.environment, "LIVE");
  assert.equal(context.currentFinancialPlan.analysis.costUsd, null);
  assert.equal(
    context.currentFinancialPlan.analysis.costCoverage,
    "NOT_REPORTED",
  );
  assert.equal(
    context.financialEvidenceScope,
    "PROJECTION_NOT_COMMERCIAL_APPROVAL",
  );
  assert.equal(context.publicationAuthorized, false);
  assert.equal(context.mediaSpendAuthorized, false);
  assert.ok(task.receivedAt);
  assert.deepEqual(
    await request(
      `${queue}/pending?processCode=pde-commercial-plan-offer&activityId=marketStrategy`,
    ),
    [],
  );
  const recovered = await request(`${queue}/${task.taskId}`);
  assert.equal(recovered.taskId, task.taskId);
  assert.equal(recovered.receivedAt, task.receivedAt);
  assert.equal(
    recovered.taskTarget.pdeContext.currentFinancialPlan.id,
    current.id,
  );
  await request(`/fixture/plans/${id}/version/2`, "POST");
  const stale = await request(`${queue}/${task.taskId}`);
  assert.equal(stale.taskTarget.pdeContext.currentFinancialPlan, undefined);
  await request(`/fixture/plans/${id}/version/1`, "POST");
  assert.equal(
    (await request(`${queue}/${task.taskId}`)).taskTarget.pdeContext
      .currentFinancialPlan.id,
    current.id,
  );
}
console.log(JSON.stringify({ result: "PASS", checks, modelsInvoked: 0 }));
