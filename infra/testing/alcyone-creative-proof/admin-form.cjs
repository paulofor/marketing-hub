const { chromium, devices } = require("playwright");
const { spawn } = require("node:child_process");
const path = require("node:path");
const fs = require("node:fs");
const assert = require("node:assert/strict");

// Executa somente fixtures HTTP locais; nenhuma tarefa ou chamada paga chega ao backend real.
const root = path.resolve(__dirname, "../../..");
const base = "http://127.0.0.1:58173";
const output = path.join(root, "artifacts/alcyone-admin-form");
fs.mkdirSync(output, { recursive: true });
const server = spawn(
  process.execPath,
  [
    path.join(root, "frontend/node_modules/vite/bin/vite.js"),
    "--host",
    "127.0.0.1",
    "--port",
    "58173",
    "--strictPort",
  ],
  {
    cwd: path.join(root, "frontend"),
    detached: true,
    env: { ...process.env, VITE_API_URL: base },
    stdio: ["ignore", "pipe", "pipe"],
  },
);
let serverLog = "";
for (const stream of [server.stdout, server.stderr])
  stream.on("data", (bytes) => {
    serverLog = (serverLog + bytes).slice(-8000);
  });

(async () => {
  let browser;
  try {
    let ready = false;
    for (let index = 0; index < 60; index++) {
      try {
        if ((await fetch(base)).ok) {
          ready = true;
          break;
        }
      } catch {}
      await new Promise((resolve) => setTimeout(resolve, 500));
    }
    assert.ok(ready, serverLog);
    browser = await chromium.launch({
      executablePath: "/usr/bin/chromium",
      args: ["--no-sandbox"],
    });
    const profiles = [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", { ...devices["iPhone 15 Pro"] }],
      ["pixel", { ...devices["Pixel 7"] }],
    ];
    for (const [index, [name, profile]] of profiles.entries()) {
      const experiment = String(90105 + index * 113);
      const context = await browser.newContext(profile);
      const page = await context.newPage();
      const reads = [],
        writes = [],
        pageErrors = [];
      let failFirstWrite = name === "pixel";
      page.on("pageerror", (error) => pageErrors.push(error.message));
      await page.route("**/api/**", async (route) => {
        const request = route.request(),
          url = new URL(request.url()),
          endpoint = url.pathname;
        if (!endpoint.startsWith("/api/")) return route.continue();
        const respond = (body, status = 200) =>
          route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify(body),
          });
        if (request.method() === "POST") {
          assert.equal(endpoint, "/api/agent-tasks");
          const payload = request.postDataJSON();
          writes.push(payload);
          if (failFirstWrite) {
            failFirstWrite = false;
            return respond({ message: "Falha sintética recuperável" }, 503);
          }
          return respond(
            { id: 910700 + index, title: payload.title, status: "PENDING" },
            201,
          );
        }
        reads.push(endpoint);
        if (endpoint === "/api/agents")
          return respond([
            {
              id: 2,
              nickname: "Psique",
              name: "Revisão de experiência",
              agentKey: "customer-agent",
              status: "TEST",
            },
          ]);
        if (endpoint === "/api/business-processes")
          return respond([
            {
              id: 117,
              processCode: "pde-construction-approval",
              name: "Construção",
              status: "PUBLISHED",
              versionNumber: 11,
              diagram: {
                nodes: [
                  {
                    id: "technicalHomologation",
                    type: "TASK",
                    owner: "Psique",
                    label: "Homologar",
                  },
                ],
              },
            },
          ]);
        if (endpoint === "/api/experiments/" + experiment)
          return respond({
            id: experiment,
            name: "Experimento sintético",
            productId: 95119 + index,
            status: "PLANNED",
          });
        if (endpoint.startsWith("/api/experiments/"))
          return respond({ message: "Ausente na fixture" }, 404);
        if (
          endpoint === "/api/experiments" ||
          endpoint.includes("/agent-tasks/agents/")
        )
          return respond(
            { message: "A coleção completa não participa desta criação" },
            503,
          );
        return respond([]);
      });
      await page.goto(base + "/agents/2?newTask=1&experimentId=" + experiment, {
        waitUntil: "domcontentloaded",
      });
      await page
        .getByText("#" + experiment + " · Experimento sintético", {
          exact: true,
        })
        .waitFor({ timeout: 60000 })
        .catch(async error => {
          await page.screenshot({ path: path.join(output, name + "-failure.png"), fullPage: true });
          console.log(JSON.stringify({ reads, pageErrors, body: (await page.locator("body").innerText()).slice(0, 6000) }));
          throw error;
        });
      await page.getByLabel("Número do experimento *").fill("99999");
      await page
        .getByRole("alert")
        .filter({ hasText: "Não foi possível consultar" })
        .waitFor();
      assert.ok(
        await page
          .getByRole("button", { name: "Enviar para Psique" })
          .isDisabled(),
      );
      assert.equal(writes.length, 0);
      await page.getByLabel("Número do experimento *").fill(experiment);
      await page
        .getByText("#" + experiment + " · Experimento sintético", {
          exact: true,
        })
        .waitFor();
      await page.getByLabel("Processo publicado *").selectOption("117");
      await page
        .getByLabel("Atividade do processo *")
        .selectOption("technicalHomologation");
      await page
        .getByLabel(
          "Captura complementar para vídeo, preservando a homologação aceita",
        )
        .check();
      await page.getByLabel("Tarefa *").fill("Captura funcional sintética");
      await page
        .getByLabel("Resultado esperado *")
        .fill(
          "Capturar entrada e resultado da mesma versão, com dados sintéticos e efeitos externos nulos.",
        );
      await page.getByRole("button", { name: "Enviar para Psique" }).click();
      if (name === "pixel") {
        await page
          .getByRole("alert")
          .filter({ hasText: "Os campos foram preservados" })
          .waitFor();
        assert.equal(
          await page.getByLabel("Tarefa *").inputValue(),
          "Captura funcional sintética",
        );
        await page.getByRole("button", { name: "Enviar para Psique" }).click();
      }
      await page
        .getByRole("status")
        .filter({ hasText: "Solicitação #" + (910700 + index) + " registrada" })
        .waitFor();
      assert.equal(writes.length, name === "pixel" ? 2 : 1);
      for (const payload of writes) {
        assert.equal(
          payload.sourceReference,
          "experiment:" + experiment + "@video-proof-v1",
        );
        assert.equal(payload.assignedAgentKey, "customer-agent");
        assert.equal(payload.processDefinitionId, 117);
        assert.equal(payload.processActivityId, "technicalHomologation");
      }
      assert.ok(!reads.includes("/api/experiments"));
      assert.ok(
        !reads.some((endpoint) => endpoint.includes("/agent-tasks/agents/")),
      );
      assert.equal(
        await page.getByText("Caixa de entrada", { exact: true }).count(),
        0,
      );
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth + 1,
        ),
        false,
      );
      assert.deepEqual(pageErrors, []);
      await page.screenshot({
        path: path.join(output, name + ".png"),
        fullPage: true,
      });
      console.log(
        JSON.stringify({
          profile: name,
          experiment,
          status: "PASS",
          fullCollections: 0,
          writes: writes.length,
        }),
      );
      await context.close();
    }
  } finally {
    if (browser) await browser.close();
    try {
      process.kill(-server.pid, "SIGTERM");
    } catch {}
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
