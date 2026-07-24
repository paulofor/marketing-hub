import { SalesVideoProviderFamily } from "./types";

export type SalesVideoProviderOption = {
  key: string;
  label: string;
  providerName: string;
  providerFamily: SalesVideoProviderFamily;
  recommendedUse: string;
  clipDurationSeconds: number;
  supportsHeroVideo: boolean;
  supportsSceneAssembly: boolean;
};

export const DEFAULT_VISUAL_PROVIDER_DIRECTIVES = [
  "Direct camera shot, no mirror and no reflection.",
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
    recommendedUse: "Hero premium do PDE, com cena mais longa e visual editorial.",
    clipDurationSeconds: 20,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
  },
  {
    key: "kling-3-0",
    label: "Kling 3.0",
    providerName: "KLING_3_0",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse: "Teste alternativo para cenas cinematográficas curtas e variações criativas.",
    clipDurationSeconds: 15,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
  },
  {
    key: "veo-teaser",
    label: "Veo",
    providerName: "VEO",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse: "Teasers curtos ou cenas isoladas; não usar sozinho para vídeo de venda de 30s.",
    clipDurationSeconds: 8,
    supportsHeroVideo: false,
    supportsSceneAssembly: true,
  },
  {
    key: "heygen-avatar-video",
    label: "HeyGen",
    providerName: "HEYGEN",
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse:
      "Teste de avatar e narração sincronizada para ofertas educativas; requer HEYGEN_API_KEY no executor.",
    clipDurationSeconds: 30,
    supportsHeroVideo: true,
    supportsSceneAssembly: false,
  },
];

export const DEFAULT_SALES_VIDEO_PROVIDER = SALES_VIDEO_PROVIDER_OPTIONS[0];

export function findSalesVideoProviderOption(providerName: string) {
  return SALES_VIDEO_PROVIDER_OPTIONS.find((option) => option.providerName === providerName);
}

export function buildSalesVideoRenderMetadata(
  provider: SalesVideoProviderOption,
  visualProviderDirectives?: string,
) {
  const normalizedVisualProviderDirectives =
    visualProviderDirectives?.trim() || DEFAULT_VISUAL_PROVIDER_DIRECTIVES;
  return JSON.stringify({
    commercial_goal: "PDE_MUSA_HERO_VIDEO",
    visual_provider_directives: normalizedVisualProviderDirectives,
    provider_strategy: {
      provider_name: provider.providerName,
      recommended_use: provider.recommendedUse,
      expected_clip_duration_seconds: provider.clipDurationSeconds,
      supports_scene_assembly: provider.supportsSceneAssembly,
      stream_required: true,
      stream_target: "HLS_ADAPTIVE",
    },
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
        },
        {
          order: 2,
          role: "RESULTADO",
          title: "Presença desejada",
          message: "Pequenos ajustes deixam a imagem mais intencional em 7 dias.",
        },
        {
          order: 3,
          role: "MECANISMO",
          title: "Mecanismo MUSA",
          message: "Ruído visual, peça-sinal, cor, acabamento e postura.",
        },
        {
          order: 4,
          role: "CTA",
          title: "Diagnóstico gratuito",
          message: "Faça o diagnóstico e veja seu Plano MUSA de 7 dias.",
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
