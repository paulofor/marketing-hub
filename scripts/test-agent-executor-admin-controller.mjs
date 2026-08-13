import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("./agent-executor-admin-controller.mjs", import.meta.url), "utf8");
const workflow = await readFile(
  new URL("../.github/workflows/agent-executor-admin-controller-ci.yml", import.meta.url),
  "utf8",
);
const dockerfile = await readFile(
  new URL("../deploy/agent-executor-admin-controller/Dockerfile", import.meta.url),
  "utf8",
);
for (const key of [
  "customer-agent",
  "financial-agent",
  "growth-operator",
  "experiment-strategist",
  "meta-ad-approver",
  "landing-generator",
]) assert.match(source, new RegExp(`\\[\\"${key}\\"`));
assert.match(source, /spawn\(command, args/);
assert.doesNotMatch(source, /exec\(|shell:\s*true/);
assert.match(source, /--force-recreate/);
assert.match(source, /operation\.operationType === "UPDATE"/);
assert.match(workflow, /docker compose up -d --build --remove-orphans/);
assert.match(workflow, /docker compose logs --tail=150/);
assert.doesNotMatch(workflow, /group: codex-agent-host-deploy/);
assert.match(dockerfile, /docker-cli-compose/);
console.log("Controlador administrativo restrito aos seis executores validado.");
