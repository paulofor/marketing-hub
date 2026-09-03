import {
  BadgeCheck,
  Ban,
  BarChart3,
  Camera,
  Clapperboard,
  ClipboardCheck,
  FileText,
  ListChecks,
  Mic2,
  Music,
  PlayCircle,
  Save,
  Scissors,
  Sparkles,
  Target,
  Timer,
  Volume2,
  Wand2,
} from "lucide-react";
import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  useCreateVideoProject,
  useUpdateVideoProject,
  useVideoProject,
  useVideoProjects,
} from "../../api/salesVideo/useVideoProjects";
import {
  DEFAULT_SALES_VIDEO_PROVIDER,
  SALES_VIDEO_PROVIDER_OPTIONS,
  type SalesVideoProviderOption,
} from "../../api/salesVideo/videoProviderCatalog";
import {
  useVideoStudioCatalog,
  type StudioCaptionPreset,
  type StudioCharacterOption,
} from "../../api/salesVideo/useVideoStudioCatalog";
import { useSalesVideoJobs } from "../../api/salesVideo/useSalesVideoJobs";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useRequestVideoRender } from "../../api/salesVideo/useRequestVideoRender";
import { useRequestSalesVideoMontage } from "../../api/salesVideo/useRequestSalesVideoMontage";
import {
  useCreateVideoProductionCycle,
  useVideoProductionCycles,
} from "../../api/salesVideo/useVideoProductionCycles";
import {
  useApolloLearningExperiments,
  useApolloSkillCandidates,
} from "../../api/salesVideo/useApolloLearningExperiments";
import {
  useEvaluateStoryboardScene,
  useVideoStoryboard,
} from "../../api/salesVideo/useVideoStoryboard";
import { useAsset } from "../../api/media/useAsset";
import { useLatestVideoReferenceAnalysis } from "../../api/salesVideo/useVideoReferences";
import { useTenantContext } from "../../utils/tenantContext";
import type {
  VideoProject,
  VideoProjectPayload,
  VideoProjectStatus,
} from "../../api/salesVideo/types";
import PageTitle from "../../components/PageTitle";
import { getStudioCommercialLabel } from "./audioVideoStudioLabels";
import "./AudioVideoStudioPage.css";

function learningMetrics(value?: string) {
  if (!value) return undefined;
  try {
    return JSON.parse(value) as {
      score?: number;
      cost?: number;
      reviewer?: string;
    };
  } catch {
    return undefined;
  }
}

function providerInstant(value?: string) {
  return value ? new Date(value).toLocaleString("pt-BR") : "—";
}

type StudioBriefing = {
  productId: string;
  commercialPlanId: string;
  experimentId: string;
  campaignKey: string;
  videoCategory: string;
  contextType: string;
  productionMode: string;
  targetChannel: string;
  format: string;
  title: string;
  objective: string;
  story: string;
  product: string;
  audience: string;
  pain: string;
  promise: string;
  mechanism: string;
  proof: string;
  cta: string;
  characterBible: string;
  environmentBible: string;
  objectBible: string;
  visualStyleGuide: string;
  imageGenerationPlan: string;
  continuityRules: string;
  scenePlan: string;
  targetDurationSeconds: string;
  funnelStage: string;
  primaryMetric: string;
  strategyGroupKey: string;
  strategyRole: string;
  commercialHypothesis: string;
  persuasionFramework: string;
  scientificBasis: string;
  measurementPlan: string;
  resultsSnapshot: string;
  learningDecision: "COLLECTING" | "CONTINUE" | "ADJUST" | "STOP";
  confirmedLearning: string;
  nextVersionRecommendation: string;
  providerPlan: string;
  characterPerformanceType: "image" | "video";
  characterPerformanceUri: string;
  referencePerformanceUri: string;
  referencePerformanceDurationSeconds: string;
  performanceConsentEvidence: string;
  performanceRightsEvidence: string;
  voiceoverPlan: string;
  soundtrackPlan: string;
  captionPlan: string;
  editingNotes: string;
  qualityGate: string;
  status: VideoProjectStatus;
};

type StudioCategoryOption = {
  value: string;
  label: string;
  durationRule: string;
  commercialUse: string;
};

type StudioSelectOption = {
  value: string;
  label: string;
  description: string;
};

type StudioPreset = {
  key: string;
  label: string;
  badge: string;
  description: string;
  briefing: StudioBriefing;
};

const premiumProductionStages = [
  {
    icon: Target,
    title: "1. Estrategia",
    section: "Oferta e funil",
    targetId: "audio-video-stage-estrategia",
    description:
      "Definir objetivo comercial, publico, dor, promessa, mecanismo, canal, duracao e metrica primaria.",
    output:
      "Brief cinematografico com papel claro no funil e proximo clique esperado.",
  },
  {
    icon: FileText,
    title: "2. Roteiro",
    section: "Narrativa",
    targetId: "audio-video-stage-roteiro",
    description:
      "Transformar a oferta em gancho, historia, demonstracao, prova, objecoes e CTA falado ou visual.",
    output: "Script por blocos de tempo, com funcao comercial de cada trecho.",
  },
  {
    icon: Camera,
    title: "3. Biblia visual",
    section: "Pre-producao",
    targetId: "audio-video-stage-biblia-visual",
    description:
      "Aprovar personagem, ambiente, objetos, marca, direcao visual, imagens mestre e regras de continuidade.",
    output:
      "Referencias persistidas antes de qualquer render para evitar cenas bonitas e incoerentes.",
  },
  {
    icon: Clapperboard,
    title: "4. Storyboard",
    section: "Plano de cenas",
    targetId: "audio-video-stage-storyboard",
    description:
      "Quebrar o video em cenas curtas com enquadramento, movimento, acao, emocao e transicao.",
    output: "Lista de cenas pronta para gerar imagem-base e video por IA.",
  },
  {
    icon: Mic2,
    title: "5. Audio",
    section: "Voz e trilha",
    targetId: "audio-video-stage-audio",
    description:
      "Definir narracao, ritmo, pausas, trilha, efeitos e legibilidade quando a usuaria estiver sem som.",
    output: "Plano de voz, trilha e legendas alinhado ao consumo mobile.",
  },
  {
    icon: Wand2,
    title: "6. Geracao IA",
    section: "Provider",
    targetId: "audio-video-stage-provider",
    description:
      "Escolher Luma, Kling, HeyGen ou outro motor conforme o tipo de cena, custo, duracao e consistencia.",
    output:
      "Jobs de clipes curtos ou avatar com request, response, custo e artefatos rastreaveis.",
  },
  {
    icon: Scissors,
    title: "7. Montagem",
    section: "Pos-producao",
    targetId: "audio-video-stage-montagem",
    description:
      "Cortar falhas, ajustar ritmo, unir cenas, inserir legenda, audio, capa, HLS e fallback MP4.",
    output:
      "Versao publicavel com acabamento premium e variações de corte quando fizer sentido.",
  },
  {
    icon: ClipboardCheck,
    title: "8. Revisao",
    section: "Gate comercial",
    targetId: "audio-video-stage-revisao",
    description:
      "Checar promessa permitida, clareza, continuidade, audio, prova, CTA, HLS e aderencia ao PDE.",
    output: "Aprovado, bloqueado com causa-raiz ou enviado para nova iteracao.",
  },
  {
    icon: BarChart3,
    title: "9. Aprendizado",
    section: "Metricas",
    targetId: "audio-video-stage-aprendizado",
    description:
      "Medir play, retencao, clique, diagnostico, paywall, checkout e compra para decidir novos cortes.",
    output: "Aprendizados acionaveis para criativos, funil e oferta.",
  },
];

const productionPillars = [
  {
    icon: FileText,
    title: "Roteiro longo",
    description:
      "Estrutura narrativa por atos, promessa, progressao emocional e CTA comercial.",
  },
  {
    icon: Clapperboard,
    title: "Varias cenas",
    description:
      "Planejamento de takes, continuidade visual, ritmo e funcao de cada cena.",
  },
  {
    icon: Volume2,
    title: "Narracao",
    description:
      "Direcao de voz, pausas, enfase e alinhamento com o nivel de sofisticacao da oferta.",
  },
  {
    icon: Music,
    title: "Trilha sonora",
    description:
      "Camada sonora planejada para aumentar retencao, desejo e percepcao premium.",
  },
  {
    icon: Scissors,
    title: "Pos-producao",
    description:
      "Montagem, cortes, legendas, revisao editorial e acabamento antes de publicar.",
  },
  {
    icon: Sparkles,
    title: "PDE premium",
    description:
      "Videos para elevar valor percebido de produtos digitais com IA aplicada ao dia a dia.",
  },
];

const videoCategoryOptions: StudioCategoryOption[] = [
  {
    value: "COMMERCIAL_SHORT",
    label: "Video comercial curto",
    durationRule: "6 a 60 segundos",
    commercialUse:
      "Hero, anuncio, Reels, Stories, retargeting e clique para diagnostico.",
  },
  {
    value: "LONG_FORM",
    label: "Video longo / VSL",
    durationRule: "180 segundos ou mais",
    commercialUse:
      "Paywall, pagina de venda, objecoes, prova e explicacao da oferta.",
  },
  {
    value: "INSTITUTIONAL_CONTENT",
    label: "Institucional / conteudo",
    durationRule: "duracao livre",
    commercialUse: "Conteudo de marca, autoridade, onboarding ou nutricao.",
  },
];

const campaignOptions: StudioSelectOption[] = [
  {
    value: "musa-pde-entry-v7-espelho-antes-de-sair",
    label: "MUSA v7 - vídeo leve dos 7 dias",
    description:
      "Hero curto para explicar a jornada MUSA pelo espelho, diagnóstico e plano de 7 dias.",
  },
  {
    value: "musa-video-manifesto-presenca-digital",
    label: "MUSA manifesto - presença digital",
    description:
      "Vídeo mais longo para história, mecanismo, prova, oferta e CTA.",
  },
];

const targetChannelOptions: StudioSelectOption[] = [
  {
    value: "PDE_HERO_DIAGNOSTIC",
    label: "Hero do PDE para diagnóstico",
    description:
      "Primeira dobra da experiência, levando a mulher para o diagnóstico gratuito.",
  },
  {
    value: "PDE_AND_SOCIAL",
    label: "PDE e redes sociais",
    description:
      "Peça reutilizável em página, Reels, TikTok, Shorts e remarketing.",
  },
  {
    value: "SOCIAL_REELS_STORIES",
    label: "Reels, Stories e TikTok",
    description: "Criativo vertical de atenção rápida para tráfego frio.",
  },
  {
    value: "PAYWALL_OFFER",
    label: "Oferta antes do checkout",
    description:
      "Vídeo para explicar valor, reduzir objeções e empurrar para compra.",
  },
];

const funnelStageOptions: StudioSelectOption[] = [
  {
    value: "AWARENESS_TO_DIAGNOSTIC",
    label: "Anúncio para diagnóstico",
    description:
      "Atrai público frio e conduz para o primeiro passo gratuito do funil.",
  },
  {
    value: "AWARENESS",
    label: "Descoberta da dor",
    description: "Gera identificação inicial com a dor e o desejo da cliente.",
  },
  {
    value: "DIAGNOSTIC_TO_PAYWALL",
    label: "Diagnóstico para oferta",
    description:
      "Conecta valor percebido no diagnóstico ao acesso completo do produto.",
  },
  {
    value: "RETARGETING_PURCHASE",
    label: "Remarketing para compra",
    description:
      "Retoma quem já demonstrou interesse e reforça motivo para avançar.",
  },
];

const primaryMetricOptions: StudioSelectOption[] = [
  {
    value:
      "CTA_CLICK_TO_DIAGNOSTIC; apoio: VIDEO_PLAY, VIDEO_75, DIAGNOSTIC_COMPLETED, PAYWALL_VIEWED, CHECKOUT_STARTED, PURCHASE",
    label: "Clique no diagnóstico",
    description:
      "Métrica principal para vídeos de entrada; acompanha retenção, paywall, checkout e compra.",
  },
  {
    value: "DIAGNOSTIC_START",
    label: "Início do diagnóstico",
    description:
      "Boa métrica quando o criativo está mais próximo da experiência gratuita.",
  },
  {
    value: "PAYWALL_VIEWED",
    label: "Visualização da oferta",
    description:
      "Mede se o vídeo aumenta chegada na etapa em que a compra é apresentada.",
  },
  {
    value: "PURCHASE",
    label: "Compra",
    description:
      "Use quando o vídeo estiver diretamente ligado à página de venda ou checkout.",
  },
];

const productionModeOptions: StudioSelectOption[] = [
  {
    value: "CINEMATIC_SCENE_BLUEPRINT",
    label: "Vídeo narrativo com cenas",
    description:
      "Cenas cinematográficas planejadas antes de renderizar em Luma, Kling ou similar.",
  },
  {
    value: "STORY_FIRST_AUDIO_VIDEO",
    label: "Roteiro com voz e montagem",
    description:
      "Começa pela história, depois organiza narração, trilha, cenas e edição.",
  },
  {
    value: "AVATAR_EXPLAINER",
    label: "Apresentadora explicando",
    description:
      "Formato com avatar/apresentadora quando a clareza da explicação for prioridade.",
  },
];

const formatOptions: StudioSelectOption[] = [
  {
    value: "VERTICAL_9_16",
    label: "Vertical para Reels/TikTok/Shorts",
    description:
      "Formato principal para mobile, hero vertical e criativos sociais.",
  },
  {
    value: "SQUARE_1_1",
    label: "Quadrado para feed",
    description: "Peça de feed e variações de mídia social.",
  },
  {
    value: "HORIZONTAL_16_9",
    label: "Horizontal para página ou YouTube",
    description: "Vídeo amplo para VSL, YouTube ou apresentação em desktop.",
  },
];

const statusOptions: { value: VideoProjectStatus; label: string }[] = [
  { value: "DRAFT", label: "Rascunho" },
  { value: "READY_FOR_SCRIPT", label: "Pronto para roteiro" },
  { value: "READY_FOR_RENDER", label: "Pronto para render" },
  { value: "IN_PRODUCTION", label: "Em producao" },
  { value: "READY_FOR_REVIEW", label: "Pronto para revisao" },
  { value: "APPROVED", label: "Aprovado" },
  { value: "ARCHIVED", label: "Arquivado" },
];

function getOptionLabel(options: StudioSelectOption[], value?: string | null) {
  return (
    options.find((option) => option.value === value)?.label ??
    getStudioCommercialLabel(value)
  );
}

function getOptionDescription(
  options: StudioSelectOption[],
  value?: string | null,
) {
  return options.find((option) => option.value === value)?.description ?? "";
}

function optionsWithCurrent(
  options: StudioSelectOption[],
  currentValue: string,
) {
  if (
    !currentValue ||
    options.some((option) => option.value === currentValue)
  ) {
    return options;
  }
  return [
    {
      value: currentValue,
      label: getStudioCommercialLabel(currentValue),
      description:
        "Valor já salvo no projeto. Troque por uma opção da lista quando quiser padronizar.",
    },
    ...options,
  ];
}

const currentFlows = [
  "Criativos de experimentos continuam nas telas de experimentos e campanhas.",
  "Videos para PDEs continuam dentro dos produtos e jornadas especificas.",
  "Videos organicos curtos continuam nos fluxos atuais de producao rapida.",
  "Aprovacao e provedores seguem nas areas operacionais ja existentes.",
];

const buildSteps = [
  "Briefing audiovisual com objetivo comercial e publico.",
  "Roteiro estruturado por cenas e funcao de cada bloco.",
  "Mapa de referencias visuais, personagens, cenarios e continuidade.",
  "Plano de voz, trilha, legenda e ritmo de edicao.",
  "Fila de renderizacao, revisao, custos e artefatos auditaveis.",
];

const longFormScriptBlocks = [
  {
    time: "0:00-0:15",
    title: "Gancho",
    objective: "Quebrar rolagem com dor clara e promessa especifica.",
  },
  {
    time: "0:15-0:45",
    title: "Dor e custo oculto",
    objective: "Mostrar o prejuizo pratico de continuar sem resolver.",
  },
  {
    time: "0:45-1:20",
    title: "Mecanismo",
    objective:
      "Apresentar a nova forma de obter o resultado com menos esforco.",
  },
  {
    time: "1:20-2:05",
    title: "Demonstracao",
    objective: "Exibir processo, exemplo, tela, antes/depois ou prova visual.",
  },
  {
    time: "2:05-2:35",
    title: "Oferta",
    objective: "Conectar promessa, entregaveis, bonus e reducao de risco.",
  },
  {
    time: "2:35-3:00",
    title: "CTA",
    objective: "Dar uma acao simples e direta para o proximo passo do funil.",
  },
];

const heroScriptBlocks = [
  {
    time: "0:00-0:08",
    title: "Dor do espelho",
    objective:
      "Gerar identificacao imediata com a sensacao de faltar presenca.",
  },
  {
    time: "0:08-0:16",
    title: "Resultado possivel",
    objective:
      "Mostrar elegancia acessivel, sem luxo e sem transformacao exagerada.",
  },
  {
    time: "0:16-0:26",
    title: "Mecanismo MUSA",
    objective:
      "Tangibilizar ruido visual, peca-sinal, cor, acabamento e postura.",
  },
  {
    time: "0:26-0:34",
    title: "Diagnostico",
    objective: "Levar ao clique no plano MUSA de 7 dias.",
  },
];

const productionChecklist = [
  "Briefing comercial preenchido",
  "Personagens com imagens mestre aprovadas",
  "Ambientes com placas visuais e mapa de continuidade",
  "Objetos/produto com referencias separadas",
  "Roteiro narrado compatível com a duracao escolhida",
  "Blocos de cena com funcao clara",
  "Voz definida com ritmo, pausas e emocao",
  "Trilha escolhida sem competir com a narracao",
  "Legenda planejada para consumo sem audio",
  "CTA final conectado ao funil de venda",
];

const defaultScenePrompts = [
  "Cena de abertura com rosto, movimento ou demonstracao visual imediata.",
  "Cena de contraste mostrando a dor antes da solucao.",
  "Cena curta mostrando o primeiro sinal do resultado desejado.",
  "Cena do mecanismo com uma unica microacao visual simples.",
  "Cena de demonstracao do segundo passo do mecanismo.",
  "Cena de prova com resultado, depoimento ou dado verificavel.",
  "Cena da oferta com entregaveis e ganho percebido.",
  "Cena final com CTA, URL, produto ou proximo passo.",
];

const musaV7ScenePrompts = [
  "Cena 1 (3-4s): mulher urbana brasileira diante do espelho, pronta para sair, percebe ruido visual e demonstra duvida discreta sob luz natural suave.",
  "Cena 2 (3-4s): quadro aproximado das maos removendo dois acessorios e mantendo somente uma peca-sinal, sem repetir gesto.",
  "Cena 3 (3-4s): a mesma mulher compara junto ao rosto tecidos creme e vinho e escolhe o vinho com decisao clara.",
  "Cena 4 (3-4s): diante do mesmo espelho, ela dobra a manga com acabamento limpo e alinha os ombros.",
  "Cena 5 (3-4s): detalhe da peca-sinal aplicada ao figurino preservado, com camera acompanhando o movimento da mao.",
  "Cena 6 (3-4s): a mesma mulher caminha em ambiente urbano claro, postura segura e elegancia acessivel sem ostentacao.",
  "Cena 7 (3-4s): ela confere no espelho o resultado coerente, com sorriso discreto e composicao antes/depois compreensivel.",
  "Cena 8 (3-4s): mulher segura o celular, inicia o diagnostico sem UI legivel e encerra com gesto claro de proximo passo.",
];
const MAX_CINEMATIC_SCENES = 48;

export function resolveStudioSceneRole(sceneIndex: number, sceneCount: number) {
  if (sceneIndex === 0) return "DOR";
  if (sceneIndex === sceneCount - 1) return "CTA";
  if (sceneIndex === 1) return "RESULTADO";
  return "MECANISMO";
}

export function buildStudioSceneMetadata(
  project: VideoProject,
  provider: SalesVideoProviderOption,
  scenePrompt: string,
  sceneIndex: number,
  sourceImage?: { assetId: number; url: string },
  sceneCount = 4,
) {
  return JSON.stringify({
    commercial_goal: "PDE_MUSA_HERO_VIDEO_SCENE",
    generation_strategy: "SCENE_BY_SCENE_MONTAGE",
    studio_project_id: project.id,
    campaign_key: project.campaignKey,
    scene: {
      order: sceneIndex + 1,
      role: resolveStudioSceneRole(sceneIndex, sceneCount),
      prompt: scenePrompt,
      duration_seconds: provider.clipDurationSeconds,
    },
    continuity: {
      character_bible: project.characterBible,
      environment_bible: project.environmentBible,
      object_bible: project.objectBible,
      visual_style_guide: project.visualStyleGuide,
      rules: project.continuityRules,
    },
    post_production: {
      caption_plan: project.captionPlan,
      voiceover_plan: project.voiceoverPlan,
      soundtrack_plan: project.soundtrackPlan,
      cta_text: project.ctaText,
    },
    provider_strategy: {
      provider_name: provider.providerName,
      expected_clip_duration_seconds: provider.clipDurationSeconds,
    },
    characterPerformance:
      provider.providerName === "RUNWAY_ACT_TWO"
        ? {
            characterType: project.characterPerformanceType,
            characterUri: project.characterPerformanceUri,
            referencePerformanceUri: project.referencePerformanceUri,
            referencePerformanceDurationSeconds:
              project.referencePerformanceDurationSeconds,
            consentEvidence: project.performanceConsentEvidence,
            performanceRightsEvidence: project.performanceRightsEvidence,
            bodyControl: true,
            expressionIntensity: 3,
          }
        : undefined,
    image_to_video: {
      enabled: Boolean(sourceImage),
      source_image_provider: sourceImage ? "APPROVED_ASSET" : null,
      source_image_asset_id: sourceImage?.assetId ?? null,
      source_image_url: sourceImage?.url ?? null,
      animation_provider: provider.providerName,
    },
  });
}

export function actTwoConfigurationIssue(project: VideoProject) {
  if (
    project.characterPerformanceType !== "image" &&
    project.characterPerformanceType !== "video"
  ) {
    return "Selecione se a personagem autorizada e uma imagem ou um video.";
  }
  if (!project.characterPerformanceUri?.startsWith("https://")) {
    return "Informe a URL HTTPS da personagem autorizada.";
  }
  if (!project.referencePerformanceUri?.startsWith("https://")) {
    return "Informe a URL HTTPS da performance de referencia autorizada.";
  }
  const duration = project.referencePerformanceDurationSeconds;
  if (!duration || duration < 3 || duration > 30) {
    return "Informe a duracao medida da performance entre 3 e 30 segundos.";
  }
  if (!project.performanceConsentEvidence?.trim()) {
    return "Registre a evidencia de consentimento da personagem.";
  }
  if (!project.performanceRightsEvidence?.trim()) {
    return "Registre a evidencia dos direitos da performance.";
  }
  return "";
}

export function readStudioSceneOrder(
  metadataJson?: string | null,
  auditSnapshotJson?: string | null,
) {
  try {
    const audit = auditSnapshotJson
      ? (JSON.parse(auditSnapshotJson) as { renderMetadataJson?: string })
      : undefined;
    const effectiveMetadataJson = metadataJson?.includes('"studio_project_id"')
      ? metadataJson
      : audit?.renderMetadataJson;
    if (!effectiveMetadataJson) return undefined;
    const metadata = JSON.parse(effectiveMetadataJson) as {
      studio_project_id?: number;
      campaign_key?: string;
      scene?: { order?: number; role?: string };
    };
    return {
      projectId: metadata.studio_project_id,
      campaignKey: metadata.campaign_key,
      order: metadata.scene?.order,
      role: metadata.scene?.role,
    };
  } catch {
    return undefined;
  }
}

export function selectSingleJobForScene(
  selectedJobIds: number[],
  jobId: number,
  jobIdsFromSameScene: number[],
) {
  if (selectedJobIds.includes(jobId)) {
    return selectedJobIds.filter((id) => id !== jobId);
  }
  return [
    ...selectedJobIds.filter((id) => !jobIdsFromSameScene.includes(id)),
    jobId,
  ];
}

const exampleStory =
  "Uma consultora independente sente que sua presenca digital nao mostra sua autoridade real. Ela tenta postar melhor, ajustar foto, escrever bio e criar conteudo, mas tudo parece solto. Ao entrar no Metodo MUSA, ela recebe um diagnostico guiado por IA que transforma sinais dispersos em uma direcao clara de imagem, conteudo e posicionamento. Em poucos dias, ela entende o que precisa ajustar, passa a se apresentar com mais seguranca e convida outras pessoas para fazerem o mesmo diagnostico.";

const defaultBriefing: StudioBriefing = {
  productId: "",
  commercialPlanId: "",
  experimentId: "",
  campaignKey: "musa-video-manifesto-presenca-digital",
  videoCategory: "LONG_FORM",
  contextType: "PDE",
  productionMode: "STORY_FIRST_AUDIO_VIDEO",
  targetChannel: "PDE_AND_SOCIAL",
  format: "VERTICAL_9_16",
  title: "MUSA - video manifesto de presenca digital",
  objective:
    "Testar uma narrativa audiovisual para aumentar desejo, confianca e acao no Metodo MUSA.",
  story: exampleStory,
  product: "Metodo MUSA",
  audience: "Mulheres que vendem sua imagem, conhecimento ou atendimento",
  pain: "Esta se esforcando para aparecer melhor, mas sua presenca digital nao traduz autoridade",
  promise:
    "Sair da sensacao de improviso e enxergar os proximos ajustes de imagem com clareza",
  mechanism: "Diagnostico de presenca publica guiado por IA",
  proof:
    "Antes e depois da clareza de posicionamento, bio, imagem e direcao de conteudo",
  cta: "Fazer o diagnostico MUSA",
  characterBible:
    "Personagem principal: mulher consultora, 35-45 anos, rosto frontal, tres quartos, perfil, corpo inteiro, figurino principal, acessorios e URLs/IDs das imagens aprovadas.",
  environmentBible:
    "Ambiente principal: escritorio claro e elegante, plano geral, angulo oposto, lateral, detalhes da mesa, entradas/saidas e URL/ID da imagem mestra.",
  objectBible:
    "Produto e objetos: tela do diagnostico MUSA, celular, notebook, elementos de marca e qualquer texto/logotipo como arquivo separado para composicao.",
  visualStyleGuide:
    "Realista premium, luz suave, pele natural, fundo limpo, composicao vertical 9:16, contraste moderado e paleta elegante sem excesso de efeitos.",
  imageGenerationPlan:
    "Solicitar ao modelo de imagem OpenAI primeiro as imagens mestre de personagem, ambiente, produto e frames-chave; aprovar antes de pedir video.",
  continuityRules:
    "Manter rosto, cabelo, figurino, acessorios, escala, temperatura de cor, posicao de objetos fixos e arquitetura do ambiente em todas as cenas.",
  scenePlan: defaultScenePrompts.join("\n"),
  targetDurationSeconds: "180",
  funnelStage: "AWARENESS",
  primaryMetric: "DIAGNOSTIC_START",
  strategyGroupKey: "musa-two-video-funnel-v1",
  strategyRole: "CAMPAIGN_QUALIFICATION",
  commercialHypothesis:
    "Se a campanha mostrar uma dor cotidiana e um microajuste realista, mulheres que se identificam chegarao mais qualificadas ao primeiro ajuste MUSA.",
  persuasionFramework: "PAS + JTBD + mecanismo + message match",
  scientificBasis:
    "Vincular artigos e conceitos aplicados, registrando promessa permitida e limites antes da publicacao.",
  measurementPlan:
    "Video 1: retencao 3s, conclusao, clique e sessao humana. Video 2: progresso de reproducao, inicio do primeiro ajuste, diagnostico, checkout e venda atribuida.",
  resultsSnapshot: "Aguardando eventos reais atribuídos a esta versao.",
  learningDecision: "COLLECTING",
  confirmedLearning: "",
  nextVersionRecommendation: "",
  providerPlan:
    "Comecar com roteiro e storyboard; depois testar narracao, cenas-chave e montagem em jobs auditaveis.",
  characterPerformanceType: "image",
  characterPerformanceUri: "",
  referencePerformanceUri: "",
  referencePerformanceDurationSeconds: "",
  performanceConsentEvidence: "",
  performanceRightsEvidence: "",
  voiceoverPlan:
    "Voz proxima, confiante e acolhedora, com ritmo medio e pausas curtas para reforcar pontos de virada.",
  soundtrackPlan:
    "Trilha leve, moderna e aspiracional, sempre abaixo da narracao.",
  captionPlan:
    "Legendas curtas com palavras-chave de dor, mecanismo, prova e CTA.",
  editingNotes:
    "Priorizar cortes limpos, prova visual concreta e CTA sem excesso de texto.",
  qualityGate:
    "Aprovar somente se a historia estiver clara, o mecanismo parecer plausivel, o audio for compreensivel e o CTA estiver conectado ao funil.",
  status: "READY_FOR_SCRIPT",
};

const musaV7Briefing: StudioBriefing = {
  productId: "4",
  commercialPlanId: "",
  experimentId: "",
  campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
  videoCategory: "COMMERCIAL_SHORT",
  contextType: "PDE",
  productionMode: "CINEMATIC_SCENE_BLUEPRINT",
  targetChannel: "PDE_HERO_DIAGNOSTIC",
  format: "VERTICAL_9_16",
  title: "MUSA v7 - O espelho antes de sair",
  objective:
    "Qualificar mulheres que reconhecem a dor do espelho e conduzi-las ao diagnostico MUSA sem quebrar a promessa da campanha.",
  story:
    "Antes de sair, uma mulher urbana se olha no espelho e percebe que nao precisa comprar uma vida nova. Ela precisa ajustar pequenos sinais da presenca que ja quer comunicar: limpar ruido visual, escolher uma peca-sinal, alinhar cor, acabamento e postura. O video conduz essa passagem de duvida discreta para clareza elegante e termina no diagnostico gratuito do Plano MUSA de 7 dias.",
  product: "Metodo MUSA - Presenca Elegante em 7 Dias",
  audience:
    "Mulheres urbanas que querem presenca mais elegante, marcante e segura com escolhas acessiveis e sem esforco excessivo",
  pain: "A cliente se arruma, olha no espelho e sente que sua imagem ainda fica comum, apagada ou com ruido visual.",
  promise:
    "Em 7 dias, pequenos ajustes podem deixar a imagem mais intencional, elegante e coerente usando o que ela ja tem.",
  mechanism:
    "Arquitetura de Presenca Elegante Acessivel: ruido visual, peca-sinal, cor, acabamento, postura e repeticao diaria.",
  proof:
    "Mostrar microacoes reais: retirar excesso visual, escolher um acessorio discreto, comparar paleta, ajustar acabamento e iniciar o diagnostico.",
  cta: "Ver meu plano MUSA de 7 dias",
  characterBible:
    "Sofia MUSA ou mulher urbana brasileira adulta, elegante sem ostentacao, roupa simples com acabamento bonito, expressao natural, postura discreta e confiante. Usar imagem-semente aprovada do produto quando disponivel; preservar cabelo, faixa de idade, figurino e energia visual entre as cenas.",
  environmentBible:
    "Quarto claro com espelho, luz natural suave e detalhes editoriais acessiveis; rua urbana elegante e realista; ambiente final claro com celular em maos sem texto legivel de interface.",
  objectBible:
    "Espelho, celular com diagnostico sem textos legiveis, acessorio discreto como peca-sinal, tecido, paleta creme/vinho, bolsa ou colar simples. Nao usar sacolas de luxo, marcas ou UI deformada.",
  visualStyleGuide:
    "Editorial realista, intimo e premium acessivel. Paleta vinho MUSA, creme editorial, blush quente, dourado discreto, grafite suave e oliva. Sem estetica de slide, palestra, banco de imagem ou luxo inacessivel.",
  imageGenerationPlan:
    "Gerar ou selecionar primeiro imagem mestre da personagem e frames-chave de espelho, caminhada, detalhes de mecanismo e gesto no celular. Aprovar antes de pedir cenas Luma/Kling.",
  continuityRules:
    "Manter a mesma personagem, cabelo, roupa base, temperatura de luz, estilo de ambiente e nivel de elegancia. O video deve funcionar sem audio, com legendas adicionadas na montagem final.",
  scenePlan: musaV7ScenePrompts.join("\n"),
  targetDurationSeconds: "30",
  funnelStage: "AWARENESS_TO_DIAGNOSTIC",
  primaryMetric:
    "CTA_CLICK_TO_DIAGNOSTIC; apoio: VIDEO_PLAY, VIDEO_75, DIAGNOSTIC_COMPLETED, PAYWALL_VIEWED, CHECKOUT_STARTED, PURCHASE",
  strategyGroupKey: "musa-two-video-funnel-v1",
  strategyRole: "CAMPAIGN_QUALIFICATION",
  commercialHypothesis:
    "O video de campanha identifica a dor e qualifica a chegada; o hero preserva a promessa, explica o mecanismo e aumenta o inicio do primeiro ajuste.",
  persuasionFramework:
    "Problem-Agitate-Solve + Jobs to Be Done + Mechanism-first + Message match",
  scientificBasis:
    "Conceitos dos artigos MUSA devem sustentar percepcao, coerencia visual e microacoes, sem prometer aprovacao externa ou transformacao psicologica.",
  measurementPlan:
    "Campanha: retencao 3s, conclusao, CTR e sessoes humanas. Hero: reproducao por trecho, primeiro ajuste, diagnostico, login, paywall, checkout, compra e reembolso.",
  resultsSnapshot: "Aguardando publicacao aprovada e eventos reais.",
  learningDecision: "COLLECTING",
  confirmedLearning: "",
  nextVersionRecommendation: "",
  providerPlan:
    "Luma Ray como principal para cenas editoriais e movimento; Kling como alternativa de realismo/custo; HeyGen apenas se a decisao mudar para apresentadora/avatar.",
  characterPerformanceType: "image",
  characterPerformanceUri: "",
  referencePerformanceUri: "",
  referencePerformanceDurationSeconds: "",
  performanceConsentEvidence: "",
  performanceRightsEvidence: "",
  voiceoverPlan:
    "Opcional. Se usar voz, pt-BR feminina, intima, baixa, segura e sem entusiasmo artificial. A peca precisa vender mesmo com audio desligado.",
  soundtrackPlan:
    "Trilha feminina, leve, editorial, crescente e sofisticada, sempre discreta para nao parecer propaganda agressiva.",
  captionPlan:
    "Legendas obrigatorias adicionadas fora do modelo: 'Voce se arruma... mas sente que ainda falta presenca?', 'Em 7 dias, pequenos ajustes deixam sua imagem mais intencional.', 'Ruido visual. Peca-sinal. Cor. Acabamento. Postura.', 'Faça o diagnostico gratuito e veja seu Plano MUSA de 7 dias.'",
  editingNotes:
    "Montagem 28-34s: dor do espelho, resultado acessivel, mecanismo em cortes sensoriais e CTA no diagnostico. Criar tambem cortes de 15s e 6-8s para Reels/Stories e retargeting.",
  qualityGate:
    "Aprovar somente se completar Dor -> Resultado -> Mecanismo -> CTA, preservar promessa sem garantia absoluta, nao parecer luxo inacessivel, funcionar em mobile sem audio, ter HLS/fallback e revisao humana antes de entrar em heroVideos da v7.",
  status: "READY_FOR_SCRIPT",
};

const vega91Briefing: StudioBriefing = {
  ...musaV7Briefing,
  experimentId: "91",
  campaignKey: "vega-91-instagram-research-intelligence-v1",
  targetChannel: "INSTAGRAM_REELS_STORIES",
  title: "Vega #91 - O espelho antes de sair",
  objective:
    "Transformar o reconhecimento íntimo diante do espelho em clique qualificado para o diagnóstico MUSA, com um criativo vertical orientado por pesquisa e medido até a venda.",
  strategyGroupKey: "vega-91-instagram-v1",
  strategyRole: "CAMPAIGN_QUALIFICATION",
  commercialHypothesis:
    "Se o Reels espelhar a dúvida discreta antes de sair, demonstrar um microajuste acessível e preservar a continuidade até o diagnóstico, aumentará a chegada qualificada sem depender de luxo ou promessa absoluta.",
  scientificBasis:
    "Aplicar somente cartões rastreáveis das coleções video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c. Artigos orientam decisões, mas não contam como venda, prova do produto ou autorização de publicação.",
  measurementPlan:
    "Segregar QA de humanos e medir retenção 3s, VIDEO_25/50/75/100, clique no CTA, diagnóstico iniciado e concluído, paywall, checkout, pagamento aprovado, reembolso, custo por venda e retrabalho do ativo.",
  resultsSnapshot:
    "Piloto ainda não publicado: aguardando ativo aprovado e eventos humanos segregados do experimento 91.",
};

function researchAuthorityLabel(authority: string) {
  const labels: Record<string, string> = {
    PRODUCTION_ADVISORY: "orienta produção",
    COMMUNICATION_ADVISORY: "orienta comunicação",
    REVIEW_CRITERIA_ONLY: "somente revisão",
  };
  return labels[authority] ?? authority;
}

function researchCardCountLabel(count: number) {
  return count === 1 ? "1 cartão" : `${count} cartões`;
}

const studioPresets: StudioPreset[] = [
  {
    key: "vega-91-research-pilot",
    label: "Vega #91 · piloto Instagram",
    badge: "Piloto com pesquisa",
    description:
      "Criativo Reels/Stories reutilizando cartões rastreáveis no harness de Apolo, Íris, Psique e Têmis.",
    briefing: vega91Briefing,
  },
  {
    key: "musa-v7",
    label: "MUSA v7 hero cinematografico",
    badge: "Caso real",
    description:
      "Blueprint comercial do video hero da Semana dos 7 Sinais de Presenca.",
    briefing: musaV7Briefing,
  },
  {
    key: "musa-manifesto",
    label: "Manifesto MUSA 3 minutos",
    badge: "Modelo longo",
    description:
      "Estrutura longa para historia, mecanismo, prova, oferta e CTA.",
    briefing: defaultBriefing,
  },
];

function buildBriefingFromProject(project: VideoProject): StudioBriefing {
  return {
    productId: project.productId ? String(project.productId) : "",
    commercialPlanId: project.commercialPlanId
      ? String(project.commercialPlanId)
      : "",
    experimentId: project.experimentId ? String(project.experimentId) : "",
    campaignKey: project.campaignKey || "",
    videoCategory: project.videoCategory || defaultBriefing.videoCategory,
    contextType: project.contextType || defaultBriefing.contextType,
    productionMode: project.productionMode || defaultBriefing.productionMode,
    targetChannel: project.targetChannel || defaultBriefing.targetChannel,
    format: project.format || defaultBriefing.format,
    title: project.title,
    objective: project.objective,
    story: project.storyText || project.objective,
    product: project.contextType || defaultBriefing.product,
    audience:
      getStudioCommercialLabel(project.targetChannel) ||
      defaultBriefing.audience,
    pain: project.hookText || project.objective,
    promise:
      getStudioCommercialLabel(project.primaryMetric) ||
      project.objective ||
      defaultBriefing.promise,
    mechanism:
      getStudioCommercialLabel(project.productionMode) ||
      defaultBriefing.mechanism,
    proof: project.visualReferences || defaultBriefing.proof,
    cta: project.ctaText || defaultBriefing.cta,
    characterBible: project.characterBible || defaultBriefing.characterBible,
    environmentBible:
      project.environmentBible || defaultBriefing.environmentBible,
    objectBible: project.objectBible || defaultBriefing.objectBible,
    visualStyleGuide:
      project.visualStyleGuide || defaultBriefing.visualStyleGuide,
    imageGenerationPlan:
      project.imageGenerationPlan || defaultBriefing.imageGenerationPlan,
    continuityRules: project.continuityRules || defaultBriefing.continuityRules,
    scenePlan: project.scenePlan || defaultBriefing.scenePlan,
    targetDurationSeconds: project.targetDurationSeconds
      ? String(project.targetDurationSeconds)
      : defaultBriefing.targetDurationSeconds,
    funnelStage: project.funnelStage || defaultBriefing.funnelStage,
    primaryMetric: project.primaryMetric || defaultBriefing.primaryMetric,
    strategyGroupKey:
      project.strategyGroupKey || defaultBriefing.strategyGroupKey,
    strategyRole: project.strategyRole || defaultBriefing.strategyRole,
    commercialHypothesis:
      project.commercialHypothesis || defaultBriefing.commercialHypothesis,
    persuasionFramework:
      project.persuasionFramework || defaultBriefing.persuasionFramework,
    scientificBasis: project.scientificBasis || defaultBriefing.scientificBasis,
    measurementPlan: project.measurementPlan || defaultBriefing.measurementPlan,
    resultsSnapshot: project.resultsSnapshot || defaultBriefing.resultsSnapshot,
    learningDecision:
      project.learningDecision || defaultBriefing.learningDecision,
    confirmedLearning: project.confirmedLearning || "",
    nextVersionRecommendation: project.nextVersionRecommendation || "",
    providerPlan: project.providerPlan || defaultBriefing.providerPlan,
    characterPerformanceType:
      project.characterPerformanceType ||
      defaultBriefing.characterPerformanceType,
    characterPerformanceUri: project.characterPerformanceUri || "",
    referencePerformanceUri: project.referencePerformanceUri || "",
    referencePerformanceDurationSeconds:
      project.referencePerformanceDurationSeconds?.toString() || "",
    performanceConsentEvidence: project.performanceConsentEvidence || "",
    performanceRightsEvidence: project.performanceRightsEvidence || "",
    voiceoverPlan: project.voiceoverPlan || defaultBriefing.voiceoverPlan,
    soundtrackPlan: project.soundtrackPlan || defaultBriefing.soundtrackPlan,
    captionPlan: project.captionPlan || defaultBriefing.captionPlan,
    editingNotes: project.editingNotes || defaultBriefing.editingNotes,
    qualityGate: project.qualityGate || defaultBriefing.qualityGate,
    status: project.status || defaultBriefing.status,
  };
}

function parsePositiveInteger(value: string) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function parseOptionalNumber(value: string) {
  if (!value.trim()) {
    return undefined;
  }
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export function findProviderFromPlan(providerPlan?: string | null) {
  if (!providerPlan) {
    return DEFAULT_SALES_VIDEO_PROVIDER;
  }
  const normalizedPlan = providerPlan.toUpperCase();
  const explicitlySelected = SALES_VIDEO_PROVIDER_OPTIONS.find((option) =>
    normalizedPlan.includes(
      `PROVIDER ESCOLHIDO NO ESTUDIO: ${option.label.toUpperCase()} (${option.providerName})`,
    ),
  );
  if (explicitlySelected) {
    return explicitlySelected;
  }
  const primaryProvider = SALES_VIDEO_PROVIDER_OPTIONS.find(
    (option) =>
      normalizedPlan.includes(`${option.label.toUpperCase()} COMO PRINCIPAL`) ||
      normalizedPlan.includes(`${option.providerName} COMO PRINCIPAL`),
  );
  return primaryProvider ?? DEFAULT_SALES_VIDEO_PROVIDER;
}

function durationValidationMessage(
  videoCategory: string,
  targetDurationSeconds?: number,
) {
  if (!targetDurationSeconds) {
    return "Informe uma duracao alvo valida para o projeto.";
  }
  if (
    videoCategory === "COMMERCIAL_SHORT" &&
    (targetDurationSeconds < 6 || targetDurationSeconds > 60)
  ) {
    return "Video comercial curto deve ter entre 6 e 60 segundos.";
  }
  if (videoCategory === "LONG_FORM" && targetDurationSeconds < 180) {
    return "Video longo ou VSL deve ter 180 segundos ou mais.";
  }
  return "";
}

export default function AudioVideoStudioPage() {
  const tenantContext = useTenantContext();
  const [searchParams] = useSearchParams();
  const referenceId = searchParams.get("referenceId") ?? undefined;
  const referenceAnalysis = useLatestVideoReferenceAnalysis(referenceId);
  const { projectId } = useParams<{ projectId: string }>();
  const parsedProjectId = projectId ? Number(projectId) : undefined;
  const editableProjectId =
    parsedProjectId && Number.isFinite(parsedProjectId)
      ? parsedProjectId
      : undefined;
  const selectedProjectQuery = useVideoProject(editableProjectId);
  const selectedProject = selectedProjectQuery.data;
  const videoProjectsQuery = useVideoProjects();
  const studioCatalogQuery = useVideoStudioCatalog();
  const linkedProfileId = selectedProject?.salesVideoProfileId;
  const linkedJobsQuery = useSalesVideoJobs(linkedProfileId ?? undefined);
  const requestSceneRender = useRequestVideoRender(
    linkedProfileId ?? undefined,
  );
  const requestMontage = useRequestSalesVideoMontage(
    selectedProject?.productId ?? undefined,
  );
  const createVideoProject = useCreateVideoProject();
  const updateVideoProject = useUpdateVideoProject();
  const productionCycles = useVideoProductionCycles(editableProjectId);
  const apolloLearning = useApolloLearningExperiments();
  const apolloSkills = useApolloSkillCandidates();
  const storyboardQuery = useVideoStoryboard(editableProjectId);
  const evaluateStoryboardScene = useEvaluateStoryboardScene(editableProjectId);
  const createProductionCycle =
    useCreateVideoProductionCycle(editableProjectId);
  const [cycleBudgetUsd, setCycleBudgetUsd] = useState("");
  const [cycleProductionProfile, setCycleProductionProfile] = useState<
    "DRAFT_INSTAGRAM" | "FINAL_CAMPAIGN"
  >("FINAL_CAMPAIGN");
  const [cycleLearningObjective, setCycleLearningObjective] = useState("");
  const [cycleSuccessCriterion, setCycleSuccessCriterion] = useState("");
  const [salesVideoProfileId, setSalesVideoProfileId] = useState("");
  const [saveFeedback, setSaveFeedback] = useState("");
  const [selectedSceneJobIds, setSelectedSceneJobIds] = useState<number[]>([]);
  const [sourceImageAssetId, setSourceImageAssetId] = useState("1953");
  const [briefing, setBriefing] = useState<StudioBriefing>(defaultBriefing);
  const productProfilesQuery = useSalesVideoProfiles(briefing.productId);
  const [persistedScenePlan, setPersistedScenePlan] = useState(
    defaultBriefing.scenePlan,
  );
  const parsedSourceImageAssetId = parsePositiveInteger(sourceImageAssetId);
  const sourceImageAssetQuery = useAsset(parsedSourceImageAssetId);

  const isEditingProject = Boolean(editableProjectId);
  const isSavingProject =
    createVideoProject.isPending || updateVideoProject.isPending;
  const targetDurationSeconds = parsePositiveInteger(
    briefing.targetDurationSeconds,
  );

  useEffect(() => {
    if (selectedProject) {
      const projectBriefing = buildBriefingFromProject(selectedProject);
      setBriefing(projectBriefing);
      setSalesVideoProfileId(
        selectedProject.salesVideoProfileId?.toString() ?? "",
      );
      setPersistedScenePlan(projectBriefing.scenePlan);
      setSaveFeedback("");
    }
  }, [selectedProject]);

  const scriptDraft = useMemo(
    () => [
      `Historia: ${briefing.story}`,
      `Gancho: ${briefing.audience}, se ${briefing.pain.toLowerCase()}, este video mostra um caminho mais simples.`,
      `Promessa: com ${briefing.product}, a proposta e ${briefing.promise.toLowerCase()}.`,
      `Mecanismo: a solucao usa ${briefing.mechanism.toLowerCase()}, reduzindo esforco e aumentando clareza.`,
      `Prova: use ${briefing.proof.toLowerCase()} para tornar o ganho visivel antes da oferta.`,
      `CTA: ${briefing.cta}.`,
    ],
    [briefing],
  );

  const selectedTimeline =
    targetDurationSeconds && targetDurationSeconds <= 60
      ? heroScriptBlocks
      : longFormScriptBlocks;
  const selectedScenePrompts = briefing.scenePlan
    .split("\n")
    .slice(0, MAX_CINEMATIC_SCENES)
    .map((prompt) => prompt.trim());
  const selectedCategory =
    videoCategoryOptions.find(
      (option) => option.value === briefing.videoCategory,
    ) ?? videoCategoryOptions[1];
  const characterOptions = studioCatalogQuery.data?.characters ?? [];
  const captionPresets = studioCatalogQuery.data?.captionPresets ?? [];
  const selectedProvider = useMemo(
    () => findProviderFromPlan(briefing.providerPlan),
    [briefing.providerPlan],
  );
  const durationIssue = durationValidationMessage(
    briefing.videoCategory,
    targetDurationSeconds,
  );

  const studioSceneJobs = useMemo(
    () =>
      (linkedJobsQuery.data ?? [])
        .map((job) => ({
          job,
          scene: readStudioSceneOrder(job.metadataJson, job.auditSnapshotJson),
        }))
        .filter(
          ({ scene }) =>
            scene?.projectId === selectedProject?.id && Boolean(scene?.order),
        )
        .sort(
          (first, second) =>
            (first.scene?.order ?? 0) - (second.scene?.order ?? 0),
        ),
    [linkedJobsQuery.data, selectedProject?.id],
  );

  const updateBriefing =
    (field: keyof StudioBriefing) =>
    (
      event: ChangeEvent<
        HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
      >,
    ) => {
      setBriefing((current) => ({ ...current, [field]: event.target.value }));
    };

  const updateScenePrompt = (sceneIndex: number, prompt: string) => {
    setBriefing((current) => {
      const prompts = current.scenePlan.split("\n");
      while (prompts.length < 4) {
        prompts.push("");
      }
      prompts[sceneIndex] = prompt.replace(/\s*\n+\s*/g, " ");
      return {
        ...current,
        scenePlan: prompts.slice(0, MAX_CINEMATIC_SCENES).join("\n"),
      };
    });
  };

  const applyPreset = (preset: StudioPreset) => {
    setBriefing(preset.briefing);
    setSaveFeedback("");
  };

  const applyReferenceRecipe = () => {
    const analysis = referenceAnalysis.data?.output;
    if (!analysis) {
      setSaveFeedback("A receita de Apolo ainda nao esta disponivel.");
      return;
    }
    const blueprint = analysis.productionBlueprint;
    const category =
      blueprint.targetDurationSeconds <= 60
        ? "COMMERCIAL_SHORT"
        : blueprint.targetDurationSeconds >= 180
          ? "LONG_FORM"
          : "INSTITUTIONAL_CONTENT";
    const sourceTitle = String(
      referenceAnalysis.data?.input.title ?? "referencia",
    );
    setBriefing((current) => ({
      ...current,
      videoCategory: category,
      productionMode: "REFERENCE_RECIPE_V1",
      format: blueprint.format,
      title: `${current.product || "Novo produto"} - receita ${sourceTitle}`,
      story: blueprint.story,
      commercialHypothesis: analysis.commercialDiagnosis,
      persuasionFramework: analysis.narrativePattern,
      characterBible: blueprint.characterBible,
      environmentBible: blueprint.environmentBible,
      objectBible: blueprint.objectBible,
      visualStyleGuide: blueprint.visualStyleGuide,
      imageGenerationPlan: blueprint.imageGenerationPlan,
      continuityRules: blueprint.continuityRules,
      scenePlan: blueprint.scenePlan.join("\n"),
      targetDurationSeconds: String(blueprint.targetDurationSeconds),
      providerPlan: blueprint.providerPlan,
      voiceoverPlan: blueprint.voiceoverPlan,
      soundtrackPlan: blueprint.soundtrackPlan,
      captionPlan: blueprint.captionPlan,
      editingNotes: blueprint.editingNotes,
      qualityGate: [
        blueprint.qualityGate,
        "Bloqueios de direitos herdados da referencia:",
        ...analysis.rightsRisks.map((risk) => `- ${risk}`),
      ].join("\n"),
      status: "READY_FOR_SCRIPT",
    }));
    setSaveFeedback(
      `Receita #${referenceId} aplicada. Selecione o produto, ajuste a oferta e salve antes de gerar qualquer cena.`,
    );
  };

  const applyCharacterOption = (option: StudioCharacterOption) => {
    setBriefing((current) => ({
      ...current,
      characterBible: option.bibleText,
      qualityGate:
        option.status === "Reprovado"
          ? `${current.qualityGate}\n\nBloqueio visual: ${option.name} nao deve ser usado. ${option.reason}`
          : current.qualityGate,
    }));
    setSaveFeedback(`Personagem aplicado: ${option.name} - ${option.status}`);
  };

  const applyCaptionPreset = (preset: StudioCaptionPreset) => {
    setBriefing((current) => ({
      ...current,
      captionPlan: `${preset.planText}\n\nTexto base: ${current.captionPlan}`,
    }));
    setSaveFeedback(`Preset de legenda aplicado: ${preset.label}`);
  };

  const applyProviderOption = (option: SalesVideoProviderOption) => {
    setBriefing((current) => ({
      ...current,
      providerPlan: [
        `Provider escolhido no Estudio: ${option.label} (${option.providerName}).`,
        `Uso recomendado: ${option.recommendedUse}`,
        `Duracao por clipe: ${option.clipDurationSeconds}s; duracao direta maxima: ${option.maxDirectDurationSeconds ?? option.clipDurationSeconds}s.`,
        option.supportsSceneAssembly
          ? "Permite montagem por cenas para preservar a narrativa visual."
          : "Nao e indicado para montagem cinematografica por cenas.",
        option.providerName === "HEYGEN"
          ? "Usar apenas quando a decisao comercial for apresentadora/avatar explicando."
          : "Indicado para testar cenas cinematograficas sem avatar fixo.",
      ].join("\n"),
    }));
    setSaveFeedback(`Provider aplicado: ${option.label}`);
  };

  const buildProjectPayload = (): VideoProjectPayload => ({
    productId: parseOptionalNumber(briefing.productId)!,
    commercialPlanId: parseOptionalNumber(briefing.commercialPlanId)!,
    experimentId: parseOptionalNumber(briefing.experimentId),
    salesVideoProfileId: parseOptionalNumber(salesVideoProfileId),
    campaignKey:
      briefing.campaignKey || selectedProject?.campaignKey || undefined,
    videoCategory: briefing.videoCategory || "LONG_FORM",
    contextType: briefing.contextType || "PDE",
    productionMode: briefing.productionMode || "STORY_FIRST_AUDIO_VIDEO",
    targetChannel: briefing.targetChannel || "PDE_AND_SOCIAL",
    format: briefing.format || "VERTICAL_9_16",
    title: briefing.title,
    objective: briefing.objective,
    storyText: briefing.story,
    funnelStage: briefing.funnelStage || "AWARENESS",
    primaryMetric: briefing.primaryMetric || "DIAGNOSTIC_START",
    strategyGroupKey: briefing.strategyGroupKey,
    strategyRole: briefing.strategyRole,
    commercialHypothesis: briefing.commercialHypothesis,
    persuasionFramework: briefing.persuasionFramework,
    scientificBasis: briefing.scientificBasis,
    measurementPlan: briefing.measurementPlan,
    resultsSnapshot: briefing.resultsSnapshot,
    learningDecision: briefing.learningDecision,
    confirmedLearning: briefing.confirmedLearning,
    nextVersionRecommendation: briefing.nextVersionRecommendation,
    hookText: `${briefing.audience}, se ${briefing.pain.toLowerCase()}, este video mostra um caminho mais simples.`,
    scriptText: scriptDraft.join("\n\n"),
    scenePlan: briefing.scenePlan,
    visualReferences: briefing.proof,
    characterBible: briefing.characterBible,
    environmentBible: briefing.environmentBible,
    objectBible: briefing.objectBible,
    visualStyleGuide: briefing.visualStyleGuide,
    imageGenerationPlan: briefing.imageGenerationPlan,
    continuityRules: briefing.continuityRules,
    voiceoverPlan: briefing.voiceoverPlan,
    soundtrackPlan: briefing.soundtrackPlan,
    captionPlan: briefing.captionPlan,
    ctaText: briefing.cta,
    targetDurationSeconds: targetDurationSeconds ?? null,
    providerPlan: briefing.providerPlan,
    characterPerformanceType: briefing.characterPerformanceType,
    characterPerformanceUri: briefing.characterPerformanceUri,
    referencePerformanceUri: briefing.referencePerformanceUri,
    referencePerformanceDurationSeconds: parseOptionalNumber(
      briefing.referencePerformanceDurationSeconds,
    ),
    performanceConsentEvidence: briefing.performanceConsentEvidence,
    performanceRightsEvidence: briefing.performanceRightsEvidence,
    editingNotes: briefing.editingNotes,
    qualityGate: briefing.qualityGate,
    status: briefing.status || selectedProject?.status || "READY_FOR_SCRIPT",
    createdBy: isEditingProject ? undefined : "codex-mkt",
    updatedBy: "codex-mkt",
  });

  const handleSaveProject = async () => {
    setSaveFeedback("");
    if (!parseOptionalNumber(briefing.productId)) {
      setSaveFeedback("Selecione o produto antes de criar o projeto.");
      return;
    }
    if (durationIssue) {
      setSaveFeedback(durationIssue);
      return;
    }
    try {
      if (editableProjectId) {
        const project = await updateVideoProject.mutateAsync({
          projectId: editableProjectId,
          payload: buildProjectPayload(),
        });
        setSaveFeedback(
          `Projeto atualizado: #${project.id} - ${project.title}`,
        );
        setPersistedScenePlan(project.scenePlan ?? briefing.scenePlan);
        return;
      }

      const project = await createVideoProject.mutateAsync(
        buildProjectPayload(),
      );
      setSaveFeedback(`Projeto criado: #${project.id} - ${project.title}`);
    } catch {
      setSaveFeedback(
        "Nao foi possivel salvar o projeto agora. Revise a conexao com o backend e tente novamente.",
      );
    }
  };

  const handleRequestSceneRender = async (sceneIndex: number) => {
    if (!selectedProject || !linkedProfileId) {
      setSaveFeedback(
        "Vincule um perfil de video ao projeto antes de gerar cenas.",
      );
      return;
    }
    const previousSceneJobId =
      sceneIndex === 0
        ? undefined
        : selectedSceneJobIds.find((jobId) =>
            studioSceneJobs.some(
              ({ job, scene }) =>
                job.id === jobId && scene?.order === sceneIndex,
            ),
          );
    if (sceneIndex > 0 && !previousSceneJobId) {
      setSaveFeedback(
        `Aprove um clipe pronto da cena ${sceneIndex} antes de gerar a cena ${sceneIndex + 1}. O quadro final aprovado sera usado como ponte visual.`,
      );
      return;
    }
    if (briefing.scenePlan.trim() !== persistedScenePlan.trim()) {
      setSaveFeedback(
        "Salve os prompts das cenas antes de gerar. O render pago deve usar a versao persistida e auditavel no Marketing Hub.",
      );
      return;
    }
    const usesActTwo = selectedProvider.providerName === "RUNWAY_ACT_TWO";
    const actTwoIssue = usesActTwo
      ? actTwoConfigurationIssue(selectedProject)
      : "";
    if (actTwoIssue) {
      setSaveFeedback(actTwoIssue);
      return;
    }
    if (
      !usesActTwo &&
      (!parsedSourceImageAssetId || !sourceImageAssetQuery.data?.publicUrl)
    ) {
      setSaveFeedback(
        "Selecione uma imagem-base valida antes de gerar a cena com Kling ou Runway.",
      );
      return;
    }
    try {
      const job = await requestSceneRender.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerFamily: selectedProvider.providerFamily,
        providerName: selectedProvider.providerName,
        executionMode: "TEST",
        targetDurationSeconds: selectedProvider.clipDurationSeconds,
        continuitySourceJobId: previousSceneJobId,
        metadataJson: buildStudioSceneMetadata(
          selectedProject,
          selectedProvider,
          selectedScenePrompts[sceneIndex],
          sceneIndex,
          usesActTwo
            ? undefined
            : {
                assetId: parsedSourceImageAssetId!,
                url: sourceImageAssetQuery.data!.publicUrl!,
              },
          selectedScenePrompts.length,
        ),
      });
      setSaveFeedback(
        `Cena ${sceneIndex + 1} enviada para geracao no job #${job.id}.`,
      );
    } catch {
      setSaveFeedback(`Nao foi possivel gerar a cena ${sceneIndex + 1}.`);
    }
  };

  const handleToggleSceneApproval = (jobId: number, sceneOrder: number) => {
    const jobIdsFromSameScene = studioSceneJobs
      .filter(({ scene }) => scene?.order === sceneOrder)
      .map(({ job }) => job.id);
    setSelectedSceneJobIds((current) =>
      selectSingleJobForScene(current, jobId, jobIdsFromSameScene),
    );
  };

  const handleRequestSceneMontage = async () => {
    if (selectedSceneJobIds.length !== selectedScenePrompts.length) {
      setSaveFeedback(
        "Aprove exatamente um clipe pronto para cada plano do storyboard.",
      );
      return;
    }
    const orderedSceneJobIds = studioSceneJobs
      .filter(({ job }) => selectedSceneJobIds.includes(job.id))
      .sort(
        (first, second) =>
          (first.scene?.order ?? 0) - (second.scene?.order ?? 0),
      )
      .map(({ job }) => job.id);
    try {
      const job = await requestMontage.mutateAsync({
        requestedBy: tenantContext.userEmail,
        sourceJobIds: orderedSceneJobIds,
      });
      setSaveFeedback(
        `Montagem dos ${selectedScenePrompts.length} planos solicitada no job #${job.id}.`,
      );
    } catch {
      setSaveFeedback(
        "Nao foi possivel solicitar a montagem das cenas aprovadas.",
      );
    }
  };

  const recentProjects = videoProjectsQuery.data?.slice(0, 4) ?? [];
  const renderedJob = useMemo(
    () =>
      linkedJobsQuery.data
        ?.filter((job) => job.status === "VIDEO_READY" && job.assetId)
        .sort((first, second) => {
          const firstDate =
            first.finishedAt ?? first.updatedAt ?? first.createdAt ?? "";
          const secondDate =
            second.finishedAt ?? second.updatedAt ?? second.createdAt ?? "";
          return secondDate.localeCompare(firstDate);
        })[0],
    [linkedJobsQuery.data],
  );
  const renderedAssetQuery = useAsset(renderedJob?.assetId);
  const renderedAssetUrl =
    renderedJob?.streamPlaybackUrl?.trim() ||
    renderedAssetQuery.data?.publicUrl ||
    renderedAssetQuery.data?.url ||
    "";

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title={
          isEditingProject
            ? "Editor de Audio e Video"
            : "Estudio de Audio e Video"
        }
        subtitle={
          isEditingProject
            ? "Projeto carregado para continuar roteiro, cenas, audio, montagem e revisao comercial."
            : "Todo video comercial nasce de um blueprint: funil, promessa, cenas, audio, provider, aprovacao e metrica antes da renderizacao."
        }
      />

      {isEditingProject ? (
        <Link
          className="audio-video-studio-page__secondary-action audio-video-studio-page__back-link"
          to="/audio-video-studio/projects"
        >
          Voltar para lista de projetos
        </Link>
      ) : null}

      {selectedProjectQuery.isLoading ? (
        <article className="audio-video-studio-page__project-card">
          Carregando projeto selecionado...
        </article>
      ) : null}

      {selectedProjectQuery.isError ? (
        <article className="audio-video-studio-page__project-card">
          Nao foi possivel carregar este projeto.
        </article>
      ) : null}

      {referenceId ? (
        <section className="audio-video-studio-page__section">
          <article className="audio-video-studio-page__project-card">
            <span>Receita de video #{referenceId}</span>
            {referenceAnalysis.isLoading ? (
              <p>Carregando a análise de Apolo...</p>
            ) : referenceAnalysis.data?.status === "COMPLETED" &&
              referenceAnalysis.data.output ? (
              <>
                <strong>
                  {referenceAnalysis.data.output.productionBlueprint.archetype}
                </strong>
                <p>
                  A receita preenche narrativa, cenas, continuidade, audio,
                  legenda, providers e gate. Produto, oferta e CTA permanecem
                  sob controle deste projeto.
                </p>
                <button
                  className="audio-video-studio-page__primary-action"
                  type="button"
                  onClick={applyReferenceRecipe}
                >
                  <Wand2 size={17} aria-hidden="true" />
                  Aplicar receita ao projeto
                </button>
              </>
            ) : (
              <p>
                A receita ainda não está concluída. Volte ao resultado da
                análise para acompanhar ou reenfileirar.
              </p>
            )}
          </article>
        </section>
      ) : null}

      <section className="audio-video-studio-page__intro">
        <div>
          <p className="audio-video-studio-page__eyebrow">
            Blueprint comercial
          </p>
          <h2>Padronize o video antes de gerar cenas.</h2>
          <p>
            O Estudio organiza a producao audiovisual como um ativo comercial:
            funil, promessa, roteiro, cenas, voz, trilha, provider, montagem,
            revisao e metrica antes de qualquer renderizacao.
          </p>
        </div>
        <div
          className="audio-video-studio-page__status"
          aria-label="Status do modulo"
        >
          <Timer size={22} aria-hidden="true" />
          <span>Projeto atual</span>
          <strong>
            {targetDurationSeconds ?? 0}s /{" "}
            {getOptionLabel(formatOptions, briefing.format)}
          </strong>
          <small>
            {selectedCategory.label} ·{" "}
            {getOptionLabel(funnelStageOptions, briefing.funnelStage)}
          </small>
        </div>
      </section>

      {!isEditingProject ? (
        <section className="audio-video-studio-page__preset-grid">
          {studioPresets.map((preset) => (
            <button
              className="audio-video-studio-page__preset"
              key={preset.key}
              type="button"
              onClick={() => applyPreset(preset)}
            >
              <span>{preset.badge}</span>
              <strong>{preset.label}</strong>
              <small>{preset.description}</small>
            </button>
          ))}
        </section>
      ) : null}

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <h2>Etapas de producao premium com IA</h2>
          <p>
            A tela do estudio segue a ordem que evita desperdicio: primeiro
            clareza comercial, depois referencias visuais, geracao, montagem,
            aprovacao e aprendizado de venda.
          </p>
        </div>
        <div className="audio-video-studio-page__stage-grid">
          {premiumProductionStages.map((stage) => (
            <a
              className={`audio-video-studio-page__stage-card audio-video-studio-page__stage-card--${stage.targetId.replace(
                "audio-video-stage-",
                "",
              )}`}
              href={`#${stage.targetId}`}
              key={stage.title}
            >
              <div className="audio-video-studio-page__stage-card-header">
                <span>{stage.section}</span>
                <stage.icon size={22} aria-hidden="true" />
              </div>
              <h3>{stage.title}</h3>
              <p>{stage.description}</p>
              <small>{stage.output}</small>
              <strong className="audio-video-studio-page__stage-link">
                Ir para esta etapa
              </strong>
            </a>
          ))}
        </div>
      </section>

      <section className="audio-video-studio-page__workspace">
        <div
          id="audio-video-stage-estrategia"
          className="audio-video-studio-page__briefing"
          role="form"
          aria-label="Blueprint operacional de video comercial"
        >
          <div className="audio-video-studio-page__stage-heading">
            <h2>
              {isEditingProject
                ? "1. Estrategia do projeto"
                : "1. Estrategia e oferta"}
            </h2>
            <p>
              {isEditingProject
                ? "Continue o trabalho a partir dos dados persistidos neste projeto."
                : "Use o preset MUSA v7 como primeiro caso real ou ajuste o blueprint para outro video comercial."}
            </p>
          </div>
          <div className="audio-video-studio-page__briefing-grid">
            <label>
              ID do produto *
              <input
                value={briefing.productId}
                onChange={updateBriefing("productId")}
                required
              />
            </label>
            <label>
              ID do plano comercial
              <input
                value={briefing.commercialPlanId}
                onChange={updateBriefing("commercialPlanId")}
              />
            </label>
            <label>
              ID do experimento
              <input
                value={briefing.experimentId}
                onChange={updateBriefing("experimentId")}
              />
              <small>
                Mantém atribuição, auditoria e métricas separadas por
                experimento.
              </small>
            </label>
            <label>
              Perfil de vídeo para Apolo
              <select
                aria-label="Perfil de vídeo para Apolo"
                value={salesVideoProfileId}
                onChange={(event) => setSalesVideoProfileId(event.target.value)}
              >
                <option value="">Selecione um perfil</option>
                {(productProfilesQuery.data ?? []).map((profile) => (
                  <option key={profile.id} value={profile.id}>
                    #{profile.id} · {profile.title} · {profile.status}
                  </option>
                ))}
              </select>
              <small>
                O ciclo autônomo só é aberto depois que o projeto estiver
                vinculado a um perfil do mesmo produto.
              </small>
            </label>
            <label>
              Campanha
              <select
                value={briefing.campaignKey}
                onChange={updateBriefing("campaignKey")}
              >
                {optionsWithCurrent(campaignOptions, briefing.campaignKey).map(
                  (option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ),
                )}
              </select>
              <small>
                {getOptionDescription(campaignOptions, briefing.campaignKey)}
              </small>
            </label>
            <label>
              Categoria do video
              <select
                value={briefing.videoCategory}
                onChange={updateBriefing("videoCategory")}
              >
                {videoCategoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Canal alvo
              <select
                value={briefing.targetChannel}
                onChange={updateBriefing("targetChannel")}
              >
                {optionsWithCurrent(
                  targetChannelOptions,
                  briefing.targetChannel,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(
                  targetChannelOptions,
                  briefing.targetChannel,
                )}
              </small>
            </label>
            <label>
              Duracao alvo
              <input
                type="number"
                min="1"
                value={briefing.targetDurationSeconds}
                onChange={updateBriefing("targetDurationSeconds")}
              />
            </label>
            <label>
              Status operacional
              <select
                value={briefing.status}
                onChange={updateBriefing("status")}
              >
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="audio-video-studio-page__category-note">
            <ListChecks size={18} aria-hidden="true" />
            <div>
              <strong>{selectedCategory.durationRule}</strong>
              <span>{selectedCategory.commercialUse}</span>
            </div>
          </div>
          {durationIssue ? (
            <p className="audio-video-studio-page__duration-block">
              {durationIssue}
            </p>
          ) : null}
          <label>
            Titulo do projeto
            <input value={briefing.title} onChange={updateBriefing("title")} />
          </label>
          <label>
            Objetivo comercial do vídeo
            <textarea
              value={briefing.objective}
              onChange={updateBriefing("objective")}
              rows={3}
            />
          </label>
          <label>
            Historia inicial
            <textarea
              value={briefing.story}
              onChange={updateBriefing("story")}
              rows={7}
            />
          </label>
          <label>
            Produto
            <input
              value={briefing.product}
              onChange={updateBriefing("product")}
            />
          </label>
          <label>
            Publico
            <input
              value={briefing.audience}
              onChange={updateBriefing("audience")}
            />
          </label>
          <label>
            Dor principal
            <textarea
              value={briefing.pain}
              onChange={updateBriefing("pain")}
              rows={2}
            />
          </label>
          <label>
            Promessa
            <textarea
              value={briefing.promise}
              onChange={updateBriefing("promise")}
              rows={2}
            />
          </label>
          <label>
            Mecanismo
            <input
              value={briefing.mechanism}
              onChange={updateBriefing("mechanism")}
            />
          </label>
          <div className="audio-video-studio-page__briefing-grid">
            <label>
              Etapa do funil
              <select
                value={briefing.funnelStage}
                onChange={updateBriefing("funnelStage")}
              >
                {optionsWithCurrent(
                  funnelStageOptions,
                  briefing.funnelStage,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(funnelStageOptions, briefing.funnelStage)}
              </small>
            </label>
            <label>
              Metrica primaria
              <select
                value={briefing.primaryMetric}
                onChange={updateBriefing("primaryMetric")}
              >
                {optionsWithCurrent(
                  primaryMetricOptions,
                  briefing.primaryMetric,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(
                  primaryMetricOptions,
                  briefing.primaryMetric,
                )}
              </small>
            </label>
            <label>
              Modo de producao
              <select
                value={briefing.productionMode}
                onChange={updateBriefing("productionMode")}
              >
                {optionsWithCurrent(
                  productionModeOptions,
                  briefing.productionMode,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(
                  productionModeOptions,
                  briefing.productionMode,
                )}
              </small>
            </label>
            <label>
              Formato
              <select
                value={briefing.format}
                onChange={updateBriefing("format")}
              >
                {optionsWithCurrent(formatOptions, briefing.format).map(
                  (option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ),
                )}
              </select>
              <small>
                {getOptionDescription(formatOptions, briefing.format)}
              </small>
            </label>
          </div>
          <label>
            Prova visual
            <input value={briefing.proof} onChange={updateBriefing("proof")} />
          </label>
          <label>
            CTA
            <input value={briefing.cta} onChange={updateBriefing("cta")} />
          </label>
          <div
            className="audio-video-studio-page__stage-heading"
            id="audio-video-stage-roteiro"
          >
            <FileText size={18} aria-hidden="true" />
            <div>
              <h2>2. Roteiro e storyboard</h2>
              <span>
                O rascunho narrativo e a estrutura de cenas ficam visiveis ao
                lado para transformar a estrategia em blocos de producao.
              </span>
            </div>
          </div>
          <div className="audio-video-studio-page__draft">
            <div className="audio-video-studio-page__section-heading">
              <h2>Rascunho narrativo</h2>
              <p>
                Base pronta para transformar em roteiro falado e plano de cenas.
              </p>
            </div>
            <ol>
              {scriptDraft.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ol>
          </div>
          <div
            className="audio-video-studio-page__visual-bible"
            id="audio-video-stage-biblia-visual"
          >
            <div className="audio-video-studio-page__stage-heading">
              <h2>3. Biblia visual premium</h2>
              <p>
                Defina referencias mestras antes de gerar cenas para preservar
                consistencia entre takes.
              </p>
            </div>
            <label>
              Personagens e imagens mestre
              <textarea
                value={briefing.characterBible}
                onChange={updateBriefing("characterBible")}
                rows={4}
              />
            </label>
            <div className="audio-video-studio-page__asset-section">
              <div className="audio-video-studio-page__section-heading">
                <h2>Personagens do video</h2>
                <p>
                  Aprove ou bloqueie visualmente a personagem antes de pedir
                  renderizacao.
                </p>
              </div>
              <div className="audio-video-studio-page__character-grid">
                {studioCatalogQuery.isLoading ? (
                  <article className="audio-video-studio-page__project-card">
                    Carregando personagens do backend...
                  </article>
                ) : studioCatalogQuery.isError ? (
                  <article className="audio-video-studio-page__project-card">
                    Nao foi possivel carregar personagens do estudio.
                  </article>
                ) : (
                  characterOptions.map((option) => (
                    <button
                      className="audio-video-studio-page__character-card"
                      key={option.key}
                      type="button"
                      onClick={() => applyCharacterOption(option)}
                    >
                      <img src={option.imageUrl} alt={option.name} />
                      <span
                        className={`audio-video-studio-page__asset-status audio-video-studio-page__asset-status--${option.status.toLowerCase()}`}
                      >
                        {option.status === "Reprovado" ? (
                          <Ban size={14} aria-hidden="true" />
                        ) : (
                          <BadgeCheck size={14} aria-hidden="true" />
                        )}
                        {option.status}
                      </span>
                      <strong>{option.name}</strong>
                      <small>{option.description}</small>
                      <em>{option.reason}</em>
                    </button>
                  ))
                )}
              </div>
            </div>
            <label>
              Ambientes e imagens mestre
              <textarea
                value={briefing.environmentBible}
                onChange={updateBriefing("environmentBible")}
                rows={4}
              />
            </label>
            <label>
              Objetos, produto e marca
              <textarea
                value={briefing.objectBible}
                onChange={updateBriefing("objectBible")}
                rows={3}
              />
            </label>
            <label>
              Direcao visual e acabamento
              <textarea
                value={briefing.visualStyleGuide}
                onChange={updateBriefing("visualStyleGuide")}
                rows={3}
              />
            </label>
            <label>
              Plano para solicitar imagens via OpenAI
              <textarea
                value={briefing.imageGenerationPlan}
                onChange={updateBriefing("imageGenerationPlan")}
                rows={3}
              />
            </label>
            <label>
              Regras de continuidade
              <textarea
                value={briefing.continuityRules}
                onChange={updateBriefing("continuityRules")}
                rows={3}
              />
            </label>
            <label>
              Provider e renderizacao
              <textarea
                value={briefing.providerPlan}
                onChange={updateBriefing("providerPlan")}
                rows={3}
              />
            </label>
            <section
              className="audio-video-studio-page__stage-section"
              id="audio-video-stage-storyboard"
            >
              <div className="audio-video-studio-page__stage-heading">
                <h2>4. Storyboard</h2>
                <p>
                  Sequencia do blueprint atual para testar retencao, desejo e
                  acao sem depender de improviso.
                </p>
              </div>
              <div className="audio-video-studio-page__timeline">
                {selectedTimeline.map((block) => (
                  <article
                    className="audio-video-studio-page__timeline-block"
                    key={block.time}
                  >
                    <span>{block.time}</span>
                    <h3>{block.title}</h3>
                    <p>{block.objective}</p>
                  </article>
                ))}
              </div>
              <div className="audio-video-studio-page__columns">
                <div className="audio-video-studio-page__panel">
                  <h2>Piloto de autoaperfeicoamento de Apolo</h2>
                  {apolloLearning.isLoading ? (
                    <p>Carregando experimentos governados...</p>
                  ) : apolloLearning.isError ? (
                    <p>Nao foi possivel consultar o piloto de Apolo.</p>
                  ) : apolloLearning.data?.length ? (
                    <ol>
                      {apolloLearning.data.slice(0, 5).map((experiment) => {
                        const baseline = learningMetrics(
                          experiment.baselineResultJson,
                        );
                        const candidate = learningMetrics(
                          experiment.candidateResultJson,
                        );
                        return (
                          <li key={experiment.id}>
                            <strong>
                              #{experiment.id} · {experiment.status}
                            </strong>
                            <div>
                              Baseline {experiment.baselineVersion}: nota{" "}
                              {baseline?.score ?? "—"}, custo US${" "}
                              {baseline?.cost ?? "—"}
                            </div>
                            <div>
                              Candidata {experiment.candidateVersion}: nota{" "}
                              {candidate?.score ?? "—"}, custo US${" "}
                              {candidate?.cost ?? "—"}
                            </div>
                            <div>
                              Memoria candidata #{experiment.memoryId} · QA{" "}
                              {candidate?.reviewer ?? "pendente"}
                            </div>
                            <div>
                              {experiment.decisionEvidence ??
                                "Aguardando amostra e decisao independente."}
                            </div>
                          </li>
                        );
                      })}
                    </ol>
                  ) : (
                    <p>
                      Nenhum experimento concluido. O piloto aguarda 10 casos de
                      replay e 5 de holdout sem provider pago.
                    </p>
                  )}
                  <h3>Skills versionadas</h3>
                  {apolloSkills.isError ? (
                    <p>Nao foi possivel consultar as skills de Apolo.</p>
                  ) : apolloSkills.data?.length ? (
                    <ol>
                      {apolloSkills.data.slice(0, 5).map((skill) => (
                        <li key={skill.id}>
                          <strong>
                            {skill.skillKey} · {skill.status}
                          </strong>
                          <div>
                            {skill.baselineVersion} → {skill.candidateVersion} ·
                            seguranca {skill.safetyDecision}
                          </div>
                          <div>{skill.diffSummary}</div>
                          <div>
                            Monitoramento: {skill.approvedCases}/
                            {skill.monitoredCases} aprovados
                          </div>
                          <div>
                            {skill.rollbackReason ?? skill.safetyEvidence}
                          </div>
                        </li>
                      ))}
                    </ol>
                  ) : (
                    <p>Nenhuma skill candidata foi materializada.</p>
                  )}
                </div>
                <div className="audio-video-studio-page__panel">
                  <fieldset>
                    <legend>Prompts editaveis e persistidos por cena</legend>
                    <small>
                      Cada prompt e salvo no projeto antes do render. Descreva
                      uma unica conclusao visual; movimento, camera e o que nao
                      deve acontecer. A imagem-base aprovada define o primeiro
                      quadro e a continuidade visual.
                    </small>
                    {selectedScenePrompts.map((_, index) => (
                      <label key={`studio-scene-${index + 1}`}>
                        Cena {index + 1} ·{" "}
                        {resolveStudioSceneRole(
                          index,
                          selectedScenePrompts.length,
                        )}
                        <textarea
                          value={selectedScenePrompts[index] ?? ""}
                          onChange={(event) =>
                            updateScenePrompt(index, event.target.value)
                          }
                          rows={4}
                        />
                      </label>
                    ))}
                  </fieldset>
                </div>
                <div className="audio-video-studio-page__panel">
                  <h2>Checklist de producao</h2>
                  <ul className="audio-video-studio-page__checklist">
                    {productionChecklist.map((item) => (
                      <li key={item}>
                        <BadgeCheck size={16} aria-hidden="true" />
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
              <div className="audio-video-studio-page__panel">
                <h2>Estrategia e aprendizado do video</h2>
                <p>
                  Vincule as duas pecas pelo mesmo grupo: campanha identifica a
                  dor; hero explica mecanismo e jornada sem quebrar a promessa.
                </p>
                <div className="audio-video-studio-page__columns">
                  <label>
                    Grupo da estrategia
                    <input
                      value={briefing.strategyGroupKey}
                      onChange={updateBriefing("strategyGroupKey")}
                    />
                  </label>
                  <label>
                    Funcao da peca
                    <select
                      value={briefing.strategyRole}
                      onChange={updateBriefing("strategyRole")}
                    >
                      <option value="CAMPAIGN_QUALIFICATION">
                        Video 1 - identificacao e qualificacao
                      </option>
                      <option value="PDE_HERO_CONVERSION">
                        Video 2 - hero e conversao
                      </option>
                    </select>
                  </label>
                </div>
                <label>
                  Hipotese comercial
                  <textarea
                    rows={3}
                    value={briefing.commercialHypothesis}
                    onChange={updateBriefing("commercialHypothesis")}
                  />
                </label>
                <label>
                  Framework de persuasao
                  <input
                    value={briefing.persuasionFramework}
                    onChange={updateBriefing("persuasionFramework")}
                  />
                </label>
                <label>
                  Evidencias cientificas e limites da promessa
                  <textarea
                    rows={3}
                    value={briefing.scientificBasis}
                    onChange={updateBriefing("scientificBasis")}
                  />
                </label>
                <label>
                  Plano de medicao
                  <textarea
                    rows={3}
                    value={briefing.measurementPlan}
                    onChange={updateBriefing("measurementPlan")}
                  />
                </label>
                <label>
                  Resultados reais observados
                  <textarea
                    rows={3}
                    value={briefing.resultsSnapshot}
                    onChange={updateBriefing("resultsSnapshot")}
                  />
                </label>
                <label>
                  Decisao
                  <select
                    value={briefing.learningDecision}
                    onChange={updateBriefing("learningDecision")}
                  >
                    <option value="COLLECTING">Coletando amostra</option>
                    <option value="CONTINUE">Continuar</option>
                    <option value="ADJUST">Ajustar</option>
                    <option value="STOP">Parar versao</option>
                  </select>
                </label>
                <label>
                  Aprendizado confirmado
                  <textarea
                    rows={3}
                    value={briefing.confirmedLearning}
                    onChange={updateBriefing("confirmedLearning")}
                  />
                </label>
                <label>
                  Proxima versao recomendada
                  <textarea
                    rows={3}
                    value={briefing.nextVersionRecommendation}
                    onChange={updateBriefing("nextVersionRecommendation")}
                  />
                </label>
              </div>
              {selectedProject?.researchIntelligence ? (
                <div className="audio-video-studio-page__panel audio-video-studio-page__research-intelligence">
                  <div className="audio-video-studio-page__research-heading">
                    <div>
                      <p className="audio-video-studio-page__eyebrow">
                        Seleção contextual deste projeto
                      </p>
                      <h2>Biblioteca de Inteligência do Harness v1</h2>
                    </div>
                    <span>
                      {selectedProject.researchIntelligence.totalAvailableCards}{" "}
                      artigos compilados
                    </span>
                  </div>
                  <p>
                    Cada agente recebe no máximo quatro cartões curtos. Os
                    artigos orientam decisões, mas não contam como venda, prova
                    do produto ou autorização de gasto e publicação.
                  </p>
                  <Link
                    className="audio-video-studio-page__research-catalog-link"
                    to="/audio-video-studio/research-library"
                  >
                    Ver o catálogo global usado por todos os projetos
                  </Link>
                  <small className="audio-video-studio-page__research-fingerprint">
                    Seleção{" "}
                    {selectedProject.researchIntelligence.contractVersion}
                    {" · "}
                    {selectedProject.researchIntelligence.contextFingerprint.slice(
                      0,
                      12,
                    )}
                  </small>
                  <div className="audio-video-studio-page__research-routes">
                    {selectedProject.researchIntelligence.routes.map(
                      (route) => (
                        <details
                          key={route.agentKey}
                          className="audio-video-studio-page__research-route"
                        >
                          <summary>
                            <span>
                              <strong>{route.agentName}</strong>
                              <small>{route.purpose}</small>
                            </span>
                            <span>
                              {researchCardCountLabel(route.cards.length)} ·{" "}
                              {researchAuthorityLabel(route.authority)}
                            </span>
                          </summary>
                          <p>{route.selectionReason}</p>
                          <div className="audio-video-studio-page__research-cards">
                            {route.cards.map((card) => (
                              <article key={card.cardId}>
                                <span>
                                  {card.collection} · {card.cardId}
                                </span>
                                <strong>{card.title}</strong>
                                <p>{card.finding}</p>
                                <small>
                                  Fonte: {card.sourcePath} · SHA{" "}
                                  {card.sourceSha256.slice(0, 12)}
                                  {card.validUntil
                                    ? ` · válida até ${card.validUntil}`
                                    : ""}
                                </small>
                              </article>
                            ))}
                          </div>
                        </details>
                      ),
                    )}
                  </div>
                  <ul>
                    {selectedProject.researchIntelligence.limitations.map(
                      (limitation) => (
                        <li key={limitation}>{limitation}</li>
                      ),
                    )}
                  </ul>
                </div>
              ) : !editableProjectId ? (
                <div className="audio-video-studio-page__panel audio-video-studio-page__research-intelligence">
                  <p className="audio-video-studio-page__eyebrow">
                    Biblioteca global do Marketing Hub
                  </p>
                  <h2>Pesquisa será selecionada para qualquer projeto</h2>
                  <p>
                    O backend compilará os artigos e entregará a cada agente
                    somente a rota aderente ao contexto. Salve o blueprint para
                    ver cartões, fontes e hashes da seleção deste projeto.
                  </p>
                  <Link
                    className="audio-video-studio-page__research-catalog-link"
                    to="/audio-video-studio/research-library"
                  >
                    Explorar a biblioteca completa
                  </Link>
                </div>
              ) : null}
            </section>
            <div
              className="audio-video-studio-page__stage-heading"
              id="audio-video-stage-audio"
            >
              <Mic2 size={18} aria-hidden="true" />
              <div>
                <h2>5. Audio e ritmo</h2>
                <span>
                  Voz, trilha e legenda precisam sustentar a promessa mesmo em
                  mobile e com som desligado.
                </span>
              </div>
            </div>
            <label>
              Voz
              <textarea
                value={briefing.voiceoverPlan}
                onChange={updateBriefing("voiceoverPlan")}
                rows={3}
              />
            </label>
            <label>
              Trilha
              <textarea
                value={briefing.soundtrackPlan}
                onChange={updateBriefing("soundtrackPlan")}
                rows={3}
              />
            </label>
            <label>
              Legendas
              <textarea
                value={briefing.captionPlan}
                onChange={updateBriefing("captionPlan")}
                rows={3}
              />
            </label>
            <div className="audio-video-studio-page__asset-section">
              <div className="audio-video-studio-page__section-heading">
                <h2>Estilo de legenda</h2>
                <p>
                  Escolha um preset visual antes da pos-producao para garantir
                  leitura em celular.
                </p>
              </div>
              <div className="audio-video-studio-page__caption-grid">
                {studioCatalogQuery.isLoading ? (
                  <article className="audio-video-studio-page__project-card">
                    Carregando presets de legenda do backend...
                  </article>
                ) : studioCatalogQuery.isError ? (
                  <article className="audio-video-studio-page__project-card">
                    Nao foi possivel carregar presets de legenda.
                  </article>
                ) : (
                  captionPresets.map((preset) => (
                    <button
                      className="audio-video-studio-page__caption-card"
                      key={preset.key}
                      type="button"
                      onClick={() => applyCaptionPreset(preset)}
                    >
                      <FileText size={18} aria-hidden="true" />
                      <strong>{preset.label}</strong>
                      <span>{preset.style}</span>
                      <div className="audio-video-studio-page__caption-preview">
                        <PlayCircle size={16} aria-hidden="true" />
                        <b>7 dias para ajustar sua presenca</b>
                      </div>
                      <small>{preset.description}</small>
                    </button>
                  ))
                )}
              </div>
            </div>
            <div
              className="audio-video-studio-page__asset-section"
              id="audio-video-stage-provider"
            >
              <div className="audio-video-studio-page__stage-heading">
                <h2>6. Provider de video</h2>
                <p>
                  Escolha o motor de geracao de acordo com o tipo de cena antes
                  de pedir uma nova renderizacao.
                </p>
              </div>
              <article className="audio-video-studio-page__project-card">
                <strong>Apolo · produção completa no Estúdio</strong>
                <p>
                  Apolo pode planejar roteiro e storyboard, usar imagens mestre,
                  gerar e corrigir cenas, escolher o provider adequado, montar,
                  narrar, sonorizar, legendar, criar HLS e inspecionar o
                  candidato. Plutus preserva o teto financeiro e o QA
                  independente decide a aprovação final.
                </p>
                <p>
                  A curadoria autônoma do Videomaker compara a resolução
                  realmente entregue; anúncio de exportação 4K não vale como
                  prova de geração nativa 4K.
                </p>
                {selectedProject?.strategyGroupKey ===
                "musa-two-video-funnel-v1" ? (
                  <p>
                    <strong>Primeira missão:</strong> finalizar os dois vídeos
                    da nova versão do MUSA, preservando o papel de campanha e o
                    papel de conversão no PDE.
                  </p>
                ) : null}
                {selectedProject ? (
                  <div>
                    <label>
                      Teto do ciclo em USD *
                      <input
                        aria-label="Teto do ciclo em USD"
                        min="0.01"
                        step="0.01"
                        type="number"
                        value={cycleBudgetUsd}
                        onChange={(event) =>
                          setCycleBudgetUsd(event.target.value)
                        }
                      />
                    </label>
                    <label>
                      Perfil de produção *
                      <select
                        aria-label="Perfil de produção do ciclo"
                        value={cycleProductionProfile}
                        onChange={(event) =>
                          setCycleProductionProfile(
                            event.target.value as
                              "DRAFT_INSTAGRAM" | "FINAL_CAMPAIGN",
                          )
                        }
                      >
                        <option value="DRAFT_INSTAGRAM">
                          Rascunho Instagram · otimizar custo
                        </option>
                        <option value="FINAL_CAMPAIGN">
                          Final de campanha · otimizar qualidade
                        </option>
                      </select>
                    </label>
                    <label>
                      Objetivo de aprendizado *
                      <textarea
                        aria-label="Objetivo de aprendizado"
                        value={cycleLearningObjective}
                        onChange={(event) =>
                          setCycleLearningObjective(event.target.value)
                        }
                      />
                    </label>
                    <label>
                      Critério de sucesso *
                      <textarea
                        aria-label="Critério de sucesso"
                        value={cycleSuccessCriterion}
                        onChange={(event) =>
                          setCycleSuccessCriterion(event.target.value)
                        }
                      />
                    </label>
                    <button
                      type="button"
                      disabled={
                        createProductionCycle.isPending ||
                        !Number.isFinite(Number(cycleBudgetUsd)) ||
                        Number(cycleBudgetUsd) <= 0 ||
                        !cycleLearningObjective.trim() ||
                        !cycleSuccessCriterion.trim()
                      }
                      onClick={() =>
                        createProductionCycle.mutate({
                          budgetLimitUsd: Number(cycleBudgetUsd),
                          productionProfile: cycleProductionProfile,
                          learningObjective: cycleLearningObjective.trim(),
                          successCriterion: cycleSuccessCriterion.trim(),
                          requestedBy: "Usuário do Marketing Hub",
                        })
                      }
                    >
                      {createProductionCycle.isPending ? (
                        <span className="spinner-border spinner-border-sm" />
                      ) : null}
                      Solicitar produção a Apolo sob controle de Plutus
                    </button>
                    <small>
                      O teto não é meta de gasto. O preflight consulta e simula
                      sem cobrança; Plutus avalia antes de qualquer geração
                      paga. O ledger registra apenas custos novos deste ciclo.
                      Apolo trabalha em TEST e não publica.
                    </small>
                    {createProductionCycle.isError ? (
                      <p role="alert">Não foi possível abrir o ciclo.</p>
                    ) : null}
                    {productionCycles.data?.[0] ? (
                      <div>
                        <p>
                          Ciclo #{productionCycles.data[0].id}:{" "}
                          <strong>{productionCycles.data[0].status}</strong>
                          {productionCycles.data[0].financialReason
                            ? ` · ${productionCycles.data[0].financialReason}`
                            : ""}
                        </p>
                        <p>
                          <strong>Plano de cortes:</strong>{" "}
                          {productionCycles.data[0].generationClipCount} clipes
                          solicitados ao provider, com até{" "}
                          {productionCycles.data[0].providerClipDurationSeconds}
                          s cada, transformados em{" "}
                          {productionCycles.data[0].editCutCount} cortes na
                          edição. Texto, legenda e CTA são aplicados na
                          pós-produção.
                        </p>
                        {productionCycles.data[0].providerPreflight ? (
                          <section
                            className="audio-video-studio-page__provider-preflight"
                            aria-label="Preflight financeiro do provider"
                          >
                            <h3>Preflight do agregador</h3>
                            <p>
                              <strong>
                                {
                                  productionCycles.data[0].providerPreflight
                                    .aggregatorName
                                }{" "}
                                ·{" "}
                                {
                                  productionCycles.data[0].providerPreflight
                                    .accountKey
                                }
                              </strong>
                              {" · "}
                              {
                                productionCycles.data[0].providerPreflight
                                  .status
                              }
                            </p>
                            <dl>
                              <div>
                                <dt>Custo previsto</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .estimatedCredits ?? "—"}{" "}
                                  créditos · US${" "}
                                  {productionCycles.data[0].providerPreflight.estimatedCostUsd?.toFixed(
                                    2,
                                  ) ?? "—"}
                                </dd>
                              </div>
                              <div>
                                <dt>Reserva máxima protegida</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .maximumAuthorizedCredits ?? "—"}{" "}
                                  créditos · US${" "}
                                  {productionCycles.data[0].providerPreflight.maximumAuthorizedCostUsd?.toFixed(
                                    2,
                                  ) ?? "—"}
                                </dd>
                              </div>
                              <div>
                                <dt>Saldo oficial</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .officialBalanceCredits ?? "—"}{" "}
                                  créditos
                                </dd>
                              </div>
                              <div>
                                <dt>Reservado por outros ciclos no snapshot</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .reservedCreditsSnapshot ?? "—"}{" "}
                                  créditos
                                </dd>
                              </div>
                              <div>
                                <dt>Disponível para este ciclo</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .availableCreditsSnapshot ?? "—"}{" "}
                                  créditos
                                </dd>
                              </div>
                              <div>
                                <dt>Limite mensal de compra da conta</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .maxMonthlyCreditSpend ?? "—"}{" "}
                                  créditos
                                </dd>
                              </div>
                              <div>
                                <dt>Configuração do router</dt>
                                <dd>
                                  {productionCycles.data[0].providerPreflight
                                    .routerConfigId ?? "Aguardando dry run"}
                                </dd>
                              </div>
                              {productionCycles.data[0].providerPreflight
                                .reservation ? (
                                <div>
                                  <dt>Reserva preventiva do ciclo</dt>
                                  <dd>
                                    {
                                      productionCycles.data[0].providerPreflight
                                        .reservation.status
                                    }
                                    {" · "}
                                    {
                                      productionCycles.data[0].providerPreflight
                                        .reservation.reservedCredits
                                    }{" "}
                                    créditos
                                    {productionCycles.data[0].providerPreflight
                                      .reservation.actualCredits != null
                                      ? ` · ${productionCycles.data[0].providerPreflight.reservation.actualCredits} consumidos`
                                      : ""}
                                    {" · "}
                                    {productionCycles.data[0].providerPreflight
                                      .reservation.settledAt
                                      ? `liquidada em ${providerInstant(productionCycles.data[0].providerPreflight.reservation.settledAt)}`
                                      : productionCycles.data[0]
                                            .providerPreflight.reservation
                                            .releasedAt
                                        ? `liberada em ${providerInstant(productionCycles.data[0].providerPreflight.reservation.releasedAt)}`
                                        : `válida até ${providerInstant(productionCycles.data[0].providerPreflight.reservation.expiresAt)}`}
                                  </dd>
                                </div>
                              ) : null}
                            </dl>
                            {productionCycles.data[0].providerPreflight
                              .failureDetail ? (
                              <p role="alert">
                                <strong>
                                  {productionCycles.data[0].providerPreflight
                                    .failureCode ?? "Bloqueio"}
                                  :
                                </strong>{" "}
                                {
                                  productionCycles.data[0].providerPreflight
                                    .failureDetail
                                }
                              </p>
                            ) : null}
                            {productionCycles.data[0].providerPreflight
                              .quotaSnapshotJson ? (
                              <details>
                                <summary>Ver saldo de quota por modelo</summary>
                                <pre>
                                  {
                                    productionCycles.data[0].providerPreflight
                                      .quotaSnapshotJson
                                  }
                                </pre>
                              </details>
                            ) : null}
                            {productionCycles.data[0].providerPreflight
                              .sourceUrl ? (
                              <a
                                href={
                                  productionCycles.data[0].providerPreflight
                                    .sourceUrl
                                }
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                Conferir fonte oficial da conta
                              </a>
                            ) : null}
                          </section>
                        ) : null}
                        {productionCycles.data[0].recommendedAggregator ? (
                          <section
                            className="audio-video-studio-page__provider-preflight"
                            aria-label="Parecer de custo-benefício de Plutus"
                          >
                            <h3>Parecer de Plutus</h3>
                            <p>
                              <strong>
                                {productionCycles.data[0].recommendedAggregator}
                                {" · "}
                                {productionCycles.data[0].recommendedRoute}
                              </strong>
                            </p>
                            <p>{productionCycles.data[0].costBenefitBasis}</p>
                            <p>
                              Ação de crédito:{" "}
                              <strong>
                                {productionCycles.data[0].creditAction}
                              </strong>
                              {productionCycles.data[0]
                                .recommendedRechargeCredits != null
                                ? ` · recarga mínima sugerida: ${productionCycles.data[0].recommendedRechargeCredits} créditos`
                                : ""}
                            </p>
                            {productionCycles.data[0].rechargeUrl ? (
                              <a
                                href={productionCycles.data[0].rechargeUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                Abrir conta indicada por Plutus
                              </a>
                            ) : null}
                          </section>
                        ) : null}
                        <p
                          role={
                            productionCycles.data[0].budgetMonitorStatus ===
                            "BLOCKED"
                              ? "alert"
                              : "status"
                          }
                        >
                          <strong>
                            Monitor financeiro:{" "}
                            {productionCycles.data[0].budgetMonitorStatus}
                          </strong>
                          {" · "}
                          {productionCycles.data[0].monitoredTaskCount} tasks ·{" "}
                          {productionCycles.data[0].monitoredCredits} créditos ·{" "}
                          US$ {productionCycles.data[0].knownCostUsd.toFixed(2)}{" "}
                          / US${" "}
                          {productionCycles.data[0].budgetLimitUsd.toFixed(2)}
                          {productionCycles.data[0].budgetAlertDetail
                            ? ` · ${productionCycles.data[0].budgetAlertDetail}`
                            : " · Aguardando a primeira task do provider."}
                        </p>
                        {productionCycles.data[0].lastFailedJobId ? (
                          <p role="alert">
                            Apolo falhou no job #
                            {productionCycles.data[0].lastFailedJobId}
                            {productionCycles.data[0].lastApolloFailureCode
                              ? ` (${productionCycles.data[0].lastApolloFailureCode})`
                              : ""}
                            :{" "}
                            {productionCycles.data[0].lastApolloFailureDetail ||
                              "O job terminou sem detalhar a causa."}{" "}
                            {productionCycles.data[0].salesVideoJobId !==
                            productionCycles.data[0].lastFailedJobId
                              ? `Uma nova tentativa foi reconciliada no job #${productionCycles.data[0].salesVideoJobId}.`
                              : "O ciclo requer uma nova tentativa pelo fluxo oficial."}
                          </p>
                        ) : null}
                      </div>
                    ) : null}
                  </div>
                ) : null}
              </article>
              <div className="audio-video-studio-page__provider-grid">
                {SALES_VIDEO_PROVIDER_OPTIONS.map((option) => {
                  const isSelected =
                    selectedProvider.providerName === option.providerName;
                  return (
                    <button
                      className={`audio-video-studio-page__provider-card${
                        isSelected
                          ? " audio-video-studio-page__provider-card--selected"
                          : ""
                      }`}
                      key={option.providerName}
                      type="button"
                      onClick={() => applyProviderOption(option)}
                    >
                      <Clapperboard size={18} aria-hidden="true" />
                      <strong>{option.label}</strong>
                      <small>{option.recommendedUse}</small>
                      <span>
                        {option.supportsSceneAssembly
                          ? "Cenas cinematograficas"
                          : "Formato direto/avatar"}
                      </span>
                      <em>
                        Clipe {option.clipDurationSeconds}s
                        {option.maxDirectDurationSeconds
                          ? ` · direto ate ${option.maxDirectDurationSeconds}s`
                          : ""}
                      </em>
                    </button>
                  );
                })}
              </div>
              {selectedProvider.providerName === "RUNWAY_ACT_TWO" ? (
                <article className="audio-video-studio-page__project-card">
                  <strong>Gate de performance autorizada</strong>
                  <p>
                    Estes dados ficam no projeto e bloqueiam a chamada paga se
                    estiverem ausentes. Use somente personagem, voz, movimento e
                    gravacao próprios ou licenciados.
                  </p>
                  <div className="audio-video-studio-page__briefing-grid">
                    <label>
                      Tipo da personagem *
                      <select
                        value={briefing.characterPerformanceType}
                        onChange={updateBriefing("characterPerformanceType")}
                        required
                      >
                        <option value="image">Imagem autorizada</option>
                        <option value="video">Video autorizado</option>
                      </select>
                    </label>
                    <label>
                      URL HTTPS da personagem *
                      <input
                        type="url"
                        value={briefing.characterPerformanceUri}
                        onChange={updateBriefing("characterPerformanceUri")}
                        placeholder="https://.../personagem-aprovada.png"
                        required
                      />
                    </label>
                    <label>
                      URL HTTPS da performance *
                      <input
                        type="url"
                        value={briefing.referencePerformanceUri}
                        onChange={updateBriefing("referencePerformanceUri")}
                        placeholder="https://.../performance-autorizada.mp4"
                        required
                      />
                    </label>
                    <label>
                      Duracao medida da performance *
                      <input
                        type="number"
                        min="3"
                        max="30"
                        value={briefing.referencePerformanceDurationSeconds}
                        onChange={updateBriefing(
                          "referencePerformanceDurationSeconds",
                        )}
                        required
                      />
                    </label>
                    <label>
                      Evidencia de consentimento *
                      <input
                        value={briefing.performanceConsentEvidence}
                        onChange={updateBriefing("performanceConsentEvidence")}
                        placeholder="Documento, asset ou aprovacao auditavel"
                        required
                      />
                    </label>
                    <label>
                      Evidencia dos direitos da performance *
                      <input
                        value={briefing.performanceRightsEvidence}
                        onChange={updateBriefing("performanceRightsEvidence")}
                        placeholder="Contrato, licenca ou asset auditavel"
                        required
                      />
                    </label>
                  </div>
                  <small>
                    Act-Two permanece em homologacao. Salvar o projeto nao
                    autoriza gasto, publicacao ou uso de pessoa reconhecivel.
                  </small>
                </article>
              ) : null}
              {isEditingProject && selectedProject ? (
                <div className="audio-video-studio-page__scene-production">
                  <div className="audio-video-studio-page__section-heading">
                    <h3>Geracao plano a plano</h3>
                    <p>
                      Gere e revise cada funcao narrativa separadamente. Uma
                      cena reprovada pode ser refeita sem descartar as demais.
                    </p>
                  </div>
                  {selectedProvider.providerName !== "RUNWAY_ACT_TWO" ? (
                    <label>
                      Imagem-base aprovada para animacao
                      <input
                        type="number"
                        min="1"
                        value={sourceImageAssetId}
                        onChange={(event) =>
                          setSourceImageAssetId(event.target.value)
                        }
                      />
                      <small>
                        {sourceImageAssetQuery.data?.publicUrl
                          ? `Asset #${sourceImageAssetQuery.data.id} sera enviado como image_to_video.`
                          : "Informe o ID de um asset de imagem aprovado no Marketing Hub."}
                      </small>
                    </label>
                  ) : null}
                  <div className="audio-video-studio-page__scene-production-grid">
                    {selectedScenePrompts.map((prompt, index) => {
                      const jobs = studioSceneJobs.filter(
                        ({ scene }) => scene?.order === index + 1,
                      );
                      return (
                        <article key={prompt}>
                          <span>
                            Cena {index + 1} ·{" "}
                            {resolveStudioSceneRole(
                              index,
                              selectedScenePrompts.length,
                            )}
                          </span>
                          <p>{prompt}</p>
                          <button
                            className="audio-video-studio-page__secondary-action"
                            type="button"
                            disabled={requestSceneRender.isPending}
                            onClick={() => handleRequestSceneRender(index)}
                          >
                            <Wand2 size={16} aria-hidden="true" />
                            {jobs.length
                              ? "Gerar nova variacao"
                              : index === 0
                                ? "Gerar clipe"
                                : "Gerar com quadro-ponte"}
                          </button>
                          {jobs.map(({ job }) => (
                            <label key={job.id}>
                              <input
                                type="checkbox"
                                disabled={
                                  job.status !== "VIDEO_READY" || !job.assetId
                                }
                                checked={selectedSceneJobIds.includes(job.id)}
                                onChange={() =>
                                  handleToggleSceneApproval(job.id, index + 1)
                                }
                              />
                              Job #{job.id} ·{" "}
                              {getStudioCommercialLabel(job.status)}
                            </label>
                          ))}
                        </article>
                      );
                    })}
                  </div>
                  <section
                    className="audio-video-studio-page__storyboard"
                    aria-label="Storyboard de consumo e aproveitamento"
                  >
                    <div className="audio-video-studio-page__section-heading">
                      <h3>Storyboard: custo e aproveitamento</h3>
                      <p>
                        Verdade consolidada do plano, provider, arquivo e
                        montagem. Aproveitamento mede uso editorial real, não
                        qualidade presumida.
                      </p>
                    </div>
                    {storyboardQuery.isLoading ? (
                      <p>Carregando storyboard…</p>
                    ) : null}
                    {storyboardQuery.isError ? (
                      <p role="alert">
                        Não foi possível carregar o storyboard.
                      </p>
                    ) : null}
                    {storyboardQuery.data &&
                    Array.isArray(storyboardQuery.data.scenes) ? (
                      <>
                        <div className="audio-video-studio-page__storyboard-summary">
                          <strong>
                            {storyboardQuery.data.plannedSceneCount} cenas
                            planejadas
                          </strong>
                          <span>
                            {storyboardQuery.data.expectedCredits} créditos
                            previstos
                          </span>
                          <span>
                            {storyboardQuery.data.consumedCredits} créditos
                            consumidos
                          </span>
                          <span>
                            {storyboardQuery.data.utilizationPercent ?? "—"}%
                            aproveitado
                          </span>
                          <span>
                            IA:{" "}
                            {storyboardQuery.data.plannerStatus ??
                              "Aguardando plano"}
                            {storyboardQuery.data.plannerModel
                              ? ` · ${storyboardQuery.data.plannerModel}`
                              : ""}
                          </span>
                          <span>
                            Orçamento:{" "}
                            {storyboardQuery.data.budgetGate ?? "Não aprovado"}
                            {storyboardQuery.data.expectedCostUsd != null
                              ? ` · US$ ${storyboardQuery.data.expectedCostUsd.toFixed(2)}`
                              : ""}
                          </span>
                        </div>
                        <div className="audio-video-studio-page__storyboard-grid">
                          {storyboardQuery.data.scenes.map((scene, index) => (
                            <article
                              key={`${scene.sceneNumber}-${scene.jobId ?? "plan"}-${index}`}
                            >
                              <header>
                                <strong>Cena {scene.sceneNumber}</strong>
                                <span>{scene.commercialRole}</span>
                              </header>
                              <p>
                                {scene.plan ||
                                  "Plano não preservado na versão histórica."}
                              </p>
                              <dl>
                                <div>
                                  <dt>Duração</dt>
                                  <dd>
                                    {scene.requestedDurationSeconds
                                      ? `${scene.requestedDurationSeconds}s`
                                      : "Não solicitada"}
                                  </dd>
                                </div>
                                <div>
                                  <dt>Créditos previstos</dt>
                                  <dd>
                                    {scene.expectedCredits ?? "A calcular"}
                                  </dd>
                                </div>
                                <div>
                                  <dt>Créditos consumidos</dt>
                                  <dd>{scene.consumedCredits ?? "Pendente"}</dd>
                                </div>
                                <div>
                                  <dt>Aproveitamento</dt>
                                  <dd>
                                    {scene.utilizationPercent == null
                                      ? "Sem arquivo"
                                      : `${scene.utilizationPercent}%`}
                                  </dd>
                                </div>
                              </dl>
                              <small>
                                {scene.jobId
                                  ? `Job #${scene.jobId} · ${scene.jobStatus}`
                                  : "Ainda não enviada ao provider"}
                              </small>
                              {scene.producedFileUrl ? (
                                <a
                                  href={scene.producedFileUrl}
                                  target="_blank"
                                  rel="noreferrer"
                                >
                                  Abrir arquivo produzido
                                </a>
                              ) : (
                                <span className="audio-video-studio-page__storyboard-no-file">
                                  Nenhum arquivo produzido
                                </span>
                              )}
                              {scene.consumptionId ? (
                                <form
                                  aria-label={`Avaliação comercial da cena ${scene.sceneNumber}`}
                                  onSubmit={(event) => {
                                    event.preventDefault();
                                    const form = new FormData(
                                      event.currentTarget,
                                    );
                                    evaluateStoryboardScene.mutate({
                                      consumptionId: scene.consumptionId!,
                                      status: String(form.get("status")),
                                      utilizationPercent: Number(
                                        form.get("utilizationPercent"),
                                      ),
                                      notes: String(form.get("notes") ?? ""),
                                      evaluatedBy: "Estúdio",
                                    });
                                  }}
                                >
                                  <label>
                                    Parecer comercial
                                    <select
                                      name="status"
                                      defaultValue={
                                        scene.commercialEvaluationStatus ??
                                        "PARTIAL"
                                      }
                                    >
                                      <option value="APPROVED">Aprovada</option>
                                      <option value="PARTIAL">
                                        Aproveitamento parcial
                                      </option>
                                      <option value="REJECTED">
                                        Reprovada
                                      </option>
                                    </select>
                                  </label>
                                  <label>
                                    Aproveitamento (%)
                                    <input
                                      name="utilizationPercent"
                                      type="number"
                                      min="0"
                                      max="100"
                                      defaultValue={
                                        scene.utilizationPercent ?? 0
                                      }
                                    />
                                  </label>
                                  <label>
                                    Evidência editorial
                                    <textarea
                                      name="notes"
                                      defaultValue={
                                        scene.commercialEvaluationNotes ?? ""
                                      }
                                    />
                                  </label>
                                  <button
                                    type="submit"
                                    disabled={evaluateStoryboardScene.isPending}
                                  >
                                    Salvar avaliação
                                  </button>
                                </form>
                              ) : null}
                            </article>
                          ))}
                        </div>
                      </>
                    ) : null}
                  </section>
                </div>
              ) : null}
            </div>
            <div
              className="audio-video-studio-page__stage-heading"
              id="audio-video-stage-montagem"
            >
              <Scissors size={18} aria-hidden="true" />
              <div>
                <h2>7. Montagem</h2>
                <span>
                  Cortes, ritmo, legenda, audio e arquivos publicaveis ficam
                  consolidados antes da aprovacao comercial.
                </span>
              </div>
            </div>
            <label>
              Montagem e cortes
              <textarea
                value={briefing.editingNotes}
                onChange={updateBriefing("editingNotes")}
                rows={3}
              />
            </label>
            {isEditingProject ? (
              <button
                className="audio-video-studio-page__primary-action"
                type="button"
                disabled={
                  requestMontage.isPending ||
                  selectedSceneJobIds.length !== selectedScenePrompts.length
                }
                onClick={handleRequestSceneMontage}
              >
                <Scissors size={18} aria-hidden="true" />
                Montar planos aprovados
              </button>
            ) : null}
            <div
              className="audio-video-studio-page__stage-heading"
              id="audio-video-stage-revisao"
            >
              <ClipboardCheck size={18} aria-hidden="true" />
              <div>
                <h2>8. Revisao</h2>
                <span>
                  A entrega so avanca quando o video estiver claro, coerente,
                  publicavel e aderente a promessa permitida do PDE.
                </span>
              </div>
            </div>
            <label>
              Gate de aprovacao
              <textarea
                value={briefing.qualityGate}
                onChange={updateBriefing("qualityGate")}
                rows={4}
              />
            </label>
            <section
              className="audio-video-studio-page__stage-section"
              id="audio-video-stage-aprendizado"
            >
              <div className="audio-video-studio-page__stage-heading">
                <h2>9. Aprendizado e metricas</h2>
                <p>
                  O funil mede consumo, cliques, diagnostico, paywall, checkout
                  e compra para decidir novos cortes e promessas.
                </p>
              </div>
              <div className="audio-video-studio-page__columns">
                <div className="audio-video-studio-page__panel">
                  <h2>Experimentos recomendados</h2>
                  <ol>
                    <li>
                      Video educativo com promessa forte para publico frio.
                    </li>
                    <li>
                      Video demonstrativo com prova visual para leads mornos.
                    </li>
                    <li>Video de oferta com urgencia leve para remarketing.</li>
                  </ol>
                </div>
                <div className="audio-video-studio-page__panel">
                  <h2>Recursos atuais conectaveis</h2>
                  <ul>
                    <li>Gerador de imagem para referencias e cenas-chave.</li>
                    <li>Fluxos de videos de produto para assets curtos.</li>
                    <li>Revisao comercial antes de campanha ou PDE.</li>
                  </ul>
                </div>
              </div>
            </section>
          </div>
          <button
            className="audio-video-studio-page__primary-action"
            type="button"
            onClick={handleSaveProject}
            disabled={
              isSavingProject ||
              selectedProjectQuery.isLoading ||
              Boolean(durationIssue) ||
              !parseOptionalNumber(briefing.productId)
            }
          >
            <Save size={18} aria-hidden="true" />
            {isSavingProject
              ? "Salvando projeto..."
              : isEditingProject
                ? "Salvar continuidade"
                : "Criar blueprint"}
          </button>
          {saveFeedback ? (
            <p className="audio-video-studio-page__feedback">{saveFeedback}</p>
          ) : null}
        </div>
      </section>

      {isEditingProject ? (
        <section className="audio-video-studio-page__section">
          <div className="audio-video-studio-page__section-heading">
            <h2>MP4 gerado para revisao</h2>
            <p>
              Ultimo arquivo renderizado ligado a este projeto para assistir,
              revisar e decidir se entra no funil.
            </p>
          </div>
          {linkedJobsQuery.isLoading ? (
            <article className="audio-video-studio-page__project-card">
              Buscando renders do projeto...
            </article>
          ) : renderedJob && renderedAssetUrl ? (
            <article className="audio-video-studio-page__render-card">
              <div className="audio-video-studio-page__render-preview">
                <video
                  controls
                  playsInline
                  preload="metadata"
                  src={renderedAssetUrl}
                >
                  Seu navegador nao conseguiu carregar este video.
                </video>
              </div>
              <div className="audio-video-studio-page__render-details">
                <span>Pronto para revisao</span>
                <h3>{briefing.title}</h3>
                <p>
                  Job #{renderedJob.id}
                  {renderedJob.providerName
                    ? ` · ${renderedJob.providerName}`
                    : ""}
                  {renderedJob.assetId
                    ? ` · Asset #${renderedJob.assetId}`
                    : ""}
                </p>
                <a
                  className="audio-video-studio-page__secondary-action"
                  href={renderedAssetUrl}
                  rel="noreferrer"
                  target="_blank"
                >
                  Abrir MP4
                </a>
              </div>
            </article>
          ) : (
            <article className="audio-video-studio-page__project-card">
              Nenhum MP4 pronto foi encontrado para este projeto ainda.
            </article>
          )}
        </section>
      ) : null}

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <h2>Projetos recentes do estudio</h2>
          <p>
            Lista operacional para confirmar se o projeto exemplo foi gravado e
            seguir os proximos testes.
          </p>
        </div>
        <div className="audio-video-studio-page__project-list">
          {videoProjectsQuery.isLoading ? (
            <article className="audio-video-studio-page__project-card">
              Carregando projetos...
            </article>
          ) : recentProjects.length > 0 ? (
            recentProjects.map((project) => (
              <article
                className="audio-video-studio-page__project-card"
                key={project.id}
              >
                <span>#{project.id}</span>
                <h3>{project.title}</h3>
                <p>{project.storyText || project.objective}</p>
                <small>{getStudioCommercialLabel(project.status)}</small>
              </article>
            ))
          ) : (
            <article className="audio-video-studio-page__project-card">
              Nenhum projeto criado ainda. Use o exemplo MUSA para iniciar os
              testes.
            </article>
          )}
        </div>
      </section>

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <h2>Escopo do estudio</h2>
          <p>
            A primeira versao organiza o tipo de producao que sera evoluido no
            modulo antes de automatizar cadastros, jobs e revisoes.
          </p>
        </div>
        <div className="audio-video-studio-page__grid">
          {productionPillars.map((pillar) => (
            <article
              className="audio-video-studio-page__pillar"
              key={pillar.title}
            >
              <pillar.icon size={22} aria-hidden="true" />
              <h3>{pillar.title}</h3>
              <p>{pillar.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="audio-video-studio-page__columns">
        <div className="audio-video-studio-page__panel">
          <h2>O que continua onde esta</h2>
          <ul>
            {currentFlows.map((flow) => (
              <li key={flow}>{flow}</li>
            ))}
          </ul>
        </div>
        <div className="audio-video-studio-page__panel">
          <h2>Proximas etapas de construcao</h2>
          <ol>
            {buildSteps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </div>
      </section>

      <div className="audio-video-studio-page__next-action">
        <PlayCircle size={22} aria-hidden="true" />
        <strong>Proximo incremento:</strong>
        <span>
          apos criar o projeto exemplo, evoluir jobs auditaveis de roteiro, voz,
          cenas, montagem e revisao.
        </span>
      </div>
    </div>
  );
}
