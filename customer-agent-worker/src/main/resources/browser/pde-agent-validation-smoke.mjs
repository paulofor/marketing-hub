import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const sourceUrl = process.env.PDE_AGENT_VALIDATION_URL?.trim();
const internalToken = process.env.PDE_INTERNAL_API_TOKEN?.trim();
if (!sourceUrl || !internalToken) {
  throw new Error("Defina PDE_AGENT_VALIDATION_URL e PDE_INTERNAL_API_TOKEN para a rodada local.");
}

const directory = await fs.mkdtemp(path.join(os.tmpdir(), "pde-agent-validation-live-"));
const inputPath = path.join(directory, "input.json");
const outputPath = path.join(directory, "output.json");
const evidenceDirectory = path.join(directory, "evidence");
const script = path.join(path.dirname(fileURLToPath(import.meta.url)), "pde-agent-validation-harness.mjs");
const input = {
  mode: "TECHNICAL",
  scenarioCode: "",
  captureSessionId: randomUUID(),
  sourceUrl,
  sourceReference: "product:10@agent-validation-v1",
  productId: 10,
  productSlug: "orientacao-digital-rotina-pele-madura",
  prototypeVersion: "mira-private-v1",
};

try {
  await fs.writeFile(inputPath, JSON.stringify(input));
  const child = spawn(process.execPath, [script, inputPath, outputPath, evidenceDirectory], {
    env: process.env,
  });
  let log = "";
  child.stdout.on("data", (chunk) => {
    log += chunk.toString();
  });
  child.stderr.on("data", (chunk) => {
    log += chunk.toString();
  });
  const [code] = await once(child, "close");
  assert.equal(code, 0, log);
  const outputText = await fs.readFile(outputPath, "utf8");
  const output = JSON.parse(outputText);
  assert.equal(output.contractVersion, "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1");
  assert.equal(output.decision, "APPROVED");
  assert.equal(output.sourceReference, input.sourceReference);
  assert.equal(output.prototypeVersion, input.prototypeVersion);
  assert.equal(output.trafficClass, "AGENT_VALIDATION");
  assert.equal(output.internalMarker, "mh_internal_test");
  assert.equal(output.humanEvidenceClaimed, false);
  assert.equal(output.commercialEvidenceClaimed, false);
  assert.deepEqual(
    new Set(output.devices.filter((item) => item.status === "PASS").map((item) => item.deviceProfile)),
    new Set(["DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"]),
  );
  assert.deepEqual(
    new Set(output.scenarios.filter((item) => item.status === "PASS").map((item) => item.scenarioCode)),
    new Set(["ADHERENT", "RECOVERY", "SAFETY"]),
  );
  assert.equal(output.scenarios.length, 5);
  assert.equal(output.artifacts.length, 5);
  assert.equal(Object.values(output.checks).every(Boolean), true);
  assert.deepEqual(output.sideEffects, {
    paymentEnabled: false,
    published: false,
    campaignCreated: false,
    mediaSpendBrl: 0,
  });
  assert.equal(outputText.includes(internalToken), false);
  for (const artifact of output.artifacts) {
    const pixels = await fs.readFile(artifact.localPath);
    assert.deepEqual([...pixels.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
  }
  process.stdout.write(
    JSON.stringify({
      decision: output.decision,
      scenarios: output.scenarios.length,
      devices: output.devices.length,
      artifacts: output.artifacts.length,
      durationSeconds: output.durationSeconds,
    }) + "\n",
  );
} finally {
  await fs.rm(directory, { recursive: true, force: true });
}
