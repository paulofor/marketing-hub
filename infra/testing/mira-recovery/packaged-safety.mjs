import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import { randomUUID } from "node:crypto";

// Executado por stdin dentro da imagem Psique, somente contra a topologia local.
await writeFile("/tmp/mira-input.json", JSON.stringify({ mode: "TECHNICAL",
  captureSessionId: randomUUID(), sourceReference: "product:10@agent-validation-v1",
  productId: 10, productSlug: "pde-planejado-36", sourceUrl: "http://mira-ui/mira-private" }));
const child = spawnSync(process.execPath, ["/app/browser/pde-agent-validation-harness.mjs",
  "/tmp/mira-input.json", "/tmp/mira-output.json", "/tmp/mira-evidence"],
  { encoding: "utf8", timeout: 90000 });
assert.equal(child.status, 0, child.stderr);
const result = JSON.parse(await readFile("/tmp/mira-output.json", "utf8"));
assert.equal(result.decision, "APPROVED");
assert.equal(result.prototypeVersion, "mira-private-v3");
assert.equal(result.scenarios.length, 5);
assert.equal(result.devices.length, 3);
assert.equal(Object.values(result.checks).every(Boolean), true);
for (const scenario of result.scenarios) {
  assert.equal(scenario.trafficClass, "AGENT_VALIDATION");
  assert.equal(scenario.humanEvidenceClaimed, false);
  assert.equal(scenario.commercialEvidenceClaimed, false);
}
console.log(JSON.stringify({ decision: result.decision, prototypeVersion: result.prototypeVersion,
  scenarios: result.scenarios.length, devices: result.devices.length, checks: result.checks }));
