import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { createRequire } from "node:module";
import { copyFile, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { basename, join, resolve } from "node:path";

const require = createRequire(import.meta.url);
const { chromium } = require("@playwright/test");

const [contractFile, proofDirectory, outputDirectory, storyboardFile] =
  process.argv.slice(2);
if (!contractFile || !proofDirectory || !outputDirectory || !storyboardFile) {
  throw new Error(
    "Uso: node generate-assets.mjs <contrato> <provas> <saida> <storyboard-apolo>",
  );
}

const contract = JSON.parse(await readFile(resolve(contractFile), "utf8"));
const storyboard = JSON.parse(await readFile(resolve(storyboardFile), "utf8"));
const output = resolve(outputDirectory);
const work = join(output, ".work");
await mkdir(work, { recursive: true });

const proofFiles = Object.fromEntries(
  await Promise.all(
    contract.sourceProofs.map(async (proof) => {
      const bytes = await readFile(join(resolve(proofDirectory), proof.file));
      return [proof.file, `data:image/png;base64,${bytes.toString("base64")}`];
    }),
  ),
);

const browser = await chromium.launch({
  headless: true,
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ??
    process.env.CHROMIUM_BIN ??
    process.env.CHROME_BIN,
});
const page = await browser.newPage({ deviceScaleFactor: 1 });

const escapeHtml = (value) =>
  String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");

const baseStyles = (width, height) => `
  * { box-sizing: border-box; }
  html, body { margin: 0; width: ${width}px; height: ${height}px; overflow: hidden; }
  body { font-family: Inter, "Segoe UI", Arial, sans-serif; background: #061522; color: #f8fbff; }
  #creative { position: relative; width: ${width}px; height: ${height}px; overflow: hidden;
    background: radial-gradient(circle at 82% 4%, rgba(46, 208, 188, .27), transparent 31%),
                radial-gradient(circle at 5% 93%, rgba(255, 174, 101, .20), transparent 36%),
                linear-gradient(155deg, #071828 0%, #0b2940 52%, #071a2c 100%); }
  #creative::before { content: ""; position: absolute; inset: 22px; border: 1px solid rgba(255,255,255,.12); border-radius: 40px; pointer-events: none; }
  .brand { color: #74e8d8; text-transform: uppercase; letter-spacing: .17em; font-weight: 800; font-size: 28px; }
  h1 { margin: 0; font-size: 66px; line-height: 1.02; letter-spacing: -.045em; }
  h2 { margin: 0; font-size: 56px; line-height: 1.05; letter-spacing: -.04em; }
  p { margin: 0; }
  .eyebrow { color: #ffbd80; font-size: 25px; font-weight: 800; text-transform: uppercase; letter-spacing: .11em; }
  .body { color: #dbe9f4; font-size: 31px; line-height: 1.3; }
  .pill { display: inline-flex; align-items: center; padding: 13px 20px; border-radius: 999px; background: rgba(116,232,216,.12); border: 1px solid rgba(116,232,216,.42); color: #d9fff8; font-size: 23px; font-weight: 700; }
  .proof { overflow: hidden; border-radius: 28px; border: 2px solid rgba(116,232,216,.55); background: #f7fbff; box-shadow: 0 28px 70px rgba(0,0,0,.30); }
  .proof img { width: 100%; height: 100%; object-fit: contain; object-position: center; display: block; }
  .price { font-size: 46px; font-weight: 900; color: #fff; }
  .cta { background: #ffad67; color: #102131; border-radius: 22px; padding: 22px 28px; font-size: 29px; font-weight: 900; text-align: center; box-shadow: 0 16px 40px rgba(255,173,103,.23); }
  .fine { color: #b8cddd; font-size: 21px; line-height: 1.35; }
  .step { color: #74e8d8; font-weight: 900; font-size: 22px; letter-spacing: .08em; }
`;

async function render(width, height, markup, file) {
  await page.setViewportSize({ width, height });
  await page.setContent(
    `<!doctype html><html><head><meta charset="utf-8"><style>${baseStyles(width, height)}</style></head><body>${markup}</body></html>`,
    { waitUntil: "load" },
  );
  await page
    .locator("#creative")
    .screenshot({ path: file, animations: "disabled" });
}

const copy = Object.fromEntries(
  Object.entries(contract.copy).map(([key, value]) => [key, escapeHtml(value)]),
);
const responseProof = proofFiles["rigel-tasting-response.png"];
const questionProof = proofFiles["rigel-tasting-question.png"];
const followupsProof = proofFiles["rigel-tasting-followups.png"];
const offerProof = proofFiles["rigel-offer-proof.png"];
const proofImage = (source, label, height, compact = false) => `
  <div class="proof" style="height:${height}px;border-radius:${compact ? 14 : 22}px">
    <img src="${source}" alt="${label} da interface real em exemplo fictício" style="object-fit:contain">
  </div>`;
const proofCrop = (source, label, height, objectPosition) => `
  <div class="proof" style="height:${height}px;position:relative">
    <img src="${source}" alt="${label}" style="object-fit:cover;object-position:${objectPosition}">
  </div>`;
const directCard = join(output, "rigel-direct-card-1080x1350.png");
const directCards = [
  {
    file: directCard,
    markup: `<main id="creative" style="padding:60px 64px;display:flex;flex-direction:column;justify-content:space-between">
      <header><div class="brand">Kit WhatsApp Pronto · 1/6</div><div class="eyebrow" style="margin-top:48px">Atendimento com intenção</div><h1 style="font-size:74px;margin-top:16px">${copy.hook}</h1></header>
      <section><div class="pill" style="font-size:34px">Implantação personalizada e assistida</div><p class="body" style="font-size:38px;font-weight:700;margin-top:28px">${copy.mechanism}</p><p class="body" style="font-size:34px;margin-top:24px">Veja nos próximos cards uma resposta, uma pergunta e três follow-ups reais da amostra.</p></section>
      <footer><div class="price" style="font-size:52px">R$ 349 · pagamento único, sem recorrência</div><p class="fine" style="font-size:34px;font-weight:700;margin-top:15px">${copy.delivery}</p><p class="fine" style="font-size:34px;font-weight:700;margin-top:12px">${copy.control}</p><div class="cta" style="font-size:36px;margin-top:24px">${copy.cta}</div></footer>
    </main>`,
  },
  {
    file: join(output, "rigel-direct-response-1080x1350.png"),
    markup: `<main id="creative" style="padding:62px 66px;display:flex;flex-direction:column;justify-content:space-between">
      <header><div class="brand">Kit WhatsApp Pronto · 2/6</div><div class="eyebrow" style="margin-top:45px">Uma resposta para revisar</div><h1 style="font-size:69px;margin-top:15px">Pare de começar cada conversa do zero</h1></header>
      <section><div class="pill" style="font-size:32px;margin-bottom:22px">Interface real · exemplo fictício · resposta completa</div>${proofImage(responseProof, "Resposta inicial completa", 500)}</section>
      <footer><p class="body" style="font-size:38px;font-weight:800">Implantação personalizada e assistida</p><p class="fine" style="font-size:33px;font-weight:700;margin-top:12px">Você revisa antes de usar · sem bot ou envio automático</p></footer>
    </main>`,
  },
  {
    file: join(output, "rigel-direct-question-1080x1350.png"),
    markup: `<main id="creative" style="padding:62px 66px;display:flex;flex-direction:column;justify-content:space-between">
      <header><div class="brand">Kit WhatsApp Pronto · 3/6</div><div class="eyebrow" style="margin-top:45px">Uma pergunta para qualificar</div><h1 style="font-size:69px;margin-top:15px">Faça a conversa avançar sem adivinhar</h1></header>
      <section><div class="pill" style="font-size:32px;margin-bottom:22px">Interface real · exemplo fictício · pergunta completa</div>${proofImage(questionProof, "Pergunta de qualificação completa", 390)}</section>
      <footer><p class="body" style="font-size:38px;font-weight:800">5 a 10 perguntas preparadas para o seu contexto</p><p class="fine" style="font-size:33px;font-weight:700;margin-top:12px">Você escolhe, revisa e envia manualmente</p></footer>
    </main>`,
  },
  {
    file: join(output, "rigel-direct-followups-1080x1350.png"),
    markup: `<main id="creative" style="padding:52px 62px;display:flex;flex-direction:column;justify-content:space-between">
      <header><div class="brand">Kit WhatsApp Pronto · 4/6</div><div class="eyebrow" style="margin-top:30px">Três follow-ups manuais</div><h1 style="font-size:62px;margin-top:12px">Continue sem insistência</h1></header>
      <section><div class="pill" style="font-size:31px;margin-bottom:18px">Interface real · exemplo fictício · três follow-ups completos</div>${proofImage(followupsProof, "Três follow-ups completos", 785)}</section>
      <footer><p class="fine" style="font-size:32px;font-weight:800">Sem bot · sem disparo em massa · sem envio automático</p></footer>
    </main>`,
  },
  {
    file: join(output, "rigel-direct-offer-1080x1350.png"),
    markup: `<main id="creative" style="padding:42px 58px;display:grid;grid-template-rows:auto 1fr;gap:18px">
      <header style="display:flex;justify-content:space-between;align-items:center"><div class="brand">Oferta real · 5/6</div><div class="pill" style="font-size:29px">Preço e CTA · parte 1 de 2</div></header>
      <section>${proofCrop(offerProof, "Captura real da oferta: preço e CTA", 1185, "center 16%")}</section>
    </main>`,
  },
  {
    file: join(output, "rigel-direct-conditions-1080x1350.png"),
    markup: `<main id="creative" style="padding:42px 58px;display:grid;grid-template-rows:auto 1fr;gap:18px">
      <header style="display:flex;justify-content:space-between;align-items:center"><div class="brand">Oferta real · 6/6</div><div class="pill" style="font-size:29px">Condições e políticas · parte 2 de 2</div></header>
      <section>${proofCrop(offerProof, "Captura real da oferta: condições e políticas", 1185, "center bottom")}</section>
    </main>`,
  },
];
for (const card of directCards) {
  await render(1080, 1350, card.markup, card.file);
}
const channelPreviewsDirectory = join(output, "channel-previews");
await mkdir(channelPreviewsDirectory, { recursive: true });
const directPreviewNames = [
  "rigel-direct-card-preview-360x450.png",
  "rigel-direct-response-preview-360x450.png",
  "rigel-direct-question-preview-360x450.png",
  "rigel-direct-followups-preview-360x450.png",
  "rigel-direct-offer-preview-360x450.png",
  "rigel-direct-conditions-preview-360x450.png",
];
for (let index = 0; index < directCards.length; index += 1) {
  execFileSync(
    "ffmpeg",
    [
      "-loglevel",
      "error",
      "-y",
      "-i",
      directCards[index].file,
      "-vf",
      "scale=360:450",
      "-frames:v",
      "1",
      join(channelPreviewsDirectory, directPreviewNames[index]),
    ],
    { stdio: "inherit" },
  );
}

const cuts = storyboard.cuts;
if (!Array.isArray(cuts) || cuts.length !== 5) {
  throw new Error(
    "Apolo deve entregar exatamente cinco cortes para a demonstracao local",
  );
}
const requiredCutDurations = [4, 3, 12, 5, 6];
if (
  cuts.reduce((sum, cut) => sum + cut.durationSeconds, 0) !== 30 ||
  cuts.some((cut, index) => cut.durationSeconds !== requiredCutDurations[index])
) {
  throw new Error(
    "O storyboard de Apolo deve usar [4, 3, 12, 5, 6] e totalizar 30 segundos",
  );
}

const slideMarkup = [
  `<main id="creative" style="padding:78px 68px;display:grid;grid-template-rows:auto auto auto 1fr auto;gap:28px">
    <div><div class="brand">Kit WhatsApp Pronto</div><h1 style="font-size:72px;margin-top:24px">${copy.hook}</h1></div>
    <section><div class="eyebrow" style="font-size:35px">Demonstração</div><h2 style="font-size:53px;margin-top:10px">1 resposta · 1 pergunta · 3 follow-ups</h2></section>
    <div class="pill" style="width:max-content;font-size:34px">Interface real · exemplo fictício</div>
    <section>${proofImage(responseProof, "Resposta inicial", 480)}</section>
    <div><p class="body" style="font-size:39px;font-weight:800">Implantação personalizada e assistida</p><p class="fine" style="font-size:36px;font-weight:700;margin-top:15px">Você revisa antes de usar · sem bot ou envio automático</p><div class="step" style="margin-top:24px">1 / 5 · ${escapeHtml(cuts[0].commercialRole)}</div></div>
  </main>`,
  `<main id="creative" style="padding:90px 74px;display:grid;grid-template-rows:auto auto 1fr auto;gap:34px">
    <div class="brand">A pergunta que faz a conversa avançar</div><h2 style="font-size:54px">Qualificação sem pressão e sem adivinhar</h2>
    <section><div class="pill" style="margin-bottom:15px">Interface real · exemplo fictício · pergunta da amostra</div>${proofImage(questionProof, "Pergunta de qualificação", 410)}</section>
    <div class="step">2 / 5 · ${escapeHtml(cuts[1].commercialRole)} · PROVA LEGÍVEL</div>
  </main>`,
  `<main id="creative" style="padding:90px 74px;display:grid;grid-template-rows:auto auto auto 1fr auto;gap:34px">
    <div class="brand">Continuidade sem insistência</div><h2 style="font-size:55px">Três follow-ups manuais para você revisar</h2>
    <div class="pill" style="width:max-content">Sem bot · sem envio automático</div>
    <section><div class="pill" style="margin-bottom:15px">Interface real · exemplo fictício · follow-ups da amostra</div>${proofImage(followupsProof, "Três follow-ups", 610)}</section>
    <div class="step">3 / 5 · ${escapeHtml(cuts[2].commercialRole)} · SEM AUTOMAÇÃO</div>
  </main>`,
  `<main id="creative" style="padding:70px 68px;display:grid;grid-template-rows:auto 1fr auto;gap:28px">
    <div><div class="brand">Oferta real · parte 1 de 2</div><h2 style="font-size:58px;margin-top:22px">R$ 349 · pagamento único, sem recorrência</h2></div>
    <section>${proofCrop(offerProof, "Captura real da oferta: preço e CTA", 1190, "center 16%")}</section>
    <div class="step">4 / 5 · ${escapeHtml(cuts[3].commercialRole)} · PREÇO E CTA LEGÍVEIS</div>
  </main>`,
  `<main id="creative" style="padding:70px 68px;display:grid;grid-template-rows:auto 1fr auto;gap:26px">
    <div><div class="brand">Oferta real · parte 2 de 2</div><h2 style="font-size:55px;margin-top:22px">Briefing, prazo, condições e políticas</h2></div>
    <section>${proofCrop(offerProof, "Captura real da oferta: condições e políticas", 1110, "center bottom")}</section>
    <div><div class="cta" style="font-size:40px;padding:28px">${copy.cta}</div><div class="step" style="margin-top:24px">5 / 5 · SEM PUBLICAÇÃO AUTOMÁTICA</div></div>
  </main>`,
];

const slides = [];
const reviewFramesDirectory = join(output, "review-frames");
await mkdir(reviewFramesDirectory, { recursive: true });
for (let index = 0; index < slideMarkup.length; index += 1) {
  const file = join(work, `slide-${index + 1}.png`);
  await render(1080, 1920, slideMarkup[index], file);
  slides.push(file);
  await copyFile(
    file,
    join(reviewFramesDirectory, `rigel-video-frame-${index + 1}.png`),
  );
  execFileSync(
    "ffmpeg",
    [
      "-loglevel",
      "error",
      "-y",
      "-i",
      file,
      "-vf",
      "scale=360:640",
      "-frames:v",
      "1",
      join(
        channelPreviewsDirectory,
        `rigel-video-frame-${index + 1}-preview-360x640.png`,
      ),
    ],
    { stdio: "inherit" },
  );
}
await browser.close();

const segments = [];
for (let index = 0; index < slides.length; index += 1) {
  const segment = join(work, `segment-${index + 1}.mp4`);
  execFileSync(
    "ffmpeg",
    [
      "-loglevel",
      "error",
      "-y",
      "-loop",
      "1",
      "-framerate",
      "30",
      "-i",
      slides[index],
      "-t",
      String(cuts[index].durationSeconds),
      "-vf",
      `scale=1080:1920,format=yuv420p,fade=t=in:st=0:d=0.18,fade=t=out:st=${Math.max(0, cuts[index].durationSeconds - 0.18)}:d=0.18`,
      "-c:v",
      "libx264",
      "-preset",
      "medium",
      "-crf",
      "19",
      "-an",
      segment,
    ],
    { stdio: "inherit" },
  );
  segments.push(segment);
}
const concatFile = join(work, "segments.ffconcat");
await writeFile(
  concatFile,
  `ffconcat version 1.0\n${segments.map((file) => `file '${file.replaceAll("'", "'\\''")}'`).join("\n")}\n`,
);
const video = join(output, "rigel-vertical-demo-1080x1920.mp4");
execFileSync(
  "ffmpeg",
  [
    "-loglevel",
    "error",
    "-y",
    "-f",
    "concat",
    "-safe",
    "0",
    "-i",
    concatFile,
    "-c",
    "copy",
    "-movflags",
    "+faststart",
    video,
  ],
  { stdio: "inherit" },
);

const sha256 = async (file) =>
  createHash("sha256")
    .update(await readFile(file))
    .digest("hex");
const probe = JSON.parse(
  execFileSync(
    "ffprobe",
    [
      "-v",
      "error",
      "-select_streams",
      "v:0",
      "-show_entries",
      "stream=codec_name,width,height,pix_fmt:format=duration,size",
      "-of",
      "json",
      video,
    ],
    { encoding: "utf8" },
  ),
);
const manifest = {
  contractVersion: contract.contractVersion,
  product: contract.product.internalName,
  producer: "TEMIS_LOCAL_DETERMINISTIC_COMPOSITOR",
  producerExecutionId: "temis-local-compositor-rigel-v1",
  apolloPlanSha256: await sha256(resolve(storyboardFile)),
  sourceProofs: await Promise.all(
    contract.sourceProofs.map(async (proof) => ({
      file: proof.file,
      purpose: proof.purpose,
      origin: proof.origin,
      rightsStatement: proof.rightsStatement,
      approvalStatus: "APPROVED_FOR_LOCAL_QA",
      approvalEvidence: "temis-creative-direction.json",
      sha256: await sha256(join(resolve(proofDirectory), proof.file)),
    })),
  ),
  formats: [
    {
      id: "direct-carousel",
      role: "FIRST_CONSENTED_CONTACT_SEQUENCE",
      assetFiles: directCards.map((card) => basename(card.file)),
    },
    {
      id: "vertical-demo",
      role: "OPTIONAL_PRODUCT_DEMONSTRATION",
      assetFiles: [basename(video)],
    },
  ],
  assets: [
    ...(await Promise.all(
      directCards.map(async (card) => ({
        file: basename(card.file),
        mediaType: "IMAGE",
        purposes: ["ADS", "SOCIAL"],
        width: 1080,
        height: 1350,
        sha256: await sha256(card.file),
      })),
    )),
    {
      file: basename(video),
      mediaType: "VIDEO",
      purposes: ["ADS", "SOCIAL"],
      width: probe.streams[0].width,
      height: probe.streams[0].height,
      durationSeconds: Number(probe.format.duration),
      codec: probe.streams[0].codec_name,
      pixelFormat: probe.streams[0].pix_fmt,
      bytes: Number(probe.format.size),
      sha256: await sha256(video),
    },
  ],
  reviewFrames: await Promise.all(
    slides.map(async (_, index) => {
      const file = join(
        reviewFramesDirectory,
        `rigel-video-frame-${index + 1}.png`,
      );
      return { file: basename(file), sha256: await sha256(file) };
    }),
  ),
  channelPreviews: await Promise.all(
    [
      ...directPreviewNames,
      ...Array.from(
        { length: 5 },
        (_, index) => `rigel-video-frame-${index + 1}-preview-360x640.png`,
      ),
    ].map(async (file) => ({
      file,
      sha256: await sha256(join(channelPreviewsDirectory, file)),
    })),
  ),
  destinationEvidence: {
    scope: "LOCAL_QA",
    url: contract.product.destination,
    product: contract.product.commercialName,
    priceBrl: contract.product.priceBrl,
    payment: contract.product.payment,
    delivery: contract.product.delivery,
    cta: contract.product.primaryCta,
    screenshots: await Promise.all(
      ["rigel-destination-desktop.png", "rigel-destination-mobile.png"].map(
        async (file) => ({
          file,
          sha256: await sha256(join(resolve(proofDirectory), file)),
        }),
      ),
    ),
  },
  executionSeparation: {
    producerExecutionId: "temis-local-compositor-rigel-v1",
    reviewerRequirement: "NEW_INDEPENDENT_CODEX_EXECUTION",
  },
  mobileReadabilityContract: {
    directCarouselProof: [
      "resposta completa",
      "pergunta completa",
      "três follow-ups completos",
      "oferta completa em duas partes sobrepostas",
    ],
    firstFrame:
      "Demonstração: 1 resposta, 1 pergunta e 3 follow-ups · interface real · exemplo fictício",
    deliveryMode: "Implantação personalizada e assistida",
    manualControl: "Você revisa antes de usar · sem bot ou envio automático",
    payment: contract.copy.price,
    deadline: contract.copy.delivery,
  },
  externalMediaProviderCalled: false,
  externalMediaCostUsd: 0,
  published: false,
};
await writeFile(
  join(output, "rigel-creative-manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
);
await rm(work, { recursive: true, force: true });
process.stdout.write(`${JSON.stringify(manifest, null, 2)}\n`);
