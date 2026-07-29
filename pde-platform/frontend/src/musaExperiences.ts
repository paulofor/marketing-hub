export type Theme = {
  primary: string;
  accent: string;
  background: string;
  imageUrl: string;
};

export type Diagnostic = {
  title: string;
  intro: string;
  questions: string[];
};

export type Mission = {
  id: string;
  day: number;
  title: string;
  principle: string;
  action: string;
  evidence: string;
  visualCue: string;
};

export type SupportMaterial = {
  title: string;
  type: string;
  description: string;
  url: string;
};

export type HeroVideo = {
  experienceVersion: string;
  placement: string;
  playbackUrl: string;
  hlsPlaybackUrl?: string;
  posterUrl?: string;
  autoplay?: boolean;
  muted?: boolean;
  controls?: boolean;
  loop?: boolean;
  playsInline?: boolean;
  source?: string;
  assetId?: number;
  experimentVideoAssetId?: number;
  salesVideoProfileId?: number;
  salesVideoJobId?: number;
  reviewStatus?: string;
  status?: string;
};

export type ScientificEvidencePack = {
  version: string;
  principles: string[];
  practicalApplications: string[];
  allowedLanguage: string[];
  forbiddenClaims: string[];
  references: {
    authors: string;
    year: string;
    title: string;
    source: string;
    doi: string;
  }[];
};

export type ProductExperience = {
  slug: string;
  experienceVersion?: string;
  funnelVersion?: string;
  name: string;
  promise: string;
  audience: string;
  priceLabel: string;
  theme: Theme;
  diagnostic: Diagnostic;
  missions: Mission[];
  supportMaterials: SupportMaterial[];
  heroVideos?: HeroVideo[];
  scientificEvidencePack?: ScientificEvidencePack;
  completionOffer: string;
};

export type PublicDiagnosticQuestion = {
  key: string;
  stageLabel: string;
  question: string;
  options: string[];
  imageUrl: string;
  visualTitle: string;
  visualText: string;
  journeyEventType: string;
};

export type MusaExperienceContract = {
  experienceVersion: string;
  primaryHost?: string;
  publicDiagnosticQuestions: PublicDiagnosticQuestion[];
  usesDesireRoad: boolean;
  supportsPublishedPublicDiagnosticVideoHero: boolean;
  usesMotivationalTimelineVideo: boolean;
  videoPlacements: string[];
};

export const MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION = 'musa-pde-entry-v5-video-explicativo';
export const MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION = 'musa-pde-entry-v6-video-motivacional';
export const MUSA_V7_EXPERIENCE_VERSION = 'musa-pde-entry-v7-espelho-antes-de-sair';

const MUSA_APPROVED_HERO_VIDEO_URL = '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8';

const basePublicDiagnosticQuestions: PublicDiagnosticQuestion[] = [
  {
    key: 'mainObstacle',
    stageLabel: 'Espelho da dor',
    question: 'O que mais te incomoda quando você se olha pronta?',
    options: ['Pareço comum', 'Falta acabamento', 'Nada conversa entre si', 'Sinto que exagerei'],
    imageUrl: '/assets/musa-diagnostic-slide-1.png',
    visualTitle: 'Comece pelo sinal que mais rouba elegância da sua presença.',
    visualText: 'Nomeie o que você sente ao se ver pronta. A partir disso, o MUSA aponta onde reduzir ruído visual e qual cuidado testar primeiro.',
    journeyEventType: 'PROBLEM_RECOGNIZED',
  },
  {
    key: 'presenceFocus',
    stageLabel: 'Sua rotina',
    question: 'Em qual situação você quer se sentir mais presente primeiro?',
    options: ['Trabalho ou reunião', 'Encontro ou saída', 'Rotina comum', 'Foto ou conteúdo'],
    imageUrl: '/assets/musa-diagnostic-slide-2.png',
    visualTitle: 'Escolha uma cena real, não uma mudança de vida inteira.',
    visualText: 'Você só precisa apontar onde quer se sentir mais segura hoje. O primeiro ajuste vem a partir dessa cena.',
    journeyEventType: 'REAL_INPUT_SUBMITTED',
  },
  {
    key: 'desiredSignal',
    stageLabel: 'Sinal desejado',
    question: 'Qual sinal você quer comunicar com mais força nessa cena?',
    options: ['Elegância discreta', 'Segurança', 'Leveza feminina', 'Imagem mais marcante'],
    imageUrl: '/assets/musa-diagnostic-slide-3.png',
    visualTitle: 'A Consultora MUSA conecta dor, situação e sinal desejado.',
    visualText: 'A partir do que você escolhe, o MUSA mostra qual detalhe pode deixar sua imagem mais coerente e intencional.',
    journeyEventType: 'MECHANISM_VIEWED',
  },
  {
    key: 'startingResource',
    stageLabel: 'Primeiro cuidado',
    question: 'Com o que você prefere começar hoje, sem comprar nada novo?',
    options: ['Roupa que já tenho', 'Cabelo e pele', 'Acessório ou perfume', 'Postura e presença'],
    imageUrl: '/assets/musa-diagnostic-slide-4.png',
    visualTitle: 'Escolha por onde você quer começar hoje.',
    visualText: 'Você recebe uma sugestão simples para testar hoje e decide depois se quer continuar o plano completo.',
    journeyEventType: 'CATEGORY_UNDERSTOOD',
  },
];

const musaV7PublicDiagnosticQuestions: PublicDiagnosticQuestion[] = [
  {
    key: 'mainObstacle',
    stageLabel: 'Espelho da dor',
    question: 'Quando você se olha pronta, o que mais faz o look parecer simples demais?',
    options: ['Falta acabamento', 'A roupa parece sem intenção', 'O cabelo derruba o conjunto', 'Não sei o detalhe certo'],
    imageUrl: '/assets/musa-diagnostic-slide-1.png',
    visualTitle: 'A v7 começa pelo espelho antes de sair.',
    visualText: 'O diagnóstico procura o pequeno sinal que faz a composição parecer comum mesmo quando você se arruma.',
    journeyEventType: 'PROBLEM_RECOGNIZED',
  },
  {
    key: 'presenceFocus',
    stageLabel: 'Cena real',
    question: 'Em qual cena você quer parecer mais intencional primeiro?',
    options: ['Trabalho ou reunião', 'Saída casual', 'Encontro', 'Foto ou conteúdo'],
    imageUrl: '/assets/musa-diagnostic-slide-2.png',
    visualTitle: 'A versão v7 trabalha uma cena concreta.',
    visualText: 'Em vez de prometer transformação ampla, ela entrega um ajuste visível para uma situação real.',
    journeyEventType: 'REAL_INPUT_SUBMITTED',
  },
  {
    key: 'desiredSignal',
    stageLabel: 'Sinal desejado',
    question: 'Qual sinal você quer que apareça mais no seu visual?',
    options: ['Elegância simples', 'Cuidado percebido', 'Presença feminina', 'Imagem mais marcante'],
    imageUrl: '/assets/musa-diagnostic-slide-3.png',
    visualTitle: 'A intenção visual guia o plano.',
    visualText: 'O MUSA cruza incômodo, cena e sinal desejado para sugerir um ajuste prático de hoje.',
    journeyEventType: 'MECHANISM_VIEWED',
  },
  {
    key: 'startingResource',
    stageLabel: 'Primeiro ajuste',
    question: 'Qual ponto você aceita ajustar hoje sem comprar nada?',
    options: ['Acabamento do cabelo', 'Roupa que já tenho', 'Acessório ou perfume', 'Postura e presença'],
    imageUrl: '/assets/musa-diagnostic-slide-4.png',
    visualTitle: 'A promessa da v7 é microação antes do plano completo.',
    visualText: 'A cliente recebe um primeiro ajuste agora e depois entende por que continuar por 7 dias.',
    journeyEventType: 'CATEGORY_UNDERSTOOD',
  },
];

const musaExperienceContracts: Record<string, MusaExperienceContract> = {
  'musa-pde-entry-v5-estrada-desejo': {
    experienceVersion: 'musa-pde-entry-v5-estrada-desejo',
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: false,
    usesMotivationalTimelineVideo: false,
    videoPlacements: [],
  },
  [MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION]: {
    experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
    primaryHost: 'v5.clubemusa.com.br',
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: true,
    usesMotivationalTimelineVideo: false,
    videoPlacements: ['public_diagnostic_initial_explainer'],
  },
  [MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION]: {
    experienceVersion: MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION,
    primaryHost: 'v6.clubemusa.com.br',
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: true,
    usesMotivationalTimelineVideo: true,
    videoPlacements: ['public_diagnostic_initial_explainer', 'mechanism_explainer', 'objection_breaker', 'cta_reinforcement'],
  },
  [MUSA_V7_EXPERIENCE_VERSION]: {
    experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
    primaryHost: 'v7.clubemusa.com.br',
    publicDiagnosticQuestions: musaV7PublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: true,
    usesMotivationalTimelineVideo: true,
    videoPlacements: ['opening_mirror', 'visual_proof', 'mechanism_explainer', 'objection_breaker', 'cta_reinforcement'],
  },
};

const MUSA_VERSIONED_HOSTS: Record<string, string> = Object.values(musaExperienceContracts)
  .filter((contract) => contract.primaryHost)
  .reduce<Record<string, string>>((hosts, contract) => {
    hosts[contract.primaryHost as string] = contract.experienceVersion;
    return hosts;
  }, {});

export const fallbackProduct: ProductExperience = {
  slug: 'metodo-musa-7-dias',
  experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
  funnelVersion: 'musa-membership-funnel-v1',
  name: 'Método MUSA - Experiência Guiada de 7 Dias',
  promise: 'Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro.',
  audience: 'Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.',
  priceLabel: 'R$67',
  theme: {
    primary: '#7a2444',
    accent: '#d6a75c',
    background: '#fff8f3',
    imageUrl: '/assets/musa-cover.png',
  },
  diagnostic: {
    title: 'Mapa de Presença MUSA',
    intro: 'Comece pelo espelho: descubra o primeiro passo para sua imagem comunicar mais intenção hoje, usando o que você já tem.',
    questions: ['O que minha imagem comunica hoje?'],
  },
  missions: [
    {
      id: 'dia-1-ruido-visual',
      day: 1,
      title: 'Ler o sinal que sua imagem comunica',
      principle: 'A presença cresce quando você identifica o sinal visual que mais distancia sua imagem da mulher que você quer transmitir.',
      action: 'Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe roupa, cabelo, pele, perfume e detalhe final, identifique o sinal que deixa sua imagem comum ou desalinhada e escolha uma microação para comunicar mais intenção.',
      evidence: 'Frase preenchida: hoje minha imagem comunica menos intenção quando...',
      visualCue: 'Compare a sensação antes/depois de remover ruído visual ou reforçar um sinal de presença.',
    },
  ],
  supportMaterials: [
    {
      title: 'E-book Método MUSA',
      type: 'PDF',
      description: 'Guia de consulta para entender o método, ver exemplos e revisar sua semana.',
      url: '/materials/metodo-musa-ebook.pdf',
    },
    {
      title: 'Experiência Guiada MUSA',
      type: 'HTML',
      description: 'Versão navegável da experiência para consultar a ordem, o diagnóstico e as missões de 7 dias.',
      url: '/materials/experiencia-guiada-musa.html',
    },
    {
      title: 'Plano, Checklists e Templates',
      type: 'CSV',
      description: 'Planilha com a ordem de aplicação, critérios de conclusão e pontos de atenção de cada material.',
      url: '/materials/plano-checklists-e-templates.csv',
    },
    {
      title: 'Mapa Visual MUSA',
      type: 'Infográfico',
      description: 'Resumo visual do método: coerência, redução de ruído e assinatura pessoal.',
      url: '/materials/mapa-visual-musa.png',
    },
  ],
  heroVideos: [
    {
      experienceVersion: MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION,
      placement: 'public_diagnostic_initial_explainer',
      playbackUrl: MUSA_APPROVED_HERO_VIDEO_URL,
      hlsPlaybackUrl: MUSA_APPROVED_HERO_VIDEO_URL,
      autoplay: false,
      muted: false,
      controls: true,
      loop: false,
      playsInline: true,
      source: 'MARKETING_HUB_MANAGED_HLS',
      assetId: 1935,
      experimentVideoAssetId: 22,
      salesVideoProfileId: 35,
      salesVideoJobId: 20462,
      reviewStatus: 'APPROVED',
      status: 'READY',
    },
  ],
  completionOffer: 'Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais.',
};

export function resolveMusaVersionedHostConfig(hostname: string) {
  const experienceVersion = MUSA_VERSIONED_HOSTS[hostname.toLowerCase()];
  return experienceVersion ? { experienceVersion } : undefined;
}

export function resolveMusaExperienceContract(experienceVersion: string) {
  return musaExperienceContracts[experienceVersion] ?? {
    experienceVersion,
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: false,
    supportsPublishedPublicDiagnosticVideoHero: false,
    usesMotivationalTimelineVideo: false,
    videoPlacements: [],
  };
}

export function isMusaDesireRoadExperience(experienceVersion: string) {
  return resolveMusaExperienceContract(experienceVersion).usesDesireRoad;
}

export function isMusaVideoExplainerExperience(experienceVersion: string) {
  return resolveMusaExperienceContract(experienceVersion).supportsPublishedPublicDiagnosticVideoHero;
}

export function isBlockedMusaSlideVideoUrl(videoUrl: string) {
  return [
    '/assets/hls/musa-v5-video-explicativo/',
    '/assets/hls/musa-v6-video-motivacional/',
    '/assets/musa-v5-video-explicativo',
    '/assets/musa-v6-video-motivacional',
  ].some((blockedPath) => videoUrl.includes(blockedPath));
}

export function selectApprovedHeroVideo(productExperience: ProductExperience, experienceVersion: string) {
  return (productExperience.heroVideos ?? []).find((video) =>
    video.experienceVersion === experienceVersion
    && video.placement === 'public_diagnostic_initial_explainer'
    && Boolean(resolveHeroVideoHlsUrl(video))
    && video.status === 'READY'
    && video.reviewStatus === 'APPROVED'
    && ['MARKETING_HUB_APPROVED_EXPERIMENT_VIDEO', 'MARKETING_HUB_MANAGED_HLS'].includes(video.source ?? ''));
}

export function resolveHeroVideoHlsUrl(video: HeroVideo) {
  const hlsPlaybackUrl = video.hlsPlaybackUrl?.trim();
  if (hlsPlaybackUrl?.includes('.m3u8')) {
    return hlsPlaybackUrl;
  }
  const playbackUrl = video.playbackUrl?.trim();
  return playbackUrl?.includes('.m3u8') ? playbackUrl : '';
}

export function resolveHeroVideoUrl(productExperience: ProductExperience, experienceVersion: string, streamOverride = '', videoOverride = '') {
  const approvedHeroVideo = selectApprovedHeroVideo(productExperience, experienceVersion);
  if (approvedHeroVideo) {
    return resolveHeroVideoHlsUrl(approvedHeroVideo);
  }
  if (streamOverride) {
    return isBlockedMusaSlideVideoUrl(streamOverride) ? '' : streamOverride;
  }
  return videoOverride && !isBlockedMusaSlideVideoUrl(videoOverride) ? videoOverride : '';
}
