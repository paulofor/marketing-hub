import React, { useEffect, useMemo, useRef, useState } from 'react';
import { BookOpen, CalendarDays, Check, ChevronRight, ClipboardCheck, CreditCard, Gauge, KeyRound, Library, LoaderCircle, Lock, LogIn, Mail, Pencil, Sparkles, Target, User } from 'lucide-react';
import { createRoot } from 'react-dom/client';
import './styles.css';

type Theme = {
  primary: string;
  accent: string;
  background: string;
  imageUrl: string;
};

type Diagnostic = {
  title: string;
  intro: string;
  questions: string[];
};

type Mission = {
  id: string;
  day: number;
  title: string;
  principle: string;
  action: string;
  evidence: string;
  visualCue: string;
};

type SupportMaterial = {
  title: string;
  type: string;
  description: string;
  url: string;
};

type ScientificEvidencePack = {
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

type ProductExperience = {
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
  scientificEvidencePack?: ScientificEvidencePack;
  completionOffer: string;
};

type Workspace = {
  product: ProductExperience;
  email: string;
  accessSource: string;
  subscriptionStatus: 'ACTIVE' | 'TRIAL';
  completedMissions: number;
  totalMissions: number;
  progressPercent: number;
  completedMissionIds: string[];
  missionInteractions: MissionInteraction[];
};

type MissionInteraction = {
  missionId: string;
  questionKey: string;
  answerText: string;
};

type AiGuidance = {
  requestId: string;
  productSlug: string;
  missionId: string;
  guidanceType: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  headline?: string;
  summary?: string;
  signals: string[];
  microActions: string[];
  caution?: string;
  errorMessage?: string;
};

type MagicLinkResponse = {
  productSlug: string;
  email: string;
  deliveryStatus: string;
  accessUrl?: string;
};

type DiagnosticOption = {
  key: string;
  label: string;
  description: string;
};

type ApiErrorResponse = {
  error?: string;
};

type TrackingOptions = {
  accessToken?: string;
  email?: string;
  provider?: string;
  source?: string;
  metadata?: Record<string, unknown>;
};

type MissionGuidanceField = {
  key: string;
  label: string;
  placeholder: string;
  options?: string[];
};

type MissionGuidanceConfig = {
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
  fields: MissionGuidanceField[];
};

type PublicDiagnosticQuestion = {
  key: string;
  question: string;
  options: string[];
  imageUrl: string;
  visualTitle: string;
  visualText: string;
};

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: { client_id: string; callback: (response: { credential?: string }) => void }) => void;
          renderButton: (element: HTMLElement, options: Record<string, string | number | boolean>) => void;
        };
      };
    };
  }
}

const PUBLIC_DIAGNOSTIC_MAX_POLL_ATTEMPTS = 90;
const PUBLIC_DIAGNOSTIC_INITIAL_POLL_DELAY_MS = 900;
const PUBLIC_DIAGNOSTIC_POLL_INTERVAL_MS = 1800;

const fallbackProduct: ProductExperience = {
  slug: 'metodo-musa-7-dias',
  experienceVersion: 'musa-pde-entry-v3',
  funnelVersion: 'musa-membership-funnel-v1',
  name: 'Método MUSA - Experiência Guiada de 7 Dias',
  promise: 'Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro.',
  audience: 'Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.',
  priceLabel: '',
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
  completionOffer: 'Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais.',
};

const missionGuidanceConfigs: Record<string, MissionGuidanceConfig> = {
  'dia-1-ruido-visual': {
    guidanceType: 'MUSA_DAY_1_PRESENCE_DIAGNOSIS',
    kicker: 'Consultora MUSA',
    title: 'Conte sua situação real para a Consultora MUSA montar seu Mapa de Presença.',
    helperText: 'Depois de enviar, você vê o que sua imagem pode estar comunicando sem intenção e recebe uma microação para começar hoje.',
    buttonLabel: 'Ver meu plano MUSA de 7 dias',
    loadingLabel: 'Preparando seu plano MUSA...',
    pendingLabel: 'Sua Consultora MUSA está preparando seu mapa e a primeira microação prática para hoje.',
    failedLabel: 'Suas respostas ficaram salvas. Use a missão do Dia 1 manualmente enquanto a consultora automática é configurada.',
    completedKicker: 'Meu Mapa de Presença',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Aplique hoje uma das microações indicadas. Depois volte aqui e toque em “Registrar Dia 1 concluído” para liberar o próximo passo da sua jornada.',
    fields: [
      {
        key: 'presenceFocus',
        label: 'Em qual situação você mais quer se sentir mais presente agora?',
        placeholder: 'Escolha um foco',
        options: ['Trabalho ou reunião', 'Encontro ou saída', 'Rotina comum', 'Foto ou conteúdo'],
      },
      {
        key: 'mainObstacle',
        label: 'O que mais te incomoda quando você se olha pronta?',
        placeholder: 'Escolha um sinal',
        options: ['Pareço comum', 'Falta acabamento', 'Nada conversa entre si', 'Sinto que exagerei'],
      },
      {
        key: 'desiredSignal',
        label: 'Qual sinal você quer comunicar com mais força?',
        placeholder: 'Escolha um sinal',
        options: ['Elegância discreta', 'Segurança', 'Leveza feminina', 'Imagem mais marcante'],
      },
      {
        key: 'mainConstraint',
        label: 'O que mais atrapalha sua imagem no dia a dia?',
        placeholder: 'Escolha uma trava',
        options: ['Pouco tempo', 'Dúvida na roupa', 'Vontade de comprar', 'Falta de constância'],
      },
      {
        key: 'startingResource',
        label: 'Com o que você prefere começar esta semana?',
        placeholder: 'Escolha um recurso',
        options: ['Roupa que já tenho', 'Cabelo e pele', 'Acessório ou perfume', 'Postura e presença'],
      },
    ],
  },
  'dia-2-assinatura': {
    guidanceType: 'MUSA_DAY_2_SIGNATURE',
    kicker: 'Consultora MUSA',
    title: 'Escolha 3 sinais para montar sua assinatura desta semana.',
    helperText: 'Depois de enviar, você recebe uma combinação simples de sinais para repetir elegância sem precisar trocar todo o guarda-roupa.',
    buttonLabel: 'Gerar minha assinatura MUSA',
    loadingLabel: 'Montando assinatura...',
    pendingLabel: 'Sua Consultora MUSA está preparando uma orientação curta com seus 3 sinais.',
    failedLabel: 'Seus sinais ficaram salvos. A consultora automática ainda precisa ser configurada neste ambiente.',
    completedKicker: 'Minha assinatura MUSA',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Escolha uma situação real da semana, use os sinais recomendados e registre a missão quando tiver uma combinação pronta para repetir.',
    fields: [
      {
        key: 'finishSignal',
        label: 'Acabamento principal',
        placeholder: 'Escolha um acabamento',
        options: ['Cabelo polido', 'Pele iluminada', 'Maquiagem leve', 'Roupa com caimento limpo'],
      },
      {
        key: 'baseColor',
        label: 'Cor-base da semana',
        placeholder: 'Escolha uma cor-base',
        options: ['Vinho discreto', 'Preto limpo', 'Off-white', 'Verde oliva', 'Jeans escuro'],
      },
      {
        key: 'memorableSignal',
        label: 'Sinal memorável',
        placeholder: 'Escolha um sinal',
        options: ['Perfume assinatura', 'Brinco luminoso', 'Batom discreto', 'Bolsa estruturada', 'Lenço ou textura suave'],
      },
    ],
  },
  'dia-3-base-acessivel': {
    guidanceType: 'MUSA_DAY_3_WARDROBE_REUSE',
    kicker: 'Consultora MUSA',
    title: 'Mostre o que você já tem para a IA montar uma base elegante acessível.',
    helperText: 'Depois de enviar, você recebe uma base prática usando peças reais, sem transformar o método em lista de compras.',
    buttonLabel: 'Montar minha base acessível',
    loadingLabel: 'Organizando base...',
    pendingLabel: 'Sua Consultora MUSA está conectando seus itens aos sinais escolhidos.',
    failedLabel: 'Seu inventário ficou salvo. Use os itens escolhidos como base da missão de hoje.',
    completedKicker: 'Minha base acessível',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Separe as peças indicadas, monte uma combinação possível e registre a missão quando tiver uma base pronta para usar ou repetir.',
    fields: [
      {
        key: 'pieces',
        label: '5 peças que você já tem',
        placeholder: 'Ex.: calça preta, camisa branca, vestido vinho...',
      },
      {
        key: 'accessories',
        label: '2 acessórios ou acabamentos disponíveis',
        placeholder: 'Ex.: brinco dourado e perfume suave',
      },
      {
        key: 'realOccasion',
        label: 'Para qual situação real essa base precisa funcionar?',
        placeholder: 'Ex.: reunião, jantar, rotina de trabalho',
      },
    ],
  },
  'dia-4-checklist-12-minutos': {
    guidanceType: 'MUSA_DAY_4_FINISHING_RITUAL',
    kicker: 'Consultora MUSA',
    title: 'Transforme seu checklist em um acabamento de 12 minutos.',
    helperText: 'Depois de enviar, você recebe uma ordem simples de acabamento para ganhar presença mesmo com pouco tempo.',
    buttonLabel: 'Criar meu ritual de 12 minutos',
    loadingLabel: 'Ajustando ritual...',
    pendingLabel: 'Sua Consultora MUSA está priorizando o que dá mais presença em menos tempo.',
    failedLabel: 'Seu checklist ficou salvo. Execute a ordem mais simples hoje.',
    completedKicker: 'Meu ritual de acabamento',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Execute a ordem recomendada antes de sair ou gravar conteúdo e registre a missão quando perceber o acabamento mais forte.',
    fields: [
      {
        key: 'availableMinutes',
        label: 'Quanto tempo real você tem antes de sair?',
        placeholder: 'Ex.: 8, 12 ou 15 minutos',
      },
      {
        key: 'weakestFinish',
        label: 'Qual acabamento costuma falhar primeiro?',
        placeholder: 'Ex.: cabelo, pele, roupa, perfume, postura',
      },
      {
        key: 'desiredFeeling',
        label: 'Como você quer se sentir ao sair?',
        placeholder: 'Ex.: limpa, marcante, segura, feminina',
      },
    ],
  },
  'dia-5-compra-inteligente': {
    guidanceType: 'MUSA_DAY_5_ANTI_IMPULSE_DECISION',
    kicker: 'Consultora MUSA',
    title: 'Antes de comprar, deixe a IA testar se o item fortalece sua assinatura.',
    helperText: 'Depois de enviar, você recebe uma decisão mais clara: comprar com intenção, adiar ou reaproveitar algo que já tem.',
    buttonLabel: 'Avaliar minha compra',
    loadingLabel: 'Avaliando compra...',
    pendingLabel: 'Sua Consultora MUSA está separando desejo imediato de utilidade real.',
    failedLabel: 'Sua decisão ficou salva. Compare a compra com seus 3 sinais antes de avançar.',
    completedKicker: 'Minha decisão anti-impulso',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Siga a recomendação sobre compra ou reaproveitamento e registre a missão quando a decisão deixar de ser impulso e virar intenção.',
    fields: [
      {
        key: 'desiredItem',
        label: 'O que você está pensando em comprar?',
        placeholder: 'Ex.: blazer, perfume, bolsa, sapato',
      },
      {
        key: 'buyingReason',
        label: 'Qual sensação você espera resolver com essa compra?',
        placeholder: 'Ex.: parecer mais arrumada, menos comum, mais adulta',
      },
      {
        key: 'fitWithSignature',
        label: 'Como esse item conversa com sua assinatura MUSA?',
        placeholder: 'Ex.: combina com minha cor-base e acabamento',
      },
    ],
  },
  'dia-6-situacao-chave': {
    guidanceType: 'MUSA_DAY_6_OCCASION_ENTRY',
    kicker: 'Consultora MUSA',
    title: 'Planeje uma entrada marcante para uma situação real.',
    helperText: 'Depois de enviar, você recebe um plano objetivo para alinhar roupa, acabamento e detalhe final na ocasião escolhida.',
    buttonLabel: 'Preparar minha entrada',
    loadingLabel: 'Preparando entrada...',
    pendingLabel: 'Sua Consultora MUSA está alinhando roupa, acabamento e detalhe final.',
    failedLabel: 'Seu plano ficou salvo. Use a missão para ajustar a composição antes da ocasião.',
    completedKicker: 'Minha entrada MUSA',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Aplique o plano na ocasião escolhida, observe o sinal mais forte de presença e registre a missão depois de preparar sua entrada.',
    fields: [
      {
        key: 'occasion',
        label: 'Qual é a ocasião?',
        placeholder: 'Ex.: reunião, evento, encontro, gravação, almoço',
      },
      {
        key: 'plannedLook',
        label: 'Qual composição você pretende usar?',
        placeholder: 'Roupa, cabelo, pele, perfume e detalhe final',
      },
      {
        key: 'presenceRisk',
        label: 'O que pode enfraquecer sua presença nesse contexto?',
        placeholder: 'Ex.: pressa, insegurança, roupa sem caimento',
      },
    ],
  },
  'dia-7-plano-pessoal': {
    guidanceType: 'MUSA_DAY_7_MAINTENANCE_PLAN',
    kicker: 'Consultora MUSA',
    title: 'Feche a semana com um plano simples para manter sua presença.',
    helperText: 'Depois de enviar, você recebe um ritual leve para manter sua assinatura sem depender de esforço diário alto.',
    buttonLabel: 'Gerar meu plano pessoal',
    loadingLabel: 'Fechando plano...',
    pendingLabel: 'Sua Consultora MUSA está transformando a semana em um ritual fácil de repetir.',
    failedLabel: 'Seu plano ficou salvo. Releia os sinais e escolha um ritual semanal de 15 minutos.',
    completedKicker: 'Meu plano MUSA',
    nextStepTitle: 'O que fazer agora',
    nextStepText: 'Escolha o ritual recomendado, marque um horário real da semana e registre a missão quando o plano estiver pronto para repetir.',
    fields: [
      {
        key: 'bestSignal',
        label: 'Qual sinal mais funcionou nesta semana?',
        placeholder: 'Ex.: cabelo polido, cor-base vinho, perfume',
      },
      {
        key: 'hardestPoint',
        label: 'Qual ponto ainda exige esforço?',
        placeholder: 'Ex.: manter cabelo, combinar cores, evitar compras',
      },
      {
        key: 'weeklyRitual',
        label: 'Que ritual semanal você consegue repetir?',
        placeholder: 'Ex.: separar 3 combinações no domingo por 15 minutos',
      },
    ],
  },
};

function stableBrowserId(storageKey: string) {
  const existingId = window.localStorage.getItem(storageKey);
  if (existingId) {
    return existingId;
  }
  const generatedId = window.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  window.localStorage.setItem(storageKey, generatedId);
  return generatedId;
}

function resolveDeviceType() {
  const width = window.innerWidth;
  if (width < 768) {
    return 'mobile';
  }
  if (width < 1100) {
    return 'tablet';
  }
  return 'desktop';
}

function readCampaignParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    utmSource: params.get('utm_source') ?? undefined,
    utmMedium: params.get('utm_medium') ?? undefined,
    utmCampaign: params.get('utm_campaign') ?? undefined,
    utmContent: params.get('utm_content') ?? undefined,
    utmTerm: params.get('utm_term') ?? undefined,
  };
}

function isCommercialAnalyticsSuppressed() {
  const params = new URLSearchParams(window.location.search);
  return params.get('mh_preview') === 'qa' || params.get('pde_analytics') === 'off';
}

function resolveUrlHost(url: string) {
  try {
    return new URL(url).hostname;
  } catch {
    return 'invalid_checkout_url';
  }
}

function readRuntimeConfigValue(key: 'VITE_MUSA_CHECKOUT_URL' | 'VITE_GOOGLE_CLIENT_ID' | 'VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE' | 'VITE_MUSA_HERO_VIDEO_URL', fallback = '') {
  return window.__MUSA_RUNTIME_CONFIG__?.[key] || fallback;
}

function applyExperienceOverrides(productExperience: ProductExperience) {
  const experienceVersionOverride = readRuntimeConfigValue('VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE', (import.meta.env.VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE as string | undefined) ?? '');
  if (!experienceVersionOverride) {
    return productExperience;
  }
  return {
    ...productExperience,
    experienceVersion: experienceVersionOverride,
  };
}

const presenceBlockers: DiagnosticOption[] = [
  {
    key: 'descobrir_imagem_comunica_hoje',
    label: 'Descobrir o que minha imagem comunica hoje',
    description: 'Receber uma leitura simples do primeiro sinal que pode deixar sua presença mais intencional.',
  },
];

const desiredPresenceSignals: DiagnosticOption[] = [
  {
    key: 'presenca_elegante',
    label: 'Presença elegante',
    description: 'Transmitir mais cuidado, coerência e sofisticação acessível.',
  },
  {
    key: 'imagem_com_intencao',
    label: 'Imagem com intenção',
    description: 'Sentir que roupa, beleza e detalhe final contam a mesma história.',
  },
];

const publicDiagnosticQuestions: PublicDiagnosticQuestion[] = [
  {
    key: 'presenceFocus',
    question: 'Em qual situação você mais quer se sentir mais presente agora?',
    options: ['Trabalho ou reunião', 'Encontro ou saída', 'Rotina comum', 'Foto ou conteúdo'],
    imageUrl: '/assets/musa-diagnostic-slide-1.svg',
    visualTitle: 'Escolha a cena onde sua presença precisa aparecer primeiro.',
    visualText: 'A pergunta entra como uma conversa no espelho: uma escolha por vez, sem parecer questionário frio.',
  },
  {
    key: 'mainObstacle',
    question: 'O que mais te incomoda quando você se olha pronta?',
    options: ['Pareço comum', 'Falta acabamento', 'Nada conversa entre si', 'Sinto que exagerei'],
    imageUrl: '/assets/musa-diagnostic-slide-2.svg',
    visualTitle: 'Agora identifique o ruído que rouba intenção.',
    visualText: 'Cada resposta muda o foco da jornada e prepara a Consultora MUSA para gerar um plano mais específico.',
  },
  {
    key: 'desiredSignal',
    question: 'Qual sinal você quer comunicar com mais força?',
    options: ['Elegância discreta', 'Segurança', 'Leveza feminina', 'Imagem mais marcante'],
    imageUrl: '/assets/musa-diagnostic-slide-3.svg',
    visualTitle: 'Defina o sinal que deve ficar na memória.',
    visualText: 'A experiência reforça desejo, não obrigação: ela mostra a mulher que a visitante quer comunicar.',
  },
  {
    key: 'mainConstraint',
    question: 'O que mais atrapalha sua imagem no dia a dia?',
    options: ['Pouco tempo', 'Dúvida na roupa', 'Vontade de comprar', 'Falta de constância'],
    imageUrl: '/assets/musa-diagnostic-slide-4.svg',
    visualTitle: 'Traga a barreira real para o plano funcionar na rotina.',
    visualText: 'O movimento entre perguntas mantém atenção e reduz a sensação de esforço até o resultado.',
  },
  {
    key: 'startingResource',
    question: 'Com o que você prefere começar esta semana?',
    options: ['Roupa que já tenho', 'Cabelo e pele', 'Acessório ou perfume', 'Postura e presença'],
    imageUrl: '/assets/musa-diagnostic-slide-5.svg',
    visualTitle: 'Finalize com o primeiro recurso que ela já tem em mãos.',
    visualText: 'O CTA aparece depois de uma pequena vitória: ela já se enxergou e já escolheu o ponto de partida.',
  },
];

function App() {
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [product, setProduct] = useState<ProductExperience>(fallbackProduct);
  const [email, setEmail] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [activeMissionId, setActiveMissionId] = useState('');
  const [authMode, setAuthMode] = useState<'login' | 'register'>('register');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [devAccessUrl, setDevAccessUrl] = useState('');
  const [presenceBlocker, setPresenceBlocker] = useState('');
  const [desiredPresence, setDesiredPresence] = useState('');
  const [publicDiagnosticAnswers, setPublicDiagnosticAnswers] = useState<Record<string, string>>({});
  const [publicDiagnosticStep, setPublicDiagnosticStep] = useState(0);
  const [publicDiagnosticDirection, setPublicDiagnosticDirection] = useState<'forward' | 'backward'>('forward');
  const [publicDiagnosticGuidance, setPublicDiagnosticGuidance] = useState<AiGuidance | null>(null);
  const [publicDiagnosticLoading, setPublicDiagnosticLoading] = useState(false);
  const [missionAnswers, setMissionAnswers] = useState<Record<string, Record<string, string>>>({});
  const [aiGuidanceByMission, setAiGuidanceByMission] = useState<Record<string, AiGuidance>>({});
  const [generatingGuidance, setGeneratingGuidance] = useState(false);
  const [savingInteraction, setSavingInteraction] = useState(false);
  const [missionCompletionStatus, setMissionCompletionStatus] = useState<'idle' | 'processing' | 'success'>('idle');
  const [completedMissionFeedbackId, setCompletedMissionFeedbackId] = useState('');
  const firstUseTrackedRef = useRef(false);
  const visitorIdRef = useRef(stableBrowserId('musaVisitorId'));
  const sessionIdRef = useRef(window.sessionStorage.getItem('musaSessionId') ?? '');
  const visibleStartedAtRef = useRef(Date.now());
  const screenStartedAtRef = useRef(Date.now());
  const currentScreenRef = useRef('');
  const sectionSeenRef = useRef(new Set<string>());
  const fieldFocusSeenRef = useRef(new Set<string>());
  const fieldInputSeenRef = useRef(new Set<string>());
  const scrollMilestoneSeenRef = useRef(new Set<number>());
  const maxScrollDepthRef = useRef(0);
  const emailInputRef = useRef<HTMLInputElement>(null);
  const missionPanelRef = useRef<HTMLElement>(null);
  const googleClientId = readRuntimeConfigValue('VITE_GOOGLE_CLIENT_ID', (import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined) ?? '');
  const checkoutUrl = readRuntimeConfigValue('VITE_MUSA_CHECKOUT_URL', (import.meta.env.VITE_MUSA_CHECKOUT_URL as string | undefined) ?? '');
  const heroVideoUrl = readRuntimeConfigValue('VITE_MUSA_HERO_VIDEO_URL', (import.meta.env.VITE_MUSA_HERO_VIDEO_URL as string | undefined) ?? '');

  const activeMission = useMemo(() => {
    const missionList = workspace?.product.missions ?? product.missions;
    return missionList.find((mission) => mission.id === activeMissionId) ?? missionList[0];
  }, [activeMissionId, product.missions, workspace]);

  useEffect(() => {
    if (!sessionIdRef.current) {
      sessionIdRef.current = window.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      window.sessionStorage.setItem('musaSessionId', sessionIdRef.current);
    }
    trackEvent('PED_ENTRY', {
      source: 'frontend_entry',
      metadata: { actionName: 'app_entry' },
    });
    trackEvent('PAGE_VIEW', {
      source: 'frontend_entry',
      metadata: { actionName: 'page_loaded' },
    });
    const navigationEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
    if (navigationEntry) {
      trackEvent('PAGE_LOAD', {
        source: 'frontend_performance',
        metadata: {
          actionName: 'navigation_timing',
          loadMs: Math.round(navigationEntry.loadEventEnd || navigationEntry.duration),
          domContentLoadedMs: Math.round(navigationEntry.domContentLoadedEventEnd),
        },
      });
    }
    const tokenFromPath = window.location.pathname.match(/^\/access\/([^/]+)/)?.[1] ?? '';
    if (tokenFromPath) {
      setAccessToken(tokenFromPath);
      loadWorkspace(tokenFromPath, true).catch(() => setErrorMessage('Não encontramos esse acesso. Confira o link recebido após a compra.'));
      return;
    }
    fetch('/api/pde/products/metodo-musa-7-dias')
      .then((response) => (response.ok ? response.json() : fallbackProduct))
      .then((data: ProductExperience) => {
        const resolvedProduct = applyExperienceOverrides(data);
        setProduct(resolvedProduct);
        setActiveMissionId(resolvedProduct.missions[0]?.id ?? '');
      })
      .catch(() => {
        const resolvedProduct = applyExperienceOverrides(fallbackProduct);
        setProduct(resolvedProduct);
        setActiveMissionId(resolvedProduct.missions[0]?.id ?? '');
      });
  }, []);

  useEffect(() => {
    const screenName = resolveScreenName();
    if (currentScreenRef.current && currentScreenRef.current !== screenName) {
      flushScreenTime('screen_change');
      scrollMilestoneSeenRef.current.clear();
      maxScrollDepthRef.current = 0;
    }
    currentScreenRef.current = screenName;
    screenStartedAtRef.current = Date.now();
    trackEvent('SCREEN_VIEW', {
      accessToken,
      email: workspace?.email,
      provider: workspace?.accessSource,
      metadata: {
        screenName,
        missionId: activeMission?.id,
        subscriptionStatus: workspace?.subscriptionStatus,
        progressPercent: workspace?.progressPercent,
        actionName: 'screen_view',
      },
    });
  }, [workspace?.email, workspace?.subscriptionStatus, workspace?.progressPercent, accessToken, activeMission?.id, authMode]);

  useEffect(() => {
    const observedSections = Array.from(document.querySelectorAll<HTMLElement>('[data-analytics-section]'));
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const sectionId = (entry.target as HTMLElement).dataset.analyticsSection;
          if (!entry.isIntersecting || !sectionId || sectionSeenRef.current.has(sectionId)) {
            return;
          }
          sectionSeenRef.current.add(sectionId);
          trackEvent('SECTION_VIEW', {
            accessToken,
            email: workspace?.email,
            provider: workspace?.accessSource,
            metadata: { sectionId },
          });
        });
      },
      { threshold: 0.45 },
    );
    observedSections.forEach((section) => observer.observe(section));
    return () => observer.disconnect();
  }, [workspace, accessToken]);

  useEffect(() => {
    const flushVisibleTime = () => {
      const visibleMs = Date.now() - visibleStartedAtRef.current;
      visibleStartedAtRef.current = Date.now();
      if (visibleMs < 1000) {
        return;
      }
      sendTrackingBeacon('PAGE_VISIBLE_TIME', {
        accessToken,
        email: workspace?.email,
        provider: workspace?.accessSource,
        metadata: {
          visibleMs,
          screenName: currentScreenRef.current,
          actionName: 'page_visibility_flush',
        },
      });
      flushScreenTime('page_visibility_flush');
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        flushVisibleTime();
      } else {
        visibleStartedAtRef.current = Date.now();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('pagehide', flushVisibleTime);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('pagehide', flushVisibleTime);
    };
  }, [accessToken, workspace?.email, workspace?.accessSource, product.slug]);

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      const element = event.target instanceof Element ? event.target.closest('button, a, [role="button"], input[type="button"], input[type="submit"]') : null;
      if (!element) {
        return;
      }
      const isLink = element.tagName.toLowerCase() === 'a';
      sendTrackingBeacon(isLink ? 'LINK_CLICK' : 'UI_CLICK', {
        accessToken,
        email: workspace?.email,
        provider: workspace?.accessSource,
        metadata: {
          ...describeInteractiveElement(element),
          screenName: currentScreenRef.current,
          sectionId: resolveAnalyticsSection(element),
          actionName: isLink ? 'link_click' : 'ui_click',
        },
      });
    };

    const handleFocusIn = (event: FocusEvent) => {
      const element = resolveFieldElement(event.target);
      if (!element) {
        return;
      }
      const field = describeFieldElement(element);
      const key = `${window.location.pathname}:${field.fieldName}:focus`;
      if (fieldFocusSeenRef.current.has(key)) {
        return;
      }
      fieldFocusSeenRef.current.add(key);
      sendTrackingBeacon('FIELD_FOCUS', {
        accessToken,
        email: workspace?.email,
        provider: workspace?.accessSource,
        metadata: {
          ...field,
          screenName: currentScreenRef.current,
          sectionId: resolveAnalyticsSection(element),
          actionName: 'field_focus',
        },
      });
    };

    const handleInput = (event: Event) => {
      const element = resolveFieldElement(event.target);
      if (!element) {
        return;
      }
      const field = describeFieldElement(element);
      const key = `${window.location.pathname}:${field.fieldName}:input`;
      if (fieldInputSeenRef.current.has(key)) {
        return;
      }
      fieldInputSeenRef.current.add(key);
      sendTrackingBeacon('FIELD_INPUT', {
        accessToken,
        email: workspace?.email,
        provider: workspace?.accessSource,
        metadata: {
          ...field,
          screenName: currentScreenRef.current,
          sectionId: resolveAnalyticsSection(element),
          actionName: 'field_input_started',
        },
      });
    };

    const handleFocusOut = (event: FocusEvent) => {
      const element = resolveFieldElement(event.target);
      if (!element) {
        return;
      }
      const field = describeFieldElement(element);
      sendTrackingBeacon(field.valueLength > 0 ? 'FIELD_FILLED' : 'FIELD_ABANDONED', {
        accessToken,
        email: workspace?.email,
        provider: workspace?.accessSource,
        metadata: {
          ...field,
          screenName: currentScreenRef.current,
          sectionId: resolveAnalyticsSection(element),
          actionName: field.valueLength > 0 ? 'field_filled' : 'field_abandoned',
        },
      });
    };

    const handleScroll = () => {
      const scrollDepth = calculateScrollDepth();
      maxScrollDepthRef.current = Math.max(maxScrollDepthRef.current, scrollDepth);
      [25, 50, 75, 90, 100].forEach((milestone) => {
        if (scrollDepth < milestone || scrollMilestoneSeenRef.current.has(milestone)) {
          return;
        }
        scrollMilestoneSeenRef.current.add(milestone);
        sendTrackingBeacon('SCROLL_DEPTH', {
          accessToken,
          email: workspace?.email,
          provider: workspace?.accessSource,
          metadata: {
            screenName: currentScreenRef.current,
            scrollDepthPercent: milestone,
            maxScrollDepthPercent: maxScrollDepthRef.current,
            actionName: 'scroll_depth',
          },
        });
      });
    };

    document.addEventListener('click', handleClick, true);
    document.addEventListener('focusin', handleFocusIn, true);
    document.addEventListener('input', handleInput, true);
    document.addEventListener('focusout', handleFocusOut, true);
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      document.removeEventListener('click', handleClick, true);
      document.removeEventListener('focusin', handleFocusIn, true);
      document.removeEventListener('input', handleInput, true);
      document.removeEventListener('focusout', handleFocusOut, true);
      window.removeEventListener('scroll', handleScroll);
    };
  }, [accessToken, workspace?.email, workspace?.accessSource]);

  useEffect(() => {
    if (!googleClientId || workspace) {
      return;
    }
    const scriptId = 'google-identity-services';
    const renderGoogleButton = () => {
      const container = document.getElementById('google-login-button');
      if (!container || !window.google) {
        return;
      }
      container.innerHTML = '';
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: (response) => {
          if (response.credential) {
            submitGoogleAccess(response.credential);
          }
        },
      });
      window.google.accounts.id.renderButton(container, {
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        width: 320,
      });
    };
    const existingScript = document.getElementById(scriptId);
    if (existingScript) {
      renderGoogleButton();
      return;
    }
    const script = document.createElement('script');
    script.id = scriptId;
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = renderGoogleButton;
    document.head.appendChild(script);
  }, [googleClientId, workspace]);

  useEffect(() => {
    if (workspace?.subscriptionStatus === 'TRIAL') {
      trackEvent('PAYWALL_VIEWED', {
        accessToken,
        email: workspace.email,
        provider: workspace.accessSource,
        metadata: { placement: 'dashboard_paywall' },
      });
    }
  }, [workspace?.subscriptionStatus, accessToken]);

  async function submitAccess() {
    if (authMode === 'register' && (!presenceBlocker || !desiredPresence)) {
      setErrorMessage('Toque primeiro para descobrir o que sua imagem comunica hoje.');
      return;
    }
    if (!email.trim()) {
      setErrorMessage(authMode === 'login' ? 'Informe o e-mail que você usou para criar seu acesso MUSA.' : 'Informe seu melhor e-mail para receber o Mapa de Presença.');
      return;
    }
    setLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    setDevAccessUrl('');
    const endpoint = authMode === 'login' ? '/api/pde/access/login-link' : '/api/pde/access/magic-link';
    try {
      await trackEvent('LOGIN_STARTED', {
        email,
        provider: 'EMAIL_MAGIC_LINK',
        metadata: {
          authMode,
          presenceBlocker: authMode === 'register' ? presenceBlocker : undefined,
          desiredPresence: authMode === 'register' ? desiredPresence : undefined,
        },
      });
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, email }),
      });
      if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}) as ApiErrorResponse);
        throw new Error(errorBody.error ?? 'Não foi possível enviar o link de acesso.');
      }
      const result: MagicLinkResponse = await response.json();
      if (result.accessUrl) {
        setDevAccessUrl(result.accessUrl);
      }
      setSuccessMessage(resolveMagicLinkMessage(result));
    } catch (error) {
      if (authMode === 'login' && error instanceof Error && error.message.includes('Cadastro')) {
        setErrorMessage('Não encontramos cadastro com esse e-mail. Use “Primeiro acesso” para entrar gratuitamente e liberar o Dia 1.');
      } else {
        setErrorMessage('Não conseguimos enviar seu link agora. Confira o e-mail e tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  async function submitGoogleAccess(idToken: string) {
    setLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      await trackEvent('LOGIN_STARTED', {
        provider: 'GOOGLE',
        metadata: { authMode: 'google' },
      });
      const response = await fetch('/api/pde/access/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, idToken }),
      });
      if (!response.ok) {
        throw new Error('Google não autorizado.');
      }
      const access = await response.json();
      setAccessToken(access.token);
      window.history.replaceState(null, '', access.accessUrl);
      await loadWorkspace(access.token, true);
    } catch {
      setErrorMessage('Não conseguimos entrar com Google agora. Use o link por e-mail como alternativa.');
    } finally {
      setLoading(false);
    }
  }

  async function loadWorkspace(token: string, resetScroll = false) {
    const response = await fetch(`/api/pde/access/${token}/workspace`);
    if (!response.ok) {
      throw new Error('Acesso não encontrado.');
    }
    const data = (await response.json()) as Workspace;
    const resolvedWorkspace = {
      ...data,
      product: applyExperienceOverrides(data.product),
    };
    setWorkspace(resolvedWorkspace);
    setProduct(resolvedWorkspace.product);
    setActiveMissionId(resolvedWorkspace.product.missions[0]?.id ?? '');
    setMissionAnswers(resolveAllMissionAnswers(resolvedWorkspace));
    if (resetScroll) {
      window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'auto' }));
    }
  }

  async function trackEvent(eventType: string, options: TrackingOptions = {}) {
    if (isCommercialAnalyticsSuppressed()) {
      return;
    }
    try {
      await fetch('/api/pde/access/events', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(buildTrackingPayload(eventType, options)),
      });
    } catch {
      // Eventos de funil não devem bloquear login, compra ou consumo da experiência.
    }
  }

  function sendTrackingBeacon(eventType: string, options: TrackingOptions = {}) {
    if (isCommercialAnalyticsSuppressed()) {
      return;
    }
    const payload = JSON.stringify(buildTrackingPayload(eventType, options));
    if (navigator.sendBeacon) {
      navigator.sendBeacon('/api/pde/access/events', new Blob([payload], { type: 'application/json' }));
      return;
    }
    void fetch('/api/pde/access/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload,
      keepalive: true,
    }).catch(() => undefined);
  }

  function flushScreenTime(actionName: string) {
    const screenVisibleMs = Date.now() - screenStartedAtRef.current;
    screenStartedAtRef.current = Date.now();
    if (!currentScreenRef.current || screenVisibleMs < 1000) {
      return;
    }
    sendTrackingBeacon('SCREEN_TIME', {
      accessToken,
      email: workspace?.email,
      provider: workspace?.accessSource,
      metadata: {
        screenName: currentScreenRef.current,
        visibleMs: screenVisibleMs,
        maxScrollDepthPercent: maxScrollDepthRef.current,
        actionName,
      },
    });
  }

  function buildTrackingPayload(eventType: string, options: TrackingOptions = {}) {
    const campaignParams = readCampaignParams();
    return {
      productSlug: product.slug,
      eventType,
      accessToken: options.accessToken,
      email: options.email,
      provider: options.provider,
      source: options.source ?? 'pde-platform-frontend',
      pageUrl: window.location.href,
      metadata: {
        visitorId: visitorIdRef.current,
        sessionId: sessionIdRef.current,
        referrerUrl: document.referrer || undefined,
        deviceType: resolveDeviceType(),
        screenWidth: window.screen.width,
        screenHeight: window.screen.height,
        viewportWidth: window.innerWidth,
        viewportHeight: window.innerHeight,
        path: window.location.pathname,
        experienceVersion: resolveExperienceVersion(product),
        funnelVersion: product.funnelVersion,
        ...campaignParams,
        ...options.metadata,
      },
    };
  }

  function resolveExperienceVersion(productExperience: ProductExperience) {
    return productExperience.experienceVersion || 'sem-versao';
  }

  function resolveScreenName() {
    if (!workspace) {
      return authMode === 'login' ? 'login_returning_customer' : 'login_first_access';
    }
    if (!hasActiveSubscription && dayOneCompleted) {
      return 'member_paywall_after_day_1';
    }
    return activeMission ? `member_mission_day_${activeMission.day}` : 'member_dashboard';
  }

  function resolveAnalyticsSection(element: Element) {
    const section = element.closest<HTMLElement>('[data-analytics-section]');
    return section?.dataset.analyticsSection;
  }

  function normalizeElementText(text: string | null | undefined) {
    const normalized = (text ?? '').replace(/\s+/g, ' ').trim();
    return normalized.length > 80 ? `${normalized.slice(0, 77)}...` : normalized || undefined;
  }

  function describeInteractiveElement(element: Element) {
    const htmlElement = element as HTMLElement;
    const input = element instanceof HTMLInputElement ? element : null;
    const link = element instanceof HTMLAnchorElement ? element : null;
    return {
      elementTag: element.tagName.toLowerCase(),
      elementRole: htmlElement.getAttribute('role') ?? undefined,
      elementType: input?.type,
      elementText: normalizeElementText(htmlElement.innerText || htmlElement.getAttribute('aria-label') || input?.value),
      elementLabel: normalizeElementText(htmlElement.getAttribute('aria-label') || htmlElement.getAttribute('title')),
      elementClass: htmlElement.className ? String(htmlElement.className).slice(0, 120) : undefined,
      disabled: element.hasAttribute('disabled') || htmlElement.getAttribute('aria-disabled') === 'true',
      hrefHost: link?.href ? resolveUrlHost(link.href) : undefined,
      hrefPath: link?.href ? new URL(link.href).pathname : undefined,
    };
  }

  function resolveFieldElement(target: EventTarget | null) {
    if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target instanceof HTMLSelectElement) {
      return target;
    }
    return null;
  }

  function describeFieldElement(element: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement) {
    const label = element.closest('label');
    const fieldName = element.name || element.id || element.getAttribute('aria-label') || normalizeElementText(label?.textContent) || element.type;
    return {
      fieldName: normalizeElementText(fieldName),
      fieldTag: element.tagName.toLowerCase(),
      fieldType: element instanceof HTMLInputElement ? element.type : element.tagName.toLowerCase(),
      valueLength: element.value.length,
      filled: element.value.trim().length > 0,
    };
  }

  function calculateScrollDepth() {
    const documentElement = document.documentElement;
    const scrollableHeight = Math.max(1, documentElement.scrollHeight - window.innerHeight);
    return Math.min(100, Math.round((window.scrollY / scrollableHeight) * 100));
  }

  async function handleSubscriptionClick() {
    if (!workspace) {
      return;
    }
    await trackEvent('SUBSCRIPTION_CLICKED', {
      accessToken,
      email: workspace.email,
      provider: workspace.accessSource,
      metadata: { checkoutConfigured: Boolean(checkoutUrl) },
    });
    if (checkoutUrl) {
      await trackEvent('CHECKOUT_STARTED', {
        accessToken,
        email: workspace.email,
        provider: workspace.accessSource,
        metadata: {
          actionName: 'checkout_opened',
          checkoutHost: resolveUrlHost(checkoutUrl),
        },
      });
      window.open(checkoutUrl, '_blank', 'noopener,noreferrer');
      return;
    }
    setErrorMessage('Checkout de assinatura ainda não configurado para este ambiente.');
  }

  function editAccessEmail() {
    setSuccessMessage('');
    setDevAccessUrl('');
    emailInputRef.current?.focus();
    emailInputRef.current?.select();
  }

  function startPresenceMap() {
    const blocker = presenceBlockers[0];
    const desiredSignal = desiredPresenceSignals[0];
    setPresenceBlocker(blocker.key);
    setDesiredPresence(desiredSignal.key);
    setErrorMessage('');
    trackEvent('PRESENCE_MAP_CHOICE_SELECTED', {
      metadata: {
        authMode,
        diagnosticStep: 'single_cta',
        selectedOption: blocker.key,
        desiredSignal: desiredSignal.key,
        actionName: 'presence_map_single_cta_clicked',
      },
    });
    trackEvent('DIAGNOSTIC_CHOICE_SELECTED', {
      metadata: {
        authMode,
        diagnosticStep: 'single_cta',
        selectedOption: blocker.key,
        desiredSignal: desiredSignal.key,
        actionName: 'diagnostic_single_cta_clicked',
      },
    });
    window.requestAnimationFrame(() => emailInputRef.current?.focus());
  }

  function resolveMagicLinkMessage(result: MagicLinkResponse) {
    if (result.deliveryStatus === 'SENT') {
      return authMode === 'login' ? 'Enviamos um novo link para seu e-mail. Abra o link para voltar à sua Área MUSA.' : 'Seu primeiro acesso foi criado. Abra o link enviado por e-mail para ver o diagnóstico e começar o Dia 1.';
    }
    if (result.accessUrl) {
      return authMode === 'login' ? 'Link de teste encontrado para esse cadastro. Use o botão Abrir minha Área MUSA para voltar.' : 'Primeiro acesso de teste criado. Use o botão Abrir minha Área MUSA para ver o diagnóstico e começar o Dia 1.';
    }
    if (result.deliveryStatus === 'EMAIL_SEND_FAILED') {
      return 'Seu acesso foi criado, mas o e-mail ainda não pôde ser entregue. A equipe MUSA precisa concluir a configuração do domínio de envio.';
    }
    return 'O envio por e-mail ainda não está configurado neste ambiente. Configure o envio ou habilite o link de teste para entrar.';
  }

  function resolvePublicDiagnosticEmailMessage(result: MagicLinkResponse) {
    if (result.deliveryStatus === 'SENT') {
      return 'Enviei para seu e-mail o caminho para salvar seu diagnóstico e abrir o roteiro detalhado dos 7 dias.';
    }
    if (result.accessUrl) {
      return 'Seu roteiro detalhado foi liberado em ambiente de teste. Use o botão para abrir sua Área MUSA.';
    }
    if (result.deliveryStatus === 'EMAIL_SEND_FAILED') {
      return 'Seu acesso foi criado, mas o e-mail ainda não pôde ser entregue. A equipe MUSA precisa concluir a configuração do domínio de envio.';
    }
    return 'Seu acesso foi criado, mas o envio por e-mail ainda não está configurado neste ambiente.';
  }

  function openDevAccess(accessUrl: string) {
    window.history.replaceState(null, '', accessUrl);
    const token = accessUrl.split('/access/')[1] ?? '';
    setAccessToken(token);
    loadWorkspace(token, true);
  }

  function trackFirstUse(activationType: string, metadata: Record<string, unknown> = {}) {
    if (!workspace || !hasActiveSubscription || firstUseTrackedRef.current) {
      return;
    }
    firstUseTrackedRef.current = true;
    trackEvent('FIRST_USE', {
      accessToken,
      email: workspace.email,
      provider: workspace.accessSource,
      metadata: { activationType, ...metadata },
    });
  }

  function openMission(missionId: string, activationType = 'mission_open') {
    setActiveMissionId(missionId);
    window.setTimeout(() => {
      missionPanelRef.current?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    }, 40);
    trackEvent('MISSION_OPEN', {
      accessToken,
      email: workspace?.email,
      provider: workspace?.accessSource,
      metadata: { missionId, actionName: activationType },
    });
    trackFirstUse(activationType, { missionId });
  }

  async function completeMission(missionId: string) {
    if (!accessToken) {
      return;
    }
    setMissionCompletionStatus('processing');
    setCompletedMissionFeedbackId('');
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const [response] = await Promise.all([
        fetch(`/api/pde/access/${accessToken}/missions/${missionId}/complete`, {
          method: 'POST',
        }),
        new Promise((resolve) => window.setTimeout(resolve, 900)),
      ]);
      if (!response.ok) {
        throw new Error('Não foi possível registrar a missão.');
      }
      const data = await response.json();
      setWorkspace(data);
      setMissionCompletionStatus('success');
      setCompletedMissionFeedbackId(missionId);
      trackEvent('MISSION_COMPLETED', {
        accessToken,
        email: data.email,
        provider: data.accessSource,
        metadata: { missionId },
      });
    } catch {
      setMissionCompletionStatus('idle');
      setErrorMessage('Não conseguimos registrar sua conclusão agora. Tente novamente em alguns instantes.');
    }
  }

  async function saveMissionInteraction(missionId: string) {
    if (!accessToken || !workspace) {
      return;
    }
    const answers = sanitizeAnswers(missionAnswers[missionId] ?? {});
    if (Object.keys(answers).length < 3) {
      setErrorMessage('Preencha os 3 pontos da missão para salvar sua personalização.');
      return;
    }
    setSavingInteraction(true);
    setErrorMessage('');
    try {
      const response = await fetch(`/api/pde/access/${accessToken}/missions/${missionId}/interactions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers }),
      });
      if (!response.ok) {
        throw new Error('Não foi possível salvar sua personalização.');
      }
      const data = await response.json();
      setWorkspace(data);
      setMissionAnswers(resolveAllMissionAnswers(data));
      trackEvent('MISSION_INTERACTION_SAVED', {
        accessToken,
        email: data.email,
        provider: data.accessSource,
        metadata: { missionId, answerKeys: Object.keys(answers) },
      });
      setSuccessMessage('Personalização salva. Agora registre a conclusão quando executar sua microação.');
    } catch {
      setErrorMessage('Não conseguimos salvar sua personalização agora. Tente novamente antes de concluir a missão.');
    } finally {
      setSavingInteraction(false);
    }
  }

  async function requestMissionGuidance(missionId: string) {
    if (!accessToken || !workspace) {
      return;
    }
    const config = missionGuidanceConfigs[missionId];
    if (!config) {
      await saveMissionInteraction(missionId);
      return;
    }
    const answers = sanitizeAnswers(missionAnswers[missionId] ?? {});
    if (Object.keys(answers).length < 3) {
      setErrorMessage('Preencha os 3 pontos para a Consultora MUSA montar sua orientação.');
      return;
    }
    setGeneratingGuidance(true);
    setAiGuidanceByMission((current) => {
      const updated = { ...current };
      delete updated[missionId];
      return updated;
    });
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/pde/access/${accessToken}/missions/${missionId}/ai-guidance`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ guidanceType: config.guidanceType, answers }),
      });
      if (!response.ok) {
        throw new Error('Não foi possível solicitar sua orientação MUSA.');
      }
      const guidance = (await response.json()) as AiGuidance;
      setAiGuidanceByMission((current) => ({
        ...current,
        [missionId]: guidance,
      }));
      setMissionAnswers(resolveAllMissionAnswers(await refreshWorkspace()));
      await pollGuidanceUntilFinished(guidance.requestId);
    } catch {
      setErrorMessage('Não conseguimos acionar a Consultora MUSA agora. Suas respostas podem ser salvas e usadas manualmente.');
    } finally {
      setGeneratingGuidance(false);
    }
  }

  async function refreshWorkspace() {
    const response = await fetch(`/api/pde/access/${accessToken}/workspace`);
    if (!response.ok) {
      throw new Error('Acesso não encontrado.');
    }
    const data = (await response.json()) as Workspace;
    const resolvedWorkspace = {
      ...data,
      product: applyExperienceOverrides(data.product),
    };
    setWorkspace(resolvedWorkspace);
    setProduct(resolvedWorkspace.product);
    return resolvedWorkspace;
  }

  async function pollGuidanceUntilFinished(requestId: string) {
    for (let attempt = 0; attempt < 10; attempt += 1) {
      await new Promise((resolve) => window.setTimeout(resolve, attempt === 0 ? 900 : 1800));
      const response = await fetch(`/api/pde/access/${accessToken}/ai-guidance/${requestId}`);
      if (!response.ok) {
        throw new Error('Orientação não encontrada.');
      }
      const guidance = (await response.json()) as AiGuidance;
      setAiGuidanceByMission((current) => ({
        ...current,
        [guidance.missionId]: guidance,
      }));
      if (guidance.status === 'COMPLETED' || guidance.status === 'FAILED') {
        return;
      }
    }
  }

  async function submitPublicPresenceDiagnostic() {
    const answers = sanitizeAnswers(publicDiagnosticAnswers);
    if (Object.keys(answers).length < publicDiagnosticQuestions.length) {
      setErrorMessage('Responda as 5 perguntas para a Consultora MUSA montar seu plano personalizado.');
      return;
    }
    setPublicDiagnosticLoading(true);
    setPublicDiagnosticGuidance(null);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      await trackEvent('DIAGNOSTIC_CHOICE_SELECTED', {
        metadata: {
          diagnosticStep: 'presence_diagnostic_5_questions',
          answerKeys: Object.keys(answers),
          actionName: 'presence_diagnostic_submitted',
        },
      });
      const response = await fetch('/api/pde/public/presence-diagnostic', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers }),
      });
      if (!response.ok) {
        throw new Error('Não foi possível solicitar o diagnóstico.');
      }
      const guidance = (await response.json()) as AiGuidance;
      setPublicDiagnosticGuidance(guidance);
      await pollPublicPresenceDiagnostic(guidance.requestId);
    } catch {
      setPublicDiagnosticGuidance(null);
      setErrorMessage('Não conseguimos acionar a Consultora MUSA agora. Tente enviar novamente em alguns instantes.');
    } finally {
      setPublicDiagnosticLoading(false);
    }
  }

  async function submitPublicDiagnosticEmail() {
    if (!publicDiagnosticGuidance || publicDiagnosticGuidance.status !== 'COMPLETED') {
      setErrorMessage('Envie o diagnóstico antes para a Consultora MUSA montar seu plano.');
      return;
    }
    if (!email.trim()) {
      setErrorMessage('Informe seu melhor e-mail para receber o roteiro detalhado do seu plano de 7 dias.');
      return;
    }
    setLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    setDevAccessUrl('');
    try {
      await trackEvent('LOGIN_STARTED', {
        email,
        provider: 'EMAIL_MAGIC_LINK',
        metadata: {
          authMode: 'public_diagnostic_plan',
          diagnosticRequestId: publicDiagnosticGuidance.requestId,
          actionName: 'public_diagnostic_email_submitted',
        },
      });
      const response = await fetch('/api/pde/access/magic-link', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, email }),
      });
      if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}) as ApiErrorResponse);
        throw new Error(errorBody.error ?? 'Não foi possível enviar seu roteiro por e-mail.');
      }
      const result: MagicLinkResponse = await response.json();
      if (result.accessUrl) {
        setDevAccessUrl(result.accessUrl);
      }
      setSuccessMessage(resolvePublicDiagnosticEmailMessage(result));
    } catch {
      setErrorMessage('Não conseguimos enviar seu roteiro agora. Confira o e-mail e tente novamente.');
    } finally {
      setLoading(false);
    }
  }

  async function pollPublicPresenceDiagnostic(requestId: string) {
    for (let attempt = 0; attempt < PUBLIC_DIAGNOSTIC_MAX_POLL_ATTEMPTS; attempt += 1) {
      await new Promise((resolve) =>
        window.setTimeout(resolve, attempt === 0 ? PUBLIC_DIAGNOSTIC_INITIAL_POLL_DELAY_MS : PUBLIC_DIAGNOSTIC_POLL_INTERVAL_MS),
      );
      const response = await fetch(`/api/pde/public/presence-diagnostic/${requestId}`);
      if (!response.ok) {
        throw new Error('Diagnóstico não encontrado.');
      }
      const guidance = (await response.json()) as AiGuidance;
      setPublicDiagnosticGuidance(guidance);
      if (guidance.status === 'COMPLETED' || guidance.status === 'FAILED') {
        return;
      }
    }
    throw new Error('Tempo limite ao aguardar diagnóstico público.');
  }

  function resolveAllMissionAnswers(workspaceData: Workspace) {
    return (workspaceData.missionInteractions ?? []).reduce<Record<string, Record<string, string>>>((answers, interaction) => {
      answers[interaction.missionId] = answers[interaction.missionId] ?? {};
      answers[interaction.missionId][interaction.questionKey] = interaction.answerText;
      return answers;
    }, {});
  }

  function sanitizeAnswers(answers: Record<string, string>) {
    return Object.fromEntries(
      Object.entries(answers)
        .map(([key, value]) => [key, value.trim()])
        .filter(([, value]) => value),
    );
  }

  const currentProduct = workspace?.product ?? product;
  const completedMissionIds = new Set(workspace?.completedMissionIds ?? []);
  const firstMission = currentProduct.missions[0];
  const nextMission = currentProduct.missions.find((mission) => !completedMissionIds.has(mission.id)) ?? currentProduct.missions[0];
  const nextMissionIsFirstMission = Boolean(firstMission && nextMission?.id === firstMission.id);
  const hasActiveSubscription = workspace?.subscriptionStatus === 'ACTIVE';
  const dayOneCompleted = Boolean(firstMission && completedMissionIds.has(firstMission.id));
  const trialNeedsPaymentForNextDay = Boolean(!hasActiveSubscription && dayOneCompleted && nextMission && !nextMissionIsFirstMission);
  const canCompleteActiveMission = Boolean(activeMission && (hasActiveSubscription || activeMission.id === firstMission?.id));
  const activeMissionGuidanceConfig = activeMission ? missionGuidanceConfigs[activeMission.id] : undefined;
  const activeMissionAnswers = activeMission ? (missionAnswers[activeMission.id] ?? {}) : {};
  const activeMissionGuidance = activeMission ? aiGuidanceByMission[activeMission.id] : undefined;
  const selectedBlocker = presenceBlockers.find((option) => option.key === presenceBlocker);
  const selectedDesiredPresence = desiredPresenceSignals.find((option) => option.key === desiredPresence);
  const diagnosticReadyForEmail = authMode === 'login' || Boolean(presenceBlocker && desiredPresence);
  const showVideoHero = resolveExperienceVersion(currentProduct) === 'musa-pde-entry-v4-video-hero';
  const canRegisterActiveMission = Boolean(canCompleteActiveMission && (!activeMissionGuidanceConfig || isMissionInteractionSaved(activeMission?.id ?? '')));

  function isMissionInteractionSaved(missionId: string) {
    const config = missionGuidanceConfigs[missionId];
    if (!config) {
      return true;
    }
    const answers = missionAnswers[missionId] ?? {};
    return config.fields.every((field) => answers[field.key]?.trim());
  }

  function updateMissionAnswer(missionId: string, key: string, value: string) {
    setMissionAnswers((current) => ({
      ...current,
      [missionId]: {
        ...(current[missionId] ?? {}),
        [key]: value,
      },
    }));
  }

  function updatePublicDiagnosticAnswer(key: string, value: string) {
    setPublicDiagnosticAnswers((current) => ({
      ...current,
      [key]: value,
    }));
    const answeredQuestionIndex = publicDiagnosticQuestions.findIndex((question) => question.key === key);
    if (answeredQuestionIndex >= 0 && answeredQuestionIndex < publicDiagnosticQuestions.length - 1) {
      window.setTimeout(() => goToPublicDiagnosticStep(answeredQuestionIndex + 1), 180);
    }
  }

  function goToPublicDiagnosticStep(nextStep: number) {
    const boundedNextStep = Math.max(0, Math.min(publicDiagnosticQuestions.length - 1, nextStep));
    setPublicDiagnosticDirection(boundedNextStep >= publicDiagnosticStep ? 'forward' : 'backward');
    setPublicDiagnosticStep(boundedNextStep);
  }

  if (!workspace) {
    const publicDiagnosticReady = publicDiagnosticQuestions.every((question) => publicDiagnosticAnswers[question.key]?.trim());
    const publicDiagnosticPending = publicDiagnosticLoading || publicDiagnosticGuidance?.status === 'PENDING';
    const publicDiagnosticCompleted = publicDiagnosticGuidance?.status === 'COMPLETED';
    const activePublicDiagnosticQuestion = publicDiagnosticQuestions[publicDiagnosticStep] ?? publicDiagnosticQuestions[0];
    const activePublicDiagnosticAnswer = publicDiagnosticAnswers[activePublicDiagnosticQuestion.key];
    const answeredPublicDiagnosticCount = publicDiagnosticQuestions.filter((question) => publicDiagnosticAnswers[question.key]?.trim()).length;
    const publicDiagnosticProgressPercent = Math.round((answeredPublicDiagnosticCount / publicDiagnosticQuestions.length) * 100);

    return (
      <main className="app-shell public-diagnostic-shell">
        <section className="public-diagnostic-page" data-analytics-section="public_presence_diagnostic">
          <div className="public-diagnostic-intro">
            <h1>Sua imagem comunica a mulher que você quer ser vista como?</h1>
            <p>Responda em 30 segundos e veja o primeiro passo que mais pode aumentar sua presença hoje.</p>
          </div>

          <section className="public-diagnostic-form" aria-label="Diagnóstico de Presença">
            <div className="public-diagnostic-experience">
              <div
                className={`public-diagnostic-visual public-diagnostic-visual-${publicDiagnosticDirection}`}
                key={`slide-${activePublicDiagnosticQuestion.key}`}
              >
                <img src={activePublicDiagnosticQuestion.imageUrl} alt="" />
                <div className="public-diagnostic-visual-copy">
                  <span>Diagnóstico de Presença</span>
                  <strong>{activePublicDiagnosticQuestion.visualTitle}</strong>
                  <p>{activePublicDiagnosticQuestion.visualText}</p>
                </div>
                <fieldset className="public-question-card public-question-card-active">
                  <legend>
                    <span>{publicDiagnosticStep + 1}</span>
                    {activePublicDiagnosticQuestion.question}
                  </legend>
                  <div className="public-option-grid">
                    {activePublicDiagnosticQuestion.options.map((option) => (
                      <button
                        key={option}
                        className={activePublicDiagnosticAnswer === option ? 'selected' : ''}
                        type="button"
                        onClick={() => updatePublicDiagnosticAnswer(activePublicDiagnosticQuestion.key, option)}
                      >
                        {option}
                        <ChevronRight size={17} />
                      </button>
                    ))}
                  </div>
                </fieldset>
                <div className="public-question-navigation">
                  {publicDiagnosticStep > 0 && (
                    <button className="public-back-button" type="button" onClick={() => goToPublicDiagnosticStep(publicDiagnosticStep - 1)}>
                      Voltar
                    </button>
                  )}
                  <div className="public-question-dots" aria-label="Navegar entre perguntas">
                    {publicDiagnosticQuestions.map((question, index) => (
                      <button
                        key={question.key}
                        aria-label={`Pergunta ${index + 1}`}
                        className={index === publicDiagnosticStep ? 'active' : publicDiagnosticAnswers[question.key] ? 'answered' : ''}
                        type="button"
                        onClick={() => goToPublicDiagnosticStep(index)}
                      />
                    ))}
                  </div>
                </div>
              </div>
              <div className="public-diagnostic-stage">
                <div className="public-diagnostic-stage-top">
                  <p className="section-kicker">Diagnóstico de Presença</p>
                  <span>{answeredPublicDiagnosticCount}/5 respostas</span>
                </div>
                <h2>Toque nas respostas e receba um plano de 7 dias feito para sua rotina.</h2>
                <div className="public-progress-track" aria-label={`Progresso do diagnóstico: ${publicDiagnosticProgressPercent}%`}>
                  <span style={{ width: `${publicDiagnosticProgressPercent}%` }} />
                </div>
                <div className="public-progress-strip" aria-label="Progresso do diagnóstico">
                  <span>
                    <Check size={15} /> 5 perguntas rápidas
                  </span>
                  <span>
                    <Sparkles size={15} /> Primeiro passo hoje
                  </span>
                  <span>
                    <Lock size={15} /> Sem preço antes do resultado
                  </span>
                </div>
              </div>
            </div>
            {errorMessage && <p className="form-message">{errorMessage}</p>}
            <button className="primary-button public-submit-button" disabled={publicDiagnosticPending || !publicDiagnosticReady} onClick={submitPublicPresenceDiagnostic}>
              {publicDiagnosticPending ? <LoaderCircle className="button-spinner" size={18} /> : <Sparkles size={18} />}
              {publicDiagnosticPending ? 'Montando seu plano...' : 'Ver meu primeiro passo'}
            </button>
          </section>

          {publicDiagnosticPending && (
            <section className="public-ai-status" role="status" aria-live="polite">
              <LoaderCircle className="button-spinner" size={20} />
              <span>A Consultora MUSA está lendo suas respostas e criando um plano de 7 dias para sua rotina.</span>
            </section>
          )}

          {publicDiagnosticGuidance?.status === 'FAILED' && (
            <section className="public-ai-status public-ai-status-error" role="status">
              <Sparkles size={20} />
              <span>Suas respostas foram recebidas, mas a IA não concluiu agora. Envie novamente em alguns instantes.</span>
            </section>
          )}

          {publicDiagnosticCompleted && (
            <section className="public-diagnostic-result" aria-label="Plano personalizado de 7 dias">
              <p className="section-kicker">Plano personalizado por IA</p>
              <h2>{publicDiagnosticGuidance.headline}</h2>
              <p>{publicDiagnosticGuidance.summary}</p>
              <div className="public-signal-grid">
                {publicDiagnosticGuidance.signals.map((signal) => (
                  <span key={signal}>{signal}</span>
                ))}
              </div>
              <ol className="public-seven-day-plan">
                {publicDiagnosticGuidance.microActions.map((action) => (
                  <li key={action}>{action}</li>
                ))}
              </ol>
              {publicDiagnosticGuidance.caution && <small>{publicDiagnosticGuidance.caution}</small>}
              <div className="public-email-capture" data-analytics-section="public_diagnostic_email_capture">
                <p className="section-kicker">Receba o roteiro detalhado</p>
                <h3>Quer saber exatamente como executar esse plano sem se perder no dia a dia?</h3>
                <p>
                  Envie seu e-mail para salvar seu diagnóstico e receber o caminho da Área MUSA, com as instruções dos 7 dias organizadas para você aplicar uma missão por vez.
                </p>
                <label className="email-box public-email-box">
                  Seu melhor e-mail
                  <input
                    ref={emailInputRef}
                    type="email"
                    placeholder="seuemail@exemplo.com"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        submitPublicDiagnosticEmail();
                      }
                    }}
                  />
                </label>
                {errorMessage && <p className="form-message">{errorMessage}</p>}
                {successMessage && <p className="form-message success-message">{successMessage}</p>}
                {devAccessUrl ? (
                  <button className="primary-button public-email-button" onClick={() => openDevAccess(devAccessUrl)} type="button">
                    <LogIn size={18} />
                    Abrir minha Área MUSA
                  </button>
                ) : (
                  <button className="primary-button public-email-button" onClick={submitPublicDiagnosticEmail} disabled={loading}>
                    <Mail size={18} />
                    {loading ? 'Enviando roteiro...' : 'Receber roteiro detalhado dos 7 dias'}
                  </button>
                )}
              </div>
            </section>
          )}
        </section>
      </main>
    );
  }

  if (!workspace) {
    return (
      <main className="app-shell login-shell">
        <section className="login-hero" data-analytics-section="login_hero">
          <div className="login-panel">
            <img className="brand-logo login-brand-logo" src="/assets/logo-musa.svg" alt="Clube MUSA" />
            <h1>Sua imagem comunica a mulher que você quer ser vista como?</h1>
            <p className="promise">Toque uma vez e receba seu Mapa de Presença do Dia 1: o primeiro sinal que pode deixar sua imagem mais intencional hoje, usando o que você já tem.</p>
            <div className="diagnostic-promise-strip" aria-label="O que o diagnóstico gratuito entrega">
              <span>
                <Sparkles size={16} /> Mapa de Presença do Dia 1
              </span>
              <span>
                <Check size={16} /> Microação com o que você já tem
              </span>
              <span>
                <Lock size={16} /> Plano completo só depois
              </span>
            </div>
            <div className="auth-tabs" aria-label="Tipo de acesso">
              <button
                className={authMode === 'register' ? 'active' : ''}
                onClick={() => {
                  setAuthMode('register');
                  setErrorMessage('');
                  setSuccessMessage('');
                  setDevAccessUrl('');
                }}
                type="button"
              >
                Diagnóstico gratuito
              </button>
              <button
                className={authMode === 'login' ? 'active' : ''}
                onClick={() => {
                  setAuthMode('login');
                  setErrorMessage('');
                  setSuccessMessage('');
                  setDevAccessUrl('');
                }}
                type="button"
              >
                Já tenho acesso
              </button>
            </div>
            <p className="auth-help">
              {authMode === 'login' ? (
                <>
                  <strong>Voltando ao Clube MUSA?</strong> Informe o e-mail da compra ou do primeiro acesso para receber um novo link seguro.
                </>
              ) : (
                <>
                  <strong>Comece pelo espelho.</strong> Primeiro veja o que sua imagem pode estar comunicando; depois informe seu e-mail para salvar o mapa.
                </>
              )}
            </p>
            {authMode === 'register' && (
              <div className="interactive-diagnostic" data-analytics-section="interactive_diagnostic">
                <div className="diagnostic-step">
                  <span>Diagnóstico gratuito</span>
                  <h2>Veja o primeiro passo para aumentar sua presença hoje.</h2>
                  <button className={presenceBlocker ? 'diagnostic-start-button selected' : 'diagnostic-start-button'} onClick={startPresenceMap} type="button">
                    <Sparkles size={18} />
                    <strong>Descobrir o que minha imagem comunica hoje</strong>
                    <small>Sem escolher perfil, sem responder questionário longo e sem mostrar preço agora.</small>
                  </button>
                </div>
                {selectedBlocker && selectedDesiredPresence && (
                  <div className="diagnostic-result-teaser">
                    <Check size={18} />
                    <p>
                      Seu Mapa de Presença vai mostrar o primeiro sinal que sua imagem comunica hoje e uma microação prática para reforçar <strong>{selectedDesiredPresence.label.toLowerCase()}</strong>.
                    </p>
                  </div>
                )}
              </div>
            )}
            <div className="login-scene-banner" aria-label="Mulher percebendo sua presença elegante no espelho">
              <img src="/assets/musa-editorial-presenca.png" alt="" />
              <span>Descubra o que sua imagem pode estar comunicando sem intenção antes de gastar com novas peças.</span>
            </div>
            {googleClientId && (
              <div className="social-login-block">
                <div id="google-login-button" aria-label="Entrar com Google" />
                <span>Mais rápido para entrar e salvar sua primeira orientação.</span>
              </div>
            )}
            {diagnosticReadyForEmail && (
              <>
                <div className="auth-divider">
                  <span>{authMode === 'login' ? 'receba um link de retorno por e-mail' : 'receba seu Mapa de Presença por e-mail'}</span>
                </div>
                <label className="email-box login-email-box">
                  {authMode === 'login' ? 'E-mail do seu acesso MUSA' : 'Seu melhor e-mail para receber o Mapa de Presença'}
                  <input
                    ref={emailInputRef}
                    type="email"
                    placeholder="seuemail@exemplo.com"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        submitAccess();
                      }
                    }}
                  />
                </label>
              </>
            )}
            {errorMessage && <p className="form-message">{errorMessage}</p>}
            {successMessage && <p className="form-message success-message">{successMessage}</p>}
            {successMessage && (
              <button className="inline-action edit-email-button" onClick={editAccessEmail} type="button">
                <Pencil size={16} />
                Editar e-mail
              </button>
            )}
            {devAccessUrl ? (
              <button className="primary-button login-button" onClick={() => openDevAccess(devAccessUrl)} type="button">
                <LogIn size={18} />
                Abrir minha Área MUSA
              </button>
            ) : (
              <button className="primary-button login-button" onClick={submitAccess} disabled={loading || !diagnosticReadyForEmail}>
                <Mail size={18} />
                {loading ? 'Enviando link...' : authMode === 'login' ? 'Receber link de entrada' : 'Receber meu Mapa de Presença'}
              </button>
            )}
            <div className="login-value-strip" aria-label="O que fica disponível ao entrar">
              <span>
                <Check size={16} /> Sem compromisso inicial
              </span>
              <span>
                <Sparkles size={16} /> Microação personalizada
              </span>
              <span>
                <Lock size={16} /> Dias 2 a 7 no premium
              </span>
            </div>
            <div className="login-preview-card" data-analytics-section="free_diagnostic_preview">
              <div>
                <span>O que você libera agora</span>
                <strong>Mapa de Presença do Dia 1</strong>
                <p>Descubra o sinal que sua imagem comunica sem intenção e uma microação para parecer mais coerente hoje.</p>
              </div>
              <ChevronRight size={22} />
            </div>
            <p className="access-note">A primeira orientação é gratuita. O acesso completo só aparece depois que você entender seu mapa inicial e decidir continuar o plano guiado dos Dias 2 a 7.</p>
          </div>
          {showVideoHero ? (
            <div className="experience-card login-cover video-login-cover" aria-label="Vídeo da experiência Método MUSA" data-analytics-section="musa_video_hero">
              {heroVideoUrl ? (
                <video className="musa-hero-video" src={heroVideoUrl} autoPlay muted loop playsInline poster="/assets/musa-editorial-presenca.png" />
              ) : (
                <div className="musa-video-fallback" aria-hidden="true">
                  <img src="/assets/musa-editorial-presenca.png" alt="" />
                </div>
              )}
              <div className="video-hero-caption">
                <span>{heroVideoUrl ? 'Prévia em vídeo' : 'Prévia visual'}</span>
                <strong>Veja o gesto simples que muda a percepção da sua presença.</strong>
                <p>{heroVideoUrl ? 'O Mapa de Presença começa antes do e-mail e mostra o primeiro passo prático do Dia 1.' : 'Vídeo VEO ainda não configurado neste ambiente. A tela preserva a promessa e bloqueia liberação como vídeo real.'}</p>
              </div>
            </div>
          ) : (
            <div className="experience-card login-cover" aria-label="Prévia da experiência Método MUSA" data-analytics-section="musa_product_preview">
              <div className="cover-mark">
                <Sparkles size={32} />
              </div>
              <div className="style-preview" aria-hidden="true">
                <span />
                <span />
                <span />
              </div>
              <div className="login-editorial-preview" aria-hidden="true">
                <div className="preview-page">
                  <span>Dia 1</span>
                  <strong>Presença</strong>
                  <i />
                  <i />
                  <i />
                </div>
                <div className="preview-note">
                  <Check size={18} />
                  <span>1 microação visível hoje</span>
                </div>
              </div>
              <div className="login-cover-content">
                <p>Método MUSA</p>
                <strong>Uma jornada de 7 dias para parecer mais elegante com o que você já tem.</strong>
                <span>Entre, veja seu Mapa de Presença e descubra a microação que pode deixar sua imagem mais intencional hoje.</span>
              </div>
              <div className="login-unlock-list" aria-label="Prévia da experiência MUSA">
                <span>
                  <Target size={16} /> Diagnóstico personalizado
                </span>
                <span>
                  <CalendarDays size={16} /> 7 missões guiadas
                </span>
                <span>
                  <Sparkles size={16} /> Microações práticas para hoje
                </span>
              </div>
            </div>
          )}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell dashboard-shell">
      <section className="musa-first-fold" data-analytics-section="member_first_fold">
        <div className="musa-hero-copy">
          <img className="brand-logo dashboard-brand-logo" src="/assets/logo-musa.svg" alt="Clube MUSA" />
          <p className="eyebrow">Sua Jornada MUSA</p>
          <h1>Sua presença elegante começa hoje.</h1>
          <p className="promise">{currentProduct.promise}</p>
          <div className="musa-hero-actions">
            <button className="primary-button" onClick={() => openMission(firstMission?.id ?? '', 'primary_start')} disabled={!firstMission}>
              <Sparkles size={18} />
              Começar agora
            </button>
            <span>Toque e vá direto para a orientação do Dia 1.</span>
          </div>
        </div>
        <article className="next-mission-hero">
          <div className="next-mission-topline">
            <span>Próxima missão</span>
            <strong>{nextMission ? `Dia ${nextMission.day}` : 'Jornada finalizada'}</strong>
          </div>
          <h2>{nextMission?.title ?? 'Continue sua assinatura MUSA'}</h2>
          <p>{trialNeedsPaymentForNextDay ? 'Sua primeira microação foi registrada. O Dia 2 continua a transformação com sua assinatura simples, mas precisa do acesso completo para abrir.' : nextMission ? 'Escolha uma combinação real, identifique o sinal que sua imagem comunica sem intenção e registre a frase que vai guiar sua primeira microação.' : currentProduct.completionOffer}</p>
          {trialNeedsPaymentForNextDay ? (
            <button className="secondary-button next-mission-button" onClick={handleSubscriptionClick}>
              <CreditCard size={18} />
              Liberar Dia 2 e continuar
            </button>
          ) : nextMission && !nextMissionIsFirstMission ? (
            <button className="secondary-button next-mission-button" onClick={() => openMission(nextMission.id, 'next_mission_open')}>
              Abrir próxima missão
              <ChevronRight size={18} />
            </button>
          ) : (
            <div className="next-mission-guidance">
              <Sparkles size={18} />
              <span>Toque no botão acima para iniciar sua primeira missão.</span>
            </div>
          )}
        </article>
        <aside className="progress-hero-card" aria-label="Progresso da jornada MUSA">
          <Gauge size={24} />
          <span>Progresso</span>
          <strong>{workspace.progressPercent}%</strong>
          <div className="progress-track" aria-label="Progresso da experiência">
            <span style={{ width: `${workspace.progressPercent}%` }} />
          </div>
          <p>
            {workspace.completedMissions} de {workspace.totalMissions} missões concluídas.
          </p>
        </aside>
      </section>

      <section className="dashboard-overview dashboard-overview-secondary" aria-label="Resumo da Área MUSA">
        <article className="status-card account-status-card">
          <User size={20} />
          <span>Acesso liberado</span>
          <strong>{workspace.email}</strong>
          <p>Use este e-mail para manter sua jornada salva.</p>
        </article>
        <article className="status-card">
          <ClipboardCheck size={20} />
          <span>Diagnóstico</span>
          <strong>Comece pelo espelho</strong>
          <p>Nomeie o que hoje deixa você arrumada, mas pouco marcante.</p>
        </article>
        <article className="status-card">
          <CalendarDays size={20} />
          <span>Plano guiado</span>
          <strong>7 dias</strong>
          <p>Diagnóstico, microações e próximos passos em sequência.</p>
        </article>
        <article className="status-card">
          <KeyRound size={20} />
          <span>{hasActiveSubscription ? 'Produto ativo' : 'Assinatura'}</span>
          <strong>{hasActiveSubscription ? 'Acesso completo' : 'Pendente'}</strong>
          <p>{hasActiveSubscription ? 'Método MUSA liberado para uso.' : 'Assine para liberar todos os recursos.'}</p>
        </article>
      </section>

      {!hasActiveSubscription && (
        <section className="subscription-paywall" aria-label="Oferta de assinatura MUSA" data-analytics-section="subscription_paywall">
          <div>
            <p className="section-kicker">Liberar área completa</p>
            <h2>Assine o Clube MUSA para continuar seu plano personalizado dos 7 dias.</h2>
            <p>Seu Mapa de Presença inicial já mostrou o primeiro sinal. O acesso completo libera os próximos dias, a sequência de microações e o acompanhamento para transformar isso em presença repetível.</p>
          </div>
          <button className="primary-button" onClick={handleSubscriptionClick}>
            <CreditCard size={18} />
            Liberar meu plano de 7 dias
          </button>
        </section>
      )}

      <section className="dashboard-header compact-dashboard-header">
        <div className="dashboard-title">
          <div className="dashboard-icon">
            <CalendarDays size={22} />
          </div>
          <div>
            <p className="eyebrow">Roteiro guiado</p>
            <h2>Mapa de Presença e plano de 7 dias</h2>
            <p>Responda, receba seu mapa e siga a próxima microação indicada pela jornada.</p>
          </div>
        </div>
      </section>

      <section className="dashboard-main" data-analytics-section="guided_experience">
        <aside className="customer-sidebar">
          <div
            className="mini-cover"
            style={{
              backgroundImage: currentProduct.theme.imageUrl ? `url(${currentProduct.theme.imageUrl})` : undefined,
            }}
          >
            <Sparkles size={24} />
            <span>Método MUSA</span>
          </div>
          <div className="diagnostic-panel">
            <Target size={22} />
            <h2>{currentProduct.diagnostic.title}</h2>
            <p>{currentProduct.diagnostic.intro}</p>
            <ul>
              {currentProduct.diagnostic.questions.map((question) => (
                <li key={question}>{question}</li>
              ))}
            </ul>
          </div>
        </aside>

        <section className="mission-panel" ref={missionPanelRef}>
          {firstMission && (
            <article className="start-here-panel">
              <p className="section-kicker">Comece aqui</p>
              <h2>Dia 1: {firstMission.title}</h2>
              <p>A primeira missão é escolher uma combinação real, identificar o sinal que sua imagem comunica sem intenção e escrever a frase do seu mapa. Você termina o dia sabendo exatamente o que reforçar antes de pensar em comprar algo novo.</p>
              {!hasActiveSubscription && (
                <div className="trial-unlock-note">
                  <Sparkles size={17} />
                  <span>O Dia 1 está liberado gratuitamente. Dias 2 a 7 aparecem depois do acesso completo.</span>
                </div>
              )}
              <button className="inline-action" onClick={() => openMission(firstMission.id, 'start_here_open')}>
                Abrir orientação do Dia 1
                <ChevronRight size={17} />
              </button>
            </article>
          )}
          <div className="mission-tabs" aria-label="Dias da experiência">
            {currentProduct.missions.map((mission) => (
              <button
                key={mission.id}
                className={`${mission.id === activeMission?.id ? 'active' : ''} ${!hasActiveSubscription && mission.id !== firstMission?.id ? 'locked' : ''}`}
                onClick={() => {
                  if (!hasActiveSubscription && mission.id !== firstMission?.id) {
                    handleSubscriptionClick();
                    return;
                  }
                  openMission(mission.id, 'mission_tab_open');
                }}
                title={`Dia ${mission.day}: ${mission.title}`}
              >
                {!hasActiveSubscription && mission.id !== firstMission?.id ? <Lock size={15} /> : completedMissionIds.has(mission.id) ? <Check size={16} /> : mission.day}
              </button>
            ))}
          </div>

          {activeMission && (
            <article className="mission-detail">
              <p className="section-kicker">Missão ativa - Dia {activeMission.day}</p>
              <h2>{activeMission.title}</h2>
              <div className="mission-block">
                <strong>Princípio aplicado</strong>
                <p>{activeMission.principle}</p>
              </div>
              <div className="mission-block">
                <strong>Ação de hoje</strong>
                <p>{activeMission.action}</p>
              </div>
              <div className="mission-block">
                <strong>Evidência de progresso</strong>
                <p>{activeMission.evidence}</p>
              </div>
              <div className="visual-cue">
                <ChevronRight size={18} />
                {activeMission.visualCue}
              </div>
              {activeMissionGuidanceConfig && (hasActiveSubscription || activeMission.id === firstMission?.id) && (
                <div className="personalization-panel musa-signature-panel">
                  <p className="section-kicker">{activeMissionGuidanceConfig.kicker}</p>
                  <h3>{activeMissionGuidanceConfig.title}</h3>
                  <p className="guidance-helper-text">{activeMissionGuidanceConfig.helperText}</p>
                  {activeMissionGuidanceConfig.fields.map((field) => (
                    <label key={field.key}>
                      {field.label}
                      {field.options ? (
                        <select value={activeMissionAnswers[field.key] ?? ''} onChange={(event) => updateMissionAnswer(activeMission.id, field.key, event.target.value)}>
                          <option value="">{field.placeholder}</option>
                          {field.options.map((option) => (
                            <option key={option} value={option}>
                              {option}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <textarea rows={3} maxLength={360} value={activeMissionAnswers[field.key] ?? ''} placeholder={field.placeholder} onChange={(event) => updateMissionAnswer(activeMission.id, field.key, event.target.value)} />
                      )}
                    </label>
                  ))}
                  {isMissionInteractionSaved(activeMission.id) && (
                    <div className="signature-preview-grid" aria-label="Sinais escolhidos para sua assinatura MUSA">
                      {activeMissionGuidanceConfig.fields.map((field) => (
                        <span key={field.key}>{activeMissionAnswers[field.key]}</span>
                      ))}
                    </div>
                  )}
                  <button className="inline-save-button" disabled={generatingGuidance || savingInteraction || completedMissionIds.has(activeMission.id)} onClick={() => requestMissionGuidance(activeMission.id)}>
                    {generatingGuidance ? <LoaderCircle className="button-spinner" size={16} /> : <Sparkles size={16} />}
                    {generatingGuidance ? activeMissionGuidanceConfig.loadingLabel : activeMissionGuidanceConfig.buttonLabel}
                  </button>
                  {activeMissionGuidance?.status === 'PENDING' && (
                    <div className="personalized-summary">
                      <LoaderCircle className="button-spinner" size={17} />
                      <span>{activeMissionGuidanceConfig.pendingLabel}</span>
                    </div>
                  )}
                  {activeMissionGuidance?.status === 'FAILED' && (
                    <div className="personalized-summary">
                      <Sparkles size={17} />
                      <span>{activeMissionGuidanceConfig.failedLabel}</span>
                    </div>
                  )}
                  {activeMissionGuidance?.status === 'COMPLETED' && (
                    <div className="ai-guidance-card">
                      <p className="section-kicker">{activeMissionGuidanceConfig.completedKicker}</p>
                      <h3>{activeMissionGuidance.headline}</h3>
                      <p>{activeMissionGuidance.summary}</p>
                      <div className="signature-preview-grid">
                        {activeMissionGuidance.signals.map((signal) => (
                          <span key={signal}>{signal}</span>
                        ))}
                      </div>
                      <ul>
                        {activeMissionGuidance.microActions.map((action) => (
                          <li key={action}>{action}</li>
                        ))}
                      </ul>
                      {activeMissionGuidance.caution && <small>{activeMissionGuidance.caution}</small>}
                      <div className="guidance-next-step">
                        <Check size={18} />
                        <div>
                          <strong>{activeMissionGuidanceConfig.nextStepTitle}</strong>
                          <p>{activeMissionGuidanceConfig.nextStepText}</p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}
              {missionCompletionStatus === 'processing' && activeMission.id === firstMission?.id && (
                <div className="mission-processing-panel" role="status" aria-live="polite">
                  <div className="processing-image">
                    <img src="/assets/musa-editorial-presenca.png" alt="" />
                    <LoaderCircle size={28} />
                  </div>
                  <div>
                    <p className="section-kicker">Registrando seu progresso</p>
                    <h3>Estamos guardando sua microação do Dia 1.</h3>
                    <p>Em alguns segundos você verá o próximo passo da jornada MUSA, sem perder sua personalização.</p>
                  </div>
                </div>
              )}
              {missionCompletionStatus === 'success' && completedMissionFeedbackId === firstMission?.id && dayOneCompleted && (
                <div className="mission-success-panel" role="status" aria-live="polite">
                  <div className="success-mark">
                    <Check size={24} />
                  </div>
                  <div>
                    <p className="section-kicker">Dia 1 concluído</p>
                    <h3>Seu primeiro sinal de presença ficou salvo.</h3>
                    <p>Agora você já sabe qual sinal reduz intenção no conjunto. O Dia 2 abre a próxima camada: criar uma assinatura simples para repetir elegância sem esforço.</p>
                    {!hasActiveSubscription && (
                      <button className="primary-button" onClick={handleSubscriptionClick}>
                        <CreditCard size={18} />
                        Liberar Dia 2 e acesso completo
                      </button>
                    )}
                  </div>
                </div>
              )}
              <button className="secondary-button" disabled={!workspace || completedMissionIds.has(activeMission.id) || !canRegisterActiveMission || missionCompletionStatus === 'processing'} onClick={() => completeMission(activeMission.id)}>
                {missionCompletionStatus === 'processing' ? <LoaderCircle className="button-spinner" size={18} /> : canRegisterActiveMission ? <Check size={18} /> : <Lock size={18} />}
                {missionCompletionStatus === 'processing' ? `Registrando seu Dia ${activeMission.day}...` : canRegisterActiveMission ? (completedMissionIds.has(activeMission.id) ? 'Missão concluída' : `Registrar Dia ${activeMission.day} concluído`) : activeMission.id === firstMission?.id ? 'Receba seu diagnóstico para concluir' : activeMissionGuidanceConfig ? 'Preencha os 3 pontos para concluir' : 'Assine para salvar esta missão'}
              </button>
            </article>
          )}
        </section>
      </section>

      {hasActiveSubscription && (
        <section className="library-section">
          <div className="section-heading">
            <Library size={22} />
            <div>
              <p className="section-kicker">Apoio de consulta</p>
              <h2>Use só quando precisar revisar</h2>
              <p className="library-support-copy">O coração do MUSA é a jornada guiada. Estes arquivos ficam como apoio secundário para consultar depois da missão.</p>
            </div>
          </div>
          <div className="material-grid">
            {currentProduct.supportMaterials.map((material) => (
              <article className="material-card" key={material.title}>
                <BookOpen size={20} />
                <span>{material.type}</span>
                <h3>{material.title}</h3>
                <p>{material.description}</p>
                <a
                  href={material.url}
                  target="_blank"
                  rel="noreferrer"
                  onClick={() => {
                    trackEvent('MATERIAL_OPEN', {
                      accessToken,
                      email: workspace.email,
                      provider: workspace.accessSource,
                      metadata: {
                        materialTitle: material.title,
                        materialType: material.type,
                      },
                    });
                    trackFirstUse('material_open', {
                      materialTitle: material.title,
                      materialType: material.type,
                    });
                  }}
                >
                  Abrir material
                </a>
              </article>
            ))}
          </div>
        </section>
      )}

      <section className="completion-band">
        <Sparkles size={22} />
        <p>{currentProduct.completionOffer}</p>
      </section>
    </main>
  );
}

const root = createRoot(document.getElementById('root') as HTMLElement);
root.render(<App />);
