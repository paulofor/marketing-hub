#!/usr/bin/env node

import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPOSITORY = resolve(SCRIPT_DIR, "..");
const PROCESS_CODE = "pde-commercial-plan-offer";
const MODEL = "gpt-5.6-sol";

export const CONTRACTS = Object.freeze({
  "landing-generator": {
    activities: ["positioning", "format", "decisionCard", "experience"],
    prompt: "landing-generator-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/offer-design.md",
    schema: "landing-generator-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/offer-design-schema.json"
  },
  "meta-ad-approver": {
    activities: ["decisionCard", "proof", "control", "review"],
    prompt: "meta-ad-approver-worker/src/main/resources/prompts/pde-commercial-plan/v4/commercial-review.md",
    schema: "meta-ad-approver-worker/src/main/resources/prompts/pde-commercial-plan/v4/commercial-review-schema.json"
  },
  "growth-operator": {
    activities: ["distribution", "control"],
    prompt: "growth-operator-worker/src/main/resources/prompts/pde-commercial-plan/v4/distribution-control.md",
    schema: "growth-operator-worker/src/main/resources/prompts/pde-commercial-plan/v4/distribution-control-schema.json"
  },
  "financial-agent": {
    activities: ["economics"],
    prompt: "financial-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/economics.md",
    schema: "financial-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/economics-schema.json"
  },
  "customer-agent": {
    activities: ["review"],
    prompt: "customer-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/customer-review.md",
    schema: "customer-agent-worker/src/main/resources/prompts/pde-commercial-plan/v4/customer-review-schema.json"
  }
});

/** Resolve o contrato versionado sem permitir que um agente consuma responsabilidade alheia. */
export function resolveContract(agentKey, activityId) {
  const contract = CONTRACTS[agentKey];
  if (!contract || !contract.activities.includes(activityId)) {
    throw new Error(`Contrato não suportado para ${agentKey}/${activityId}.`);
  }
  return contract;
}

/** Lê a última medição cumulativa e preserva o tier apenas quando informado pelo runtime. */
export function parseUsage(jsonLines) {
  let inputTokens = 0;
  let cachedInputTokens = 0;
  let outputTokens = 0;
  let informed = false;
  let serviceTier = null;
  for (const line of jsonLines.split(/\r?\n/)) {
    if (!line.trim().startsWith("{")) continue;
    let event;
    try {
      event = JSON.parse(line);
    } catch {
      continue;
    }
    const usage = event.usage ?? event.response?.usage;
    if (usage && typeof usage === "object") {
      informed = true;
      inputTokens = Math.max(inputTokens, Number(usage.input_tokens ?? usage.inputTokens ?? 0));
      cachedInputTokens = Math.max(
        cachedInputTokens,
        Number(
          usage.cached_input_tokens ??
            usage.cachedInputTokens ??
            usage.input_tokens_details?.cached_tokens ??
            0
        )
      );
      outputTokens = Math.max(outputTokens, Number(usage.output_tokens ?? usage.outputTokens ?? 0));
    }
    serviceTier ??= event.service_tier ?? event.response?.service_tier ?? null;
  }
  return { informed, inputTokens, cachedInputTokens, outputTokens, serviceTier };
}

/** Exige a decisão e a atividade do contrato antes de persistir qualquer callback. */
export function validateResult(result, activityId) {
  if (result?.activity !== activityId) {
    throw new Error(`Resposta declarou atividade ${result?.activity ?? "ausente"}; esperado ${activityId}.`);
  }
  if (!["APPROVE", "ADJUST", "REJECT"].includes(result?.decision)) {
    throw new Error("Resposta sem decisão comercial válida.");
  }
  const compared = result.alternatives ?? result.scenarios ?? result.interpretations;
  if (!Array.isArray(compared) || compared.length !== 3) {
    throw new Error("Resposta deve comparar exatamente três alternativas.");
  }
}

/** Recupera o contexto original de uma execução bloqueada para permitir correção auditável. */
export function taskFromPayload(payload) {
  const task = payload?.task ?? payload;
  if (!task?.taskId) throw new Error("Arquivo de tarefa sem contexto original.");
  return task;
}

/** Normaliza o limite do executor e rejeita configuração que criaria timeout imediato. */
export function executionTimeout(value) {
  const timeoutMs = Number(value ?? 600000);
  if (!Number.isFinite(timeoutMs) || timeoutMs < 1000) {
    throw new Error("Timeout do agente deve ser um número de pelo menos 1000 ms.");
  }
  return timeoutMs;
}

/** Consolida resultados atuais de todos os agentes ao retomar uma atividade corrigida. */
export function refreshedProcessContext(taskLists, sourceReference) {
  const latestByOwnerActivity = new Map();
  taskLists
    .flat()
    .filter(
      task =>
        task.sourceReference === sourceReference &&
        task.processCode === PROCESS_CODE &&
        task.status === "COMPLETED"
    )
    .sort((left, right) => left.id - right.id)
    .forEach(task =>
      latestByOwnerActivity.set(
        `${task.assignedAgentKey ?? task.assignedAgentId}:${task.processActivityId}`,
        task
      )
    );
  const completedActivities = [...latestByOwnerActivity.values()]
    .sort((left, right) => left.id - right.id)
    .map(task => ({
      taskId: task.id,
      activityId: task.processActivityId,
      activityName: task.processActivityName,
      resultJson: task.resultJson,
      evidenceJson: task.evidenceJson,
      deliveredAt: task.deliveredAt
    }));
  return JSON.stringify({ completedActivities });
}

/** Atualiza o contexto predecessor sem criar uma nova tarefa ou perder a auditoria da tentativa. */
async function refreshTaskContext(backendUrl, task) {
  const taskLists = await Promise.all(
    Object.keys(CONTRACTS).map(async agentKey => {
      const url = new URL(`/api/agent-tasks/agents/${agentKey}`, backendUrl);
      const response = await fetch(url);
      if (!response.ok) throw new Error(`Falha ao atualizar contexto de ${agentKey}: HTTP ${response.status}.`);
      return response.json();
    })
  );
  return {
    ...task,
    processContextJson: refreshedProcessContext(taskLists, task.sourceReference)
  };
}

/** Interpreta somente as opções operacionais necessárias ao executor local. */
function parseArguments(argv) {
  const values = {};
  for (let index = 0; index < argv.length; index += 2) {
    const key = argv[index];
    const value = argv[index + 1];
    if (!key?.startsWith("--") || value == null) throw new Error(`Argumento inválido: ${key ?? "ausente"}`);
    values[key.slice(2)] = value;
  }
  for (const required of ["agent-key", "activity-id"]) {
    if (!values[required]) throw new Error(`Informe --${required}.`);
  }
  return values;
}

/** Busca e reserva somente a atividade pedida no endpoint pending canônico. */
async function claimTask(backendUrl, agentKey, activityId) {
  const url = new URL(`/api/internal/agent-tasks/${agentKey}/stage-executions/pending`, backendUrl);
  url.searchParams.set("processCode", PROCESS_CODE);
  url.searchParams.set("activityId", activityId);
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Falha ao reservar tarefa: HTTP ${response.status}.`);
  const tasks = await response.json();
  if (!Array.isArray(tasks) || tasks.length === 0) throw new Error("Nenhuma tarefa elegível encontrada.");
  return tasks[0];
}

/** Executa o Codex com filesystem somente leitura, schema obrigatório e telemetria JSONL. */
async function runCodex(prompt, schemaPath, outputPath, logPath, reasoningEffort, timeoutMs) {
  const args = [
    "--search", "exec", "-", "--ephemeral", "--skip-git-repo-check",
    "--sandbox", "read-only", "--cd", REPOSITORY,
    "--output-schema", schemaPath, "--output-last-message", outputPath,
    "--json", "--color", "never", "--config", 'approval_policy="never"',
    "--config", `model_reasoning_effort="${reasoningEffort}"`, "--model", MODEL
  ];
  const child = spawn(process.env.CODEX_COMMAND || "codex", args, {
    cwd: REPOSITORY,
    env: process.env,
    stdio: ["pipe", "pipe", "pipe"]
  });
  let log = "";
  let timedOut = false;
  child.stdout.on("data", chunk => { log += chunk.toString(); });
  child.stderr.on("data", chunk => { log += chunk.toString(); });
  child.stdin.end(prompt);
  const timeout = setTimeout(() => {
    timedOut = true;
    child.kill("SIGTERM");
  }, timeoutMs);
  const exitCode = await new Promise((resolveExit, reject) => {
    child.once("error", reject);
    child.once("close", resolveExit);
  });
  clearTimeout(timeout);
  await writeFile(logPath, log, "utf8");
  if (timedOut || exitCode !== 0) {
    const error = new Error(
      timedOut
        ? `Codex excedeu o limite operacional de ${timeoutMs} ms.`
        : `Codex encerrou com código ${exitCode}.`
    );
    error.codexLog = log;
    throw error;
  }
  return log;
}

/** Persiste falha técnica e eventual telemetria parcial para não deixar atividade órfã. */
async function reportTechnicalFailure(backendUrl, agentKey, task, error) {
  const usage = parseUsage(error.codexLog ?? "");
  const body = {
    error: error.message,
    evidenceJson: JSON.stringify({
      executionMode: "LOCAL_SANDBOX",
      externalSideEffects: false,
      agentKey,
      model: MODEL,
      sourceReference: task.sourceReference,
      activityId: task.activityId,
      technicalFailure: true
    })
  };
  if (usage.informed) {
    body.modelUsages = [{
      modelCode: MODEL,
      serviceTier: usage.serviceTier?.toUpperCase() === "FLEX" ? "FLEX" : "STANDARD",
      inputTokens: usage.inputTokens,
      cachedInputTokens: usage.cachedInputTokens,
      outputTokens: usage.outputTokens
    }];
  }
  const url = new URL(
    `/api/internal/agent-tasks/${agentKey}/stage-executions/${task.taskId}/failure`,
    backendUrl
  );
  const response = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(`Falha ao registrar erro técnico: HTTP ${response.status} ${await response.text()}`);
  }
}

/** Envia resultado, evidência e uso medido pela mesma execução ao backend. */
async function callback(backendUrl, agentKey, task, result, usage, contract) {
  const approved = result.decision === "APPROVE";
  const endpoint = approved ? "result" : "failure";
  const evidence = {
    executionMode: "LOCAL_SANDBOX",
    externalSideEffects: false,
    agentKey,
    model: MODEL,
    reportedServiceTier: usage.serviceTier,
    pricingTierAssumption: usage.serviceTier ? null : "STANDARD_FALLBACK_CLI_DID_NOT_REPORT",
    promptContract: contract.prompt,
    schemaContract: contract.schema,
    sourceReference: task.sourceReference,
    activityId: task.activityId,
    sources: result.sources ?? []
  };
  const body = {
    resultJson: JSON.stringify(result),
    evidenceJson: JSON.stringify(evidence)
  };
  if (!approved) body.error = `Agente decidiu ${result.decision}: ${result.rationale}`;
  if (usage.informed) {
    body.modelUsages = [{
      modelCode: MODEL,
      serviceTier: usage.serviceTier?.toUpperCase() === "FLEX" ? "FLEX" : "STANDARD",
      inputTokens: usage.inputTokens,
      cachedInputTokens: usage.cachedInputTokens,
      outputTokens: usage.outputTokens
    }];
  }
  const url = new URL(
    `/api/internal/agent-tasks/${agentKey}/stage-executions/${task.taskId}/${endpoint}`,
    backendUrl
  );
  const response = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!response.ok) throw new Error(`Callback rejeitado: HTTP ${response.status} ${await response.text()}`);
}

/** Executa uma atividade e mantém os arquivos resultantes apenas no diretório solicitado. */
export async function main(argv = process.argv.slice(2)) {
  const args = parseArguments(argv);
  const agentKey = args["agent-key"];
  const activityId = args["activity-id"];
  const contract = resolveContract(agentKey, activityId);
  const backendUrl = args["backend-url"] || "http://191.252.181.168";
  const claimedTask = args["task-file"]
    ? taskFromPayload(JSON.parse(await readFile(resolve(args["task-file"]), "utf8")))
    : await claimTask(backendUrl, agentKey, activityId);
  const task = await refreshTaskContext(backendUrl, claimedTask);
  if (task.processCode !== PROCESS_CODE || task.activityId !== activityId) {
    throw new Error("Tarefa recebida fora do contrato solicitado.");
  }
  const temporary = await mkdtemp(join(tmpdir(), "commercial-plan-agent-"));
  const outputPath = join(temporary, "result.json");
  const logPath = join(temporary, "codex.jsonl");
  try {
    const template = await readFile(resolve(REPOSITORY, contract.prompt), "utf8");
    const prompt = template.replace("{{TASK_CONTEXT}}", JSON.stringify(task));
    let log;
    try {
      log = await runCodex(
        prompt,
        resolve(REPOSITORY, contract.schema),
        outputPath,
        logPath,
        args["reasoning-effort"] || "high",
        executionTimeout(args["timeout-ms"] || process.env.COMMERCIAL_PLAN_AGENT_TIMEOUT_MS)
      );
    } catch (error) {
      await reportTechnicalFailure(backendUrl, agentKey, task, error);
      throw error;
    }
    const result = JSON.parse(await readFile(outputPath, "utf8"));
    validateResult(result, activityId);
    const usage = parseUsage(log);
    if (args["result-file"]) {
      await writeFile(
        resolve(args["result-file"]),
        `${JSON.stringify({ task, result, usage }, null, 2)}\n`,
        "utf8"
      );
    }
    if (args["dry-run"] !== "true") {
      await callback(backendUrl, agentKey, task, result, usage, contract);
    }
    process.stdout.write(`${JSON.stringify({ taskId: task.taskId, activityId, decision: result.decision, usage })}\n`);
    return { task, result, usage };
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
