import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const frontendDirectory = path.resolve(scriptDirectory, "..");
const outputPath = path.join(
  frontendDirectory,
  "public-mira-commercial/media/mira-commercial-control-v2.png",
);
const port = 57182;
const baseUrl = `http://127.0.0.1:${port}`;
const viteExecutable = path.join(
  frontendDirectory,
  "node_modules/vite/bin/vite.js",
);
const chromiumExecutable =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.CHROMIUM_BIN ||
  process.env.CHROME_BIN ||
  process.env.PUPPETEER_EXECUTABLE_PATH ||
  "/usr/bin/chromium";

const readySession = {
  experienceVersion: "mira-commercial-v1",
  status: "READY",
  objective: "Organizar os cuidados que já tenho",
  products: [
    { name: "Sabonete suave", labelDirections: "Limpar e enxaguar" },
    {
      name: "Hidratante diário",
      labelDirections: "Aplicar após a limpeza",
    },
  ],
  routine: [
    {
      productName: "Sabonete suave",
      order: 10,
      documentedDirection: "Limpar e enxaguar",
      safetyNote:
        "Ordem limitada ao texto documentado informado; não é prescrição.",
    },
    {
      productName: "Hidratante diário",
      order: 20,
      documentedDirection: "Aplicar após a limpeza",
      safetyNote:
        "Ordem limitada ao texto documentado informado; não é prescrição.",
    },
  ],
  blocker: null,
  attemptsUsed: 0,
  attemptsLimit: 2,
  events: ["VALUE_MOMENT", "READY_RESULT_USED"],
  generatedAt: "2026-09-28T00:00:00Z",
  completedAt: null,
};

const server = spawn(
  process.execPath,
  [
    viteExecutable,
    "--config",
    "vite.mira-commercial.config.ts",
    "--host",
    "127.0.0.1",
    "--port",
    String(port),
  ],
  {
    cwd: frontendDirectory,
    env: { ...process.env, NODE_ENV: "development" },
    stdio: ["ignore", "pipe", "pipe"],
  },
);

let serverError = "";
server.stderr.on("data", (chunk) => {
  serverError += chunk.toString();
});

try {
  await waitForServer();
  const browser = await chromium.launch({ executablePath: chromiumExecutable });
  try {
    const page = await browser.newPage({
      viewport: { width: 1080, height: 1350 },
      deviceScaleFactor: 1,
    });
    await page.route("**/api/pde/mira/commercial/v1/session", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(readySession),
      }),
    );
    await page.route("**/api/pde/access/events", (route) =>
      route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({ status: "RECORDED" }),
      }),
    );
    await page.goto(`${baseUrl}/?mh_test=1#access=static-control-capture`, {
      waitUntil: "networkidle",
    });
    await page.getByText("Sua rotina está pronta").waitFor();
    await page.addStyleTag({
      content: `
        html, body { width: 1080px; height: 1350px; overflow: hidden; }
        body { background: #f7f2fa; }
        .mira-commercial-shell {
          width: 1080px;
          height: 1350px;
          margin: 0;
          padding: 34px 54px;
          overflow: hidden;
        }
        .mira-commercial-header {
          position: absolute;
          inset: 48px 64px auto;
          z-index: 2;
          padding: 0;
        }
        .mira-commercial-header span { font-size: 1.2rem; }
        .mira-commercial-header strong { font-size: 1.28rem; }
        .mira-commercial-card {
          margin: 82px 0 0;
          padding: 54px;
          height: 1030px;
          border-radius: 32px;
          box-shadow: 0 20px 70px rgba(73, 42, 88, 0.11);
          display: flex;
          flex-direction: column;
          justify-content: center;
        }
        .mira-commercial-kicker { font-size: 1rem; }
        .mira-commercial-card h1 { font-size: 3.7rem; margin-bottom: 32px; }
        .mira-routine-list { gap: 24px; }
        .mira-routine-list li { grid-template-columns: 58px 1fr; gap: 20px; padding: 30px; }
        .mira-routine-list li > span { width: 54px; height: 54px; font-size: 1.25rem; }
        .mira-routine-list strong { font-size: 1.4rem; }
        .mira-routine-list p { font-size: 1.35rem; line-height: 1.35; }
        .mira-routine-list small { font-size: 1.05rem; line-height: 1.4; }
        .mira-commercial-card button, footer { display: none; }
      `,
    });
    await page.screenshot({ path: outputPath, fullPage: false });
  } finally {
    await browser.close();
  }
  const bytes = await readFile(outputPath);
  const sha256 = createHash("sha256").update(bytes).digest("hex");
  process.stdout.write(
    `Controle estático de Mira gerado: ${outputPath}\nSHA-256: ${sha256}\n`,
  );
} finally {
  server.kill("SIGTERM");
  await Promise.race([
    new Promise((resolve) => server.once("exit", resolve)),
    new Promise((resolve) => setTimeout(resolve, 5_000)),
  ]);
}

async function waitForServer() {
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline) {
    if (server.exitCode != null) {
      throw new Error(`Vite encerrou antes da captura. ${serverError}`);
    }
    try {
      const response = await fetch(baseUrl);
      if (response.ok) return;
    } catch {
      // O servidor ainda está iniciando.
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Vite não iniciou para a captura. ${serverError}`);
}
