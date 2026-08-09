import { SalesVideoProviderFamily } from "./types";

export type SalesVideoProviderOption = {
  key: string;
  label: string;
  providerName: string;
  providerFamily: SalesVideoProviderFamily;
  recommendedUse: string;
  clipDurationSeconds: number;
  maxDirectDurationSeconds?: number;
  supportsHeroVideo: boolean;
  supportsSceneAssembly: boolean;
  supportsOpenAiReferenceImage: boolean;
  creditsUrl?: string;
};

export type SalesVideoRenderMetadataOptions = {
  visualProviderDirectives?: string;
  openAiReferenceImageEnabled?: boolean;
  openAiReferenceImagePrompt?: string;
  referenceImageCount?: number;
  sourceImageAssetId?: number;
  sourceImageUrl?: string;
  heygenAvatarId?: string;
  heygenAvatarGroupId?: string;
  heygenVoiceId?: string;
};

export const DEFAULT_VISUAL_PROVIDER_DIRECTIVES = [
  "Use direct camera shots. A full-length mirror may appear only when the scene brief asks for it; never show the camera, crew or an impossible duplicate reflection.",
  "Very sharp image, crisp focus on face and eyes, clear skin texture.",
  "Stable exposure, constant soft natural daylight, no haze, no blur, no dreamy filter, no flickering.",
  "Brazilian urban woman in her 30s, elegant but accessible, premium but human.",
  "No embedded text, no logos, no distorted hands, no luxury ostentation.",
].join(" ");

export const SALES_VIDEO_PROVIDER_OPTIONS: SalesVideoProviderOption[] = [
  {
    key: "luma-ray-3-2",
    label: "Luma Ray 3.2",
    providerName: "LUMA_RAY_3_2",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Hero premium do PDE, com cena mais longa e visual editorial.",
    clipDurationSeconds: 10,
    maxDirectDurationSeconds: 30,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: true,
  },
  {
    key: "kling-3-0",
    label: "Kling 3.0",
    providerName: "KLING_3_0",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Teste alternativo para cenas cinematográficas curtas e variações criativas.",
    clipDurationSeconds: 10,
    maxDirectDurationSeconds: 10,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
  },
  {
    key: "runway-gen-4-5",
    label: "Runway Gen-4.5",
    providerName: "RUNWAY",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Cenas curtas e variações criativas com boa consistência visual; para hero de 30s usar montagem por cenas.",
    clipDurationSeconds: 10,
    maxDirectDurationSeconds: 10,
    supportsHeroVideo: false,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
    creditsUrl: "https://dev.runwayml.com/",
  },
  {
    key: "runway-seedance-2-5",
    label: "Seedance 2.5 via Runway",
    providerName: "RUNWAY_SEEDANCE_2_5",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Cenas comerciais com áudio opcional e maior flexibilidade de duração; validar custo e consistência antes de escalar.",
    clipDurationSeconds: 10,
    maxDirectDurationSeconds: 15,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
    creditsUrl: "https://dev.runwayml.com/",
  },
  {
    key: "runway-hailuo-3",
    label: "Hailuo 3 via Runway",
    providerName: "RUNWAY_HAILUO_3",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Candidato econômico para movimento corporal, expressão facial e aderência ao prompt; exige QA antes de produção.",
    clipDurationSeconds: 10,
    maxDirectDurationSeconds: 10,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
    creditsUrl: "https://dev.runwayml.com/",
  },
  {
    key: "runway-gen-4-turbo",
    label: "Runway Gen-4 Turbo",
    providerName: "RUNWAY_GEN_4_TURBO",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Variações rápidas e econômicas a partir de uma imagem aprovada; exige imagem-base.",
    clipDurationSeconds: 10,
    maxDirectDurationSeconds: 10,
    supportsHeroVideo: false,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
    creditsUrl: "https://dev.runwayml.com/",
  },
  {
    key: "runway-veo-3-1-fast",
    label: "Veo 3.1 Fast via Runway",
    providerName: "RUNWAY_VEO_3_1_FAST",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Produção rápida de criativos com boa relação entre qualidade, áudio e custo.",
    clipDurationSeconds: 8,
    maxDirectDurationSeconds: 8,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
    creditsUrl: "https://dev.runwayml.com/",
  },
  {
    key: "runway-veo-3-1",
    label: "Veo 3.1 via Runway",
    providerName: "RUNWAY_VEO_3_1",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Hero e cenas premium quando a qualidade final justificar o maior consumo de créditos.",
    clipDurationSeconds: 8,
    maxDirectDurationSeconds: 8,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
    creditsUrl: "https://dev.runwayml.com/",
  },
  {
    key: "veo-teaser",
    label: "Veo",
    providerName: "VEO",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Teasers curtos ou cenas isoladas; não usar sozinho para vídeo de venda de 30s.",
    clipDurationSeconds: 8,
    maxDirectDurationSeconds: 8,
    supportsHeroVideo: false,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
  },
  {
    key: "heygen-avatar-video",
    label: "HeyGen",
    providerName: "HEYGEN",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Teste de avatar e narração sincronizada para ofertas educativas; requer HEYGEN_API_KEY no executor.",
    clipDurationSeconds: 30,
    maxDirectDurationSeconds: 600,
    supportsHeroVideo: true,
    supportsSceneAssembly: false,
    supportsOpenAiReferenceImage: false,
  },
];

export const DEFAULT_SALES_VIDEO_PROVIDER = SALES_VIDEO_PROVIDER_OPTIONS[0];

export function findSalesVideoProviderOption(providerName: string) {
  return SALES_VIDEO_PROVIDER_OPTIONS.find(
    (option) => option.providerName === providerName,
  );
}

export function buildSalesVideoRenderMetadata(
  provider: SalesVideoProviderOption,
  options?: string | SalesVideoRenderMetadataOptions,
) {
  const renderOptions =
    typeof options === "string"
      ? { visualProviderDirectives: options }
      : options;
  const normalizedVisualProviderDirectives =
    renderOptions?.visualProviderDirectives?.trim() ||
    DEFAULT_VISUAL_PROVIDER_DIRECTIVES;
  const openAiReferenceImageEnabled = Boolean(
    (renderOptions?.openAiReferenceImageEnabled ??
      provider.supportsOpenAiReferenceImage) &&
    provider.supportsOpenAiReferenceImage,
  );
  const referenceImageCount = Math.min(
    2,
    Math.max(1, Number(renderOptions?.referenceImageCount ?? 1)),
  );
  const sourceImageUrl = renderOptions?.sourceImageUrl?.trim();
  const sourceImageAssetId = renderOptions?.sourceImageAssetId;
  const heygenAvatarId = renderOptions?.heygenAvatarId?.trim();
  const heygenAvatarGroupId = renderOptions?.heygenAvatarGroupId?.trim();
  const heygenVoiceId = renderOptions?.heygenVoiceId?.trim();
  const providerUsesApprovedSourceImage = Boolean(
    sourceImageUrl && provider.providerName !== "LUMA_RAY_3_2",
  );
  return JSON.stringify({
    commercial_goal: "PDE_MUSA_HERO_VIDEO",
    generation_strategy: openAiReferenceImageEnabled
      ? "OPENAI_IMAGE_TO_LUMA_VIDEO"
      : providerUsesApprovedSourceImage
        ? "APPROVED_IMAGE_TO_VIDEO"
        : "TEXT_TO_VIDEO",
    visual_provider_directives: normalizedVisualProviderDirectives,
    image_to_video: {
      enabled: openAiReferenceImageEnabled || providerUsesApprovedSourceImage,
      source_image_provider: openAiReferenceImageEnabled
        ? "OPENAI"
        : providerUsesApprovedSourceImage
          ? "APPROVED_ASSET"
          : null,
      source_image_asset_id: sourceImageAssetId ?? null,
      source_image_url: sourceImageUrl || null,
      animation_provider: provider.providerName,
      reference_image_count: providerUsesApprovedSourceImage
        ? 1
        : referenceImageCount,
      image_prompt:
        renderOptions?.openAiReferenceImagePrompt?.trim() ||
        "Quadro-base MUSA anti-sensualizacao: mulher brasileira adulta em acao pratica, organizando visual com clareza, alivio e presenca elegante acessivel.",
      expected_benefit: providerUsesApprovedSourceImage
        ? "Animar uma imagem ja aprovada comercialmente para preservar personagem, postura, luz e enquadramento nos testes de criativo."
        : "Controlar composicao, postura e luz antes de animar na Luma para reduzir cenas sensualizadas ou nebulosas.",
    },
    provider_strategy: {
      provider_name: provider.providerName,
      recommended_use: provider.recommendedUse,
      expected_clip_duration_seconds: provider.clipDurationSeconds,
      supports_scene_assembly: provider.supportsSceneAssembly,
      stream_required: true,
      stream_target: "HLS_ADAPTIVE",
    },
    heygen_avatar:
      provider.providerName === "HEYGEN"
        ? {
            heygen_avatar_id: heygenAvatarId || null,
            heygen_avatar_group_id: heygenAvatarGroupId || null,
            heygen_voice_id: heygenVoiceId || null,
          }
        : undefined,
    heygen_avatar_id:
      provider.providerName === "HEYGEN" ? heygenAvatarId || null : undefined,
    heygen_avatar_group_id:
      provider.providerName === "HEYGEN"
        ? heygenAvatarGroupId || null
        : undefined,
    heygen_voice_id:
      provider.providerName === "HEYGEN" ? heygenVoiceId || null : undefined,
    assembly_plan: {
      required: true,
      final_target_duration_seconds: 30,
      minimum_accepted_duration_seconds: 28,
      scenes: [
        {
          order: 1,
          role: "DOR",
          title: "Dor do espelho",
          message: "Você se arruma, mas sente que ainda falta presença.",
          location:
            "Quarto brasileiro claro e realista, diante de um espelho de corpo inteiro.",
          action:
            "A mesma mulher adulta ajusta a manga e um acessório, observa o conjunto com dúvida discreta e retira um elemento que cria ruído visual.",
          camera:
            "Começar em close de 1 segundo no olhar refletido, abrir para plano médio lateral e terminar nas mãos retirando o excesso.",
        },
        {
          order: 2,
          role: "RESULTADO",
          title: "Presença desejada",
          message:
            "Pequenos ajustes deixam a imagem mais intencional em 7 dias.",
          location:
            "Entrada do mesmo apartamento e rua urbana cotidiana, sem luxo.",
          action:
            "A mesma mulher sai com a roupa-base preservada, agora com uma única peça-sinal, acabamento alinhado e postura natural mais segura.",
          camera:
            "Match cut do espelho para movimento de saída; travelling curto frontal e detalhe do acessório, sem caminhada repetitiva prolongada.",
        },
        {
          order: 3,
          role: "MECANISMO",
          title: "Mecanismo MUSA",
          message: "Ruído visual, peça-sinal, cor, acabamento e postura.",
          location: "Mesa clara e espelho do mesmo apartamento.",
          action:
            "Montagem de microações: afastar dois acessórios e manter um, comparar creme e vinho, dobrar a manga com acabamento e alinhar os ombros.",
          camera:
            "Quatro inserts rápidos e distintos de mãos, tecido, acessório e postura; nenhum plano deve repetir a caminhada da cena anterior.",
        },
        {
          order: 4,
          role: "CTA",
          title: "Diagnóstico gratuito",
          message: "Faça o diagnóstico e veja seu Plano MUSA de 7 dias.",
          location: "Perto do espelho, com luz natural e fundo limpo.",
          action:
            "A mesma mulher toca uma única vez no celular, sem interface ou texto legível, recebe clareza e olha novamente para o espelho com alívio sutil.",
          camera:
            "Close curto no gesto do celular, rack focus para o rosto e espaço negativo inferior seguro para a legenda e o CTA da pós-produção.",
        },
      ],
    },
    playback_plan: {
      primary_delivery: "streamPlaybackUrl",
      fallback_delivery: "assetId",
      public_player: "HLS com fallback MP4",
    },
  });
}

export type OrganicVideoRenderMetadataOptions = {
  productId: number;
  productName?: string;
  productSlug?: string;
  day: number;
  sequence: number;
  category: string;
  funnelStage: string;
  mentalShift: string;
  hook: string;
  scene: string;
  message: string;
  callToAction: string;
  primaryMetric: string;
  platformPriority: string;
};

export function buildOrganicVideoRenderMetadata(
  provider: SalesVideoProviderOption,
  video: OrganicVideoRenderMetadataOptions,
) {
  return JSON.stringify({
    commercial_goal: "ORGANIC_VIDEO_MUSA_SIGNAL_TEST",
    generation_strategy: "ORGANIC_TEXT_TO_VIDEO",
    visual_provider_directives: [
      DEFAULT_VISUAL_PROVIDER_DIRECTIVES,
      "Vertical social video, native creator style, fast first frame, natural movement, no embedded text.",
    ].join(" "),
    organic_video_plan: {
      product_id: video.productId,
      product_name: video.productName ?? null,
      product_slug: video.productSlug ?? null,
      day: video.day,
      sequence: video.sequence,
      category: video.category,
      funnel_stage: video.funnelStage,
      mental_shift: video.mentalShift,
      platform_priority: video.platformPriority,
      primary_metric: video.primaryMetric,
    },
    script_brief: {
      hook: video.hook,
      scene: video.scene,
      message: video.message,
      call_to_action: video.callToAction,
    },
    provider_strategy: {
      provider_name: provider.providerName,
      recommended_use: provider.recommendedUse,
      expected_clip_duration_seconds: provider.clipDurationSeconds,
      supports_scene_assembly: provider.supportsSceneAssembly,
      stream_required: true,
      stream_target: "HLS_ADAPTIVE",
    },
    quality_gate: {
      reject_if: [
        "visual sexualizado",
        "texto embutido ilegivel",
        "rosto distorcido",
        "maos distorcidas",
        "movimento artificial que prejudica retencao",
      ],
      commercial_reason:
        "Video organico precisa parecer nativo e gerar leitura de sinal antes de virar anuncio ou retargeting.",
    },
  });
}
