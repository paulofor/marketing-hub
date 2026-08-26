import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const moduleDirectory = dirname(fileURLToPath(import.meta.url));
const fixturePath = resolve(
  moduleDirectory,
  "inputs/LOCAL_QA-2026-08-26-b2c-instagram-v5.json",
);

test("bloqueia o score sem validação e libera os agentes finais após duas leituras", async (context) => {
  const temporaryDirectory = await mkdtemp(join(tmpdir(), "pde-purchase-gate-"));
  context.after(() => rm(temporaryDirectory, { recursive: true, force: true }));
  const runnerPath = join(temporaryDirectory, "agent-double.mjs");
  await writeFile(runnerPath, agentDoubleSource(), "utf8");

  const blockedRolesPath = join(temporaryDirectory, "blocked-roles.log");
  const blockedAuditDirectory = join(temporaryDirectory, "blocked-audit");
  await runProcess(fixturePath, blockedAuditDirectory, runnerPath, blockedRolesPath);
  const blockedReport = JSON.parse(
    await readFile(join(blockedAuditDirectory, "process-report.json"), "utf8"),
  );
  assert.equal(blockedReport.finalDecision.decision, "RESEARCH_MORE");
  assert.equal(blockedReport.finalDecision.agentDecisions.dedalo, "NOT_EXECUTED");
  assert.equal(blockedReport.finalDecision.agentDecisions.psique, "NOT_EXECUTED");
  assert.deepEqual(
    (await readFile(blockedRolesPath, "utf8")).trim().split("\n"),
    ["argos", "hermes"],
  );

  const repositoryRoot = join(temporaryDirectory, "repository");
  await createLiveCollections(repositoryRoot);
  const eligibleInputPath = join(temporaryDirectory, "eligible.json");
  const eligibleInput = JSON.parse(await readFile(fixturePath, "utf8"));
  makeEligible(eligibleInput);
  await writeFile(eligibleInputPath, JSON.stringify(eligibleInput), "utf8");
  const eligibleRolesPath = join(temporaryDirectory, "eligible-roles.log");
  const eligibleAuditDirectory = join(temporaryDirectory, "eligible-audit");

  await runProcess(
    eligibleInputPath,
    eligibleAuditDirectory,
    runnerPath,
    eligibleRolesPath,
    repositoryRoot,
  );
  const eligibleReport = JSON.parse(
    await readFile(join(eligibleAuditDirectory, "process-report.json"), "utf8"),
  );
  assert.equal(eligibleReport.purchaseMomentGate.status, "PASS");
  assert.equal(eligibleReport.finalDecision.decision, "APPROVE");
  assert.deepEqual(
    (await readFile(eligibleRolesPath, "utf8")).trim().split("\n"),
    ["argos", "hermes", "dedalo", "psique"],
  );
  assert.deepEqual(eligibleReport.commercialEffects, {
    contacts: 0,
    purchases: 0,
    sales: 0,
    revenue: 0,
    mediaSpend: 0,
    publications: 0,
  });
});

/** Executa o fluxo completo com agentes substituídos por respostas estruturadas locais. */
async function runProcess(
  inputPath,
  auditDirectory,
  runnerPath,
  rolesPath,
  repositoryRoot,
) {
  await new Promise((resolveExecution, rejectExecution) => {
    const child = spawn(process.execPath, ["run-process.mjs", inputPath], {
      cwd: moduleDirectory,
      env: {
        ...process.env,
        AUDIT_DIR: auditDirectory,
        PDE_OPPORTUNITY_AGENT_RUNNER: runnerPath,
        AGENT_DOUBLE_ROLES_PATH: rolesPath,
        ...(repositoryRoot
          ? { PDE_OPPORTUNITY_REPOSITORY_ROOT: repositoryRoot }
          : {}),
      },
      stdio: ["ignore", "pipe", "pipe"],
    });
    let stderr = "";
    child.stderr.on("data", (chunk) => (stderr += chunk));
    child.on("error", rejectExecution);
    child.on("close", (code) => {
      if (code === 0) resolveExecution();
      else rejectExecution(new Error(`Fluxo encerrou com código ${code}: ${stderr}`));
    });
  });
}

/** Cria as três coleções vivas com os IDs usados pelo dossiê de teste. */
async function createLiveCollections(repositoryRoot) {
  const articles = [
    "pesquisas/gartner/2026-08-26-gartner.md",
    "pesquisas/ia-aplicada/2026-08-24-produtos-digitais-tendencias-consumo.md",
    "pesquisas/ia-aplicada/2026-08-25-produtos-digitais-tendencias-consumo.md",
    "pesquisas/ia-aplicada/2026-08-26-produtos-digitais-tendencias-consumo.md",
    "pesquisas/momentos-de-compra-b2c/2026-08-26-momento.md",
  ];
  for (const article of articles) {
    const absolutePath = join(repositoryRoot, article);
    await mkdir(dirname(absolutePath), { recursive: true });
    await writeFile(absolutePath, `Resumo auditável de teste: ${article}\n`, "utf8");
  }
}

/** Acrescenta fatos privados suficientes para exercitar o caminho feliz do gate. */
function makeEligible(input) {
  input.inspirations.hotmartContract.status = "CURRENT";
  for (const product of input.inspirations.hotmartProducts) {
    product.collectedAt = "2026-08-26T09:00:00Z";
    product.price = "R$ 67,00";
  }
  input.purchaseMomentValidation = {
    sourceQuality: {
      evaluatedAt: "2026-08-26T14:00:00Z",
      maxAgeDays: 30,
    },
    successCriteria: {
      declaredAt: "2026-08-26T08:00:00Z",
      minimumEligibleParticipantsPerReading: 5,
      minimumExperienceStartRate: 0.7,
      minimumValueMomentRate: 0.6,
      minimumPrototypePreferenceRate: 0.6,
      minimumCheckoutStartRate: 0.2,
    },
    candidates: [
      {
        candidateName: "Entrevista sem Branco",
        scene: {
          trigger: "Entrevista marcada",
          deadline: "Sete dias",
          costOfError: "Perda de uma oportunidade de renda",
          budgetEvidence: "Compara treinos pagos de baixo tíquete",
          failedAttempt: "Ensaiou sem feedback estruturado",
          currentPaidBehavior: "Compra simuladores e preparação profissional",
        },
        freeAlternative: {
          name: "Ensaio sozinho com ChatGPT",
          prototypeAdvantage: "Compara duas respostas faladas da própria pessoa",
        },
        prototype: {
          prototypeId: "PRIVATE-INTERVIEW-1",
          private: true,
          published: false,
          paymentEnabled: false,
          mediaSpend: 0,
          testMarker: "PRIVATE_PROTOTYPE",
        },
        readings: [
          reading("R1", "2026-08-26T10:00:00Z"),
          reading("R2", "2026-08-26T12:00:00Z"),
        ],
      },
    ],
  };
}

function reading(readingId, observedAt) {
  return {
    readingId,
    observedAt,
    eligibleParticipants: 5,
    experienceStarted: 5,
    valueMoments: 4,
    prototypePreferredOverFree: 4,
    checkoutStarted: 2,
    psiqueDecision: "APPROVE",
    temisDecision: "APPROVE",
    eventSource: "FIRST_PARTY_EVENTS",
    testMarker: "PRIVATE_PROTOTYPE",
  };
}

function agentDoubleSource() {
  return `
import { appendFileSync } from "node:fs";
let input = "";
process.stdin.on("data", (chunk) => (input += chunk));
process.stdin.on("end", () => {
  JSON.parse(input);
  const role = process.env.AGENT_ROLE;
  appendFileSync(process.env.AGENT_DOUBLE_ROLES_PATH, role + "\\n");
  const results = {
    argos: { decision: "APPROVE" },
    hermes: { decision: "APPROVE" },
    dedalo: {
      decision: "APPROVE",
      chosenOpportunity: {
        sourceAlternativeName: "Entrevista sem Branco",
        workingProductName: "Protótipo de teste",
      },
      comparison: [{ name: "Entrevista sem Branco", totalScore: 83 }],
    },
    psique: { decision: "APPROVE", valueScore: 80 },
  };
  process.stdout.write(JSON.stringify({
    model: "test-double",
    serviceTier: "flex",
    status: "COMPLETED",
    responseId: "local-" + role,
    usage: {},
    result: results[role],
  }));
});
`;
}
