import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  BookOpen,
  CalendarDays,
  Check,
  ChevronRight,
  ClipboardCheck,
  CreditCard,
  Gauge,
  KeyRound,
  Library,
  LoaderCircle,
  Lock,
  LogIn,
  Mail,
  Pencil,
  Sparkles,
  Target,
  User,
} from 'lucide-react';
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

type ProductExperience = {
  slug: string;
  name: string;
  promise: string;
  audience: string;
  priceLabel: string;
  theme: Theme;
  diagnostic: Diagnostic;
  missions: Mission[];
  supportMaterials: SupportMaterial[];
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
  buttonLabel: string;
  loadingLabel: string;
  pendingLabel: string;
  failedLabel: string;
  completedKicker: string;
  fields: MissionGuidanceField[];
};

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
          }) => void;
          renderButton: (element: HTMLElement, options: Record<string, string | number | boolean>) => void;
        };
      };
    };
  }
}

const fallbackProduct: ProductExperience = {
  slug: 'metodo-musa-7-dias',
  name: 'Método MUSA - Experiência Guiada de 7 Dias',
  promise: 'Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.',
  audience: 'Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.',
  priceLabel: '',
  theme: {
    primary: '#7a2444',
    accent: '#d6a75c',
    background: '#fff8f3',
    imageUrl: '/assets/musa-cover.png',
  },
  diagnostic: {
    title: 'Diagnóstico MUSA',
    intro: 'Comece pelo momento do espelho: quando você está pronta, mas sente que ainda falta presença, acabamento ou intenção.',
    questions: [
      'Quando você se vê pronta, o que faz pensar: está ok, mas ainda não está marcante?',
      'Seu cabelo, pele, roupa, perfume e acessórios parecem conversar entre si?',
      'Qual compra você está quase fazendo para tentar compensar essa sensação?',
    ],
  },
  missions: [
    {
      id: 'dia-1-ruido-visual',
      day: 1,
      title: 'Sair do quase bom',
      principle: 'A presença cresce quando você identifica o detalhe que mais apaga o conjunto.',
      action: 'Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe cabelo, pele, roupa, perfume e acessórios, escolha o detalhe que mais apaga sua presença e ajuste apenas esse ponto.',
      evidence: 'Frase preenchida: eu me sinto arrumada, mas pouco marcante quando...',
      visualCue: 'Compare a sensação antes/depois de remover ou ajustar um detalhe.',
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
    title: 'Conte sua situação real para receber o primeiro ajuste de presença.',
    buttonLabel: 'Receber meu ajuste do Dia 1',
    loadingLabel: 'Preparando ajuste...',
    pendingLabel: 'Sua Consultora MUSA está preparando uma orientação curta para o seu primeiro ajuste.',
    failedLabel: 'Suas respostas ficaram salvas. Use a missão do Dia 1 manualmente enquanto a consultora automática é configurada.',
    completedKicker: 'Meu ajuste MUSA',
    fields: [
      {
        key: 'presenceFocus',
        label: 'Onde você mais quer sentir presença hoje?',
        placeholder: 'Escolha um foco',
        options: ['Trabalho ou reunião', 'Encontro ou saída', 'Rotina comum', 'Conteúdo ou foto'],
      },
      {
        key: 'mainObstacle',
        label: 'Qual detalhe mais apaga o conjunto?',
        placeholder: 'Escolha um detalhe',
        options: ['Cabelo sem acabamento', 'Roupa sem intenção', 'Cores brigando entre si', 'Falta de acessório ou perfume'],
      },
      {
        key: 'evidencePhrase',
        label: 'Complete sua frase de evidência',
        placeholder: 'Eu me sinto arrumada, mas pouco marcante quando...',
      },
    ],
  },
  'dia-2-assinatura': {
    guidanceType: 'MUSA_DAY_2_SIGNATURE',
    kicker: 'Consultora MUSA',
    title: 'Escolha 3 sinais para montar sua assinatura desta semana.',
    buttonLabel: 'Gerar minha assinatura MUSA',
    loadingLabel: 'Montando assinatura...',
    pendingLabel: 'Sua Consultora MUSA está preparando uma orientação curta com seus 3 sinais.',
    failedLabel: 'Seus sinais ficaram salvos. A consultora automática ainda precisa ser configurada neste ambiente.',
    completedKicker: 'Minha assinatura MUSA',
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
    buttonLabel: 'Montar minha base acessível',
    loadingLabel: 'Organizando base...',
    pendingLabel: 'Sua Consultora MUSA está conectando seus itens aos sinais escolhidos.',
    failedLabel: 'Seu inventário ficou salvo. Use os itens escolhidos como base da missão de hoje.',
    completedKicker: 'Minha base acessível',
    fields: [
      { key: 'pieces', label: '5 peças que você já tem', placeholder: 'Ex.: calça preta, camisa branca, vestido vinho...' },
      { key: 'accessories', label: '2 acessórios ou acabamentos disponíveis', placeholder: 'Ex.: brinco dourado e perfume suave' },
      { key: 'realOccasion', label: 'Para qual situação real essa base precisa funcionar?', placeholder: 'Ex.: reunião, jantar, rotina de trabalho' },
    ],
  },
  'dia-4-checklist-12-minutos': {
    guidanceType: 'MUSA_DAY_4_FINISHING_RITUAL',
    kicker: 'Consultora MUSA',
    title: 'Transforme seu checklist em um acabamento de 12 minutos.',
    buttonLabel: 'Criar meu ritual de 12 minutos',
    loadingLabel: 'Ajustando ritual...',
    pendingLabel: 'Sua Consultora MUSA está priorizando o que dá mais presença em menos tempo.',
    failedLabel: 'Seu checklist ficou salvo. Execute a ordem mais simples hoje.',
    completedKicker: 'Meu ritual de acabamento',
    fields: [
      { key: 'availableMinutes', label: 'Quanto tempo real você tem antes de sair?', placeholder: 'Ex.: 8, 12 ou 15 minutos' },
      { key: 'weakestFinish', label: 'Qual acabamento costuma falhar primeiro?', placeholder: 'Ex.: cabelo, pele, roupa, perfume, postura' },
      { key: 'desiredFeeling', label: 'Como você quer se sentir ao sair?', placeholder: 'Ex.: limpa, marcante, segura, feminina' },
    ],
  },
  'dia-5-compra-inteligente': {
    guidanceType: 'MUSA_DAY_5_ANTI_IMPULSE_DECISION',
    kicker: 'Consultora MUSA',
    title: 'Antes de comprar, deixe a IA testar se o item fortalece sua assinatura.',
    buttonLabel: 'Avaliar minha compra',
    loadingLabel: 'Avaliando compra...',
    pendingLabel: 'Sua Consultora MUSA está separando desejo imediato de utilidade real.',
    failedLabel: 'Sua decisão ficou salva. Compare a compra com seus 3 sinais antes de avançar.',
    completedKicker: 'Minha decisão anti-impulso',
    fields: [
      { key: 'desiredItem', label: 'O que você está pensando em comprar?', placeholder: 'Ex.: blazer, perfume, bolsa, sapato' },
      { key: 'buyingReason', label: 'Qual sensação você espera resolver com essa compra?', placeholder: 'Ex.: parecer mais arrumada, menos comum, mais adulta' },
      { key: 'fitWithSignature', label: 'Como esse item conversa com sua assinatura MUSA?', placeholder: 'Ex.: combina com minha cor-base e acabamento' },
    ],
  },
  'dia-6-situacao-chave': {
    guidanceType: 'MUSA_DAY_6_OCCASION_ENTRY',
    kicker: 'Consultora MUSA',
    title: 'Planeje uma entrada marcante para uma situação real.',
    buttonLabel: 'Preparar minha entrada',
    loadingLabel: 'Preparando entrada...',
    pendingLabel: 'Sua Consultora MUSA está alinhando roupa, acabamento e detalhe final.',
    failedLabel: 'Seu plano ficou salvo. Use a missão para ajustar a composição antes da ocasião.',
    completedKicker: 'Minha entrada MUSA',
    fields: [
      { key: 'occasion', label: 'Qual é a ocasião?', placeholder: 'Ex.: reunião, evento, encontro, gravação, almoço' },
      { key: 'plannedLook', label: 'Qual composição você pretende usar?', placeholder: 'Roupa, cabelo, pele, perfume e detalhe final' },
      { key: 'presenceRisk', label: 'O que pode enfraquecer sua presença nesse contexto?', placeholder: 'Ex.: pressa, insegurança, roupa sem caimento' },
    ],
  },
  'dia-7-plano-pessoal': {
    guidanceType: 'MUSA_DAY_7_MAINTENANCE_PLAN',
    kicker: 'Consultora MUSA',
    title: 'Feche a semana com um plano simples para manter sua presença.',
    buttonLabel: 'Gerar meu plano pessoal',
    loadingLabel: 'Fechando plano...',
    pendingLabel: 'Sua Consultora MUSA está transformando a semana em um ritual fácil de repetir.',
    failedLabel: 'Seu plano ficou salvo. Releia os sinais e escolha um ritual semanal de 15 minutos.',
    completedKicker: 'Meu plano MUSA',
    fields: [
      { key: 'bestSignal', label: 'Qual sinal mais funcionou nesta semana?', placeholder: 'Ex.: cabelo polido, cor-base vinho, perfume' },
      { key: 'hardestPoint', label: 'Qual ponto ainda exige esforço?', placeholder: 'Ex.: manter cabelo, combinar cores, evitar compras' },
      { key: 'weeklyRitual', label: 'Que ritual semanal você consegue repetir?', placeholder: 'Ex.: separar 3 combinações no domingo por 15 minutos' },
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

function resolveUrlHost(url: string) {
  try {
    return new URL(url).hostname;
  } catch {
    return 'invalid_checkout_url';
  }
}

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
  const sectionSeenRef = useRef(new Set<string>());
  const emailInputRef = useRef<HTMLInputElement>(null);
  const missionPanelRef = useRef<HTMLElement>(null);
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;
  const checkoutUrl = (import.meta.env.VITE_MUSA_CHECKOUT_URL as string | undefined) ?? '';

  const activeMission = useMemo(() => {
    const missionList = workspace?.product.missions ?? product.missions;
    return missionList.find((mission) => mission.id === activeMissionId) ?? missionList[0];
  }, [activeMissionId, product.missions, workspace]);

  useEffect(() => {
    if (!sessionIdRef.current) {
      sessionIdRef.current = window.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      window.sessionStorage.setItem('musaSessionId', sessionIdRef.current);
    }
    trackEvent('PED_ENTRY', { source: 'frontend_entry', metadata: { actionName: 'app_entry' } });
    trackEvent('PAGE_VIEW', { source: 'frontend_entry', metadata: { actionName: 'page_loaded' } });
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
      .then((response) => response.ok ? response.json() : fallbackProduct)
      .then((data: ProductExperience) => {
        setProduct(data);
        setActiveMissionId(data.missions[0]?.id ?? '');
      })
      .catch(() => {
        setProduct(fallbackProduct);
        setActiveMissionId(fallbackProduct.missions[0]?.id ?? '');
      });
  }, []);

  useEffect(() => {
    const observedSections = Array.from(document.querySelectorAll<HTMLElement>('[data-analytics-section]'));
    const observer = new IntersectionObserver((entries) => {
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
    }, { threshold: 0.45 });
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
        metadata: { visibleMs, actionName: 'page_visibility_flush' },
      });
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
    if (!email.trim()) {
      setErrorMessage(authMode === 'login'
        ? 'Informe o e-mail que você usou para criar seu acesso MUSA.'
        : 'Informe seu melhor e-mail para liberar seu diagnóstico inicial.');
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
        metadata: { authMode },
      });
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, email }),
      });
      if (!response.ok) {
        const errorBody = await response.json().catch(() => ({} as ApiErrorResponse));
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
    const data = await response.json();
    setWorkspace(data);
    setProduct(data.product);
    setActiveMissionId(data.product.missions[0]?.id ?? '');
    setMissionAnswers(resolveAllMissionAnswers(data));
    if (resetScroll) {
      window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'auto' }));
    }
  }

  async function trackEvent(
    eventType: string,
    options: TrackingOptions = {},
  ) {
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
        ...campaignParams,
        ...options.metadata,
      },
    };
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
        metadata: { actionName: 'checkout_opened', checkoutHost: resolveUrlHost(checkoutUrl) },
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

  function resolveMagicLinkMessage(result: MagicLinkResponse) {
    if (result.deliveryStatus === 'SENT') {
      return authMode === 'login'
        ? 'Enviamos um novo link para seu e-mail. Abra o link para voltar à sua Área MUSA.'
        : 'Seu primeiro acesso foi criado. Abra o link enviado por e-mail para ver o diagnóstico e começar o Dia 1.';
    }
    if (result.accessUrl) {
      return authMode === 'login'
        ? 'Link de teste encontrado para esse cadastro. Use o botão Abrir minha Área MUSA para voltar.'
        : 'Primeiro acesso de teste criado. Use o botão Abrir minha Área MUSA para ver o diagnóstico e começar o Dia 1.';
    }
    if (result.deliveryStatus === 'EMAIL_SEND_FAILED') {
      return 'Seu acesso foi criado, mas o e-mail ainda não pôde ser entregue. A equipe MUSA precisa concluir a configuração do domínio de envio.';
    }
    return 'O envio por e-mail ainda não está configurado neste ambiente. Configure o envio ou habilite o link de teste para entrar.';
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
      missionPanelRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
        fetch(`/api/pde/access/${accessToken}/missions/${missionId}/complete`, { method: 'POST' }),
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
      setSuccessMessage('Personalização salva. Agora registre a conclusão quando executar o ajuste.');
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
      const guidance = await response.json() as AiGuidance;
      setAiGuidanceByMission((current) => ({ ...current, [missionId]: guidance }));
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
    const data = await response.json() as Workspace;
    setWorkspace(data);
    setProduct(data.product);
    return data;
  }

  async function pollGuidanceUntilFinished(requestId: string) {
    for (let attempt = 0; attempt < 10; attempt += 1) {
      await new Promise((resolve) => window.setTimeout(resolve, attempt === 0 ? 900 : 1800));
      const response = await fetch(`/api/pde/access/${accessToken}/ai-guidance/${requestId}`);
      if (!response.ok) {
        throw new Error('Orientação não encontrada.');
      }
      const guidance = await response.json() as AiGuidance;
      setAiGuidanceByMission((current) => ({ ...current, [guidance.missionId]: guidance }));
      if (guidance.status === 'COMPLETED' || guidance.status === 'FAILED') {
        return;
      }
    }
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
  const canCompleteActiveMission = Boolean(
    activeMission && (hasActiveSubscription || activeMission.id === firstMission?.id),
  );
  const activeMissionGuidanceConfig = activeMission ? missionGuidanceConfigs[activeMission.id] : undefined;
  const activeMissionAnswers = activeMission ? missionAnswers[activeMission.id] ?? {} : {};
  const activeMissionGuidance = activeMission ? aiGuidanceByMission[activeMission.id] : undefined;
  const canRegisterActiveMission = Boolean(
    canCompleteActiveMission
      && (!activeMissionGuidanceConfig || isMissionInteractionSaved(activeMission?.id ?? '')),
  );

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

  if (!workspace) {
    return (
      <main className="app-shell login-shell">
        <section className="login-hero" data-analytics-section="login_hero">
          <div className="login-panel">
            <p className="eyebrow">Clube MUSA</p>
            <h1>Entre e descubra o detalhe que hoje apaga sua presença.</h1>
            <p className="promise">
              Libere seu diagnóstico inicial e o Dia 1 do Método MUSA: uma experiência guiada para
              parecer mais elegante com escolhas simples, sem luxo caro nem compra por impulso.
            </p>
            <div className="login-scene-banner" aria-label="Mulher percebendo sua presença elegante no espelho">
              <img src="/assets/musa-editorial-presenca.png" alt="" />
              <span>Uma mudança visível começa por um ajuste simples.</span>
            </div>
            <div className="auth-tabs" aria-label="Tipo de acesso">
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
                Entrar
              </button>
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
                Primeiro acesso
              </button>
            </div>
            <p className="auth-help">
              {authMode === 'login' ? (
                <>
                  <strong>Já entrou antes?</strong> Informe o mesmo e-mail para receber um novo link seguro.
                </>
              ) : (
                <>
                  <strong>Nova por aqui?</strong> O primeiro acesso libera o diagnóstico e o Dia 1 gratuitamente.
                </>
              )}
            </p>
            {googleClientId && (
              <div className="social-login-block">
                <div id="google-login-button" aria-label="Entrar com Google" />
                <span>Mais rápido para entrar e salvar sua primeira orientação.</span>
              </div>
            )}
            <div className="auth-divider">
              <span>{authMode === 'login' ? 'receba um link de retorno por e-mail' : 'receba seu primeiro link por e-mail'}</span>
            </div>
            <label className="email-box login-email-box">
              {authMode === 'login' ? 'E-mail do seu acesso MUSA' : 'E-mail para liberar o Dia 1'}
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
            {errorMessage && <p className="form-message">{errorMessage}</p>}
            {successMessage && <p className="form-message success-message">{successMessage}</p>}
            {successMessage && (
              <button className="inline-action edit-email-button" onClick={editAccessEmail} type="button">
                <Pencil size={16} />
                Editar e-mail
              </button>
            )}
            {devAccessUrl ? (
              <button
                className="primary-button login-button"
                onClick={() => openDevAccess(devAccessUrl)}
                type="button"
              >
                <LogIn size={18} />
                Abrir minha Área MUSA
              </button>
            ) : (
              <button className="primary-button login-button" onClick={submitAccess} disabled={loading}>
                <Mail size={18} />
                {loading
                  ? 'Enviando link...'
                  : (authMode === 'login' ? 'Receber link de entrada' : 'Solicitar primeiro acesso')}
              </button>
            )}
            <div className="login-value-strip" aria-label="O que fica disponível ao entrar">
              <span><Check size={16} /> Diagnóstico gratuito</span>
              <span><Sparkles size={16} /> Dia 1 liberado</span>
              <span><Lock size={16} /> Continuação premium</span>
            </div>
            <div className="login-preview-card" data-analytics-section="free_diagnostic_preview">
              <div>
                <span>Primeira parte liberada</span>
                <strong>Seu espelho MUSA</strong>
                <p>Nomeie o detalhe que faz você se sentir arrumada, mas ainda pouco marcante.</p>
              </div>
              <ChevronRight size={22} />
            </div>
            <p className="access-note">
              O login libera a primeira parte da experiência. Dias 2 a 7, biblioteca e materiais
              premium aparecem dentro da área e são desbloqueados com o acesso completo.
            </p>
          </div>
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
                <span>1 ajuste visível hoje</span>
              </div>
            </div>
            <div className="login-cover-content">
              <p>Método MUSA</p>
              <strong>Uma jornada de 7 dias para parecer mais elegante com o que você já tem.</strong>
              <span>Entre, veja seu primeiro diagnóstico e descubra o próximo ajuste que pode mudar sua presença.</span>
            </div>
            <div className="login-unlock-list" aria-label="Prévia da experiência MUSA">
              <span><CalendarDays size={16} /> 7 missões guiadas</span>
              <span><BookOpen size={16} /> Biblioteca de apoio</span>
              <span><Target size={16} /> Checklists práticos</span>
            </div>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell dashboard-shell">
      <section className="musa-first-fold" data-analytics-section="member_first_fold">
        <div className="musa-hero-copy">
          <p className="eyebrow">Sua Jornada MUSA</p>
          <h1>Sua presença elegante começa hoje.</h1>
          <p className="promise">{currentProduct.promise}</p>
          <div className="musa-hero-actions">
            <button
              className="primary-button"
              onClick={() => openMission(firstMission?.id ?? '', 'primary_start')}
              disabled={!firstMission}
            >
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
          <p>
            {trialNeedsPaymentForNextDay
              ? 'Seu primeiro ajuste foi registrado. O Dia 2 continua a transformação com sua assinatura simples, mas precisa do acesso completo para abrir.'
              : nextMission
              ? 'Escolha uma combinação real, identifique o detalhe que apaga sua presença e registre a frase que vai guiar seu primeiro ajuste.'
              : currentProduct.completionOffer}
          </p>
          {trialNeedsPaymentForNextDay ? (
            <button
              className="secondary-button next-mission-button"
              onClick={handleSubscriptionClick}
            >
              <CreditCard size={18} />
              Liberar Dia 2 e continuar
            </button>
          ) : nextMission && !nextMissionIsFirstMission ? (
            <button
              className="secondary-button next-mission-button"
              onClick={() => openMission(nextMission.id, 'next_mission_open')}
            >
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
          <Library size={20} />
          <span>Biblioteca</span>
          <strong>{currentProduct.supportMaterials.length} materiais</strong>
          <p>E-book, experiência guiada e arquivos de apoio.</p>
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
            <h2>Assine o Clube MUSA para acessar todas as missões, biblioteca e próximos desafios.</h2>
            <p>
              Seu login já salvou a entrada na área. A assinatura remove o bloqueio e permite continuar a experiência completa.
            </p>
          </div>
          <button className="primary-button" onClick={handleSubscriptionClick}>
            <CreditCard size={18} />
            Solicitar acesso pago
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
            <h2>Diagnóstico, missão e materiais de apoio</h2>
            <p>Depois de iniciar o Dia 1, use o diagnóstico e a biblioteca apenas como apoio para executar sem se perder.</p>
          </div>
        </div>
      </section>

      <section className="dashboard-main" data-analytics-section="guided_experience">
        <aside className="customer-sidebar">
          <div
            className="mini-cover"
            style={{
              backgroundImage: currentProduct.theme.imageUrl
                ? `url(${currentProduct.theme.imageUrl})`
                : undefined,
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
              <p>
                A primeira missão é escolher uma combinação real, identificar o detalhe que mais
                apaga sua presença e escrever a frase de diagnóstico. Você termina o dia sabendo
                exatamente o que ajustar antes de pensar em comprar algo novo.
              </p>
              {!hasActiveSubscription && (
                <div className="trial-unlock-note">
                  <Sparkles size={17} />
                  <span>O Dia 1 está liberado gratuitamente. Dias 2 a 7 e biblioteca premium aparecem depois do acesso completo.</span>
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
                {!hasActiveSubscription && mission.id !== firstMission?.id
                  ? <Lock size={15} />
                  : completedMissionIds.has(mission.id) ? <Check size={16} /> : mission.day}
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
                  {activeMissionGuidanceConfig.fields.map((field) => (
                    <label key={field.key}>
                      {field.label}
                      {field.options ? (
                        <select
                          value={activeMissionAnswers[field.key] ?? ''}
                          onChange={(event) => updateMissionAnswer(activeMission.id, field.key, event.target.value)}
                        >
                          <option value="">{field.placeholder}</option>
                          {field.options.map((option) => <option key={option} value={option}>{option}</option>)}
                        </select>
                      ) : (
                        <textarea
                          rows={3}
                          maxLength={360}
                          value={activeMissionAnswers[field.key] ?? ''}
                          placeholder={field.placeholder}
                          onChange={(event) => updateMissionAnswer(activeMission.id, field.key, event.target.value)}
                        />
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
                  <button
                    className="inline-save-button"
                    disabled={generatingGuidance || savingInteraction || completedMissionIds.has(activeMission.id)}
                    onClick={() => requestMissionGuidance(activeMission.id)}
                  >
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
                        {activeMissionGuidance.signals.map((signal) => <span key={signal}>{signal}</span>)}
                      </div>
                      <ul>
                        {activeMissionGuidance.microActions.map((action) => <li key={action}>{action}</li>)}
                      </ul>
                      {activeMissionGuidance.caution && <small>{activeMissionGuidance.caution}</small>}
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
                    <h3>Estamos guardando seu ajuste do Dia 1.</h3>
                    <p>
                      Em alguns segundos você verá o próximo passo da jornada MUSA, sem perder sua personalização.
                    </p>
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
                    <p>
                      Agora você já sabe qual detalhe mais apaga o conjunto. O Dia 2 abre a próxima camada:
                      criar uma assinatura simples para repetir elegância sem esforço.
                    </p>
                    {!hasActiveSubscription && (
                      <button className="primary-button" onClick={handleSubscriptionClick}>
                        <CreditCard size={18} />
                        Liberar Dia 2 e acesso completo
                      </button>
                    )}
                  </div>
                </div>
              )}
              <button
                className="secondary-button"
                disabled={!workspace || completedMissionIds.has(activeMission.id) || !canRegisterActiveMission || missionCompletionStatus === 'processing'}
                onClick={() => completeMission(activeMission.id)}
              >
                {missionCompletionStatus === 'processing'
                  ? <LoaderCircle className="button-spinner" size={18} />
                  : canRegisterActiveMission ? <Check size={18} /> : <Lock size={18} />}
                {missionCompletionStatus === 'processing'
                  ? `Registrando seu Dia ${activeMission.day}...`
                  : canRegisterActiveMission
                  ? (completedMissionIds.has(activeMission.id) ? 'Missão concluída' : `Registrar Dia ${activeMission.day} concluído`)
                  : activeMission.id === firstMission?.id
                  ? 'Salve seu ajuste para concluir'
                  : activeMissionGuidanceConfig
                  ? 'Preencha os 3 pontos para concluir'
                  : 'Assine para salvar esta missão'}
              </button>
            </article>
          )}
        </section>
      </section>

      <section className="library-section">
        <div className="section-heading">
          <Library size={22} />
          <div>
            <p className="section-kicker">Biblioteca da cliente</p>
            <h2>Materiais de apoio do método</h2>
          </div>
        </div>
        <div className="material-grid">
          {currentProduct.supportMaterials.map((material) => (
            <article className="material-card" key={material.title}>
              <BookOpen size={20} />
              <span>{material.type}</span>
              <h3>{material.title}</h3>
              <p>{material.description}</p>
              {hasActiveSubscription ? (
                <a
                  href={material.url}
                  target="_blank"
                  rel="noreferrer"
                  onClick={() => {
                    trackEvent('MATERIAL_OPEN', {
                      accessToken,
                      email: workspace.email,
                      provider: workspace.accessSource,
                      metadata: { materialTitle: material.title, materialType: material.type },
                    });
                    trackFirstUse('material_open', { materialTitle: material.title, materialType: material.type });
                  }}
                >
                  Abrir material
                </a>
              ) : (
                <button className="inline-action" onClick={handleSubscriptionClick}>
                  <Lock size={16} />
                  Liberar com assinatura
                </button>
              )}
            </article>
          ))}
        </div>
      </section>

      <section className="completion-band">
        <Sparkles size={22} />
        <p>{currentProduct.completionOffer}</p>
      </section>
    </main>
  );
}

const root = createRoot(document.getElementById('root') as HTMLElement);
root.render(<App />);
