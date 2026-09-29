import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

const repo = process.cwd();
const output = "/tmp/aihub-mira-commercial-approved-v4-20260928";
const publicEvidence = "/tmp/mira-current-evidence-v6";
const irisPackageSchema =
  "/tmp/aihub-mira-commercial-approved-v1-20260928/work/iris-schema.json";
const mediaDir = path.join(repo, "pde-platform/frontend/public-mira-commercial/media");
const mode = process.argv[2];

const staticHash = "66482a136dce80aa14121417b3229e8b70c0b575e3879b8e4fffe861faa65fcc";
const videoHash = "0ba5720d4e3ec6beeed4ee76430a6be47b681a36eca9dfefe349d3a66aee3377";
const proofHash = "4ffda62d502d8ea644fa06a0cd6cf0768c39c4278b43bdcefa3665e9d578c2f3";
const sourceFingerprint = "37d838ba1d6d7ca4bed441b4264edcfa64db148d97ca6f4d614bd98c3f2a4424";
const deployedSourceFingerprint = sourceFingerprint;

const ensureDir = (directory) => fs.mkdirSync(directory, { recursive: true });
const writeText = (file, value) => {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, value.trim() + "\n", "utf8");
};
const writeJson = (file, value) => writeText(file, JSON.stringify(value, null, 2));
const readJson = (file) => JSON.parse(fs.readFileSync(file, "utf8"));
const sha256 = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const copy = (source, destination) => {
  ensureDir(path.dirname(destination));
  fs.copyFileSync(source, destination);
};
const run = (command, args) => {
  const result = spawnSync(command, args, { stdio: "inherit" });
  if (result.status !== 0) throw new Error(`${command} falhou com código ${result.status}`);
};

const contract = {
  contractVersion: "mira-commercial-v4-package-2026-09-28",
  product: {
    productId: 10,
    commercialPlanId: 8,
    experimentId: 93,
    name: "Mira — orientação digital individualizada de rotina para pele madura",
    experienceVersion: "mira-commercial-v1",
    priceBrl: 49,
    entitlement:
      "duas organizações individualizadas no total; cada organização concluída usa uma das duas tentativas disponíveis",
    delivery:
      "aplicação web com rotina consultável baseada somente nas orientações documentadas informadas pela cliente",
    limits: ["sem diagnóstico", "sem prescrição", "sem indicação de nova compra", "pagamento único"],
  },
  canonicalPromise:
    "Organize os produtos que você já tem em uma rotina individualizada, clara e consultável, por R$ 49, com limites explícitos.",
  audience:
    "Mulheres de 35 a 60 anos com pele madura e produtos de skincare já disponíveis, alcançadas exclusivamente por Instagram Ads.",
  channel: "Instagram Ads",
  destination: "https://mira.digicomdigital.com.br",
  publishingIdentity: {
    instagram: "@produtividade360_",
    instagramAccountId: "17841468261725306",
    facebookPageId: "485863027935937",
  },
  routeDecision: {
    selected: "STATIC_VIDEO_CONTROL",
    rationale:
      "Teste controlado com uma única variável de formato: captura estática do produto real versus vídeo editorial sucessor do #48, usando a mesma copy e o mesmo destino.",
  },
  publicationCopy: {
    headline: "Organize seu skincare",
    primaryText:
      "Informe seus produtos e receba uma rotina individualizada, clara e consultável, por R$ 49.",
    description: "Rotina individualizada",
    cta: "LEARN_MORE",
  },
  sourceProofs: [
    {
      file: "mira-commercial-product-proof-v1.png",
      purpose: "PRODUCT_PROOF",
      origin: "Ativo visual #320 aprovado na Biblioteca do plano #8; gerado por código versionado no PR #5411",
      rightsStatement:
        "Interface e conteúdo próprios do produto Mira/Marketing Hub; uso comercial autorizado para produto #10, anúncios, landing pages e redes sociais",
      sha256: proofHash,
    },
  ],
  formats: [
    {
      kind: "STATIC_CONTROL",
      file: "mira-commercial-control-v4.png",
      placement: "Instagram feed 4:5",
      sha256: staticHash,
    },
    {
      kind: "APPROVED_VIDEO_SUCCESSOR",
      file: "mira-commercial-demo-v3.mp4",
      sourceVideoAssetId: 49,
      predecessorVideoAssetId: 48,
      placement: "Instagram vertical 9:16",
      durationSeconds: 15,
      sha256: videoHash,
    },
  ],
  constraints: {
    noUnsupportedTimeClaim: true,
    noBeforeAfter: true,
    noTestimonial: true,
    noClinicalPromise: true,
    published: false,
    externalMediaSpendAuthorized: false,
    qaExcludedFromMarketEvidence: true,
  },
};

const executionIdsFile = path.join(output, "work/execution-ids.json");
const executionIds = () => readJson(executionIdsFile);

function baseContext(manifest) {
  return {
    process: "creative-production-approval-v8",
    sourceReference: "experiment:93",
    commercialPlanId: 8,
    productId: 10,
    experimentId: 93,
    reviewWorkspace: output,
    contract,
    technicalEvidence: {
      static: {
        artifactId: 1,
        file: "mira-commercial-control-v4.png",
        width: 1080,
        height: 1350,
        sha256: staticHash,
        productionReference: "pde-platform/frontend/scripts/generate-mira-commercial-control.mjs",
        sourceFingerprint,
        meaning:
          "controle estático final composto com a prova real #320; identifica aplicação web, declara duas organizações individualizadas no total e explicita que cada uma usa uma das duas tentativas",
      },
      video: {
        artifactId: 49,
        sourceVideoAssetId: 49,
        predecessorVideoAssetId: 48,
        relationship:
          "sucessor corretivo do vídeo #48, cadastrado e aprovado como vídeo #49; usa prova real #320 e mantém a faixa aprovada do #47 somente até 8,42s",
        file: "mira-commercial-demo-v3.mp4",
        width: 1080,
        height: 1920,
        durationSeconds: 15,
        videoCodec: "h264",
        audioCodec: "aac",
        sha256: videoHash,
        productionReference: "pde-platform/frontend/scripts/generate-mira-commercial-video.sh",
        sourceFingerprint,
        lineage:
          "bytes versionados no merge de09bc8e032058bb09c082180e48db4274d76f46 e publicados pela imagem 949ef2e5c8cce369ffeb850195e74a2dc3c7fb16; vídeo #49 aprovado; custo externo US$ 0",
      },
      destination: {
        url: "https://mira.digicomdigital.com.br",
        deploymentCommit: "949ef2e5c8cce369ffeb850195e74a2dc3c7fb16",
        experienceVersion: "mira-commercial-v1",
        sourceFingerprint: deployedSourceFingerprint,
        screenshots: [
          {
            device: "desktop",
            file: "mira-destination-desktop.png",
            sha256: "36ce8ba247bc4efbf5c1236a384cffc2be43894d7f6eedd0c53df55287bba1d9",
          },
          {
            device: "iPhone 15 Pro",
            file: "mira-destination-iphone-15-pro.png",
            sha256: "c9a95e2d859d53e36f54aa2de1976d2bfbff40f3ab2cc947cd83881eacb64ddb",
          },
          {
            device: "Pixel 7",
            file: "mira-destination-pixel-7.png",
            sha256: "99838a6a891540e7d51dac6ad9d72af859d5260c56acb942d3d846be9d76b850",
          },
        ],
        checkoutCta: "Organizar minha rotina por R$ 49",
        checkoutPayeeDisclosure:
          "Antes da CTA, a landing identifica Digicom Digital e Paulo Forestieri como responsável comercial/recebedor no checkout.",
        checkoutProvider: "Mercado Pago",
      },
      audio: {
        sourceAssetId: 47,
        sourceWindowSeconds: { start: 0, end: 8.42 },
        sourceAudioSha256: "19be7c4776e20dae6ed783264495e85d97ee2156fc67075e85f79af639e0ef63",
        transcriptContract:
          "pde-platform/frontend/scripts/media/mira-approved-voice-asset-47-v2.json",
        transcriptContractSha256: "4c6ada87c485245c4c93f0588a766a280a042d569465549c5d5bbc4966069e8f",
        transcript: [
          "Quer uma rotina de pele madura simples, usando o que já tem?",
          "Em três passos, Mira organiza seus produtos e mostra a ordem de uso.",
        ],
        excludedClaims: [
          "Veja a tela real.",
          "Por R$ 49, receba a rotina individualizada, suporte e uma correção técnica.",
        ],
        exclusionEvidence:
          "O MP4 final usa somente 0–8,42s da faixa aprovada e completa 15 segundos com silêncio; a tela real é demonstrada pela prova #320 e a promessa divergente não integra o áudio.",
        externalGenerationCostUsd: 0,
      },
      frames: [1, 4, 7, 10, 13].map((timeSeconds, index) => ({
        timeSeconds,
        file: `mira-video-frame-${String(index + 1).padStart(2, "0")}.png`,
        sha256: sha256(
          path.join(output, "review-frames", `mira-video-frame-${String(index + 1).padStart(2, "0")}.png`),
        ),
      })),
      channelPreviews: [
        {
          file: "mira-control-feed-360x450.png",
          sha256: sha256(path.join(output, "channel-previews/mira-control-feed-360x450.png")),
        },
        {
          file: "mira-video-feed-360x640.png",
          sha256: sha256(path.join(output, "channel-previews/mira-video-feed-360x640.png")),
        },
      ],
    },
    reviewBoundaries: {
      marketEvidence: false,
      publicationAuthorized: false,
      spendAuthorized: false,
      instruction:
        "Avaliar apenas coerência perceptiva e comercial dos bytes exatos. Agentes e QA não contam como demanda, venda, receita ou preferência humana.",
    },
    copyLimits: { primaryText: 125, headline: 40, description: 25 },
    expectedFunnel:
      "impressão humana atribuída → visita → demonstração → oferta vista → checkout → compra conciliada → entrega → primeiro uso",
    stopPolicy: "Nenhuma mídia antes de público oficial, teto absoluto, Plutus e preflight do Processo 5.",
    manifest,
  };
}

function prepare() {
  fs.rmSync(output, { recursive: true, force: true });
  for (const directory of [
    "assets",
    "proof",
    "review-frames",
    "channel-previews",
    "metadata",
    "audit",
    "work",
  ]) {
    ensureDir(path.join(output, directory));
  }

  copy(path.join(mediaDir, "mira-commercial-control-v4.png"), path.join(output, "assets/mira-commercial-control-v4.png"));
  copy(path.join(mediaDir, "mira-commercial-demo-v3.mp4"), path.join(output, "assets/mira-commercial-demo-v3.mp4"));
  copy(
    path.join(mediaDir, "mira-commercial-product-proof-v1.png"),
    path.join(output, "proof/mira-commercial-product-proof-v1.png"),
  );
  for (const file of [
    "mira-destination-desktop.png",
    "mira-destination-iphone-15-pro.png",
    "mira-destination-pixel-7.png",
  ]) {
    copy(path.join(publicEvidence, file), path.join(output, "proof", file));
  }

  [1, 4, 7, 10, 13].forEach((second, index) => {
    run("ffmpeg", [
      "-loglevel",
      "error",
      "-y",
      "-ss",
      String(second),
      "-i",
      path.join(output, "assets/mira-commercial-demo-v3.mp4"),
      "-frames:v",
      "1",
      path.join(output, "review-frames", `mira-video-frame-${String(index + 1).padStart(2, "0")}.png`),
    ]);
  });
  run("ffmpeg", [
    "-loglevel",
    "error",
    "-y",
    "-i",
    path.join(output, "assets/mira-commercial-control-v4.png"),
    "-vf",
    "scale=360:450:flags=lanczos",
    "-frames:v",
    "1",
    path.join(output, "channel-previews/mira-control-feed-360x450.png"),
  ]);
  run("ffmpeg", [
    "-loglevel",
    "error",
    "-y",
    "-ss",
    "1",
    "-i",
    path.join(output, "assets/mira-commercial-demo-v3.mp4"),
    "-vf",
    "scale=360:640:flags=lanczos",
    "-frames:v",
    "1",
    path.join(output, "channel-previews/mira-video-feed-360x640.png"),
  ]);

  if (sha256(path.join(output, "assets/mira-commercial-control-v4.png")) !== staticHash) {
    throw new Error("Hash do controle v4 divergiu");
  }
  if (sha256(path.join(output, "assets/mira-commercial-demo-v3.mp4")) !== videoHash) {
    throw new Error("Hash do vídeo v3 divergiu");
  }
  if (sha256(path.join(output, "proof/mira-commercial-product-proof-v1.png")) !== proofHash) {
    throw new Error("Hash da prova do produto divergiu");
  }

  const ids = {
    IRIS: crypto.randomUUID(),
    APOLLO: crypto.randomUUID(),
    PSIQUE: crypto.randomUUID(),
    TEMIS_INDEPENDENT: crypto.randomUUID(),
  };
  writeJson(executionIdsFile, ids);

  const manifest = {
    contractVersion: contract.contractVersion,
    producerExecutionId: ids.IRIS,
    externalMediaProviderCalled: false,
    externalMediaCostUsd: 0,
    published: false,
    sourceProofs: contract.sourceProofs,
    assets: [
      {
        file: "mira-commercial-control-v4.png",
        mediaType: "IMAGE",
        purposes: ["ADS", "LANDING", "SOCIAL"],
        sha256: staticHash,
      },
      {
        file: "mira-commercial-demo-v3.mp4",
        mediaType: "VIDEO",
        purposes: ["ADS", "LANDING", "SOCIAL"],
        sha256: videoHash,
      },
    ],
    reviewFrames: [1, 2, 3, 4, 5].map((number) => ({
      file: `mira-video-frame-${String(number).padStart(2, "0")}.png`,
      sha256: sha256(path.join(output, "review-frames", `mira-video-frame-${String(number).padStart(2, "0")}.png`)),
    })),
    channelPreviews: [
      {
        file: "mira-control-feed-360x450.png",
        sha256: sha256(path.join(output, "channel-previews/mira-control-feed-360x450.png")),
      },
      {
        file: "mira-video-feed-360x640.png",
        sha256: sha256(path.join(output, "channel-previews/mira-video-feed-360x640.png")),
      },
    ],
    destinationEvidence: {
      url: contract.destination,
      screenshots: [
        {
          file: "mira-destination-desktop.png",
          sha256: sha256(path.join(output, "proof/mira-destination-desktop.png")),
        },
        {
          file: "mira-destination-iphone-15-pro.png",
          sha256: sha256(path.join(output, "proof/mira-destination-iphone-15-pro.png")),
        },
        {
          file: "mira-destination-pixel-7.png",
          sha256: sha256(path.join(output, "proof/mira-destination-pixel-7.png")),
        },
      ],
    },
  };
  writeJson(path.join(output, "metadata/contract.json"), contract);
  writeJson(path.join(output, "metadata/manifest.json"), manifest);

  const context = baseContext(manifest);
  writeJson(path.join(output, "work/context.json"), context);
  copy(irisPackageSchema, path.join(output, "work/iris-schema.json"));
  copy(
    path.join(repo, "video-management-service/src/main/resources/prompts/apollo/v2/storyboard-planner-schema.json"),
    path.join(output, "work/apollo-schema.json"),
  );
  copy(
    path.join(repo, "customer-agent-worker/src/main/resources/prompts/bpm/creative-customer-review-schema.json"),
    path.join(output, "work/psique-schema.json"),
  );
  copy(
    path.join(repo, "meta-ad-approver-worker/src/main/resources/prompts/bpm/creative-commercial-review-schema.json"),
    path.join(output, "work/temis-schema.json"),
  );

  const irisAgent = fs.readFileSync(
    path.join(repo, "communication-agent-worker/src/main/resources/prompts/iris/v1/behavioral-core.md"),
    "utf8",
  );
  const irisActivity = `# Revisão de materialização exata — Mira commercial v4

Você recebeu o contrato corrigido e a evidência técnica abaixo. Compare exatamente três alternativas boas para a combinação de formato, sem alterar os bytes já produzidos. Se os dois ativos e a copy forem fiéis, selecione STATIC_VIDEO_CONTROL. Se houver bloqueio factual, devolva BLOCKED. Não trate QA ou agentes como evidência de mercado. Preserve literalmente a copy congelada e audite ambos os hashes. O controle v4 usa a prova real aprovada #320. O MP4 v3 é o sucessor corretivo do vídeo #48 e já foi cadastrado/aprovado como #49. Os arquivos estão públicos para homologação técnica, mas não foram distribuídos em campanha e não houve gasto.

CONTEXTO:
${JSON.stringify(context, null, 2)}`;
  prepareAudit(ids.IRIS, irisAgent, irisActivity);

  const apolloAgent = "# Apolo — diretor audiovisual\n\nVocê transforma a estratégia aprovada em narrativa audiovisual clara, emocional e executável. Preserve fatos, oferta e provas; não invente ativos, capacidades, publicação ou gasto e cumpra o contrato estruturado recebido.";
  const apolloTemplate = fs.readFileSync(
    path.join(repo, "video-management-service/src/main/resources/prompts/apollo/v2/storyboard-planner.md"),
    "utf8",
  );
  const apolloContext = {
    ...context,
    requestedCutCount: 5,
    requestedCutDurationsSeconds: [3, 3, 3, 3, 3],
    instruction:
      "Audite e descreva a montagem v3 já existente, cadastrada como vídeo #49 e sucessora corretiva do vídeo #48; não solicite nova geração. Use exatamente cinco cortes de 3 segundos, preserve a progressão observada nos frames, a oferta R$ 49 e o material aprovado. appliedCardIds deve ficar vazio porque não há biblioteca de pesquisa nesta revisão.",
  };
  prepareAudit(ids.APOLLO, apolloAgent, apolloTemplate.replace("{{CONTEXT}}", JSON.stringify(apolloContext, null, 2)));
}

function prepareAudit(id, agentPrompt, activityPrompt) {
  const directory = path.join(output, "audit", id);
  ensureDir(directory);
  writeText(path.join(directory, "agent-prompt.md"), agentPrompt);
  writeText(path.join(directory, "activity-prompt.md"), activityPrompt);
  const exactAgent = fs.readFileSync(path.join(directory, "agent-prompt.md"), "utf8").trim();
  const exactActivity = fs.readFileSync(path.join(directory, "activity-prompt.md"), "utf8").trim();
  fs.writeFileSync(path.join(directory, "request.md"), `${exactAgent}\n\n${exactActivity}`, "utf8");
}

function preparePsique() {
  const ids = executionIds();
  const manifest = readJson(path.join(output, "metadata/manifest.json"));
  const context = {
    ...baseContext(manifest),
    irisDirection: readJson(path.join(output, "audit", ids.IRIS, "response.json")),
    apolloStoryboard: readJson(path.join(output, "audit", ids.APOLLO, "response.json")),
    visualEvidence: [
      {
        artifactId: 1,
        evidenceType: "CREATIVE_RENDER",
        file: "mira-commercial-control-v4.png",
        sha256: staticHash,
        role: "controle estático final v4 composto com a prova real #320; explicita o direito total e o consumo das duas tentativas",
      },
      {
        artifactId: 49,
        evidenceType: "APPROVED_VIDEO_SUCCESSOR",
        file: "mira-commercial-demo-v3.mp4",
        sha256: videoHash,
        role: "vídeo #49 aprovado, sucessor corretivo do #48, composto com prova real #320 e promessa canônica",
      },
    ],
    creativeVisualInputs: [
      { artifactId: 1, sha256: staticHash, file: "mira-commercial-control-v4.png" },
      {
        artifactId: 49,
        sha256: videoHash,
        file: "mira-commercial-demo-v3.mp4",
        reviewFrames: [1, 2, 3, 4, 5].map(
          (number) => `mira-video-frame-${String(number).padStart(2, "0")}.png`,
        ),
      },
    ],
    reviewInstruction:
      "Inspecione diretamente todos os arquivos no reviewWorkspace: prova #320, controle v4, sequência de cinco frames do vídeo #49, prévias do feed e continuidade desktop/iPhone/Pixel. Decida de forma independente. APPROVED somente se não houver mudança obrigatória. A copy do anúncio aparece acima da mídia na Meta. O controle v4 e o vídeo #49 devem usar a prova real, identificar aplicação web, declarar duas organizações individualizadas no total e que cada uma usa uma das duas tentativas. Confira a janela auditável 0–8,42s da faixa AAC e as alegações excluídas. A landing apresenta preço, direito, responsável comercial e CTA antes da área de acesso. Não trate QA como evidência de mercado.",
  };
  writeJson(path.join(output, "work/review-context.json"), context);
  const agent = fs.readFileSync(
    path.join(repo, "customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v4.md"),
    "utf8",
  );
  const template = fs.readFileSync(
    path.join(repo, "customer-agent-worker/src/main/resources/prompts/bpm/creative-customer-review.md"),
    "utf8",
  );
  const activity = template
    .replace("{{PSIQUE_BEHAVIORAL_CORE_V2}}", "")
    .replace("{{TASK_CONTEXT}}", JSON.stringify(context, null, 2));
  prepareAudit(ids.PSIQUE, agent, activity);
}

function prepareTemis() {
  const ids = executionIds();
  const context = readJson(path.join(output, "work/review-context.json"));
  context.psiqueReview = readJson(path.join(output, "audit", ids.PSIQUE, "response.json"));
  context.reviewInstruction =
    "Faça revisão independente final dos arquivos no reviewWorkspace e do destino público. Confirme a decisão real de Psique, os hashes, a prova #320, o cadastro/aprovação do vídeo #49 como sucessor do #48, promessa, preço, direito de duas organizações no total, consumo das duas tentativas, formato de entrega, CTA, recebedor, linhagem e identidade oficial. APPROVED somente se requiredChanges puder ficar vazio. Não autorize publicação nem gasto.";
  writeJson(path.join(output, "work/temis-context.json"), context);
  const agent = fs.readFileSync(
    path.join(repo, "meta-ad-approver-worker/src/main/resources/prompts/temis/v1/agent-core.md"),
    "utf8",
  );
  const template = fs.readFileSync(
    path.join(repo, "meta-ad-approver-worker/src/main/resources/prompts/bpm/creative-commercial-review.md"),
    "utf8",
  );
  prepareAudit(ids.TEMIS_INDEPENDENT, agent, template.replace("{{TASK_CONTEXT}}", JSON.stringify(context, null, 2)));
}

function extractUsage(logFile) {
  const events = fs
    .readFileSync(logFile, "utf8")
    .trim()
    .split("\n")
    .map((line) => JSON.parse(line));
  const turn = events.findLast((event) => event.type === "turn.completed");
  if (!turn?.usage) throw new Error(`Telemetria ausente em ${logFile}`);
  return turn.usage;
}

function finalize() {
  const ids = executionIds();
  const roles = ["IRIS", "APOLLO", "PSIQUE", "TEMIS_INDEPENDENT"];
  const efforts = { IRIS: "high", APOLLO: "high", PSIQUE: "max", TEMIS_INDEPENDENT: "high" };
  const executions = roles.map((agent) => {
    const id = ids[agent];
    const base = path.join(output, "audit", id);
    const relative = (name) => `audit/${id}/${name}`;
    const usage = extractUsage(path.join(base, "process.jsonl"));
    return {
      executionId: id,
      agent,
      exitCode: 0,
      agentModelCalled: true,
      model: "gpt-5.6-sol",
      reasoningEffort: efforts[agent],
      usage: {
        input_tokens: usage.input_tokens,
        cached_input_tokens: usage.cached_input_tokens ?? 0,
        output_tokens: usage.output_tokens,
      },
      requestFile: relative("request.md"),
      requestFileSha256: sha256(path.join(base, "request.md")),
      agentPromptFile: relative("agent-prompt.md"),
      agentPromptFileSha256: sha256(path.join(base, "agent-prompt.md")),
      activityPromptFile: relative("activity-prompt.md"),
      activityPromptFileSha256: sha256(path.join(base, "activity-prompt.md")),
      responseFile: relative("response.json"),
      responseFileSha256: sha256(path.join(base, "response.json")),
      logFile: relative("process.jsonl"),
      logFileSha256: sha256(path.join(base, "process.jsonl")),
    };
  });

  const iris = readJson(path.join(output, "audit", ids.IRIS, "response.json"));
  const apollo = readJson(path.join(output, "audit", ids.APOLLO, "response.json"));
  const psique = readJson(path.join(output, "audit", ids.PSIQUE, "response.json"));
  const temis = readJson(path.join(output, "audit", ids.TEMIS_INDEPENDENT, "response.json"));
  if (iris.decision !== "SELECTED" || iris.chosenRoute !== "STATIC_VIDEO_CONTROL") {
    throw new Error(`Íris não selecionou o pacote: ${iris.decision}/${iris.chosenRoute}`);
  }
  if (!Array.isArray(apollo.cuts) || apollo.cuts.length === 0) throw new Error("Apolo não produziu cortes");
  for (const [name, response] of [
    ["Psique", psique],
    ["Têmis", temis],
  ]) {
    if (response.decision !== "APPROVED" || response.requiredChanges?.length !== 0) {
      throw new Error(`${name} não aprovou sem mudanças: ${JSON.stringify(response)}`);
    }
  }
  writeJson(path.join(output, "metadata/iris-direction.json"), iris);
  writeJson(path.join(output, "metadata/apollo-storyboard.json"), apollo);
  writeJson(path.join(output, "metadata/psique-review.json"), psique);
  writeJson(path.join(output, "metadata/temis-review.json"), temis);
  writeJson(path.join(output, "metadata/agent-executions.json"), executions);

  const archive = `${output}.zip`;
  fs.rmSync(archive, { force: true });
  run("jar", [
    "--create",
    "--file",
    archive,
    "--no-manifest",
    "-C",
    output,
    "assets",
    "-C",
    output,
    "proof",
    "-C",
    output,
    "review-frames",
    "-C",
    output,
    "channel-previews",
    "-C",
    output,
    "metadata",
    "-C",
    output,
    "audit",
  ]);
  console.log(JSON.stringify({ archive, sha256: sha256(archive), executions }, null, 2));
}

if (mode === "prepare") prepare();
else if (mode === "psique") preparePsique();
else if (mode === "temis") prepareTemis();
else if (mode === "finalize") finalize();
else throw new Error("Use prepare, psique, temis ou finalize");
