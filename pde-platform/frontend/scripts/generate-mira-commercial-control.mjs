import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const frontendDirectory = path.resolve(scriptDirectory, "..");
const proofOutputPath = path.join(
  frontendDirectory,
  "public-mira-commercial/media/mira-commercial-product-proof-v1.png",
);
const controlOutputPath = path.join(
  frontendDirectory,
  "public-mira-commercial/media/mira-commercial-control-v4.png",
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
      viewport: { width: 1080, height: 1080 },
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
        html, body { width: 1080px; height: 1080px; overflow: hidden; }
        body { background: #f7f2fa; }
        .mira-commercial-shell {
          width: 1080px;
          height: 1080px;
          margin: 0;
          padding: 24px 54px;
          overflow: hidden;
        }
        .mira-commercial-header {
          position: absolute;
          inset: 30px 64px auto;
          z-index: 2;
          padding: 0;
        }
        .mira-commercial-header .mira-commercial-product-kind { font-size: 1.65rem; }
        .mira-commercial-header strong { font-size: 1.85rem; }
        .mira-commercial-header .mira-commercial-entitlement {
          max-width: 390px;
          font-size: 1.65rem;
          font-weight: 700;
          line-height: 1.2;
        }
        .mira-commercial-card {
          margin: 70px 0 0;
          padding: 42px 48px;
          height: 900px;
          border-radius: 32px;
          box-shadow: 0 20px 70px rgba(73, 42, 88, 0.11);
          display: flex;
          flex-direction: column;
          justify-content: center;
        }
        .mira-commercial-kicker { font-size: 1.45rem; }
        .mira-commercial-card h1 { font-size: 2.8rem; margin-bottom: 24px; }
        .mira-routine-list { gap: 18px; }
        .mira-routine-list li { grid-template-columns: 52px 1fr; gap: 18px; padding: 22px; }
        .mira-routine-list li > span { width: 48px; height: 48px; font-size: 1.15rem; }
        .mira-routine-list strong { font-size: 1.75rem; }
        .mira-routine-list p { font-size: 1.55rem; line-height: 1.3; }
        .mira-routine-list small { display: none; }
        .mira-commercial-safety-summary {
          margin-top: 20px;
          border-radius: 18px;
          background: #f7f2fa;
          padding: 18px 24px;
          font-size: 1.55rem;
          font-weight: 650;
          line-height: 1.35;
        }
        .mira-commercial-card button, footer { display: none; }
      `,
    });
    await page.screenshot({ path: proofOutputPath, fullPage: false });

    const proofBytes = await readFile(proofOutputPath);
    const proofDataUrl = `data:image/png;base64,${proofBytes.toString("base64")}`;
    await page.setViewportSize({ width: 1080, height: 1350 });
    await page.setContent(`
      <!doctype html>
      <html lang="pt-BR">
        <head>
          <meta charset="utf-8" />
          <style>
            * { box-sizing: border-box; }
            html, body { width: 1080px; height: 1350px; margin: 0; overflow: hidden; }
            body {
              background: linear-gradient(145deg, #f9f2fa 0%, #ead9ee 100%);
              color: #392343;
              font-family: Arial, sans-serif;
              padding: 54px 70px;
            }
            .kind {
              color: #6b3e7d;
              font-size: 27px;
              font-weight: 800;
              letter-spacing: 0.12em;
              margin: 0 0 14px;
              text-transform: uppercase;
            }
            h1 {
              font-family: Georgia, serif;
              font-size: 57px;
              line-height: 1.04;
              margin: 0 0 28px;
              max-width: 900px;
            }
            .proof {
              background: #fff;
              border: 10px solid #fff;
              border-radius: 30px;
              box-shadow: 0 24px 70px rgba(57, 35, 67, 0.19);
              display: block;
              height: 760px;
              margin: 0 auto 30px;
              object-fit: cover;
              width: 760px;
            }
            .right {
              color: #493153;
              font-size: 31px;
              font-weight: 750;
              line-height: 1.28;
              margin: 0 0 22px;
              text-align: center;
            }
            .cta {
              background: #6b3e7d;
              border-radius: 999px;
              color: #fff;
              font-size: 31px;
              font-weight: 900;
              letter-spacing: 0.02em;
              margin: 0 auto;
              padding: 23px 36px;
              text-align: center;
              width: 760px;
            }
          </style>
        </head>
        <body>
          <p class="kind">Mira · aplicação web</p>
          <h1>Uma ordem simples com o que você já tem.</h1>
          <img class="proof" src="${proofDataUrl}" alt="Captura da aplicação paga de Mira" />
          <p class="right">2 organizações individualizadas no total · cada uma usa 1 das 2 tentativas</p>
          <div class="cta">ORGANIZAR MINHA ROTINA · R$ 49</div>
        </body>
      </html>
    `);
    await page.locator(".proof").waitFor();
    await page.screenshot({ path: controlOutputPath, fullPage: false });
  } finally {
    await browser.close();
  }
  const proofBytes = await readFile(proofOutputPath);
  const controlBytes = await readFile(controlOutputPath);
  const proofSha256 = createHash("sha256").update(proofBytes).digest("hex");
  const controlSha256 = createHash("sha256").update(controlBytes).digest("hex");
  process.stdout.write(
    `Prova de produto de Mira gerada: ${proofOutputPath}\nSHA-256: ${proofSha256}\n` +
      `Controle estático de Mira gerado: ${controlOutputPath}\nSHA-256: ${controlSha256}\n`,
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
