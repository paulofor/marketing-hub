/** Compõe exemplos e vídeo de Capella com fontes verificadas, sem consumo de APIs pagas. */
import { createRequire } from "node:module";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const { chromium } = require("playwright");
const configDir = join(
  dirname(fileURLToPath(import.meta.url)),
  "capella-demo-v1",
);
const [sourceDir, outputDir, voiceModel] = process.argv.slice(2);
if (!sourceDir || !outputDir || !voiceModel)
  throw new Error("Uso: node script.mjs fontes saída voz.onnx");
const brief = JSON.parse(await readFile(join(configDir, "brief.json")));
const sources = JSON.parse(await readFile(join(configDir, "sources.json")));
const digest = (buffer) => createHash("sha256").update(buffer).digest("hex");
const escapeHtml = (s) =>
  String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll('"', "&quot;");
const run = (command, args, options = {}) =>
  execFileSync(command, args, { stdio: "pipe", ...options });
await mkdir(outputDir, { recursive: true });
for (const source of sources) {
  if (
    digest(await readFile(join(sourceDir, `approved-${source.id}.png`))) !==
    source.sha256
  )
    throw new Error(
      `Fonte divergente #${source.id}; não compor nem gerar locução.`,
    );
}
const voiceSha256 = digest(await readFile(voiceModel));
if (
  voiceSha256 !==
  "858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7"
)
  throw new Error(
    "Modelo de voz divergente; use a versão verificada antes de produzir.",
  );
const story = await readFile(join(sourceDir, "creative-523.png"));
if (
  digest(story) !==
  "99cee8b3e5ab0cd94fec66feca1069c6aa3aeb37df6c6053944dbc04ce2303fa"
)
  throw new Error("Criativo aprovado #523 divergente.");
const dataImage = async (path) =>
  `data:image/png;base64,${(await readFile(path)).toString("base64")}`;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const page = await browser.newPage({
  viewport: { width: 1080, height: 1080 },
  deviceScaleFactor: 1,
});
const files = [];
const photoFiles = [];
try {
  for (const [index, ex] of brief.examples.entries()) {
    const [x, y, width, height] = ex.crop;
    const crop = join(outputDir, `${ex.id}-photo.png`);
    run("ffmpeg", [
      "-v",
      "error",
      "-y",
      "-i",
      join(sourceDir, `approved-${ex.sourceId}.png`),
      "-vf",
      `crop=${width}:${height}:${x}:${y}`,
      "-frames:v",
      "1",
      crop,
    ]);
    photoFiles.push(await dataImage(crop));
    const split = ex.layout === "split";
    const html = `<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>
      *{box-sizing:border-box}body{margin:0;width:1080px;height:1080px;background:${ex.background};color:${ex.accent};font-family:'DejaVu Sans',sans-serif;overflow:hidden}
      .brand{position:absolute;top:52px;left:62px;font-size:44px;font-weight:700;letter-spacing:-1px}.city{position:absolute;top:110px;left:64px;font-size:25px;letter-spacing:2px}
      .photo{position:absolute;${split ? "left:552px;top:178px;width:464px;height:716px" : "left:62px;top:178px;width:956px;height:446px"};object-fit:cover;border-radius:${split ? "220px 220px 28px 28px" : "28px"}}
      .copy{position:absolute;left:62px;top:${split ? "235px" : "648px"};width:${split ? "448px" : "956px"}}
      .eyebrow{font-size:22px;font-weight:700;letter-spacing:2px;margin-bottom:18px}.title{font-family:'DejaVu Serif',serif;font-size:${split ? "68px" : "58px"};font-weight:400;line-height:1.08;white-space:pre-line;letter-spacing:-2px;margin:0}
      .body{white-space:pre-line;font-size:26px;line-height:1.35;margin-top:18px;color:#413b36}.cta{position:absolute;left:62px;right:62px;top:917px;background:${ex.accent};color:white;border-radius:18px;padding:24px 26px;font-size:27px;text-align:center;font-weight:700}
      .disclosure{position:absolute;bottom:21px;left:62px;font-size:22px;color:#554d48}
      .number{position:absolute;top:69px;right:64px;font-size:22px;letter-spacing:2px}
      </style><div class="brand">${escapeHtml(ex.brand)}</div><div class="city">${escapeHtml(ex.city)}</div><div class="number">${String(index + 1).padStart(2, "0")} / 06</div>
      <img class="photo" src="${photoFiles[index]}" alt="Foto ilustrativa de unhas"><div class="copy"><div class="eyebrow">${escapeHtml(ex.service)}</div><h1 class="title">${escapeHtml(ex.headline)}</h1><div class="body">${escapeHtml(ex.body)}</div></div><div class="cta">${escapeHtml(ex.cta)}</div><div class="disclosure">${escapeHtml(brief.disclosure)}</div></html>`;
    await page.setContent(html);
    await page.evaluate(() => document.fonts.ready);
    const readable = await page.evaluate(() => {
      const body = document.querySelector(".body").getBoundingClientRect();
      const cta = document.querySelector(".cta").getBoundingClientRect();
      const title = document.querySelector(".title");
      return (
        body.bottom <= cta.top - 12 &&
        title.scrollWidth <= title.clientWidth + 1
      );
    });
    if (!readable) throw new Error(`Texto sobreposto ou cortado em ${ex.id}.`);
    const path = join(outputDir, `${ex.id}.png`);
    await page.screenshot({ path });
    files.push({
      id: ex.id,
      path,
      sha256: digest(await readFile(path)),
      width: 1080,
      height: 1080,
      sourceId: ex.sourceId,
    });
    await writeFile(
      join(outputDir, `${ex.id}.txt`),
      `${ex.brand} · ${ex.city}\n${brief.disclosure}\n\nLEGENDA\n${ex.caption}\n\nMENSAGEM DE WHATSAPP\n${ex.whatsapp}\n\nNa entrega paga, substituir pelos dados reais do negócio. Nunca apresentar as imagens ilustrativas como trabalho realizado pela profissional.\n`,
    );
  }
  await page.setViewportSize({ width: 1080, height: 1920 });
  const segments = [];
  let cursor = 0;
  for (const [index, scene] of brief.scenes.entries()) {
    const voicePath = join(outputDir, `voice-${index}.wav`);
    run(
      "python3",
      [
        "-m",
        "piper",
        "-m",
        voiceModel,
        "--length-scale",
        "0.93",
        "-f",
        voicePath,
      ],
      { input: scene.speech },
    );
    const voiceDuration = Number(
      run("ffprobe", [
        "-v",
        "error",
        "-show_entries",
        "format=duration",
        "-of",
        "default=nw=1:nk=1",
        voicePath,
      ])
        .toString()
        .trim(),
    );
    const duration = Math.ceil((voiceDuration + 0.4) * 30) / 30;
    const example =
      scene.example !== undefined
        ? brief.examples[scene.example]
        : brief.examples[0];
    const post =
      scene.example !== undefined
        ? await dataImage(files[scene.example].path)
        : await dataImage(files[0].path);
    let content = `<img class="post" src="${post}" alt="Exemplo demonstrativo">`;
    if (scene.kind === "hook")
      content = `<div class="thought">“Sei fazer unhas.<br>O que eu publico?”</div><img class="mini" src="data:image/png;base64,${story.toString("base64")}" alt="Criativo aprovado de Capella #523"><div class="tag">Da dúvida à divulgação pronta para usar</div>`;
    if (scene.kind === "sequence")
      content = `<div class="sequence"><img src="${post}" alt="Amostra"><div><span class="label">LEGENDA PRONTA</span><p>Um cuidado para você.<br>Consulte serviços e horários<br>pelo WhatsApp.</p><span class="label">CONVITE PARA CONVERSAR</span><p>Olá! Quero consultar<br>meu próximo horário.</p><span class="label">ORIENTAÇÃO</span><p>Publique a arte.<br>Use a legenda.<br>Responda às interessadas.</p></div></div>`;
    if (scene.kind === "value")
      content = `<div class="value"><div>ANTES</div><p>“O que eu publico?”</p><div>COM O KIT</div><p>Arte + texto + sequência.<br>Prontos para você usar.</p><small>Persona demonstrativa.<br>Sem depoimento ou resultado garantido.</small></div>`;
    if (scene.kind === "offer")
      content = `<div class="list"><p><b>10</b> posts</p><p><b>10</b> stories</p><p><b>10</b> legendas</p><p><b>5</b> mensagens de WhatsApp</p><p>Calendário de <b>7 dias</b></p></div>`;
    if (scene.kind === "price")
      content = `<div class="value"><p>Kit completo personalizado</p><div class="price">R$ 67</div><p>Pagamento único</p><small>Nome · região · WhatsApp<br>serviços · estilo · cores</small></div>`;
    if (scene.kind === "delivery")
      content = `<div class="value"><div class="pill">BRIEFING SIMPLES</div><p>Pagamento aprovado<br>+ briefing completo</p><div class="arrow">↓</div><p>Link do kit por e-mail<br>em até 3 dias úteis</p><small>Sem promessa de clientes<br>ou agenda garantida.</small></div>`;
    if (scene.kind === "sample")
      content = `<div class="value sample"><img src="${post}" alt="Amostra demonstrativa gratuita"><p>1 post demonstrativo<br>+ legenda gratuita</p><div class="pill">SEM COMPRA OBRIGATÓRIA</div><p class="email">${escapeHtml(brief.supportEmail)}</p><small>Assunto: “Quero minha amostra Capella”<br>Amostra de estilo, sem personalização individual.</small></div>`;
    const html = `<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>
      *{box-sizing:border-box}body{margin:0;width:1080px;height:1920px;background:#faf3eb;color:#38252c;font-family:'DejaVu Sans',sans-serif;overflow:hidden}.top{position:absolute;left:76px;top:114px;font-size:30px;letter-spacing:8px}.heading{position:absolute;left:76px;top:216px;width:928px;font-family:'DejaVu Serif',serif;font-size:65px;line-height:1.12;white-space:pre-line;letter-spacing:-1px;font-weight:400;margin:0}.stage{position:absolute;top:435px;left:76px;width:928px;height:925px}.post{width:928px;height:928px;border-radius:22px;box-shadow:0 24px 48px #542b3426}.thought{font-size:62px;line-height:1.35;text-align:center;margin:36px 0}.mini{display:block;height:500px;width:auto;margin:auto;border-radius:18px;box-shadow:0 12px 30px #53233a33}.tag{margin-top:38px;font-size:36px;text-align:center}.value{background:#713346;color:#fff8f0;border-radius:32px;padding:64px 36px;text-align:center;font-size:32px;line-height:1.4}.value p{font-size:48px;line-height:1.25;margin:24px 0 46px}.value small{font-size:27px}.price{font-size:162px;letter-spacing:-7px}.list{font-size:49px;background:white;padding:42px 70px;border-radius:32px;box-shadow:0 16px 40px #542b341c}.list b{font-size:88px;color:#713346}.sequence{display:flex;gap:30px;flex-direction:column}.sequence img{width:360px;height:360px;align-self:center;border-radius:20px}.sequence>div{padding:28px 44px;background:#fff;border-radius:24px}.sequence p{font-size:31px;margin:10px 0 22px}.label{font-size:22px;font-weight:700;letter-spacing:2px;color:#713346}.pill{font-size:27px;border:1px solid #e5c9ba;padding:20px;border-radius:50px}.arrow{font-size:70px}.sample{padding:24px}.sample img{width:330px;border-radius:18px}.sample p{font-size:43px;margin:14px}.sample .email{font-size:36px;margin:28px 0}.sample small{font-size:24px}.bottom{position:absolute;top:1417px;left:76px;font-size:23px;color:#5d5156;line-height:1.6}.step{position:absolute;top:1374px;left:76px;font-size:22px;letter-spacing:3px;color:#713346}.bar{height:5px;margin-top:10px;width:928px;background:#dfc9c3}.fill{height:5px;width:${((index + 1) / brief.scenes.length) * 100}%;background:#713346}</style><div class="top">CAPELLA · AGENDA CHEIA NAIL DESIGN</div><h1 class="heading">${escapeHtml(scene.headline)}</h1><div class="stage">${content}</div><div class="step">${String(index + 1).padStart(2, "0")} / ${brief.scenes.length}<div class="bar"><div class="fill"></div></div></div><div class="bottom">Exemplos demonstrativos · negócios fictícios<br>Fotos e voz geradas por IA · sem promessa de agendamentos</div></html>`;
    await page.setContent(html);
    await page.evaluate(() => document.fonts.ready);
    const frame = join(outputDir, `scene-${index}.png`);
    await page.screenshot({ path: frame });
    const segment = join(outputDir, `segment-${index}.mp4`);
    run("ffmpeg", [
      "-v",
      "error",
      "-y",
      "-loop",
      "1",
      "-i",
      frame,
      "-i",
      voicePath,
      "-filter_complex",
      `[0:v]zoompan=z='1+0.006*on/${Math.ceil(duration * 30)}':x='iw/2-iw/zoom/2':y='ih/2-ih/zoom/2':d=1:s=1080x1920:fps=30,format=yuv420p[v];[1:a]apad[a]`,
      "-map",
      "[v]",
      "-map",
      "[a]",
      "-t",
      String(duration),
      "-c:v",
      "libx264",
      "-threads",
      "2",
      "-preset",
      "veryfast",
      "-crf",
      "19",
      "-c:a",
      "aac",
      "-b:a",
      "160k",
      "-ar",
      "48000",
      "-movflags",
      "+faststart",
      segment,
    ]);
    segments.push({
      scene: index,
      startSeconds: cursor,
      durationSeconds: duration,
      speech: scene.speech,
      frame,
      segment,
    });
    cursor += duration;
  }
  if (cursor > 60)
    throw new Error(
      `Locução com ${cursor.toFixed(2)}s excede o contrato. Encurte o texto antes de publicar.`,
    );
  await writeFile(
    join(outputDir, "concat.txt"),
    segments
      .map((s) => `file '${s.segment.replaceAll("'", "'\\''")}'`)
      .join("\n"),
  );
  const subtitles = [];
  let sequence = 1;
  const stamp = (seconds) => {
    const ms = Math.round(seconds * 1000);
    return `${String(Math.floor(ms / 3600000)).padStart(2, "0")}:${String(Math.floor(ms / 60000) % 60).padStart(2, "0")}:${String(Math.floor(ms / 1000) % 60).padStart(2, "0")},${String(ms % 1000).padStart(3, "0")}`;
  };
  for (const segment of segments) {
    const words = segment.speech.split(" ");
    const chunks = [];
    let lines = [""];
    for (const word of words) {
      const last = lines.length - 1;
      if ((lines[last] + " " + word).trim().length <= 35)
        lines[last] = (lines[last] + " " + word).trim();
      else if (lines.length === 1) lines.push(word);
      else {
        chunks.push(lines.join("\n"));
        lines = [word];
      }
    }
    chunks.push(lines.join("\n"));
    let time = segment.startSeconds;
    for (const chunk of chunks) {
      const length =
        ((segment.durationSeconds - 0.3) * chunk.split(/\s+/).length) /
        words.length;
      subtitles.push(
        `${sequence++}\n${stamp(time)} --> ${stamp(time + length)}\n${chunk}\n`,
      );
      time += length;
    }
  }
  const srt = join(outputDir, "captions.srt");
  await writeFile(srt, subtitles.join("\n"));
  const ass = join(outputDir, "captions.ass");
  const assTime = (s) =>
    s
      .replace(/^(\d\d):/, (_match, h) => `${Number(h)}:`)
      .replace(/,(\d\d)\d$/, ".$1");
  await writeFile(
    ass,
    `[Script Info]\nScriptType: v4.00+\nPlayResX: 1080\nPlayResY: 1920\n\n[V4+ Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\nStyle: Default,DejaVu Sans,46,&H00FFFFFF,&H00FFFFFF,&H002A1920,&HA02A1920,0,0,0,0,100,100,0,0,3,3,0,2,80,80,225,1\n\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n` +
      subtitles
        .map((item) => {
          const [, timing, ...lines] = item.trim().split("\n");
          const [start, end] = timing.split(" --> ");
          return `Dialogue: 0,${assTime(start)},${assTime(end)},Default,,0,0,0,,${lines.join("\\N")}`;
        })
        .join("\n"),
  );
  const videoPath = join(outputDir, "capella-personalizada-amostra-v1.mp4");
  run("ffmpeg", [
    "-v",
    "error",
    "-y",
    "-f",
    "concat",
    "-safe",
    "0",
    "-i",
    join(outputDir, "concat.txt"),
    "-vf",
    `ass=${ass}`,
    "-af",
    "loudnorm=I=-16:TP=-1.5:LRA=11",
    "-c:v",
    "libx264",
    "-threads",
    "2",
    "-preset",
    "fast",
    "-crf",
    "19",
    "-pix_fmt",
    "yuv420p",
    "-c:a",
    "aac",
    "-b:a",
    "160k",
    "-ar",
    "48000",
    "-movflags",
    "+faststart",
    videoPath,
  ]);
  const measuredDurationSeconds = Number(
    run("ffprobe", [
      "-v",
      "error",
      "-show_entries",
      "format=duration",
      "-of",
      "default=nw=1:nk=1",
      videoPath,
    ])
      .toString()
      .trim(),
  );
  if (measuredDurationSeconds > 60 || measuredDurationSeconds < 6)
    throw new Error("Duração final fora do contrato.");
  const manifest = {
    schemaVersion: "capella-demo.v1",
    productId: brief.productId,
    commercialPlanId: brief.commercialPlanId,
    profileId: brief.profileId,
    productionReference: "scripts/marketing/create-capella-demo-assets-v1.mjs",
    sources,
    approvedCreative: { id: 523, sha256: digest(story) },
    examples: files.map(({ path, ...item }) => ({
      ...item,
      file: path.split("/").at(-1),
    })),
    video: {
      file: videoPath.split("/").at(-1),
      sha256: digest(await readFile(videoPath)),
      durationSeconds: measuredDurationSeconds,
      width: 1080,
      height: 1920,
    },
    narration: {
      model: "pt_BR-faber-medium",
      engine: "piper-tts 1.3.0",
      modelSha256: voiceSha256,
      datasetLicense: "CC0",
      synthetic: true,
    },
    incrementalProviderConsumptionUsd: 0,
    totalOperationalCost: null,
    sample: brief.sample,
    segments: segments.map(({ segment, frame, ...s }) => s),
    status: "CANDIDATE_NOT_CAMPAIGN_AUTHORIZATION",
  };
  await writeFile(
    join(outputDir, "manifest.json"),
    JSON.stringify(manifest, null, 2),
  );
  await writeFile(
    join(outputDir, "video-caption.txt"),
    `Seu trabalho merece uma divulgação à altura. Conheça exemplos demonstrativos de posts personalizados com nome, cores e região.\n\nKit: 10 posts, 10 stories, 10 legendas, 5 mensagens de WhatsApp e calendário de 7 dias. R$ 67 em pagamento único. Link de entrega por e-mail em até 3 dias úteis após pagamento aprovado e briefing completo.\n\nPeça uma amostra demonstrativa gratuita (1 post + legenda) a ${brief.supportEmail}. Assunto: Quero minha amostra Capella. Sem compra obrigatória; amostra de estilo sem personalização individual.\n\nNegócios fictícios; imagens ilustrativas e voz sintética por IA. Não são fotos de serviços realizados nem depoimentos de clientes. Sem promessa de clientes ou agenda garantida.\n`,
  );
  console.log(
    JSON.stringify({
      examples: files.length,
      durationSeconds: cursor,
      videoPath,
      providerConsumptionUsd: 0,
    }),
  );
} finally {
  await browser.close();
}
