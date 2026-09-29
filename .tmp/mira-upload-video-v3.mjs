import path from "node:path";
import { fileURLToPath } from "node:url";
import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const repository = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const videoPath = path.join(
  repository,
  "pde-platform/frontend/public-mira-commercial/media/mira-commercial-demo-v3.mp4",
);
const baseUrl = "http://191.252.181.168:5173";
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  page.setDefaultTimeout(20_000);
  await page.goto(`${baseUrl}/experiments/93?tab=video`, {
    waitUntil: "networkidle",
    timeout: 45_000,
  });

  const uploadSection = page.locator("section").filter({
    has: page.getByRole("heading", { name: "Anexar vídeo vertical finalizado" }),
  });
  await uploadSection.waitFor({ state: "visible" });
  await uploadSection.getByLabel("Arquivo MP4").setInputFiles(videoPath);
  await uploadSection.getByText(/1080×1920 · 15s/).waitFor();
  await uploadSection
    .getByLabel("Objetivo do teste")
    .fill("Comparar a demonstração real em vídeo com o controle estático, mantendo oferta, público, CTA e destino.");
  await uploadSection
    .getByLabel("Métrica primária")
    .fill("Compras líquidas e contribuição após mídia (R$)");
  await uploadSection.getByLabel("Roteiro exibido no vídeo").fill(
    "Recorte de 0–8,42s da narração já aprovada no vídeo #47. Overlays: aplicação web Mira; organização do que a pessoa já possui; entrada dos produtos; captura real da rotina; limites explícitos; duas organizações individualizadas no total por R$ 49, cada uma consumindo uma das duas tentativas; CTA Organizar minha rotina.",
  );
  await uploadSection
    .getByLabel("Chave dos ativos visuais de origem")
    .fill("mira-commercial-v1-proof320-control-v4-video-v3");
  await uploadSection
    .getByLabel("IDs dos vídeos aprovados usados como fonte")
    .fill("48");
  await uploadSection.getByLabel("Evidência dos ativos usados").fill(
    "Sucessor corretivo versionado do vídeo #48 no commit de09bc8. A imagem sintética foi substituída pela prova real aprovada #320 (SHA-256 4ffda62d502d8ea644fa06a0cd6cf0768c39c4278b43bdcefa3665e9d578c2f3); somente a faixa aprovada de #47 entre 0 e 8,42s foi reutilizada como áudio. MP4 SHA-256 0ba5720d4e3ec6beeed4ee76430a6be47b681a36eca9dfefe349d3a66aee3377.",
  );
  await uploadSection.getByLabel("Referência versionada da produção").fill(
    "pde-platform/frontend/scripts/generate-mira-commercial-video.sh",
  );
  await uploadSection
    .getByLabel("HLS público do vídeo")
    .fill(
      "https://mira.digicomdigital.com.br/media/mira-commercial-demo-v3-hls/index.m3u8",
    );
  await uploadSection
    .getByLabel("Confirmei a reprodução e o arquivo possui áudio utilizável.")
    .check();

  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes("/api/experiments/93/video-assets/ad-uploads") &&
      response.request().method() === "POST",
  );
  await uploadSection
    .getByRole("button", { name: "Enviar para revisão do experimento" })
    .click();
  const uploadResponse = await uploadResponsePromise;
  if (!uploadResponse.ok()) {
    throw new Error(
      `Upload falhou: HTTP ${uploadResponse.status()} ${await uploadResponse.text()}`,
    );
  }
  const uploaded = await uploadResponse.json();

  const row = page.locator("table tbody tr").filter({
    has: page.locator("td").first().getByText(String(uploaded.id), { exact: true }),
  });
  await row.waitFor({ state: "visible" });
  const reviewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/api/experiments/93/video-assets/${uploaded.id}`) &&
      response.request().method() === "PATCH",
  );
  await row.getByRole("button", { name: "Aprovar", exact: true }).click();
  const reviewResponse = await reviewResponsePromise;
  if (!reviewResponse.ok()) {
    throw new Error(
      `Aprovação falhou: HTTP ${reviewResponse.status()} ${await reviewResponse.text()}`,
    );
  }
  const approved = await reviewResponse.json();
  await row.getByText("APPROVED", { exact: true }).waitFor();
  await row.screenshot({ path: "/tmp/mira-video-v3-approved-row.png" });
  process.stdout.write(`${JSON.stringify({ uploaded, approved }, null, 2)}\n`);
} finally {
  await browser.close();
}
