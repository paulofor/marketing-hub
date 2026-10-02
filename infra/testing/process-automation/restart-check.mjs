import assert from "node:assert/strict";
const base = "http://127.0.0.1:18092";
const url = (p) =>
  `${base}/api/business-processes/92001/products/${p}/automation/v1?chainId=92014&learningCycleId=${p}&sourceReference=experiment:${p}`;
const complete = await (await fetch(url(92001))).json();
assert.equal(complete.status, "COMPLETED");
assert.equal(complete.completedActivities, 3);
const active = await (await fetch(url(92003))).json();
const before = await (await fetch(base + "/fixture/tasks")).json();
const response = await fetch(
  base +
    `/api/internal/business-processes/automation/v1/stage-executions/${active.id}/reconcile`,
  {
    method: "POST",
    headers: { "X-Process-Worker-Token": "process-fixture-only" },
  },
);
assert.equal(response.status, 200);
const after = await (await fetch(base + "/fixture/tasks")).json();
assert.equal(after.length, before.length);
const events = await (
  await fetch(
    `${base}/api/business-processes/92001/products/92001/automation/v1/${complete.id}/events`,
  )
).json();
assert(events.some((e) => e.eventType === "COMPLETED"));
const retired = await (
  await fetch(
    `${base}/api/business-processes/92002/products/92031/automation/v1?chainId=92014&learningCycleId=92031&sourceReference=experiment:92031`,
  )
).json();
assert.equal(retired.status, "CLOSED");
assert.equal(retired.completedActivities, 1);
assert.equal(retired.canResume, false);
const pending = await (
  await fetch(
    base +
      "/api/internal/business-processes/automation/v1/stage-executions/pending?limit=100",
    { headers: { "X-Process-Worker-Token": "process-fixture-only" } },
  )
).json();
assert(!pending.includes(retired.id));
console.log(
  "PASS reinício preserva conclusões, encerramento de versões retiradas, tarefas ativas, histórico e deduplicação",
);
