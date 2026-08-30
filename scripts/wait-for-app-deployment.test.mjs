import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { selectMatchingRun, waitForAppDeployment } from "./wait-for-app-deployment.mjs";

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const matchingSha = "a".repeat(40);

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

test("seleciona somente o deploy push do mesmo commit", () => {
  const selected = selectMatchingRun(
    {
      workflow_runs: [
        { id: 1, head_sha: "b".repeat(40), event: "push", created_at: "2026-08-30T10:00:00Z" },
        { id: 2, head_sha: matchingSha, event: "workflow_dispatch", created_at: "2026-08-30T12:00:00Z" },
        { id: 3, head_sha: matchingSha, event: "push", created_at: "2026-08-30T11:00:00Z" },
      ],
    },
    matchingSha,
  );

  assert.equal(selected.id, 3);
});

test("aguarda visibilidade e conclusão saudável do deploy correspondente", async () => {
  const payloads = [
    { workflow_runs: [] },
    {
      workflow_runs: [
        {
          id: 10,
          head_sha: matchingSha,
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

test("workflow da Psique espera a aplicação e reporta a revisão imutável", async () => {
  const workflow = await readFile(
    path.join(repositoryRoot, ".github/workflows/customer-agent-worker-ci.yml"),
    "utf8",
  );

  assert.match(workflow, /permissions:\n  contents: read\n  actions: read/);
  assert.match(workflow, /name: Wait for matching application deployment\n        if: github\.event_name == 'push'/);
  assert.match(workflow, /GITHUB_TOKEN: \$\{\{ github\.token \}\}/);
  assert.match(workflow, /node scripts\/wait-for-app-deployment\.mjs deploy-containers\.yml/);
  assert.match(workflow, /AGENT_BUILD_REFERENCE='\$\{GITHUB_SHA\}'/);
});
