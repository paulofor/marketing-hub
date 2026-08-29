import { createHash } from "node:crypto";
import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";

const [localDirectory, stagingDirectory] = process.argv.slice(2);
if (!localDirectory || !stagingDirectory) {
  throw new Error(
    "Uso: node prepare-approved-package.mjs <diretorio-local> <staging>",
  );
}

const local = resolve(localDirectory);
const stage = resolve(stagingDirectory);
const evidence = join(local, "evidence");
const artifacts = join(evidence, "artifacts");
const sha256 = async (file) =>
  createHash("sha256")
    .update(await readFile(file))
    .digest("hex");

await rm(stage, { recursive: true, force: true });
for (const directory of ["metadata", "assets", "audit"]) {
  await mkdir(join(stage, directory), { recursive: true });
}

const metadata = [
  [join(local, "rigel-creative-contract.v1.json"), "contract.json"],
  [join(artifacts, "rigel-creative-manifest.json"), "manifest.json"],
  [join(evidence, "temis-creative-direction.json"), "temis-direction.json"],
  [join(evidence, "apollo-storyboard.json"), "apollo-storyboard.json"],
  [join(evidence, "psique-review.json"), "psique-review.json"],
  [join(evidence, "temis-independent-review.json"), "temis-review.json"],
  [
    join(evidence, "technical-verification.json"),
    "technical-verification.json",
  ],
];
for (const [source, destination] of metadata) {
  await cp(source, join(stage, "metadata", destination));
}

await cp(join(evidence, "proof"), join(stage, "proof"), { recursive: true });
const manifest = JSON.parse(
  await readFile(join(artifacts, "rigel-creative-manifest.json"), "utf8"),
);
for (const asset of manifest.assets) {
  await cp(join(artifacts, asset.file), join(stage, "assets", asset.file));
}
await cp(join(artifacts, "review-frames"), join(stage, "review-frames"), {
  recursive: true,
});
await cp(join(artifacts, "channel-previews"), join(stage, "channel-previews"), {
  recursive: true,
});

const executions = JSON.parse(
  await readFile(join(evidence, "agent-executions.json"), "utf8"),
);
const requiredAgents = [
  "TEMIS_DIRECTION",
  "APOLLO",
  "PSIQUE",
  "TEMIS_INDEPENDENT",
];
const selectedExecutions = requiredAgents.map((agent) => {
  const execution = executions.findLast(
    (candidate) => candidate.agent === agent && candidate.exitCode === 0,
  );
  if (!execution) {
    throw new Error(`Não existe execução aprovada e auditável de ${agent}.`);
  }
  return execution;
});
const normalizedExecutions = [];
for (const execution of selectedExecutions) {
  const auditRoot = `audit/${execution.executionId}`;
  await mkdir(join(stage, auditRoot), { recursive: true });
  const normalized = { ...execution };
  for (const [field, name] of [
    ["requestFile", "request.md"],
    ["agentPromptFile", "agent-prompt.md"],
    ["activityPromptFile", "activity-prompt.md"],
    ["responseFile", "response.json"],
    ["logFile", "process.jsonl"],
  ]) {
    const destination = `${auditRoot}/${name}`;
    await cp(execution[field], join(stage, destination));
    normalized[field] = destination;
    normalized[`${field}Sha256`] = await sha256(join(stage, destination));
  }
  normalizedExecutions.push(normalized);
}
await writeFile(
  join(stage, "metadata", "agent-executions.json"),
  `${JSON.stringify(normalizedExecutions, null, 2)}\n`,
);

process.stdout.write(
  `${JSON.stringify({ status: "READY", files: manifest.assets.length, agentExecutions: normalizedExecutions.length })}\n`,
);
