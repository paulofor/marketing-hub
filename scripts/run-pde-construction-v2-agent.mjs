#!/usr/bin/env node

import { execFile, spawn } from "node:child_process";
import { copyFile, mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { promisify } from "node:util";

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPOSITORY = resolve(SCRIPT_DIR, "..");
const MODEL = "gpt-5.6-sol";
const execFileAsync = promisify(execFile);
export const CODEX_SANDBOX = "danger-full-access";

export const CONTRACTS = Object.freeze({
  dedalo: {
    expectedAgent: "DEDALO",
    prompt: "landing-generator-agent-worker/src/main/resources/prompts/pde-construction/v2/construction-review.md"
  },
  temis: {
    expectedAgent: "TEMIS",
    prompt: "meta-ad-approver-worker/src/main/resources/prompts/pde-construction/v2/quality-review.md"
  },
  psique: {
    expectedAgent: "PSIQUE",
    prompt: "customer-agent-worker/src/main/resources/prompts/pde-construction/v2/customer-review.md"
  }
});

/** Lê a última telemetria cumulativa emitida pelo Codex. */
export function parseUsage(jsonLines) {
  let inputTokens = 0;
  let cachedInputTokens = 0;
  let outputTokens = 0;
  let serviceTier = null;
  let informed = false;
  for (const line of jsonLines.split(/\r?\n/)) {
    if (!line.trim().startsWith("{")) continue;
    try {
      const event = JSON.parse(line);
      const usage = event.usage ?? event.response?.usage;
      if (usage) {
        informed = true;
        inputTokens = Math.max(inputTokens, Number(usage.input_tokens ?? usage.inputTokens ?? 0));
        cachedInputTokens = Math.max(cachedInputTokens, Number(usage.cached_input_tokens ?? usage.input_tokens_details?.cached_tokens ?? 0));
        outputTokens = Math.max(outputTokens, Number(usage.output_tokens ?? usage.outputTokens ?? 0));
      }
      serviceTier ??= event.service_tier ?? event.response?.service_tier ?? null;
    } catch {
      // Linhas não JSON são preservadas no log, mas não entram na telemetria.
    }
  }
  return { informed, inputTokens, cachedInputTokens, outputTokens, serviceTier };
}

/** Impede aprovação estrutural sem agente, comparação e evidência mínimas. */
export function validateResult(result, contract) {
  if (result?.agent !== contract.expectedAgent) throw new Error("Resposta pertence a outro agente.");
  if (!["APPROVE", "ADJUST", "BLOCKED"].includes(result?.decision)) throw new Error("Decisão inválida.");
  if (!Array.isArray(result.alternatives) || result.alternatives.length !== 3) throw new Error("São exigidas três alternativas.");
  if (!Array.isArray(result.evidence) || result.evidence.length < 4) throw new Error("Evidência insuficiente.");
  if (result.decision === "APPROVE" && result.findings?.some(finding => finding.severity === "BLOCKER")) {
    throw new Error("Aprovação contradiz bloqueio material.");
  }
}

/** Interpreta os argumentos mínimos do executor local sem callback produtivo. */
function parseArguments(argv) {
  const args = {};
  for (let index = 0; index < argv.length; index += 2) {
    if (!argv[index]?.startsWith("--") || argv[index + 1] == null) throw new Error("Argumento inválido.");
    args[argv[index].slice(2)] = argv[index + 1];
  }
  if (!args.agent || !CONTRACTS[args.agent]) throw new Error("Informe --agent dedalo, temis ou psique.");
  if (!args["context-file"] || !args["result-file"]) throw new Error("Informe --context-file e --result-file.");
  return args;
}

/** Copia o estado exato do worktree para o agente auditar sem poder alterar a origem. */
async function createRepositorySnapshot(temporary) {
  const snapshot = join(temporary, "repository-snapshot");
  const { stdout } = await execFileAsync(
    "git",
    ["ls-files", "--cached", "--others", "--exclude-standard", "-z"],
    { cwd: REPOSITORY, encoding: "buffer", maxBuffer: 32 * 1024 * 1024 }
  );
  for (const relativePath of stdout.toString().split("\0").filter(Boolean)) {
    const target = join(snapshot, relativePath);
    await mkdir(dirname(target), { recursive: true });
    await copyFile(join(REPOSITORY, relativePath), target);
  }
  return snapshot;
}

/** Executa um gate local em snapshot descartável e produz saída estruturada. */
export async function main(argv = process.argv.slice(2)) {
  const args = parseArguments(argv);
  const contract = CONTRACTS[args.agent];
  const context = await readFile(resolve(args["context-file"]), "utf8");
  const core = args.agent === "psique"
    ? await readFile(resolve(REPOSITORY, "customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v2.md"), "utf8")
    : "";
  const template = await readFile(resolve(REPOSITORY, contract.prompt), "utf8");
  const prompt = template
    .replace("{{PSIQUE_BEHAVIORAL_CORE_V2}}", core)
    .replace("{{TASK_CONTEXT}}", context);
  const temporary = await mkdtemp(join(tmpdir(), "pde-construction-v2-"));
  const snapshot = await createRepositorySnapshot(temporary);
  const outputPath = join(temporary, "result.json");
  const schemaPath = resolve(REPOSITORY, "scripts/pde-construction-v2-review-schema.json");
  const argumentsList = [
    "--search", "exec", "-", "--ephemeral", "--skip-git-repo-check", "--sandbox", CODEX_SANDBOX,
    "--cd", snapshot, "--output-schema", schemaPath, "--output-last-message", outputPath,
    "--json", "--color", "never", "--config", 'approval_policy="never"',
    "--config", `model_reasoning_effort="${args.effort || "high"}"`, "--model", MODEL
  ];
  try {
    const child = spawn(process.env.CODEX_COMMAND || "codex", argumentsList, {
      cwd: snapshot,
      env: process.env,
      stdio: ["pipe", "pipe", "pipe"]
    });
    let log = "";
    child.stdout.on("data", chunk => { log += chunk.toString(); });
    child.stderr.on("data", chunk => { log += chunk.toString(); });
    child.stdin.end(prompt);
    const exitCode = await new Promise((resolveExit, reject) => {
      child.once("error", reject);
      child.once("close", resolveExit);
    });
    if (exitCode !== 0) throw new Error(`Codex encerrou com código ${exitCode}: ${log.slice(-2000)}`);
    const result = JSON.parse(await readFile(outputPath, "utf8"));
    validateResult(result, contract);
    const usage = parseUsage(log);
    await writeFile(resolve(args["result-file"]), `${JSON.stringify({ result, usage }, null, 2)}\n`, "utf8");
    process.stdout.write(`${JSON.stringify({ agent: args.agent, decision: result.decision, usage })}\n`);
    return { result, usage };
  } finally {
    await rm(temporary, { recursive: true, force: true });
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch(error => {
    process.stderr.write(`${error.stack || error.message}\n`);
    process.exitCode = 1;
  });
}
