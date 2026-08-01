const commercialLabels: Record<string, string> = {
  "musa-pde-entry-v7-espelho-antes-de-sair": "MUSA v7 - video leve dos 7 dias",
  "musa-video-manifesto-presenca-digital": "MUSA manifesto - presenca digital",
  PDE_HERO_DIAGNOSTIC: "Hero do PDE para diagnostico",
  PDE: "PDE",
  PDE_AND_SOCIAL: "PDE e redes sociais",
  SOCIAL_REELS_STORIES: "Reels, Stories e TikTok",
  PAYWALL_OFFER: "Oferta antes do checkout",
  AWARENESS_TO_DIAGNOSTIC: "Anuncio para diagnostico",
  AWARENESS: "Descoberta da dor",
  DIAGNOSTIC_TO_PAYWALL: "Diagnostico para oferta",
  RETARGETING_PURCHASE: "Remarketing para compra",
  DIAGNOSTIC_START: "Inicio do diagnostico",
  PAYWALL_VIEWED: "Visualizacao da oferta",
  PURCHASE: "Compra",
  CINEMATIC_SCENE_BLUEPRINT: "Video narrativo com cenas",
  STORY_FIRST_AUDIO_VIDEO: "Roteiro com voz e montagem",
  AVATAR_EXPLAINER: "Apresentadora explicando",
  VERTICAL_9_16: "Vertical para Reels/TikTok/Shorts",
  SQUARE_1_1: "Quadrado para feed",
  HORIZONTAL_16_9: "Horizontal para pagina ou YouTube",
  LUMA_RAY_3_2: "Luma Ray 3.2",
  KLING_3_0: "Kling 3.0",
  RUNWAY: "Runway Gen-4.5",
  VEO: "Veo",
  HEYGEN: "HeyGen",
  "HLS LANDING_HERO": "Hero HLS da landing",
  COMMERCIAL_SHORT: "Video comercial curto",
  LONG_FORM: "Video longo / VSL",
  INSTITUTIONAL_CONTENT: "Institucional / conteudo",
  DRAFT: "Rascunho",
  READY_FOR_SCRIPT: "Pronto para roteiro",
  READY_FOR_RENDER: "Pronto para render",
  IN_PRODUCTION: "Em producao",
  READY_FOR_REVIEW: "Pronto para revisao",
  APPROVED: "Aprovado",
  ARCHIVED: "Arquivado",
};

const primaryMetricLabels: Array<[string, string]> = [
  ["CTA_CLICK_TO_DIAGNOSTIC", "Clique no diagnostico"],
  ["VIDEO_75", "Retencao de 75% do video"],
  ["VIDEO_PLAY", "Inicio do video"],
  ["DIAGNOSTIC_COMPLETED", "Diagnostico concluido"],
  ["CHECKOUT_STARTED", "Checkout iniciado"],
  ["PAYWALL_VIEWED", "Visualizacao da oferta"],
  ["PURCHASE", "Compra"],
];

export function getStudioCommercialLabel(value?: string | null) {
  if (!value) {
    return "";
  }

  const exactLabel = commercialLabels[value];
  if (exactLabel) {
    return exactLabel;
  }

  const matchedMetric = primaryMetricLabels.find(([metricCode]) =>
    value.includes(metricCode),
  );
  if (matchedMetric) {
    return matchedMetric[1];
  }

  return "Valor antigo nao padronizado";
}
