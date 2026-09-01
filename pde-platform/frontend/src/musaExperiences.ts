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
  completionRole?: "CUSTOMER" | "OPERATION";
  interaction?: MissionInteractionContract;
};

export type MissionInteractionField = {
  key: string;
  label: string;
  placeholder: string;
  options: string[];
};

export type MissionInteractionContract = {
  guidanceType: string;
  kicker: string;
  title: string;
  helperText: string;
  buttonLabel: string;
  loadingLabel: string;
  pendingLabel: string;
  failedLabel: string;
  completedKicker: string;
  nextStepTitle: string;
  nextStepText: string;
  fields: MissionInteractionField[];
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
  layoutKey?: string;
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
  publicDiagnosticQuestions?: PublicDiagnosticQuestion[];
  publicFirstFold?: PublicFirstFold;
  scientificEvidencePack?: ScientificEvidencePack;
  completionOffer: string;
  serviceScope?: {
    includedItems: string[];
    excludedItems: string[];
    deadlineStartsWhen: string;
  };
  publicProofs?: {
    id: string;
    type: "RESPONSE" | "QUALIFICATION_QUESTION" | "FOLLOW_UPS" | "OFFER";
    title: string;
    content?: string;
    items?: string[];
    evidenceLabel: string;
    source: string;
  }[];
  commercialProcess?: {
    order: number;
    title: string;
    description: string;
    timing: string;
  }[];
  commercialBinding?: {
    experimentId: number;
    primaryCta: string;
    priceBrl: number;
    billingModel: "ONE_TIME";
  };
  commercialCheckout?: {
    provider: string;
    checkoutUrl: string;
    offerReference: string;
    priceBrl: number;
    currency: string;
    billingModel: "ONE_TIME";
  };
};

export type PublicFirstFold = {
  headline?: string;
  supportingText?: string;
  videoKicker?: string;
  videoHeadline?: string;
  videoSupportingText?: string;
  videoExtraText?: string;
  videoCtaLabel?: string;
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
  layoutKey: string;
  primaryHost?: string;
  publicDiagnosticQuestions: PublicDiagnosticQuestion[];
  usesDesireRoad: boolean;
  supportsPublishedPublicDiagnosticVideoHero: boolean;
  usesMotivationalTimelineVideo: boolean;
  videoPlacements: string[];
};

export type MusaPointedDomain = {
  host: string;
  url: string;
  observedAddress: string;
  label: string;
  role: "pointed" | "legacy" | "reserved";
  experienceVersion: string;
};

export const MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION =
  "musa-pde-entry-v5-video-explicativo";
export const MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION =
  "musa-pde-entry-v6-video-motivacional";
export const MUSA_V7_EXPERIENCE_VERSION =
  "musa-pde-entry-v7-espelho-antes-de-sair";

const MUSA_V5_LEGACY_HOSTS = ["v1.clubemusa.com.br", "v2.clubemusa.com.br"];
const MUSA_V7_RESERVED_HOSTS = [
  "v8.clubemusa.com.br",
  "v9.clubemusa.com.br",
  "v10.clubemusa.com.br",
];

const MUSA_APPROVED_HERO_VIDEO_URL =
  "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8";

const basePublicDiagnosticQuestions: PublicDiagnosticQuestion[] = [
  {
    key: "mainObstacle",
    stageLabel: "Espelho da dor",
    question: "Quando você se olha pronta, o que mais te incomoda?",
    options: [
      "Pareço comum",
      "Falta acabamento",
      "Nada conversa entre si",
      "Sinto que exagerei",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-1.png",
    visualTitle: "Comece pelo que você sente quando se olha pronta.",
    visualText:
      "Você não precisa saber moda. Só precisa dizer o que percebe no espelho. O MUSA transforma isso em um ajuste prático de elegância acessível.",
    journeyEventType: "PROBLEM_RECOGNIZED",
  },
  {
    key: "presenceFocus",
    stageLabel: "Sua rotina",
    question: "Em qual situação você quer se sentir mais presente primeiro?",
    options: [
      "Trabalho ou reunião",
      "Encontro ou saída",
      "Rotina comum",
      "Foto ou conteúdo",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-2.png",
    visualTitle:
      "Escolha uma cena real em que sua imagem precisa trabalhar por você.",
    visualText:
      "A proposta não é mudar seu estilo inteiro. É encontrar um ajuste pequeno para a situação em que você quer se sentir mais segura primeiro.",
    journeyEventType: "REAL_INPUT_SUBMITTED",
  },
  {
    key: "desiredSignal",
    stageLabel: "Sinal desejado",
    question: "Qual sinal você quer comunicar com mais força nessa cena?",
    options: [
      "Elegância discreta",
      "Segurança",
      "Leveza feminina",
      "Imagem mais marcante",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-3.png",
    visualTitle: "Agora o MUSA conecta incômodo, cena e intenção.",
    visualText:
      "Quando o sinal desejado fica claro, fica mais fácil escolher roupa, acabamento ou detalhe final sem depender de tentativa e erro.",
    journeyEventType: "MECHANISM_VIEWED",
  },
  {
    key: "startingResource",
    stageLabel: "Primeiro cuidado",
    question: "Com o que você prefere começar hoje, sem comprar nada novo?",
    options: [
      "Roupa que já tenho",
      "Cabelo e pele",
      "Acessório ou perfume",
      "Postura e presença",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-4.png",
    visualTitle: "Escolha o recurso que você já tem à mão.",
    visualText:
      "Você recebe uma microação simples para testar hoje. Se fizer sentido, continua para o plano completo de 7 dias.",
    journeyEventType: "CATEGORY_UNDERSTOOD",
  },
];

const musaV7PublicDiagnosticQuestions: PublicDiagnosticQuestion[] = [
  {
    key: "mainObstacle",
    stageLabel: "Mensagem visual",
    question:
      "Se sua imagem falasse antes de você hoje, qual mensagem ela passaria sem intenção?",
    options: [
      "Falta presença",
      "Pareço comum",
      "Estou improvisada",
      "Não comunica meu momento",
      "Minha imagem está coerente; quero apenas organizar minhas escolhas",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-1.png",
    visualTitle: "A jornada começa pelo idioma silencioso da roupa.",
    visualText:
      "A degustação organiza o primeiro sinal visual que você escolheu observar antes de qualquer compra nova.",
    journeyEventType: "PROBLEM_RECOGNIZED",
  },
  {
    key: "presenceFocus",
    stageLabel: "Primeira impressão",
    question: "Em qual cena você quer ser lida com mais intenção primeiro?",
    options: [
      "Trabalho ou reunião",
      "Encontro ou saída",
      "Rotina comum",
      "Foto ou conteúdo",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-2.png",
    visualTitle: "A situação real orienta um ajuste pequeno e observável.",
    visualText:
      "A roupa participa da primeira impressão, mas o contexto e a preferência de cada pessoa importam. O MUSA transforma isso em uma escolha simples para sua rotina.",
    journeyEventType: "REAL_INPUT_SUBMITTED",
  },
  {
    key: "desiredSignal",
    stageLabel: "Sinal desejado",
    question: "Qual sinal você quer reforçar com mais clareza nesta semana?",
    options: [
      "Elegância discreta",
      "Segurança calma",
      "Cuidado percebido",
      "Imagem mais marcante",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-3.png",
    visualTitle: "Cada dia trabalha um sinal de presença.",
    visualText:
      "Mensagem, peça-sinal, estrutura, primeira impressão, cor, assinatura pessoal e fórmula MUSA viram uma sequência prática.",
    journeyEventType: "MECHANISM_VIEWED",
  },
  {
    key: "startingResource",
    stageLabel: "Recurso acessível",
    question: "Qual recurso você prefere usar primeiro sem comprar nada?",
    options: [
      "Roupa que já tenho",
      "Cor e acabamento",
      "Acessório ou perfume",
      "Postura e presença",
    ],
    imageUrl: "/assets/musa-diagnostic-slide-4.png",
    visualTitle: "A promessa é um método simples traduzido em microação.",
    visualText:
      "Você recebe um primeiro ajuste agora e depois entende como os 7 sinais organizam uma presença mais coerente.",
    journeyEventType: "CATEGORY_UNDERSTOOD",
  },
];

const musaExperienceContracts: Record<string, MusaExperienceContract> = {
  "musa-pde-entry-v5-estrada-desejo": {
    experienceVersion: "musa-pde-entry-v5-estrada-desejo",
    layoutKey: "estrada-desejo",
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: false,
    usesMotivationalTimelineVideo: false,
    videoPlacements: [],
  },
  [MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION]: {
    experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
    layoutKey: "video-explicativo",
    primaryHost: "v5.clubemusa.com.br",
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: true,
    usesMotivationalTimelineVideo: false,
    videoPlacements: ["public_diagnostic_initial_explainer"],
  },
  [MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION]: {
    experienceVersion: MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION,
    layoutKey: "video-motivacional",
    primaryHost: "v6.clubemusa.com.br",
    publicDiagnosticQuestions: basePublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: true,
    usesMotivationalTimelineVideo: true,
    videoPlacements: [
      "public_diagnostic_initial_explainer",
      "mechanism_explainer",
      "objection_breaker",
      "cta_reinforcement",
    ],
  },
  [MUSA_V7_EXPERIENCE_VERSION]: {
    experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
    layoutKey: "espelho-antes-de-sair",
    primaryHost: "v7.clubemusa.com.br",
    publicDiagnosticQuestions: musaV7PublicDiagnosticQuestions,
    usesDesireRoad: true,
    supportsPublishedPublicDiagnosticVideoHero: true,
    usesMotivationalTimelineVideo: true,
    videoPlacements: [
      "opening_mirror",
      "visual_proof",
      "mechanism_explainer",
      "objection_breaker",
      "cta_reinforcement",
    ],
  },
};

const musaExperienceContractsByLayout: Record<string, MusaExperienceContract> =
  Object.values(musaExperienceContracts).reduce<
    Record<string, MusaExperienceContract>
  >((layouts, contract) => {
    layouts[contract.layoutKey] = contract;
    return layouts;
  }, {});

const MUSA_VERSIONED_HOSTS: Record<string, string> = Object.values(
  musaExperienceContracts,
)
  .filter((contract) => contract.primaryHost)
  .reduce<Record<string, string>>((hosts, contract) => {
    hosts[contract.primaryHost as string] = contract.experienceVersion;
    return hosts;
  }, {});

MUSA_V7_RESERVED_HOSTS.forEach((host) => {
  MUSA_VERSIONED_HOSTS[host] = MUSA_V7_EXPERIENCE_VERSION;
});

MUSA_V5_LEGACY_HOSTS.forEach((host) => {
  MUSA_VERSIONED_HOSTS[host] = MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION;
});

export const MUSA_POINTED_DOMAINS: MusaPointedDomain[] = [
  {
    host: "v1.clubemusa.com.br",
    url: "https://v1.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Legado v5",
    role: "legacy",
    experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
  },
  {
    host: "v2.clubemusa.com.br",
    url: "https://v2.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Legado v5",
    role: "legacy",
    experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
  },
  {
    host: "v5.clubemusa.com.br",
    url: "https://v5.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Domínio apontado v5",
    role: "pointed",
    experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
  },
  {
    host: "v6.clubemusa.com.br",
    url: "https://v6.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Domínio apontado v6",
    role: "pointed",
    experienceVersion: MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION,
  },
  {
    host: "v7.clubemusa.com.br",
    url: "https://v7.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Domínio apontado v7",
    role: "pointed",
    experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
  },
  {
    host: "v8.clubemusa.com.br",
    url: "https://v8.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Reservado v7",
    role: "reserved",
    experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
  },
  {
    host: "v9.clubemusa.com.br",
    url: "https://v9.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Reservado v7",
    role: "reserved",
    experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
  },
  {
    host: "v10.clubemusa.com.br",
    url: "https://v10.clubemusa.com.br",
    observedAddress: "163.245.200.7",
    label: "Reservado v7",
    role: "reserved",
    experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
  },
];

export const fallbackProduct: ProductExperience = {
  slug: "metodo-musa-7-dias",
  experienceVersion: MUSA_VIDEO_EXPLAINER_EXPERIENCE_VERSION,
  layoutKey: "video-explicativo",
  funnelVersion: "musa-membership-funnel-v1",
  name: "Método MUSA - Experiência Guiada de 7 Dias",
  promise:
    "Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro.",
  audience:
    "Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.",
  priceLabel: "R$67",
  theme: {
    primary: "#7a2444",
    accent: "#d6a75c",
    background: "#fff8f3",
    imageUrl: "/assets/musa-cover.png",
  },
  diagnostic: {
    title: "Mapa de Presença MUSA",
    intro:
      "Comece pelo espelho: descubra o primeiro passo para sua imagem comunicar mais intenção hoje, usando o que você já tem.",
    questions: ["O que minha imagem comunica hoje?"],
  },
  missions: [
    {
      id: "dia-1-ruido-visual",
      day: 1,
      title: "Ler o sinal que sua imagem comunica",
      principle:
        "A presença cresce quando você identifica o sinal visual que mais distancia sua imagem da mulher que você quer transmitir.",
      action:
        "Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe roupa, cabelo, pele, perfume e detalhe final, identifique o sinal que deixa sua imagem comum ou desalinhada e escolha uma microação para comunicar mais intenção.",
      evidence:
        "Frase preenchida: hoje minha imagem comunica menos intenção quando...",
      visualCue:
        "Compare a sensação antes/depois de remover ruído visual ou reforçar um sinal de presença.",
    },
  ],
  supportMaterials: [
    {
      title: "E-book Método MUSA",
      type: "PDF",
      description:
        "Guia de consulta para entender o método, ver exemplos e revisar sua semana.",
      url: "/materials/metodo-musa-ebook.pdf",
    },
    {
      title: "Experiência Guiada MUSA",
      type: "HTML",
      description:
        "Versão navegável da experiência para consultar a ordem, o diagnóstico e as missões de 7 dias.",
      url: "/materials/experiencia-guiada-musa.html",
    },
    {
      title: "Plano, Checklists e Templates",
      type: "CSV",
      description:
        "Planilha com a ordem de aplicação, critérios de conclusão e pontos de atenção de cada material.",
      url: "/materials/plano-checklists-e-templates.csv",
    },
    {
      title: "Mapa Visual MUSA",
      type: "Infográfico",
      description:
        "Resumo visual do método: coerência, redução de ruído e assinatura pessoal.",
      url: "/materials/mapa-visual-musa.png",
    },
  ],
  heroVideos: [
    {
      experienceVersion: MUSA_MOTIVATIONAL_VIDEO_EXPERIENCE_VERSION,
      placement: "public_diagnostic_initial_explainer",
      playbackUrl: MUSA_APPROVED_HERO_VIDEO_URL,
      hlsPlaybackUrl: MUSA_APPROVED_HERO_VIDEO_URL,
      autoplay: false,
      muted: false,
      controls: true,
      loop: false,
      playsInline: true,
      source: "MARKETING_HUB_MANAGED_HLS",
      assetId: 1935,
      experimentVideoAssetId: 22,
      salesVideoProfileId: 35,
      salesVideoJobId: 20462,
      reviewStatus: "APPROVED",
      status: "READY",
    },
  ],
  completionOffer:
    "Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais.",
};

export const musaV7FallbackProduct: ProductExperience = {
  ...fallbackProduct,
  experienceVersion: MUSA_V7_EXPERIENCE_VERSION,
  layoutKey: "espelho-antes-de-sair",
  name: "Método MUSA - Presença Elegante em 7 Dias",
  promise:
    "Organizar em sete dias escolhas práticas de presença elegante prioritariamente com o que a cliente já possui, sem promessa de transformação garantida.",
  audience:
    "Mulheres urbanas que querem elevar presença, cuidado percebido e segurança visual com microações acessíveis.",
  diagnostic: {
    title: "Primeiro ajuste MUSA",
    intro:
      "Responda 4 escolhas rápidas. O MUSA combina suas preferências por regras locais e sugere um ajuste pequeno para você testar com o que já possui.",
    questions: [
      "Que mensagem sua imagem comunica sem intenção?",
      "Em qual cena sua primeira impressão importa mais agora?",
      "Qual sinal de presença você quer reforçar nesta semana?",
    ],
  },
  missions: [
    {
      id: "dia-1-ruido-visual",
      day: 1,
      title: "O espelho não vê roupa, vê mensagem",
      principle:
        "A vestimenta participa da percepção de pessoa: antes de explicar quem você é, sua imagem já envia sinais.",
      action:
        "Vista uma combinação real e escreva a mensagem que ela parece transmitir hoje. Depois escolha um ajuste pequeno para aproximar essa mensagem da mulher que você quer comunicar.",
      evidence: "Frase preenchida: hoje minha imagem parece dizer...",
      visualCue:
        "Observe roupa, cabelo, pele e detalhe final como um conjunto de sinais, não como peças soltas.",
    },
    {
      id: "dia-2-assinatura",
      day: 2,
      title: "A peça que muda seu estado interno",
      principle:
        "Uma peça pode ganhar força quando carrega significado para você e faz sentido na situação escolhida.",
      action:
        "Escolha uma peça-sinal que represente presença, cuidado ou elegância para você. Use essa peça em uma cena simples do dia e registre como ela muda sua postura diante do espelho.",
      evidence: "Peça-sinal escolhida com o significado que ela carrega.",
      visualCue: "Priorize significado e intenção, não preço ou marca.",
    },
    {
      id: "dia-3-base-acessivel",
      day: 3,
      title: "Formalidade sem rigidez",
      principle:
        "Sinais de estrutura e acabamento podem ajudar você a perceber mais intenção, sempre conforme seu contexto e sua preferência.",
      action:
        "Eleve uma combinação comum com um sinal de estrutura: terceira peça, tecido mais firme, sapato, cabelo alinhado ou acabamento melhor definido.",
      evidence:
        "Antes/depois registrado com o detalhe que deixou o look mais intencional.",
      visualCue:
        "Procure o ponto em que o visual fica mais pronto sem parecer duro.",
    },
    {
      id: "dia-4-checklist-12-minutos",
      day: 4,
      title: "Primeiras impressões são leituras rápidas",
      principle:
        "Roupa e acabamento participam da primeira impressão, sem determinar como outras pessoas irão reagir.",
      action:
        "Escolha uma situação real dos próximos dias e monte o primeiro sinal que você quer transmitir ao entrar: calma, cuidado, presença, feminilidade ou segurança.",
      evidence: "Cena escolhida com o primeiro sinal planejado.",
      visualCue:
        "Imagine a primeira leitura do ambiente antes de escolher o detalhe final.",
    },
    {
      id: "dia-5-compra-inteligente",
      day: 5,
      title: "Cor como direção, não decoração",
      principle:
        "Cores podem orientar a coerência visual quando escolhidas segundo a situação e a sua preferência.",
      action:
        "Escolha uma cor-base e uma cor-sinal para comunicar calma, presença, suavidade ou força sem exagerar.",
      evidence: "Paleta de 2 cores registrada para uma ocasião real.",
      visualCue: "Use a cor como guia de intenção, não como enfeite isolado.",
    },
    {
      id: "dia-6-situacao-chave",
      day: 6,
      title: "Assinatura pessoal: ser reconhecida sem esforço",
      principle:
        "Escolhas repetidas podem facilitar uma assinatura pessoal reconhecível por você mesma.",
      action:
        "Defina 3 sinais repetíveis da sua presença: cabelo, cor, acessório, perfume, textura, maquiagem leve ou acabamento.",
      evidence: "Três sinais de assinatura MUSA definidos.",
      visualCue: "Elegância fica mais fácil quando vira repetição inteligente.",
    },
    {
      id: "dia-7-plano-pessoal",
      day: 7,
      title: "Seu algoritmo de presença elegante",
      principle:
        "Como escolhas de roupa dependem de pessoa, contexto e preferência, o método organiza opções sem prometer uma reação externa.",
      action:
        "Transforme suas respostas da semana em uma fórmula pessoal: sinais, ocasiões, regra anti-compra impulsiva e checklist antes de sair.",
      evidence: "Fórmula MUSA pessoal preenchida para repetir por 30 dias.",
      visualCue:
        "Feche a semana com um jeito seu de se arrumar com menos dúvida.",
    },
  ],
  heroVideos: [],
  publicFirstFold: {
    headline: "Sua roupa fala antes de você. Ela está falando com roteiro?",
    supportingText:
      "O Método MUSA organiza escolhas de roupa, acabamento e presença em microações simples, sem exigir um guarda-roupa novo.",
    videoKicker: "Método MUSA em 7 dias",
    videoHeadline:
      "Veja como cada dia organiza um ajuste possível usando o que você já possui.",
    videoSupportingText:
      "Mensagem visual, peça-sinal, formalidade leve, primeira impressão, cor, assinatura pessoal e fórmula MUSA viram uma jornada prática de 7 dias.",
    videoExtraText:
      "Responda 4 escolhas rápidas para organizar um primeiro ajuste possível com o que você já possui.",
    videoCtaLabel: "Ver meu primeiro ajuste MUSA",
  },
  completionOffer:
    "Ao concluir os 7 dias, sua fórmula MUSA e os materiais permanecem disponíveis durante os 90 dias do acesso, sem assinatura ou renovação automática.",
};

export function resolveMusaFallbackProduct(experienceVersion = "") {
  return experienceVersion === MUSA_V7_EXPERIENCE_VERSION
    ? musaV7FallbackProduct
    : fallbackProduct;
}

export function resolveMusaVersionedHostConfig(hostname: string) {
  const normalizedHost = hostname.toLowerCase();
  const experienceVersion = MUSA_VERSIONED_HOSTS[normalizedHost];
  const slotCode = normalizedHost.split(".", 1)[0];
  return experienceVersion
    ? {
        experienceVersion,
        slotCode: /^v\d+$/.test(slotCode) ? slotCode : undefined,
      }
    : undefined;
}

export function resolveMusaExperienceContract(
  experienceVersion: string,
  layoutKey = "",
) {
  const layoutContract = layoutKey
    ? musaExperienceContractsByLayout[layoutKey]
    : undefined;
  if (layoutContract) {
    return {
      ...layoutContract,
      experienceVersion: experienceVersion || layoutContract.experienceVersion,
    };
  }
  return (
    musaExperienceContracts[experienceVersion] ?? {
      experienceVersion,
      layoutKey: "diagnostico-classico",
      publicDiagnosticQuestions: basePublicDiagnosticQuestions,
      usesDesireRoad: false,
      supportsPublishedPublicDiagnosticVideoHero: false,
      usesMotivationalTimelineVideo: false,
      videoPlacements: [],
    }
  );
}

export function isMusaDesireRoadExperience(experienceVersion: string) {
  return resolveMusaExperienceContract(experienceVersion).usesDesireRoad;
}

export function isMusaVideoExplainerExperience(experienceVersion: string) {
  return resolveMusaExperienceContract(experienceVersion)
    .supportsPublishedPublicDiagnosticVideoHero;
}

export function isBlockedMusaSlideVideoUrl(videoUrl: string) {
  return [
    "/assets/hls/musa-v5-video-explicativo/",
    "/assets/hls/musa-v6-video-motivacional/",
    "/assets/musa-v5-video-explicativo",
    "/assets/musa-v6-video-motivacional",
  ].some((blockedPath) => videoUrl.includes(blockedPath));
}

export function selectApprovedHeroVideo(
  productExperience: ProductExperience,
  experienceVersion: string,
) {
  return (productExperience.heroVideos ?? []).find(
    (video) =>
      video.experienceVersion === experienceVersion &&
      video.placement === "public_diagnostic_initial_explainer" &&
      Boolean(resolveHeroVideoHlsUrl(video)) &&
      video.status === "READY" &&
      video.reviewStatus === "APPROVED" &&
      [
        "MARKETING_HUB_APPROVED_EXPERIMENT_VIDEO",
        "MARKETING_HUB_MANAGED_HLS",
      ].includes(video.source ?? ""),
  );
}

export function resolveHeroVideoHlsUrl(video: HeroVideo) {
  const hlsPlaybackUrl = video.hlsPlaybackUrl?.trim();
  if (hlsPlaybackUrl?.includes(".m3u8")) {
    return hlsPlaybackUrl;
  }
  const playbackUrl = video.playbackUrl?.trim();
  return playbackUrl?.includes(".m3u8") ? playbackUrl : "";
}

export function resolveHeroVideoUrl(
  productExperience: ProductExperience,
  experienceVersion: string,
  streamOverride = "",
  videoOverride = "",
) {
  const approvedHeroVideo = selectApprovedHeroVideo(
    productExperience,
    experienceVersion,
  );
  if (approvedHeroVideo) {
    return resolveHeroVideoHlsUrl(approvedHeroVideo);
  }
  if (streamOverride) {
    return isBlockedMusaSlideVideoUrl(streamOverride) ? "" : streamOverride;
  }
  return videoOverride && !isBlockedMusaSlideVideoUrl(videoOverride)
    ? videoOverride
    : "";
}
