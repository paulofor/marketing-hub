import { pathToFileURL } from "node:url";

const DEFAULT_WORKFLOW = "deploy-containers.yml";
const DEFAULT_TIMEOUT_SECONDS = 2400;
const DEFAULT_INTERVAL_SECONDS = 15;

function positiveInteger(value, fallback, name) {
  const parsed = value === undefined ? fallback : Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${name} deve ser um inteiro positivo.`);
  }
  return parsed;
}

export function selectMatchingRun(payload, headSha) {
  const runs = Array.isArray(payload?.workflow_runs) ? payload.workflow_runs : [];
  return runs
    .filter((run) => run?.head_sha === headSha && run?.event === "push")
    .sort((left, right) => String(right.created_at ?? "").localeCompare(String(left.created_at ?? "")))[0];
}

export async function waitForAppDeployment({
  repository,
  headSha,
  workflow = DEFAULT_WORKFLOW,
  token,
  apiUrl = "https://api.github.com",
  timeoutSeconds = DEFAULT_TIMEOUT_SECONDS,
  intervalSeconds = DEFAULT_INTERVAL_SECONDS,
  fetchImpl = globalThis.fetch,
  sleepImpl = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)),
  nowImpl = Date.now,
  log = console.log,
  warn = console.warn,
}) {
  if (!repository || !headSha || !token) {
    throw new Error("GITHUB_REPOSITORY, GITHUB_SHA e GITHUB_TOKEN são obrigatórios.");
  }

  const timeout = positiveInteger(timeoutSeconds, DEFAULT_TIMEOUT_SECONDS, "APP_DEPLOY_WAIT_TIMEOUT_SECONDS");
  const interval = positiveInteger(intervalSeconds, DEFAULT_INTERVAL_SECONDS, "APP_DEPLOY_WAIT_INTERVAL_SECONDS");
  const deadline = nowImpl() + timeout * 1000;
  const repositoryPath = repository.split("/").map(encodeURIComponent).join("/");
  const workflowPath = encodeURIComponent(workflow);
  const query = new URLSearchParams({ head_sha: headSha, event: "push", per_page: "20" });
  const endpoint = `${apiUrl.replace(/\/$/, "")}/repos/${repositoryPath}/actions/workflows/${workflowPath}/runs?${query}`;
  let lastState = "";

  while (nowImpl() < deadline) {
    try {
      const response = await fetchImpl(endpoint, {
        headers: {
          accept: "application/vnd.github+json",
          authorization: `Bearer ${token}`,
          "x-github-api-version": "2022-11-28",
        },
      });

      if (response.status === 401 || response.status === 403 || response.status === 404) {
        throw new Error(`GitHub recusou a consulta do workflow ${workflow}: HTTP ${response.status}.`);
      }
      if (!response.ok) {
        warn(`Consulta transitória do deploy da aplicação falhou com HTTP ${response.status}; nova tentativa será feita.`);
      } else {
        const run = selectMatchingRun(await response.json(), headSha);
        if (run) {
          const state = `${run.id}:${run.status}:${run.conclusion ?? ""}`;
          if (state !== lastState) {
            log(
              `Deploy da aplicação para ${headSha.slice(0, 12)}: status=${run.status} conclusao=${run.conclusion ?? "pendente"} ${run.html_url ?? ""}`.trim(),
            );
            lastState = state;
          }

          if (run.status === "completed") {
            if (run.conclusion === "success") {
              return run;
            }
            throw new Error(
              `Deploy da aplicação para o mesmo commit terminou com ${run.conclusion ?? "conclusão desconhecida"}: ${run.html_url ?? "URL indisponível"}.`,
            );
          }
        } else if (lastState !== "not-found") {
          log(`Aguardando o workflow ${workflow} aparecer para ${headSha.slice(0, 12)}.`);
          lastState = "not-found";
        }
      }
    } catch (error) {
      if (/HTTP (401|403|404)\./.test(error.message) || /terminou com/.test(error.message)) {
        throw error;
      }
      warn(`Consulta transitória do deploy da aplicação falhou: ${error.message}`);
    }

    await sleepImpl(interval * 1000);
  }

  throw new Error(
    `Tempo esgotado aguardando ${workflow} para ${headSha} após ${timeout} segundos; o worker não será publicado sem o backend correspondente.`,
  );
}

async function main() {
  await waitForAppDeployment({
    repository: process.env.GITHUB_REPOSITORY,
    headSha: process.env.GITHUB_SHA,
    workflow: process.argv[2] || DEFAULT_WORKFLOW,
    token: process.env.GITHUB_TOKEN,
    apiUrl: process.env.GITHUB_API_URL,
    timeoutSeconds: process.env.APP_DEPLOY_WAIT_TIMEOUT_SECONDS,
    intervalSeconds: process.env.APP_DEPLOY_WAIT_INTERVAL_SECONDS,
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
