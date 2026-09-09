import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import {
  resolveAgentSourceRun,
  selectMatchingRun,
  waitForApplicationRunRegistration,
  writeResolutionOutputs,
} from "./coordinate-agent-deployment.mjs";

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const matchingSha = "a".repeat(40);

function response(payload, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => payload,
  };
}

function applicationRun(overrides = {}) {
  return {
    id: 10,
    head_sha: matchingSha,
    head_branch: "main",
    event: "push",
    status: "pending",
    conclusion: null,
    created_at: "2026-09-09T10:00:00Z",
    html_url: "https://github.com/paulofor/marketing-hub/actions/runs/10",
    ...overrides,
  };
}

function sourceRun(overrides = {}) {
  return applicationRun({
    id: 20,
    status: "completed",
    conclusion: "success",
    html_url: "https://github.com/paulofor/marketing-hub/actions/runs/20",
    ...overrides,
  });
}

function registration(overrides = {}) {
  let now = 0;
  return waitForApplicationRunRegistration({
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

function resolution(overrides = {}) {
  let now = 0;
  return resolveAgentSourceRun({
    repository: "paulofor/marketing-hub",
    headSha: matchingSha,
    headBranch: "main",
    applicationEvent: "push",
    applicationConclusion: "success",
    sourceWorkflow: "product-discovery-worker-ci.yml",
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

function assertEventDrivenCoordination(workflow, sourceWorkflow, artifactBased) {
  const configuration = workflow.split(/^jobs:\s*$/m)[0];
  assert.match(
    configuration,
    /workflow_run:\s+workflows: \["Build & Deploy containers"\]\s+types: \[completed\]\s+branches: \[main\]/,
  );
  assert.doesNotMatch(workflow, /^  application-deployment:\s*$/m);
  assert.doesNotMatch(workflow, /wait-for-app-deployment\.mjs/);

  const testJob = jobBlock(workflow, "test");
  assert.ok(testJob, "o job de teste deve existir");
  assert.match(testJob, /if: github\.event_name != 'workflow_run'/);
  assert.match(
    testJob,
    /if: github\.event_name == 'push' && github\.ref == 'refs\/heads\/main'/,
    "o push em main deve confirmar que a coordenação central foi registrada",
  );
  assert.match(
    testJob,
    /coordinate-agent-deployment\.mjs assert-application-run deploy-containers\.yml/,
  );

  const source = jobBlock(workflow, "source-run");
  assert.ok(source, "a continuação deve resolver a execução testada de origem");
  assert.match(source, /if: github\.event_name == 'workflow_run'/);
  assert.match(
    source,
    new RegExp(`coordinate-agent-deployment\\.mjs resolve-source-run ${sourceWorkflow.replaceAll(".", "\\.")}`),
  );
  assert.match(source, /APP_DEPLOY_HEAD_SHA: \$\{\{ github\.event\.workflow_run\.head_sha \}\}/);
  assert.match(source, /APP_DEPLOY_CONCLUSION: \$\{\{ github\.event\.workflow_run\.conclusion \}\}/);
  assert.equal(hasRemoteCommand(source), false, "a resolução não pode tocar no VPS");

  const deploy = jobBlock(workflow, "deploy");
  assert.ok(deploy, "o job remoto deve existir");
  assert.match(deploy, /^      - source-run$/m);
  assert.match(deploy, /github\.event_name == 'workflow_run'/);
  assert.match(deploy, /needs\.source-run\.outputs\.required == 'true'/);
  assert.match(deploy, /DEPLOY_SOURCE_SHA:/);
  assert.match(deploy, /needs\.source-run\.outputs\.source_sha/);
  assert.match(deploy, /ref: \$\{\{ env\.DEPLOY_SOURCE_SHA \}\}/);
  assert.match(deploy, /group: deploy-vps-163-245-202-80\s+queue: max\s+cancel-in-progress: false/);
  assert.doesNotMatch(deploy, /marketing-hub\/[^\s"']+:\$\{\{ github\.sha \}\}/);
  assert.doesNotMatch(deploy, /\$\{GITHUB_SHA\}/);
  assert.equal(hasRemoteCommand(deploy), true, "somente a continuação aprovada pode acessar o VPS");

  if (artifactBased) {
    assert.match(deploy, /github-token: \$\{\{ github\.token \}\}/);
    assert.match(deploy, /run-id:.*needs\.source-run\.outputs\.source_run_id/);
    assert.match(deploy, /name: agent-images-\$\{\{ env\.DEPLOY_SOURCE_SHA \}\}/);
  }
}

test("seleciona somente a execução mais recente do SHA, branch e evento permitidos", () => {
  const selected = selectMatchingRun(
    {
      workflow_runs: [
        sourceRun({ id: 1, head_sha: "b".repeat(40) }),
        sourceRun({ id: 2, created_at: "2026-09-09T11:00:00Z" }),
        sourceRun({ id: 3, created_at: "2026-09-09T12:00:00Z", event: "workflow_run" }),
        sourceRun({ id: 4, created_at: "2026-09-09T13:00:00Z", head_branch: "feature" }),
        sourceRun({ id: 5, created_at: "2026-09-09T14:00:00Z" }),
      ],
    },
    matchingSha,
    new Set(["push"]),
  );

  assert.equal(selected.id, 5);
});

test("confirma o registro do deploy central sem aguardar sua fila terminar", async () => {
  let calls = 0;
  const run = await registration({
    fetchImpl: async (url, options) => {
      assert.match(url, /deploy-containers\.yml\/runs\?/);
      assert.match(url, /head_sha=a{40}/);
      assert.equal(options.headers.authorization, "Bearer token-de-teste");
      calls += 1;
      return response({ workflow_runs: calls === 1 ? [] : [applicationRun()] });
    },
  });

  assert.equal(run.id, 10);
  assert.equal(run.status, "pending");
  assert.equal(calls, 2);
});

test("aceita retomada manual do deploy central para o mesmo SHA", async () => {
  const run = await registration({
    fetchImpl: async () =>
      response({ workflow_runs: [applicationRun({ event: "workflow_dispatch" })] }),
  });
  assert.equal(run.event, "workflow_dispatch");
});

test("registro ausente bloqueia rapidamente sem ocupar a fila do VPS", async () => {
  await assert.rejects(
    registration({
      fetchImpl: async () => response({ workflow_runs: [] }),
      timeoutSeconds: 2,
    }),
    /não foi registrado.*agente não será publicado/,
  );
});

test("falha transitória da API é repetida, mas erro de autorização é terminal", async () => {
  let calls = 0;
  const run = await registration({
    fetchImpl: async () => {
      calls += 1;
      return calls === 1
        ? response({}, 500)
        : response({ workflow_runs: [applicationRun()] });
    },
  });
  assert.equal(run.id, 10);
  assert.equal(calls, 2);

  await assert.rejects(
    registration({ fetchImpl: async () => response({}, 403) }),
    /GitHub recusou.*HTTP 403/,
  );
});

test("continuação não aplicável termina sem publicar quando não houve workflow do agente", async () => {
  const result = await resolution({
    fetchImpl: async (url) => {
      const query = new URL(url).searchParams;
      assert.equal(query.get("head_sha"), matchingSha);
      assert.equal(query.get("branch"), "main");
      assert.equal(query.get("event"), "push");
      return response({ workflow_runs: [] });
    },
  });

  assert.equal(result.required, false);
  assert.equal(result.run, null);
});

test("deploy central verde libera somente a execução testada do mesmo SHA", async () => {
  const result = await resolution({
    fetchImpl: async () =>
      response({
        workflow_runs: [
          sourceRun({ id: 19, head_sha: "b".repeat(40) }),
          sourceRun({ id: 20 }),
        ],
      }),
  });

  assert.equal(result.required, true);
  assert.equal(result.run.id, 20);
  assert.equal(result.run.head_sha, matchingSha);
});

test("aguarda o teste do agente depois que a aplicação termina", async () => {
  let calls = 0;
  const result = await resolution({
    fetchImpl: async () => {
      calls += 1;
      return response({
        workflow_runs: [
          sourceRun({
            status: calls === 1 ? "in_progress" : "completed",
            conclusion: calls === 1 ? null : "success",
          }),
        ],
      });
    },
  });

  assert.equal(result.required, true);
  assert.equal(calls, 2);
});

test("cancelamento da execução de origem encerra a continuação sem publicar", async () => {
  const warnings = [];
  const result = await resolution({
    fetchImpl: async () =>
      response({ workflow_runs: [sourceRun({ conclusion: "cancelled" })] }),
    warn: (message) => warnings.push(message),
  });

  assert.equal(result.required, false);
  assert.equal(result.run, null);
  assert.equal(warnings.length, 1);
  assert.match(warnings[0], /foi cancelada.*sem publicar.*versão anterior preservada/);
  assert.match(warnings[0], /actions\/runs\/20/);
});

test("falha da aplicação ou do teste preserva a versão anterior do agente", async () => {
  await assert.rejects(
    resolution({
      applicationConclusion: "failure",
      fetchImpl: async () => response({ workflow_runs: [sourceRun()] }),
    }),
    /Deploy da aplicação .* terminou com failure.*versão anterior/,
  );

  await assert.rejects(
    resolution({
      fetchImpl: async () =>
        response({ workflow_runs: [sourceRun({ conclusion: "failure" })] }),
    }),
    /Execução testada do agente .* terminou com failure/,
  );
});

test("branch, evento e SHA inválidos não liberam a continuação", async () => {
  await assert.rejects(resolution({ headBranch: "feature" }), /fora de main/);
  await assert.rejects(resolution({ applicationEvent: "pull_request" }), /não autorizado/);
  await assert.rejects(resolution({ headSha: "curto" }), /SHA completo/);
});

test("timeout do teste do agente é independente do tempo passado na fila central", async () => {
  await assert.rejects(
    resolution({
      timeoutSeconds: 2,
      fetchImpl: async () =>
        response({ workflow_runs: [sourceRun({ status: "in_progress", conclusion: null })] }),
    }),
    /Tempo esgotado aguardando a execução testada/,
  );
});

test("publica no GITHUB_OUTPUT a proveniência exata ou a ausência de trabalho", async () => {
  const directory = await mkdtemp(path.join(tmpdir(), "agent-deploy-output-"));
  try {
    const requiredOutput = path.join(directory, "required");
    await writeResolutionOutputs(requiredOutput, { required: true, run: sourceRun() });
    assert.equal(
      await readFile(requiredOutput, "utf8"),
      `required=true\nsource_run_id=20\nsource_sha=${matchingSha}\n`,
    );

    const skippedOutput = path.join(directory, "skipped");
    await writeResolutionOutputs(skippedOutput, { required: false, run: null });
    assert.equal(
      await readFile(skippedOutput, "utf8"),
      "required=false\nsource_run_id=\nsource_sha=\n",
    );
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("aplicação permite retomada manual e restringe publicação a main", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/deploy-containers.yml"),
    "utf8",
  );
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

test("detecção central recupera módulos pendentes em uma retomada manual", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/deploy-containers.yml"),
    "utf8",
  );
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
      await writeFile(
        path.join(fixture, "scripts", name),
        await readFile(path.join(repositoryRoot, "scripts", name)),
      );
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
    await writeFile(
      path.join(fixture, "bin/ssh"),
      `#!/usr/bin/env bash
set -euo pipefail
case "$*" in
  *sandbox@app.sandbox.invalid*deployed-app-revision*) printf '%s\\n' "$TEST_DEPLOYED_REVISION" ;;
  *sandbox@app.sandbox.invalid*curl*) printf '{\\n  "commit": "%s"\\n}\\n' "$TEST_DEPLOYED_REVISION" ;;
  *) echo 'Acesso externo fora da fixture bloqueado' >&2; exit 1 ;;
esac
`,
      { mode: 0o755 },
    );
    const output = path.join(fixture, "outputs");
    const result = spawnSync("bash", ["-c", script], {
      cwd: fixture,
      encoding: "utf8",
      timeout: 10000,
      env: {
        ...process.env,
        PATH: `${path.join(fixture, "bin")}:${process.env.PATH}`,
        EVENT_BEFORE: "",
        GITHUB_SHA: git("rev-parse", "HEAD"),
        GITHUB_OUTPUT: output,
        TEST_DEPLOYED_REVISION: deployed,
        VIDEO_SSH_READY: "false",
        SSH_OPTS: "",
        VPS_USER: "sandbox",
        APP_VPS_IP: "app.sandbox.invalid",
        DEPLOY_DIR: "/fixture",
      },
    });
    assert.equal(result.status, 0, result.stderr || result.error?.message);
    const values = Object.fromEntries(
      (await readFile(output, "utf8"))
        .trim()
        .split("\n")
        .map((line) => line.split("=")),
    );
    assert.equal(values.backend, "true");
    assert.equal(values.frontend, "true");
    assert.equal(values.app_deploy, "true");
    assert.equal(values.video, "false");
    assert.equal(values.video_deploy, "false");
  } finally {
    await rm(fixture, { recursive: true, force: true });
  }
});

test("Argos, Psique e Íris retomam por evento sem polling nem troca de revisão", async () => {
  const productDiscovery = await readFile(
    path.join(repositoryRoot, ".github/workflows/product-discovery-worker-ci.yml"),
    "utf8",
  );
  const customer = await readFile(
    path.join(repositoryRoot, ".github/workflows/customer-agent-worker-ci.yml"),
    "utf8",
  );
  const metaApprover = await readFile(
    path.join(repositoryRoot, ".github/workflows/meta-ad-approver-worker-ci.yml"),
    "utf8",
  );

  assertEventDrivenCoordination(productDiscovery, "product-discovery-worker-ci.yml", false);
  assertEventDrivenCoordination(customer, "customer-agent-worker-ci.yml", true);
  assertEventDrivenCoordination(metaApprover, "meta-ad-approver-worker-ci.yml", true);
});

test("CI central acompanha e executa o contrato de coordenação por evento", async () => {
  const ci = await readFile(
    path.join(repositoryRoot, ".github/workflows/github-actions-contracts.yml"),
    "utf8",
  );
  for (const event of ["push", "pull_request"]) {
    const configuration = ci.split(`  ${event}:`)[1]?.split(/^  \w+:/m)[0];
    for (const script of [
      "coordinate-agent-deployment.mjs",
      "coordinate-agent-deployment.test.mjs",
    ]) {
      assert.ok(configuration?.includes(`scripts/${script}`), `${event} deve acompanhar ${script}`);
    }
  }
  assert.match(ci, /run: node --test scripts\/coordinate-agent-deployment\.test\.mjs/);
});
