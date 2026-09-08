import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { selectMatchingRun, waitForAppDeployment } from "./wait-for-app-deployment.mjs";

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const matchingSha = "a".repeat(40);

function jobBlock(workflow, jobId) {
  const jobs = workflow.split(/^jobs:\s*$/m)[1] ?? "";
  return jobs.split(new RegExp(`^  ${jobId}:\\s*$`, "m"))[1]?.split(/^  [\w-]+:\s*$/m)[0];
}

function hasRemoteCommand(job) {
  return job.split(/^      - (?:name:|uses:)/m).some((step) => {
    const command = step.split(/^        run:[ \t]*/m)[1] ?? "";
    return /\b(?:ssh|scp|rsync)\b/.test(command);
  });
}

function assertApplicationGateOutsideHostQueue(workflow) {
  const workflowConfiguration = workflow.split(/^jobs:\s*$/m)[0];
  assert.doesNotMatch(
    workflowConfiguration,
    /^concurrency:\s*$/m,
    "a coordenação da aplicação não pode serializar o workflow inteiro",
  );

  const gate = jobBlock(workflow, "application-deployment");
  assert.ok(gate, "o gate de coordenação deve existir em job separado");
  assert.match(gate, /if: github\.event_name != 'pull_request' && github\.ref == 'refs\/heads\/main'/);
  assert.match(gate, /timeout-minutes: 45/);
  assert.match(
    gate,
    /group: application-deployment-\$\{\{ github\.workflow \}\}-\$\{\{ github\.ref \}\}\s+cancel-in-progress: true/,
    "um gate mais novo deve cancelar somente a espera obsoleta do mesmo workflow",
  );
  assert.doesNotMatch(gate, /group: deploy-vps-163-245-202-80/, "o gate não pode ocupar a fila do VPS");
  assert.equal(hasRemoteCommand(gate), false, "o gate não pode acessar o host dos agentes");
  assert.match(gate, /if: github\.event_name == 'push'/);
  assert.match(gate, /node scripts\/wait-for-app-deployment\.mjs deploy-containers\.yml/);

  const deploy = jobBlock(workflow, "deploy");
  assert.ok(deploy, "o job de deploy deve existir");
  assert.match(deploy, /^      - application-deployment$/m, "o deploy deve depender do gate externo");
  assert.match(deploy, /group: deploy-vps-163-245-202-80/);
  assert.doesNotMatch(
    deploy,
    /node scripts\/wait-for-app-deployment\.mjs/,
    "o deploy não pode esperar a aplicação enquanto retém a fila do VPS",
  );
  assert.equal(hasRemoteCommand(deploy), true, "o contrato deve verificar operações remotas reais");
}

function response(payload, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => payload,
  };
}

function run(overrides = {}) {
  let now = 0;
  return waitForAppDeployment({
    repository: "paulofor/marketing-hub",
    headSha: matchingSha,
    workflow: "deploy-containers.yml",
    token: "token-de-teste",
    timeoutSeconds: 10,
    intervalSeconds: 1,
    nowImpl: () => now,
    sleepImpl: async (milliseconds) => {
      now += milliseconds;
    },
    log: () => {},
    warn: () => {},
    ...overrides,
  });
}

test("seleciona o deploy mais recente de main, por push ou retomada manual, do mesmo commit", () => {
  const selected = selectMatchingRun(
    {
      workflow_runs: [
        { id: 1, head_sha: "b".repeat(40), head_branch: "main", event: "push", created_at: "2026-08-30T10:00:00Z" },
        { id: 2, head_sha: matchingSha, head_branch: "main", event: "workflow_dispatch", created_at: "2026-08-30T12:00:00Z" },
        { id: 3, head_sha: matchingSha, head_branch: "main", event: "push", created_at: "2026-08-30T11:00:00Z" },
        { id: 4, head_sha: matchingSha, head_branch: "feature/teste", event: "workflow_dispatch", created_at: "2026-08-30T13:00:00Z" },
        { id: 5, head_sha: matchingSha, head_branch: "main", event: "pull_request", created_at: "2026-08-30T14:00:00Z" },
        { id: 6, head_sha: matchingSha, event: "workflow_dispatch", created_at: "2026-08-30T15:00:00Z" },
      ],
    },
    matchingSha,
  );

  assert.equal(selected.id, 2);
});

test("aguarda visibilidade e conclusão saudável do deploy correspondente", async () => {
  const payloads = [
    { workflow_runs: [] },
    {
      workflow_runs: [
        {
          id: 10,
          head_sha: matchingSha,
          head_branch: "main",
          event: "push",
          status: "in_progress",
          conclusion: null,
          created_at: "2026-08-30T10:00:00Z",
          html_url: "https://github.com/paulofor/marketing-hub/actions/runs/10",
        },
      ],
    },
    {
      workflow_runs: [
        {
          id: 10,
          head_sha: matchingSha,
          head_branch: "main",
          event: "push",
          status: "completed",
          conclusion: "success",
          created_at: "2026-08-30T10:00:00Z",
          html_url: "https://github.com/paulofor/marketing-hub/actions/runs/10",
        },
      ],
    },
  ];
  let calls = 0;

  const result = await run({
    fetchImpl: async (url, options) => {
      assert.match(url, /head_sha=a{40}/);
      assert.equal(options.headers.authorization, "Bearer token-de-teste");
      return response(payloads[calls++]);
    },
  });

  assert.equal(result.id, 10);
  assert.equal(calls, 3);
});

test("repete erro transitório da API sem liberar o worker", async () => {
  let calls = 0;
  const result = await run({
    fetchImpl: async () => {
      calls += 1;
      if (calls === 1) return response({}, 500);
      return response({
        workflow_runs: [
          {
            id: 11,
            head_sha: matchingSha,
            head_branch: "main",
            event: "push",
            status: "completed",
            conclusion: "success",
            created_at: "2026-08-30T10:00:00Z",
          },
        ],
      });
    },
  });

  assert.equal(result.id, 11);
  assert.equal(calls, 2);
});

test("bloqueia imediatamente quando o deploy correspondente falha", async () => {
  await assert.rejects(
    run({
      fetchImpl: async () =>
        response({
          workflow_runs: [
            {
              id: 12,
              head_sha: matchingSha,
              head_branch: "main",
              event: "push",
              status: "completed",
              conclusion: "failure",
              created_at: "2026-08-30T10:00:00Z",
              html_url: "https://github.com/paulofor/marketing-hub/actions/runs/12",
            },
          ],
        }),
    }),
    /terminou com failure.*actions\/runs\/12/,
  );
});

test("bloqueia por timeout quando o deploy correspondente não aparece", async () => {
  await assert.rejects(
    run({ fetchImpl: async () => response({ workflow_runs: [] }), timeoutSeconds: 2 }),
    /Tempo esgotado.*worker não será publicado/,
  );
});

test("recuperação manual espera sucesso antes de liberar agentes e não filtra apenas push na API", async () => {
  let calls = 0;
  const result = await run({
    fetchImpl: async (url) => {
      const query = new URL(url).searchParams;
      assert.equal(query.get("head_sha"), matchingSha);
      assert.equal(query.get("branch"), "main");
      assert.equal(query.has("event"), false);
      calls += 1;
      return response({ workflow_runs: [
        {
          id: 20, head_sha: matchingSha, head_branch: "main", event: "push",
          status: "completed", conclusion: "failure", created_at: "2026-08-30T10:00:00Z",
        },
        {
          id: 21, head_sha: matchingSha, head_branch: "main", event: "workflow_dispatch",
          status: calls === 1 ? "in_progress" : "completed",
          conclusion: calls === 1 ? null : "success", created_at: "2026-08-30T11:00:00Z",
        },
      ] });
    },
  });
  assert.equal(result.id, 21);
  assert.equal(calls, 2);
});

test("retomada manual falha não reaproveita um sucesso anterior", async () => {
  await assert.rejects(run({ fetchImpl: async () => response({ workflow_runs: [
    {
      id: 20, head_sha: matchingSha, head_branch: "main", event: "push",
      status: "completed", conclusion: "success", created_at: "2026-08-30T10:00:00Z",
    },
    {
      id: 21, head_sha: matchingSha, head_branch: "main", event: "workflow_dispatch",
      status: "completed", conclusion: "failure", created_at: "2026-08-30T11:00:00Z",
    },
  ] }) }), /terminou com failure/);
});

test("sucesso de branch, tag, PR ou commit diferente não libera os agentes", async () => {
  const completed = {
    id: 22, head_sha: matchingSha, head_branch: "main", event: "workflow_dispatch",
    status: "completed", conclusion: "success", created_at: "2026-08-30T10:00:00Z",
  };
  await assert.rejects(run({
    timeoutSeconds: 2,
    fetchImpl: async () => response({ workflow_runs: [
      { ...completed, head_branch: "feature/teste" },
      { ...completed, head_branch: "v1.0.0" },
      { ...completed, head_branch: null },
      { ...completed, event: "pull_request" },
      { ...completed, head_sha: "b".repeat(40) },
    ] }),
  }), /Tempo esgotado.*worker não será publicado/);
});

test("aplicação permite retomada manual e restringe detecção e publicação a main", async () => {
  const workflow = await readFile(path.join(repositoryRoot, ".github/workflows/deploy-containers.yml"), "utf8");
  const triggers = workflow.split(/^on:\s*$/m)[1]?.split(/^\S/m)[0];
  assert.match(triggers, /^  workflow_dispatch:\s*$/m);
  assert.match(triggers, /^  push:\n    branches: \[main\]/m);
  const detector = workflow.split(/^  detect-changes:\s*$/m)[1]?.split(/^  [\w-]+:\s*$/m)[0];
  assert.match(detector, /^    if: github\.ref == 'refs\/heads\/main'$/m);
  const jobs = workflow.split(/^jobs:\s*$/m)[1].split(/^  [\w-]+:\s*$/m).slice(2);
  for (const job of jobs) {
    assert.match(job, /^    needs: .*\bdetect-changes\b/m);
    assert.match(job, /^    if: .*needs\.detect-changes\.outputs\.\w+ == 'true'/m);
  }
});

test("detecção real do workflow recupera módulos pendentes sem before no evento manual", async () => {
  const workflow = await readFile(path.join(repositoryRoot, ".github/workflows/deploy-containers.yml"), "utf8");
  const filter = workflow.split(/^      - id: filter\s*$/m)[1]?.split(/^      - (?:name|id|uses):/m)[0];
  const script = filter?.split(/^        run: \|\s*$/m)[1]?.replace(/^          /gm, "");
  assert.ok(script?.includes("scripts/detect-deployment-changes.sh"));
  const fixture = await mkdtemp(path.join(tmpdir(), "manual-deploy-contract-"));
  const git = (...args) => execFileSync("git", args, { cwd: fixture, encoding: "utf8" }).trim();
  try {
    for (const directory of ["scripts", "bin", "backend/ads-service", "frontend", "video-management-service"]) {
      await mkdir(path.join(fixture, directory), { recursive: true });
    }
    for (const name of ["detect-deployment-changes.sh", "read-frontend-build-revision.sh"]) {
      await writeFile(path.join(fixture, "scripts", name), await readFile(path.join(repositoryRoot, "scripts", name)));
    }
    await writeFile(path.join(fixture, "backend/ads-service/fixture.txt"), "base\n");
    await writeFile(path.join(fixture, "frontend/fixture.txt"), "base\n");
    await writeFile(path.join(fixture, "video-management-service/fixture.txt"), "base\n");
    git("init", "-q", "--initial-branch=main");
    git("config", "user.name", "Deploy Contract Test");
    git("config", "user.email", "teste@sandbox.local");
    git("add", ".");
    git("commit", "-qm", "fixture publicada");
    const deployed = git("rev-parse", "HEAD");
    await writeFile(path.join(fixture, "backend/ads-service/fixture.txt"), "backend pendente\n");
    await writeFile(path.join(fixture, "frontend/fixture.txt"), "frontend pendente\n");
    git("commit", "-qam", "fixture com merge perdido");
    await writeFile(path.join(fixture, "README.md"), "documentação posterior\n");
    git("add", "README.md");
    git("commit", "-qm", "fixture posterior sem código novo");
    await writeFile(path.join(fixture, "bin/ssh"), `#!/usr/bin/env bash
set -euo pipefail
case "$*" in
  *sandbox@app.sandbox.invalid*deployed-app-revision*) printf '%s\\n' "$TEST_DEPLOYED_REVISION" ;;
  *sandbox@app.sandbox.invalid*curl*) printf '{\\n  "commit": "%s"\\n}\\n' "$TEST_DEPLOYED_REVISION" ;;
  *) echo 'Acesso externo fora da fixture bloqueado' >&2; exit 1 ;;
esac
`, { mode: 0o755 });
    const output = path.join(fixture, "outputs");
    const result = spawnSync("bash", ["-c", script], {
      cwd: fixture,
      encoding: "utf8",
      timeout: 10000,
      env: {
        ...process.env,
        PATH: `${path.join(fixture, "bin")}:${process.env.PATH}`,
        EVENT_BEFORE: "", GITHUB_SHA: git("rev-parse", "HEAD"), GITHUB_OUTPUT: output,
        TEST_DEPLOYED_REVISION: deployed, VIDEO_SSH_READY: "false", SSH_OPTS: "",
        VPS_USER: "sandbox", APP_VPS_IP: "app.sandbox.invalid", DEPLOY_DIR: "/fixture",
      },
    });
    assert.equal(result.status, 0, result.stderr || result.error?.message);
    const values = Object.fromEntries((await readFile(output, "utf8")).trim().split("\n").map((line) => line.split("=")));
    assert.equal(values.backend, "true");
    assert.equal(values.frontend, "true");
    assert.equal(values.app_deploy, "true");
    assert.equal(values.video, "false");
    assert.equal(values.video_deploy, "false");
  } finally {
    await rm(fixture, { recursive: true, force: true });
  }
});

test("workflow da Psique espera a aplicação e reporta a revisão imutável", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/customer-agent-worker-ci.yml"),
    "utf8",
  );

  assert.match(workflow, /permissions:\n  contents: read\n  actions: read/);
  assert.match(workflow, /GITHUB_TOKEN: \$\{\{ github\.token \}\}/);
  assert.match(workflow, /node scripts\/wait-for-app-deployment\.mjs deploy-containers\.yml/);
  assert.match(workflow, /AGENT_BUILD_REFERENCE='\$\{GITHUB_SHA\}'/);
  assertApplicationGateOutsideHostQueue(workflow);
});

test("workflow de Íris espera a aplicação antes de alterar o host do agente", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/meta-ad-approver-worker-ci.yml"),
    "utf8",
  );

  assert.match(workflow, /permissions:\n  contents: read\n  actions: read/);
  assert.match(workflow, /run: node --test scripts\/wait-for-app-deployment\.test\.mjs/);
  assert.match(workflow, /GITHUB_TOKEN: \$\{\{ github\.token \}\}/);
  assert.match(workflow, /node scripts\/wait-for-app-deployment\.mjs deploy-containers\.yml/);

  assertApplicationGateOutsideHostQueue(workflow);
});

test("workflow de Argos espera a aplicação antes de acessar o VPS", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/product-discovery-worker-ci.yml"),
    "utf8",
  );
  assertApplicationGateOutsideHostQueue(workflow);
});

test("renomear etapas preserva o contrato de coordenação", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/meta-ad-approver-worker-ci.yml"),
    "utf8",
  );
  assertApplicationGateOutsideHostQueue(workflow.replace(/^      - name:.*$/gm, "      - name: Etapa ssh renomeada"));
});

test("fila do host, workflow inteiro ou acesso remoto não podem envolver o gate de espera", () => {
  const gate = `  application-deployment:
    if: github.event_name != 'pull_request' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    timeout-minutes: 45
    concurrency:
      group: application-deployment-\${{ github.workflow }}-\${{ github.ref }}
      cancel-in-progress: true
    steps:
      - name: Aguardar aplicação
        if: github.event_name == 'push'
        run: node scripts/wait-for-app-deployment.mjs deploy-containers.yml
`;
  const deploy = `  deploy:
    needs:
      - application-deployment
    concurrency:
      group: deploy-vps-163-245-202-80
      queue: max
      cancel-in-progress: false
    steps:
      - name: Operação remota
        run: ssh destino-de-teste
`;
  const workflow = (...jobs) => `jobs:\n${jobs.join("")}`;

  assert.doesNotThrow(() => assertApplicationGateOutsideHostQueue(workflow(gate, deploy)));
  assert.throws(() => assertApplicationGateOutsideHostQueue(workflow(deploy)), /job separado/);
  assert.throws(
    () => assertApplicationGateOutsideHostQueue(workflow(gate.replace(
      "group: application-deployment-${{ github.workflow }}-${{ github.ref }}",
      "group: deploy-vps-163-245-202-80",
    ), deploy)),
    /gate mais novo|fila do VPS/,
  );
  assert.throws(
    () => assertApplicationGateOutsideHostQueue(workflow(gate, deploy.replace("      - application-deployment\n", ""))),
    /depender do gate externo/,
  );
  assert.throws(
    () => assertApplicationGateOutsideHostQueue(`concurrency:\n  group: workflow-inteiro\n${workflow(gate, deploy)}`),
    /workflow inteiro/,
  );
  assert.throws(
    () => assertApplicationGateOutsideHostQueue(workflow(gate.replace(
      "run: node scripts/wait-for-app-deployment.mjs deploy-containers.yml",
      "run: ssh destino-de-teste && node scripts/wait-for-app-deployment.mjs deploy-containers.yml",
    ), deploy)),
    /não pode acessar o host/,
  );
});

test("CI central acompanha e executa o contrato de coordenação", async () => {
  const ci = await readFile(path.join(repositoryRoot, ".github/workflows/github-actions-contracts.yml"), "utf8");
  for (const event of ["push", "pull_request"]) {
    const configuration = ci.split(`  ${event}:`)[1]?.split(/^  \w+:/m)[0];
    for (const script of ["wait-for-app-deployment.mjs", "wait-for-app-deployment.test.mjs"]) {
      assert.ok(configuration?.includes(`scripts/${script}`), `${event} deve acompanhar ${script}`);
    }
  }
  assert.match(ci, /run: node --test scripts\/wait-for-app-deployment\.test\.mjs/);
});
