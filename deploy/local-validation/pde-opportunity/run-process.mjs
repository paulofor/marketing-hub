import { spawn } from "node:child_process";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  buildFinalDecision,
  selectActiveResearch,
  validateResearchInput,
} from "./contract.mjs";
import {
  attachLiveArticleInspirations,
  loadLiveArticleInspirations,
} from "./live-inspirations.mjs";
import { buildPurchaseMomentGate } from "./purchase-moment-validation.mjs";

const moduleDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = process.env.PDE_OPPORTUNITY_REPOSITORY_ROOT
  ? resolve(process.env.PDE_OPPORTUNITY_REPOSITORY_ROOT)
  : resolve(moduleDirectory, "../../..");
const agentRunnerPath = process.env.PDE_OPPORTUNITY_AGENT_RUNNER
  ? resolve(process.env.PDE_OPPORTUNITY_AGENT_RUNNER)
  : "run-agent.mjs";
const inputPath = process.argv[2];
if (!inputPath) throw new Error("Informe o arquivo JSON de evidências.");

const liveArticleInspirations = await loadLiveArticleInspirations(repositoryRoot);
const research = selectActiveResearch(
  attachLiveArticleInspirations(
    JSON.parse(await readFile(resolve(inputPath), "utf8")),
    liveArticleInspirations,
  ),
);
validateResearchInput(research);
const correlationId = String(research.cycleId).replace(/[^a-zA-Z0-9_-]/g, "_");
const auditDirectory = resolve(
  process.env.AUDIT_DIR || `artifacts/pde-opportunity/${correlationId}`,
);
await mkdir(auditDirectory, { recursive: true });

const executions = {};
executions.argos = await executeAgent("argos", research);
executions.hermes = await executeAgent("hermes", {
  research,
  argos: executions.argos.result,
});
const purchaseMomentGate = buildPurchaseMomentGate(research);
await writeFile(
  `${auditDirectory}/purchase-moment-gate.json`,
  `${JSON.stringify(purchaseMomentGate, null, 2)}\n`,
  "utf8",
);

let finalDecision;
if (purchaseMomentGate.required && purchaseMomentGate.eligibleCandidateNames.length === 0) {
  finalDecision = buildBlockedPurchaseMomentDecision(
    research,
    executions,
    purchaseMomentGate,
  );
} else {
  executions.dedalo = await executeAgent("dedalo", {
    research,
    argos: executions.argos.result,
    hermes: executions.hermes.result,
    purchaseMomentGate,
  });
  executions.psique = await executeAgent("psique", {
    research,
    argos: executions.argos.result,
    hermes: executions.hermes.result,
    purchaseMomentGate,
    dedalo: executions.dedalo.result,
  });

  finalDecision = buildFinalDecision({
    research,
    argos: executions.argos.result,
    hermes: executions.hermes.result,
    dedalo: executions.dedalo.result,
    psique: executions.psique.result,
  });
}
const usage = summarizeUsage(executions);
const cost = calculateFlexCost(executions);
const report = {
  correlationId,
  environment: "LOCAL_QA",
  processCode: research.processCode,
  processVersion: research.processVersion,
  generatedAt: new Date().toISOString(),
  inspirationAudit: research.inspirations,
  purchaseMomentGate,
  finalDecision,
  usage,
  cost,
  commercialEffects: {
    contacts: 0,
    purchases: 0,
    sales: 0,
    revenue: 0,
    mediaSpend: 0,
    publications: 0,
  },
  agents: Object.fromEntries(
    Object.entries(executions).map(([agent, execution]) => [
      agent,
      {
        model: execution.model,
        serviceTier: execution.serviceTier,
        status: execution.status,
        responseId: execution.responseId,
        usage: execution.usage,
        result: execution.result,
      },
    ]),
  ),
};
await writeFile(
  `${auditDirectory}/process-report.json`,
  `${JSON.stringify(report, null, 2)}\n`,
  "utf8",
);
process.stdout.write(
  `${JSON.stringify({
    auditDirectory,
    correlationId,
    finalDecision,
    usage,
    cost: report.cost,
    commercialEffects: report.commercialEffects,
  })}\n`,
);

/** Encerra a pesquisa antes do score quando nenhum protótipo possui duas leituras válidas. */
function buildBlockedPurchaseMomentDecision(researchInput, agentExecutions, gate) {
  const candidateReasons = gate.candidates.flatMap((candidate) =>
    candidate.reasons.map((reason) => `${candidate.candidateName}: ${reason}`),
  );
  return {
    decision: gate.status === "STOP" ? "REJECT" : "RESEARCH_MORE",
    chosenOpportunity: null,
    workingProductName: null,
    totalScore: null,
    benchmarkName: researchInput.benchmark.name,
    benchmarkScore: researchInput.benchmark.score,
    benchmarkResult: "NOT_EVALUATED",
    agentDecisions: {
      argos: agentExecutions.argos.result.decision,
      hermes: agentExecutions.hermes.result.decision,
      dedalo: "NOT_EXECUTED",
      psique: "NOT_EXECUTED",
    },
    purchaseMomentGate: {
      status: gate.status,
      eligibleCandidateNames: gate.eligibleCandidateNames,
    },
    reasons: [...gate.reasons, ...candidateReasons],
  };
}

/** Executa um agente isolado e mantém stderr separado da resposta funcional. */
async function executeAgent(agentRole, context) {
  return await new Promise((resolveExecution, rejectExecution) => {
    const child = spawn(process.execPath, [agentRunnerPath], {
      cwd: moduleDirectory,
      env: {
        ...process.env,
        AGENT_ROLE: agentRole,
        RUN_ID: correlationId,
        AUDIT_DIR: auditDirectory,
      },
      stdio: ["pipe", "pipe", "pipe"],
    });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += chunk));
    child.stderr.on("data", (chunk) => (stderr += chunk));
    child.on("error", rejectExecution);
    child.on("close", (code) => {
      if (code !== 0) {
        rejectExecution(
          new Error(`${agentRole} encerrou com código ${code}: ${stderr.trim()}`),
        );
        return;
      }
      try {
        resolveExecution(JSON.parse(stdout));
      } catch (error) {
        rejectExecution(
          new Error(`${agentRole} retornou envelope inválido: ${error.message}`),
        );
      }
    });
    child.stdin.end(JSON.stringify(context));
  });
}

function summarizeUsage(executionsByAgent) {
  return Object.values(executionsByAgent).reduce(
    (total, execution) => ({
      inputTokens: total.inputTokens + Number(execution.usage?.input_tokens || 0),
      cachedInputTokens:
        total.cachedInputTokens +
        Number(execution.usage?.input_tokens_details?.cached_tokens || 0),
      outputTokens: total.outputTokens + Number(execution.usage?.output_tokens || 0),
    }),
    { inputTokens: 0, cachedInputTokens: 0, outputTokens: 0 },
  );
}

/** Calcula custo pelo snapshot oficial de Flex consultado em 2026-08-24. */
function calculateFlexCost(executionsByAgent) {
  const ratesPerMillion = {
    "gpt-5.6-terra": { input: 2, cachedInput: 0.2, output: 12 },
  };
  let estimatedUsd = 0;
  for (const execution of Object.values(executionsByAgent)) {
    const rates = ratesPerMillion[execution.model];
    if (!rates) {
      return {
        estimatedUsd: null,
        status: `MODEL_RATE_NOT_VERSIONED:${execution.model}`,
        source: "https://developers.openai.com/api/docs/models/gpt-5.6-terra",
        accessedAt: "2026-08-24",
      };
    }
    const input = Number(execution.usage?.input_tokens || 0);
    const cached = Number(execution.usage?.input_tokens_details?.cached_tokens || 0);
    const output = Number(execution.usage?.output_tokens || 0);
    estimatedUsd +=
      ((input - cached) * rates.input + cached * rates.cachedInput + output * rates.output) /
      1_000_000;
  }
  return {
    estimatedUsd: Number(estimatedUsd.toFixed(8)),
    status: "ESTIMATED_FROM_VERSIONED_FLEX_RATES",
    source: "https://developers.openai.com/api/docs/models/gpt-5.6-terra",
    accessedAt: "2026-08-24",
  };
}
