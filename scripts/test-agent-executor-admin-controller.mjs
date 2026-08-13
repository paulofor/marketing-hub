import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("./agent-executor-admin-controller.mjs", import.meta.url), "utf8");
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
console.log("Controlador administrativo restrito aos seis executores validado.");
