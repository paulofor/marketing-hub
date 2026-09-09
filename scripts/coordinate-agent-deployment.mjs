import { appendFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";

const DEFAULT_APPLICATION_WORKFLOW = "deploy-containers.yml";
const DEFAULT_REGISTRATION_TIMEOUT_SECONDS = 120;
const DEFAULT_SOURCE_TIMEOUT_SECONDS = 6900;
const DEFAULT_INTERVAL_SECONDS = 15;
const ALLOWED_APPLICATION_EVENTS = new Set(["push", "workflow_dispatch"]);

function positiveInteger(value, fallback, name) {
  const parsed = value === undefined ? fallback : Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${name} deve ser um inteiro positivo.`);
  }
  return parsed;
}

function assertSha(value, name) {
  if (!/^[0-9a-f]{40}$/.test(value ?? "")) {
    throw new Error(`${name} deve conter um SHA completo de 40 caracteres.`);
  }
}

function workflowRunsEndpoint({ apiUrl, repository, workflow, headSha, event }) {
  const repositoryPath = repository.split("/").map(encodeURIComponent).join("/");
  const workflowPath = encodeURIComponent(workflow);
  const query = new URLSearchParams({ head_sha: headSha, branch: "main", per_page: "20" });
  if (event) query.set("event", event);
  return `${apiUrl.replace(/\/$/, "")}/repos/${repositoryPath}/actions/workflows/${workflowPath}/runs?${query}`;
}

async function fetchRuns({ endpoint, token, fetchImpl }) {
  const response = await fetchImpl(endpoint, {
    headers: {
      accept: "application/vnd.github+json",
      authorization: `Bearer ${token}`,
      "x-github-api-version": "2022-11-28",
    },
  });

  if (response.status === 401 || response.status === 403 || response.status === 404) {
    throw new Error(`GitHub recusou a consulta de workflows: HTTP ${response.status}.`);
  }
  if (!response.ok) {
    throw new Error(`Consulta transitória de workflows falhou com HTTP ${response.status}.`);
  }
  return response.json();
}

export function selectMatchingRun(payload, headSha, allowedEvents) {
  const runs = Array.isArray(payload?.workflow_runs) ? payload.workflow_runs : [];
  return runs
    .filter(
      (run) =>
        run?.head_sha === headSha &&
        run?.head_branch === "main" &&
        allowedEvents.has(run?.event),
    )
    .sort((left, right) =>
      String(right.created_at ?? "").localeCompare(String(left.created_at ?? "")),
    )[0];
}

export async function waitForApplicationRunRegistration({
  repository,
  headSha,
  workflow = DEFAULT_APPLICATION_WORKFLOW,
  token,
  apiUrl = "https://api.github.com",
  timeoutSeconds = DEFAULT_REGISTRATION_TIMEOUT_SECONDS,
  intervalSeconds = DEFAULT_INTERVAL_SECONDS,
  fetchImpl = globalThis.fetch,
  sleepImpl = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)),
  nowImpl = Date.now,
  log = console.log,
  warn = console.warn,
}) {
  if (!repository || !workflow || !token) {
    throw new Error("GITHUB_REPOSITORY, workflow e GITHUB_TOKEN são obrigatórios.");
  }
  assertSha(headSha, "GITHUB_SHA");

  const timeout = positiveInteger(
    timeoutSeconds,
    DEFAULT_REGISTRATION_TIMEOUT_SECONDS,
    "APP_DEPLOY_REGISTRATION_TIMEOUT_SECONDS",
  );
  const interval = positiveInteger(
    intervalSeconds,
    DEFAULT_INTERVAL_SECONDS,
    "APP_DEPLOY_COORDINATION_INTERVAL_SECONDS",
  );
  const deadline = nowImpl() + timeout * 1000;
  const endpoint = workflowRunsEndpoint({ apiUrl, repository, workflow, headSha });

  while (nowImpl() < deadline) {
    try {
      const payload = await fetchRuns({ endpoint, token, fetchImpl });
      const run = selectMatchingRun(payload, headSha, ALLOWED_APPLICATION_EVENTS);
      if (run) {
        log(
          `Deploy da aplicação registrado para ${headSha.slice(0, 12)}: status=${run.status} ${run.html_url ?? ""}`.trim(),
        );
        return run;
      }
    } catch (error) {
      if (/HTTP (401|403|404)\./.test(error.message)) throw error;
      warn(`${error.message} Nova tentativa será feita.`);
    }
    await sleepImpl(interval * 1000);
  }

  throw new Error(
    `O workflow ${workflow} não foi registrado para ${headSha} após ${timeout} segundos; o agente não será publicado sem a coordenação central.`,
  );
}

function validateApplicationContext({ headSha, headBranch, event, conclusion }) {
  assertSha(headSha, "APP_DEPLOY_HEAD_SHA");
  if (headBranch !== "main") {
    throw new Error(`Deploy da aplicação fora de main não pode liberar agentes: ${headBranch ?? "ausente"}.`);
  }
  if (!ALLOWED_APPLICATION_EVENTS.has(event)) {
    throw new Error(`Evento do deploy da aplicação não autorizado: ${event ?? "ausente"}.`);
  }
  if (!conclusion) {
    throw new Error("APP_DEPLOY_CONCLUSION é obrigatória para coordenar a continuação.");
  }
}

export async function resolveAgentSourceRun({
  repository,
  headSha,
  headBranch,
  applicationEvent,
  applicationConclusion,
  sourceWorkflow,
  token,
  apiUrl = "https://api.github.com",
  timeoutSeconds = DEFAULT_SOURCE_TIMEOUT_SECONDS,
  intervalSeconds = DEFAULT_INTERVAL_SECONDS,
  fetchImpl = globalThis.fetch,
  sleepImpl = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)),
  nowImpl = Date.now,
  log = console.log,
  warn = console.warn,
}) {
  if (!repository || !sourceWorkflow || !token) {
    throw new Error("GITHUB_REPOSITORY, workflow de origem e GITHUB_TOKEN são obrigatórios.");
  }
  validateApplicationContext({
    headSha,
    headBranch,
    event: applicationEvent,
    conclusion: applicationConclusion,
  });

  const timeout = positiveInteger(
    timeoutSeconds,
    DEFAULT_SOURCE_TIMEOUT_SECONDS,
    "AGENT_SOURCE_WAIT_TIMEOUT_SECONDS",
  );
  const interval = positiveInteger(
    intervalSeconds,
    DEFAULT_INTERVAL_SECONDS,
    "APP_DEPLOY_COORDINATION_INTERVAL_SECONDS",
  );
  const deadline = nowImpl() + timeout * 1000;
  const endpoint = workflowRunsEndpoint({
    apiUrl,
    repository,
    workflow: sourceWorkflow,
    headSha,
    event: "push",
  });
  let sourceObserved = false;
  let lastState = "";

  while (nowImpl() < deadline) {
    try {
      const payload = await fetchRuns({ endpoint, token, fetchImpl });
      const run = selectMatchingRun(payload, headSha, new Set(["push"]));

      if (!run && !sourceObserved) {
        log(`Nenhuma execução de origem de ${sourceWorkflow} existe para ${headSha.slice(0, 12)}; continuação não aplicável.`);
        return { required: false, run: null };
      }
      if (!run) {
        warn(`A execução de origem observada para ${headSha.slice(0, 12)} ficou temporariamente indisponível.`);
      } else {
        sourceObserved = true;
        const state = `${run.id}:${run.status}:${run.conclusion ?? ""}`;
        if (state !== lastState) {
          log(
            `Execução testada de ${sourceWorkflow} para ${headSha.slice(0, 12)}: status=${run.status} conclusao=${run.conclusion ?? "pendente"} ${run.html_url ?? ""}`.trim(),
          );
          lastState = state;
        }

        if (applicationConclusion !== "success") {
          throw new Error(
            `Deploy da aplicação para ${headSha} terminou com ${applicationConclusion}; o agente correspondente permanecerá na versão anterior.`,
          );
        }
        if (run.status === "completed") {
          if (run.conclusion === "success") {
            return { required: true, run };
          }
          throw new Error(
            `Execução testada do agente para ${headSha} terminou com ${run.conclusion ?? "conclusão desconhecida"}: ${run.html_url ?? "URL indisponível"}.`,
          );
        }
      }
    } catch (error) {
      if (
        /HTTP (401|403|404)\./.test(error.message) ||
        /Deploy da aplicação .* terminou com/.test(error.message) ||
        /Execução testada do agente .* terminou com/.test(error.message)
      ) {
        throw error;
      }
      warn(`${error.message} Nova tentativa será feita.`);
    }
    await sleepImpl(interval * 1000);
  }

  throw new Error(
    `Tempo esgotado aguardando a execução testada de ${sourceWorkflow} para ${headSha} após ${timeout} segundos.`,
  );
}

export async function writeResolutionOutputs(outputFile, resolution) {
  if (!outputFile) throw new Error("GITHUB_OUTPUT é obrigatório para publicar a resolução.");
  const lines = resolution.required
    ? [
        "required=true",
        `source_run_id=${resolution.run.id}`,
        `source_sha=${resolution.run.head_sha}`,
      ]
    : ["required=false", "source_run_id=", "source_sha="];
  await appendFile(outputFile, `${lines.join("\n")}\n`, "utf8");
}

async function main() {
  const mode = process.argv[2];
  const workflow = process.argv[3];

  if (mode === "assert-application-run") {
    await waitForApplicationRunRegistration({
      repository: process.env.GITHUB_REPOSITORY,
      headSha: process.env.GITHUB_SHA,
      workflow: workflow || DEFAULT_APPLICATION_WORKFLOW,
      token: process.env.GITHUB_TOKEN,
      apiUrl: process.env.GITHUB_API_URL,
      timeoutSeconds: process.env.APP_DEPLOY_REGISTRATION_TIMEOUT_SECONDS,
      intervalSeconds: process.env.APP_DEPLOY_COORDINATION_INTERVAL_SECONDS,
    });
    return;
  }

  if (mode === "resolve-source-run") {
    const resolution = await resolveAgentSourceRun({
      repository: process.env.GITHUB_REPOSITORY,
      headSha: process.env.APP_DEPLOY_HEAD_SHA,
      headBranch: process.env.APP_DEPLOY_HEAD_BRANCH,
      applicationEvent: process.env.APP_DEPLOY_EVENT,
      applicationConclusion: process.env.APP_DEPLOY_CONCLUSION,
      sourceWorkflow: workflow,
      token: process.env.GITHUB_TOKEN,
      apiUrl: process.env.GITHUB_API_URL,
      timeoutSeconds: process.env.AGENT_SOURCE_WAIT_TIMEOUT_SECONDS,
      intervalSeconds: process.env.APP_DEPLOY_COORDINATION_INTERVAL_SECONDS,
    });
    await writeResolutionOutputs(process.env.GITHUB_OUTPUT, resolution);
    return;
  }

  throw new Error(
    "Modo inválido. Use assert-application-run ou resolve-source-run com o workflow correspondente.",
  );
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
