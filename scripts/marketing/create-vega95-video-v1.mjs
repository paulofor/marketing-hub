/** Produz a candidata de Vega com provas reais e voz offline, seguindo o harness de Apolo. */
import { createRequire } from "node:module";
import { execFileSync } from "node:child_process";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  sha256,
  validateNarrationTimeline,
  validateSources,
} from "./video-candidate-contract.mjs";

const require = createRequire(import.meta.url);
const { chromium } = require("playwright");
const scriptDir = dirname(fileURLToPath(import.meta.url));
const [sourcesArgument, outputArgument, voiceModel, voiceBank] =
  process.argv.slice(2);
if (!sourcesArgument || !outputArgument || !voiceModel || !voiceBank)
  throw new Error(
    "Uso: node create-vega95-video-v1.mjs fontes saída modelo.onnx vozes.bin",
  );
const sourceDir = resolve(sourcesArgument);
const outputDir = resolve(outputArgument);
const briefPath = join(scriptDir, "vega95-video-v1/brief.json");
const brief = JSON.parse(await readFile(briefPath));
const sources = JSON.parse(
  await readFile(join(sourceDir, "capture-manifest.json")),
);
await validateSources(sourceDir, sources, brief.experienceVersion);
if (
  sha256(await readFile(join(sourceDir, "source-2810.mp4"))) !==
  brief.sourceSha256
) {
  throw new Error("Filmagem diferente do briefing aprovado; não sintetizar.");
}
if (
  sha256(await readFile(voiceModel)) !== brief.voiceModelSha256 ||
  sha256(await readFile(voiceBank)) !== brief.voiceBankSha256
)
  throw new Error("Modelo ou banco de voz divergente; não sintetizar.");
await mkdir(outputDir, { recursive: true });
const run = (cmd, args) =>
  execFileSync(cmd, args, { stdio: "pipe", maxBuffer: 8 * 1024 * 1024 });
run("python3", [
  join(scriptDir, "render-vega95-narration-v1.py"),
  briefPath,
  outputDir,
  voiceModel,
  voiceBank,
]);
const scenes = JSON.parse(await readFile(join(outputDir, "narration.json")));
validateNarrationTimeline(scenes, brief.durationSeconds);
const escaped = (s) =>
  String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll('"', "&quot;");
const dataImage = async (file) =>
  `data:image/png;base64,${(await readFile(join(sourceDir, file))).toString("base64")}`;
const stamp = (seconds, separator = ",") => {
  const ms = Math.round(seconds * 1000);
  return `${String(Math.floor(ms / 3600000)).padStart(2, "0")}:${String(Math.floor(ms / 60000) % 60).padStart(2, "0")}:${String(Math.floor(ms / 1000) % 60).padStart(2, "0")}${separator}${String(ms % 1000).padStart(3, "0")}`;
};
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const page = await browser.newPage({
  viewport: { width: 1080, height: 1920 },
  deviceScaleFactor: 1,
});
const segments = [];
const frames = [];

/** Desenha títulos e molduras; toda imagem de produto vem de captura verificada. */
async function renderFrame(scene, index, kind, capture, label) {
  let content = `<div class="actor-placeholder"></div>`;
  if (capture)
    content = `<div class="proof-label">INTERFACE REAL · DEMONSTRAÇÃO</div><img class="capture" src="${await dataImage(capture)}" alt="Captura real do MUSA"><div class="choice-label">${escaped(label ?? "")}</div>`;
  if (kind === "result")
    content = `<div class="proof-label">RESULTADO REAL · EXEMPLO DE ESCOLHAS</div><img class="result" src="${await dataImage("result.png")}" alt="Resultado atual"><img class="action" src="${await dataImage("microaction.png")}" alt="Microação atual"><div class="note">Orientação atual do exemplo.<br>Experimente e avalie para você.</div>`;
  if (kind === "continuation")
    content = `<div class="proof-label">MATERIAL VERSIONADO · PRÉVIA</div><div class="continuation">Organize escolhas.<br>Registre sua fórmula.<br>Consulte quando precisar.</div><img class="checklist" src="${await dataImage("checklist.png")}" alt="Checklist versionado antes de sair">`;
  if (kind === "offer")
    content = `<div class="offer"><div class="offer-label">CONTINUIDADE · DIAS 2–7</div><div class="price">R$ 67</div><div class="terms">Pagamento único<br>90 dias de acesso<br>Sem renovação automática</div><div class="materials">Mapa dos sinais · checklist<br>Registro da fórmula pessoal</div><div class="cta">Ver meu primeiro<br>ajuste grátis</div><div class="free">Primeiro resultado antes do cadastro<br>ou pagamento. E-mail para salvar.</div></div>`;
  const html = `<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>
    *{box-sizing:border-box}body{margin:0;width:1080px;height:1920px;background:#f7f3ec;color:#392b32;font-family:'DejaVu Sans',sans-serif;overflow:hidden}
    .brand{position:absolute;top:102px;left:82px;font-size:31px;font-weight:700;letter-spacing:7px}.brand span{font-size:23px;letter-spacing:2px;color:#7a5b61}
    .heading{position:absolute;top:208px;left:82px;width:916px;margin:0;font-family:'DejaVu Serif',serif;font-size:68px;line-height:1.12;letter-spacing:-2px;white-space:pre-line;font-weight:400}
    .stage{position:absolute;left:82px;top:408px;width:916px;height:1008px;border-radius:32px;overflow:hidden;background:#eee6df;box-shadow:0 14px 36px #392b321a}
    .actor-placeholder{width:100%;height:100%;background:transparent}.proof-label{padding:25px 20px;background:#704255;color:white;font-size:25px;text-align:center;letter-spacing:1px}
    .capture{display:block;object-fit:contain;width:calc(100% - 48px);height:788px;margin:16px 24px;background:#fff;border-radius:20px}.choice-label{font-size:35px;line-height:1.2;text-align:center;color:#704255;padding:7px 22px}
    .result{display:block;width:868px;height:325px;object-fit:contain;margin:22px auto 10px}.action{display:block;width:868px;height:400px;object-fit:contain;margin:10px auto}.note{text-align:center;font-size:30px;line-height:1.25;color:#704255;padding:15px}
    .continuation{font-size:43px;line-height:1.3;padding:30px 35px}.checklist{width:860px;height:680px;object-fit:contain;margin:0 28px}
    .offer{height:100%;background:#704255;color:#fff9f1;text-align:center;padding:35px 24px}.offer-label{font-size:25px;letter-spacing:3px}.price{font-size:146px;line-height:1.15;letter-spacing:-7px;margin:30px 0 12px}.terms{font-size:42px;line-height:1.35}.materials{font-size:29px;line-height:1.4;margin:22px 0 24px;color:#f2dedb}.cta{margin:25px 24px;padding:26px;background:#faf3e8;color:#542f40;border-radius:26px;font-size:43px;line-height:1.12;font-weight:700}.free{font-size:26px;line-height:1.4}
    .step{position:absolute;left:82px;top:1458px;width:916px;font-size:23px;color:#704255;letter-spacing:2px}.progress{margin-top:14px;height:5px;background:#ddcfcb}.fill{height:5px;background:#704255;width:${((index + 1) / scenes.length) * 100}%}
    .disclosure{position:absolute;left:82px;top:1530px;width:916px;font-size:23px;line-height:1.5;color:#6c5b60}.illustration{color:#704255;font-weight:700}
    </style><div class="brand">MUSA <span>· MÉTODO EM 7 DIAS</span></div><h1 class="heading">${escaped(scene.headline)}</h1><div class="stage">${content}</div><div class="step">${String(index + 1).padStart(2, "0")} / 07<div class="progress"><div class="fill"></div></div></div><div class="disclosure">${escaped(brief.disclosure)}${kind === "illustration" ? '<br><span class="illustration">Aplicação ilustrativa · não é prescrição do aplicativo</span>' : ""}</div></html>`;
  await page.setContent(html);
  await page.evaluate(() => document.fonts.ready);
  const valid = await page.evaluate(() => {
    const heading = document.querySelector(".heading");
    const stage = document.querySelector(".stage").getBoundingClientRect();
    for (
      let size = 68;
      heading.getBoundingClientRect().bottom > stage.top - 18 && size >= 48;
      size -= 2
    ) {
      heading.style.fontSize = `${size}px`;
    }
    return (
      heading.getBoundingClientRect().bottom <= stage.top - 18 &&
      [
        ...document.querySelectorAll(
          ".heading,.choice-label,.terms,.cta,.free",
        ),
      ].every(
        (el) =>
          el.scrollWidth <= el.clientWidth + 1 &&
          el.getBoundingClientRect().bottom < 1418,
      )
    );
  });
  if (!valid)
    throw new Error(`Texto cortado ou fora da área segura em ${scene.id}.`);
  const frame = join(outputDir, `frame-${frames.length}.png`);
  await page.screenshot({ path: frame });
  frames.push({
    file: frame.split("/").at(-1),
    sceneId: scene.id,
    kind,
    sha256: sha256(await readFile(frame)),
  });
  return frame;
}

/** Compõe um corte, mantendo fontes e textos fixos na área segura. */
async function renderCut(scene, index, kind, capture, label, length) {
  const frame = await renderFrame(scene, index, kind, capture, label);
  const segment = join(outputDir, `cut-${segments.length}.mp4`);
  const actor = kind === "actor" || kind === "illustration";
  const args = ["-v", "error", "-y", "-loop", "1", "-i", frame];
  if (actor)
    args.push(
      "-ss",
      String(brief.sourceActorInterval[0]),
      "-t",
      String(brief.sourceActorInterval[1] - brief.sourceActorInterval[0]),
      "-i",
      join(sourceDir, "source-2810.mp4"),
    );
  const filter = actor
    ? `[1:v]scale=916:1008:force_original_aspect_ratio=increase,crop=916:1008:0:0,tpad=stop_mode=clone:stop_duration=5,setsar=1[actor];[0:v][actor]overlay=82:408:shortest=1,format=yuv420p[v]`
    : `[0:v]format=yuv420p[v]`;
  args.push(
    "-filter_complex",
    filter,
    "-map",
    "[v]",
    "-an",
    "-t",
    String(length),
    "-r",
    "30",
    "-c:v",
    "libx264",
    "-threads",
    "2",
    "-preset",
    "veryfast",
    "-crf",
    "19",
    segment,
  );
  run("ffmpeg", args);
  segments.push(segment);
}

try {
  for (const [index, scene] of scenes.entries()) {
    const length = scene.end - scene.start;
    if (scene.kind === "choices") {
      const labels = [
        "Calça e camisa ou blusa",
        "Trabalho ou reunião",
        "Elegância discreta",
        "Ajustar manga, barra ou caimento",
      ];
      for (let step = 0; step < 4; step++)
        await renderCut(
          scene,
          index,
          "choice",
          `choice-${step + 1}.png`,
          labels[step],
          2,
        );
    } else if (scene.kind === "proof") {
      await renderCut(scene, index, "result", null, null, 4);
      await renderCut(scene, index, "illustration", null, null, 4);
    } else if (scene.kind === "save") {
      await renderCut(
        scene,
        index,
        "save",
        "save.png",
        "E-mail depois do resultado, para salvar.",
        length,
      );
    } else await renderCut(scene, index, scene.kind, null, null, length);
    console.log(
      JSON.stringify({
        rendered: scene.id,
        voiceDuration: scene.voiceDuration,
        slotDuration: length,
      }),
    );
  }
  const concatLine = (path) => `file '${path.replaceAll("'", "'\\''")}'`;
  await writeFile(
    join(outputDir, "cuts.txt"),
    segments.map(concatLine).join("\n"),
  );
  await writeFile(
    join(outputDir, "voices.txt"),
    scenes
      .map((s) => concatLine(join(outputDir, `voice-${s.id}.wav`)))
      .join("\n"),
  );
  run("ffmpeg", [
    "-v",
    "error",
    "-y",
    "-f",
    "concat",
    "-safe",
    "0",
    "-i",
    join(outputDir, "voices.txt"),
    "-c:a",
    "pcm_s16le",
    join(outputDir, "narration.wav"),
  ]);
  const cues = scenes.flatMap((scene) => scene.cues);
  await writeFile(
    join(outputDir, "captions.srt"),
    cues
      .map(
        (cue, i) =>
          `${i + 1}\n${stamp(cue.start)} --> ${stamp(cue.end)}\n${cue.text}\n`,
      )
      .join("\n"),
  );
  await writeFile(
    join(outputDir, "captions.vtt"),
    "WEBVTT\n\n" +
      cues
        .map(
          (cue) =>
            `${stamp(cue.start, ".")} --> ${stamp(cue.end, ".")}\n${cue.text}\n`,
        )
        .join("\n"),
  );
  const assTime = (s) =>
    stamp(s)
      .replace(/^(\d\d):/, (_m, h) => `${Number(h)}:`)
      .replace(/,(\d\d)\d$/, ".$1");
  const ass =
    `[Script Info]\nScriptType: v4.00+\nPlayResX: 1080\nPlayResY: 1920\n\n[V4+ Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\nStyle: Default,DejaVu Sans,46,&H00FFFFFF,&H00FFFFFF,&H00392B32,&HC0392B32,0,0,0,0,100,100,0,0,3,4,0,2,82,82,210,1\n\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n` +
    cues
      .map(
        (cue) =>
          `Dialogue: 0,${assTime(cue.start)},${assTime(cue.end)},Default,,0,0,0,,${cue.text.replaceAll("\n", "\\N")}`,
      )
      .join("\n");
  const assPath = join(outputDir, "captions.ass");
  await writeFile(assPath, ass);
  const videoPath = join(outputDir, "vega95-primeiro-ajuste-v1.mp4");
  const filterPath = assPath
    .replaceAll("\\", "\\\\")
    .replaceAll(":", "\\:")
    .replaceAll("'", "'\\''");
  run("ffmpeg", [
    "-v",
    "error",
    "-y",
    "-f",
    "concat",
    "-safe",
    "0",
    "-i",
    join(outputDir, "cuts.txt"),
    "-i",
    join(outputDir, "narration.wav"),
    "-vf",
    `ass='${filterPath}'`,
    "-af",
    "loudnorm=I=-16:TP=-1.5:LRA=11",
    "-t",
    String(brief.durationSeconds),
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
  const probe = JSON.parse(
    run("ffprobe", [
      "-v",
      "error",
      "-show_streams",
      "-show_format",
      "-of",
      "json",
      videoPath,
    ]),
  );
  const video = probe.streams.find((s) => s.codec_type === "video");
  const audio = probe.streams.find((s) => s.codec_type === "audio");
  if (
    video?.width !== 1080 ||
    video?.height !== 1920 ||
    video?.codec_name !== "h264" ||
    audio?.codec_name !== "aac" ||
    Math.abs(Number(probe.format.duration) - brief.durationSeconds) > 0.1
  )
    throw new Error("Artefato final incompatível; não importar.");
  run("ffmpeg", [
    "-v",
    "error",
    "-y",
    "-ss",
    "1",
    "-i",
    videoPath,
    "-frames:v",
    "1",
    join(outputDir, "poster.jpg"),
  ]);
  const harnessFiles = ["production-mission.md", "storyboard-planner.md"];
  const harness = await Promise.all(
    harnessFiles.map(async (file) => ({
      path: `video-management-service/src/main/resources/prompts/apollo/v2/${file}`,
      sha256: sha256(
        await readFile(
          join(
            scriptDir,
            "../../video-management-service/src/main/resources/prompts/apollo/v2",
            file,
          ),
        ),
      ),
    })),
  );
  const manifest = {
    schemaVersion: "vega95-local-apolo-video.v1",
    productId: brief.productId,
    experimentId: brief.experimentId,
    projectId: brief.projectId,
    profileId: brief.profileId,
    experienceVersion: brief.experienceVersion,
    productionReference: "scripts/marketing/create-vega95-video-v1.mjs",
    harness: {
      execution: "LOCAL_VERSIONED_HARNESS_NOT_PRODUCTION_WORKER",
      sources: harness,
    },
    sources,
    sourceActorInterval: brief.sourceActorInterval,
    frames,
    scenes,
    video: {
      file: videoPath.split("/").at(-1),
      sha256: sha256(await readFile(videoPath)),
      durationSeconds: Number(probe.format.duration),
      width: video.width,
      height: video.height,
      codec: video.codec_name,
      audioCodec: audio.codec_name,
    },
    narration: {
      engine: "kokoro-onnx 0.4.9",
      voice: brief.voice,
      modelSha256: brief.voiceModelSha256,
      voiceBankSha256: brief.voiceBankSha256,
      synthetic: true,
      license: "Apache-2.0",
    },
    paidProviderCalls: 0,
    incrementalProviderConsumptionUsd: 0,
    totalOperationalCost: null,
    status: "CANDIDATE_PENDING_INDEPENDENT_REVIEW",
    campaignAuthorized: false,
    limitations: [
      "A microação atual é categorial; o ajuste da personagem é ilustrativo.",
      "Não demonstra satisfação, venda, uso ou lucro de cliente.",
      "Revisão perceptual independente e parecer financeiro para consumo futuro permanecem necessários.",
    ],
  };
  await writeFile(
    join(outputDir, "manifest.json"),
    JSON.stringify(manifest, null, 2),
  );
  console.log(
    JSON.stringify({
      videoPath,
      video: manifest.video,
      paidProviderCalls: 0,
      reviewStatus: manifest.status,
    }),
  );
} finally {
  await browser.close();
}
